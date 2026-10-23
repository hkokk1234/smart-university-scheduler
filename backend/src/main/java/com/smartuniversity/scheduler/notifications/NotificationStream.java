package com.smartuniversity.scheduler.notifications;

import com.smartuniversity.scheduler.platform.api.ApiException;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static com.smartuniversity.scheduler.notifications.ChangeModels.*;

@Component
@EnableScheduling
public class NotificationStream {
    private record Connection(UUID userId, SseEmitter emitter) {}
    private final Set<Connection> connections =new HashSet<>();
    // Slow clients cannot block the catalog transaction thread or grow an unbounded queue.
    private final ExecutorService delivery =new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), task -> {
                Thread thread =new Thread(task, "notification-stream"); thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.DiscardPolicy());

    public SseEmitter subscribe(UUID userId, Instant expiresAt) {
        long lifetime =Math.min(60000, Duration.between(Instant.now(), expiresAt).toMillis());
        if (lifetime <=0) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Your session has expired");
        SseEmitter emitter =new SseEmitter(lifetime);
        Connection connection =new Connection(userId, emitter);
        synchronized (connections) {
            if (connections.size() >=500 || connections.stream().filter(c -> c.userId().equals(userId)).count() >=5)
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "STREAM_LIMIT", "Too many live connections. Close another tab and try again.");
            connections.add(connection);
        }
        emitter.onCompletion(() -> remove(connection));
        emitter.onTimeout(() -> { remove(connection); emitter.complete(); });
        emitter.onError(error -> remove(connection));
        delivery.execute(() -> send(connection, "ready", Map.of("connected", true)));
        return emitter;
    }
    @TransactionalEventListener
    public void changed(CommittedChange change) {
        for (Connection connection : snapshot()) delivery.execute(() -> send(connection, "catalog-changed",
                Map.of("courseId", change.courseId(), "affected", change.recipients().contains(connection.userId()))));
    }
    @TransactionalEventListener
    public void inboxChanged(InboxChanged change) {
        for (Connection connection : snapshot()) if (connection.userId().equals(change.userId()))
            delivery.execute(() -> send(connection, "inbox-changed", Map.of("refresh", true)));
    }
    @Scheduled(fixedDelay =15000)
    public void heartbeat() {
        for (Connection connection : snapshot()) delivery.execute(() -> send(connection, "heartbeat", Map.of("alive", true)));
    }
    private List<Connection> snapshot() { synchronized (connections) { return List.copyOf(connections); } }
    private void remove(Connection connection) { synchronized (connections) { connections.remove(connection); } }
    private void send(Connection connection, String name, Object data) {
        try { connection.emitter().send(SseEmitter.event().name(name).data(data)); }
        catch (IOException | RuntimeException error) { remove(connection); connection.emitter().complete(); }
    }
    @PreDestroy public void close() {
        for (Connection connection : snapshot()) connection.emitter().complete();
        synchronized (connections) { connections.clear(); }
        delivery.shutdownNow();
    }
}

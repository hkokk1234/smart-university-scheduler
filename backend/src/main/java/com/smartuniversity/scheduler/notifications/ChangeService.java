package com.smartuniversity.scheduler.notifications;

import com.smartuniversity.scheduler.catalog.api.SectionDto;
import com.smartuniversity.scheduler.identity.infrastructure.UserRepository;
import com.smartuniversity.scheduler.platform.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;
import static com.smartuniversity.scheduler.notifications.ChangeModels.*;

@Service
public class ChangeService {
    private final ChangeStore store;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    public ChangeService(ChangeStore store, UserRepository users, ApplicationEventPublisher events) {
        this.store =store; this.users =users; this.events =events;
    }
    // Audit and recipient rows must use the section edit's transaction, including rollback.
    @Transactional(propagation =Propagation.MANDATORY)
    public void record(SectionDto before, SectionDto after, UUID actorId) {
        Snapshot previous =Snapshot.of(before), next =Snapshot.of(after);
        if (previous.equals(next)) return;
        String actorName =users.findById(actorId).orElseThrow(() -> ApiException.notFound("Administrator")).getDisplayName();
        Change change =new Change(UUID.randomUUID(), after.id(), after.courseId(), after.courseCode(), after.courseTitle(),
                actorName, Instant.now(), previous, next);
        store.insert(change, actorId);
        Set<UUID> recipients =store.recipients(after.id());
        store.notify(recipients, change.id());
        events.publishEvent(new CommittedChange(after.courseId(), recipients));
    }
    @Transactional(readOnly =true) public PageResponse<Change> history(UUID sectionId, int page, int size) { return store.history(sectionId, page, size); }
    @Transactional(readOnly =true) public Inbox inbox(UUID userId, int page, int size) { return store.inbox(userId, page, size); }
    @Transactional public void markRead(UUID id, UUID userId) { store.markRead(id, userId); events.publishEvent(new InboxChanged(userId)); }
    @Transactional public void markAllRead(UUID userId) { store.markAllRead(userId); events.publishEvent(new InboxChanged(userId)); }
}

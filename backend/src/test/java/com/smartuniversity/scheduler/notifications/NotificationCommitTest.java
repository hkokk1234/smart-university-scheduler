package com.smartuniversity.scheduler.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static com.smartuniversity.scheduler.notifications.ChangeModels.*;

@SpringJUnitConfig(NotificationCommitTest.Config.class)
class NotificationCommitTest {
    @Configuration(proxyBeanMethods =false)
    @EnableTransactionManagement
    static class Config {
        @Bean NotificationStream streams() { return spy(new NotificationStream()); }
        @Bean PlatformTransactionManager transactions() {
            return new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object transaction, TransactionDefinition definition) {}
                protected void doCommit(DefaultTransactionStatus status) {}
                protected void doRollback(DefaultTransactionStatus status) {}
            };
        }
    }
    @Autowired NotificationStream streams;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ApplicationEventPublisher events;

    @Test void deliveryIsDeferredUntilCommitAndSuppressedOnRollback() {
        CommittedChange committed =new CommittedChange(UUID.randomUUID(), Set.of(UUID.randomUUID()));
        CommittedChange rolledBack =new CommittedChange(UUID.randomUUID(), Set.of(UUID.randomUUID()));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(committed);
            verify(streams, never()).changed(committed);
        });
        verify(streams).changed(committed);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(rolledBack); status.setRollbackOnly();
        });
        verify(streams, never()).changed(rolledBack);
        CommittedChange outsideTransaction =new CommittedChange(UUID.randomUUID(), Set.of());
        events.publishEvent(outsideTransaction);
        verify(streams, never()).changed(outsideTransaction);
    }
}

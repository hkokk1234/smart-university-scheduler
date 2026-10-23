package com.smartuniversity.scheduler.notifications;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.identity.domain.User;
import com.smartuniversity.scheduler.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.smartuniversity.scheduler.notifications.ChangeModels.*;

class ChangeServiceTest {
    final ChangeStore store =mock(ChangeStore.class);
    final UserRepository users =mock(UserRepository.class);
    final ApplicationEventPublisher events =mock(ApplicationEventPublisher.class);
    final ChangeService service =new ChangeService(store, users, events);
    final UUID sectionId =UUID.randomUUID(), groupId =UUID.randomUUID(), semesterId =UUID.randomUUID(), courseId =UUID.randomUUID(), actor =UUID.randomUUID();
    SectionDto section(String code, int capacity, MeetingDto... meetings) {
        return new SectionDto(sectionId, groupId, semesterId, code, capacity, 1, courseId, "CS101", "Algorithms", List.of(meetings));
    }
    MeetingDto meeting(int hour) {
        return new MeetingDto(UUID.randomUUID(), sectionId, DayOfWeek.MONDAY, LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0), null, null);
    }
    @Test void noOpIgnoresReplacementMeetingIdsAndOrder() {
        service.record(section("A", 30, meeting(9), meeting(11)), section("A", 30, meeting(11), meeting(9)), actor);
        verifyNoInteractions(store, users, events);
    }
    @Test void recordsImmutableBeforeAfterAndNotifiesEachAffectedUser() {
        UUID student =UUID.randomUUID();
        when(users.findById(actor)).thenReturn(Optional.of(new User("admin@example.edu", "Catalog Admin")));
        when(store.recipients(sectionId)).thenReturn(Set.of(student));
        var before =section("A", 30, meeting(9)); var after =section("B", 40, meeting(10));
        service.record(before, after, actor);
        ArgumentCaptor<Change> capture =ArgumentCaptor.forClass(Change.class);
        verify(store).insert(capture.capture(), eq(actor));
        Change change =capture.getValue();
        assertThat(change.before()).isEqualTo(Snapshot.of(before));
        assertThat(change.after()).isEqualTo(Snapshot.of(after));
        assertThat(change.actorName()).isEqualTo("Catalog Admin");
        verify(store).notify(Set.of(student), change.id());
        verify(events).publishEvent(new CommittedChange(courseId, Set.of(student)));
    }
    @Test void aFailedAuditDoesNotPublishARealtimeSignal() {
        when(users.findById(actor)).thenReturn(Optional.of(new User("admin@example.edu", "Admin")));
        doThrow(new IllegalStateException("Database unavailable")).when(store).insert(any(), eq(actor));
        assertThatThrownBy(() -> service.record(section("A", 30, meeting(9)), section("A", 30, meeting(10)), actor)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(events);
        verify(store, never()).notify(anySet(), any());
    }
    @Test void readOperationsUseTheAuthenticatedOwnerAndPublishAfterWrite() {
        UUID user =UUID.randomUUID(), notification =UUID.randomUUID();
        service.markRead(notification, user);
        verify(store).markRead(notification, user);
        verify(events).publishEvent(new InboxChanged(user));
    }
}

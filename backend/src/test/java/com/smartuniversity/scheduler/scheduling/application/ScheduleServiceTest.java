package com.smartuniversity.scheduler.scheduling.application;
import com.smartuniversity.scheduler.catalog.application.CatalogReferences;
import com.smartuniversity.scheduler.identity.application.IdentityReferences;
import com.smartuniversity.scheduler.platform.api.ApiException;
import com.smartuniversity.scheduler.scheduling.infrastructure.SavedScheduleRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleServiceTest {
    @Test
    void ownerScopedDeleteDoesNotFallBackToUnscopedLookup() {
        var repository =mock(SavedScheduleRepository.class);
        UUID id =UUID.randomUUID(), owner =UUID.randomUUID();
        when(repository.findByIdAndUserId(id, owner)).thenReturn(Optional.empty());
        var service =new ScheduleService(repository, mock(IdentityReferences.class), mock(CatalogReferences.class));
        assertThatThrownBy(() -> service.delete(id, owner)).isInstanceOf(ApiException.class);
        verify(repository, never()).findById(id);
        verify(repository, never()).delete(any());
    }
}

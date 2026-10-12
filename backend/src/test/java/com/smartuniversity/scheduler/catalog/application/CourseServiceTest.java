package com.smartuniversity.scheduler.catalog.application;
import com.smartuniversity.scheduler.catalog.infrastructure.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseServiceTest {
    @Test
    void rejectsUnapprovedSortPropertiesBeforeQueryingDatabase() {
        var courses =mock(CourseRepository.class);
        var service =new CourseService(courses, mock(SectionGroupRepository.class), mock(SectionRepository.class));
        for (String sort : new String[]{"semester.passwordHash,asc", "title", "code,sideways", "code,asc,extra"}) {
            assertThatThrownBy(() -> service.search(null, null, null, 0, 20, sort)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(courses);
    }
}

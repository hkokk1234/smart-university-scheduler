package com.smartuniversity.scheduler.copilot.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.application.*;
import com.smartuniversity.scheduler.copilot.api.CopilotRequest;
import com.smartuniversity.scheduler.platform.api.*;
import com.smartuniversity.scheduler.scheduling.application.ScheduleGenerationService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CopilotServiceTest {
    final AdviceProvider provider=mock(AdviceProvider.class);
    final CourseService catalog=mock(CourseService.class);
    final SemesterReadService semesters=mock(SemesterReadService.class);
    final ScheduleGenerationService generator=mock(ScheduleGenerationService.class);
    final CopilotService service=new CopilotService(provider,catalog,semesters,generator,new ObjectMapper().findAndRegisterModules());
    final UUID semester=UUID.randomUUID(), id=UUID.randomUUID();
    final CourseDto course=new CourseDto(id,semester,"CS101","Programming",BigDecimal.valueOf(6));
    void ready() {
        when(provider.available()).thenReturn(true);
        when(semesters.get(semester)).thenReturn(new SemesterDto(semester,"2026-A","Autumn",LocalDate.of(2026,9,1),LocalDate.of(2027,1,31),"Europe/Athens"));
        when(catalog.search(semester,null,null,0,50,"code,asc")).thenReturn(new PageResponse<>(List.of(course),0,50,1,1));
    }
    CopilotRequest request(List<UUID> courses) { return new CopilotRequest(semester,"Help choose courses",courses,List.of(),null); }
    @Test void missingConfigurationDoesNotReadCatalogOrCallPaidProvider() {
        assertThatThrownBy(()->service.chat(request(List.of())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getCode()).isEqualTo("AI_NOT_CONFIGURED"));
        verifyNoInteractions(catalog,semesters,generator);
    }
    @Test void onlyLiveCatalogRecommendationsAreReturned() {
        ready(); when(provider.advise(anyString(),any(),eq(Set.of(id)))).thenReturn(new AdviceProvider.Advice("Consider Programming",List.of(id)));
        var result=service.chat(request(List.of()));
        assertThat(result.recommendedCourses()).containsExactly(course);
        assertThat(result.scheduleCheck()).isNull(); verifyNoInteractions(generator);
    }
    @Test void rejectsUnknownIdsEvenFromAnotherProviderImplementation() {
        ready(); when(provider.advise(anyString(),any(),anySet())).thenReturn(new AdviceProvider.Advice("Invented",List.of(UUID.randomUUID())));
        assertThatThrownBy(()->service.chat(request(List.of()))).isInstanceOf(ApiException.class);
    }
    @Test void unpublishedSectionsStillAllowCourseAdviceWithoutInventingAValidTimetable() {
        ready(); when(catalog.details(id)).thenReturn(new CourseDetailDto(course,List.of()));
        when(provider.advise(contains("unpublished"),any(),anySet())).thenReturn(new AdviceProvider.Advice("Sections are not published yet.",List.of(id)));
        assertThat(service.chat(request(List.of(id))).scheduleCheck()).isNull(); verifyNoInteractions(generator);
    }
    @Test void rejectsCrossSemesterSelections() {
        ready(); when(catalog.details(id)).thenReturn(new CourseDetailDto(new CourseDto(id,UUID.randomUUID(),"CS101","Other semester",BigDecimal.valueOf(6)),List.of()));
        assertThatThrownBy(()->service.chat(request(List.of(id)))).isInstanceOf(IllegalArgumentException.class);
        verify(provider,never()).advise(anyString(),any(),anySet());
    }
    @Test void boundsTheGlobalPaidRequestBudget() {
        ready(); when(provider.advise(anyString(),any(),anySet())).thenReturn(new AdviceProvider.Advice("Advice",List.of()));
        for (int n=0;n<20;n++) service.chat(request(List.of()));
        assertThatThrownBy(()->service.chat(request(List.of())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getCode()).isEqualTo("AI_BUSY"));
        verify(provider,times(20)).advise(anyString(),any(),anySet());
    }
}

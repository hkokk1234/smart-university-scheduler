package com.smartuniversity.scheduler.scheduling.api;

import com.smartuniversity.scheduler.platform.security.SecurityConfiguration;
import com.smartuniversity.scheduler.scheduling.application.*;
import com.smartuniversity.scheduler.scheduling.solver.TimetableModel.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers =ScheduleController.class, properties ="debug=false")
@Import(SecurityConfiguration.class)
class ScheduleRepairControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ScheduleService schedules;
    @MockitoBean ScheduleReadService reader;
    @MockitoBean ScheduleGenerationService generation;
    @MockitoBean JwtDecoder decoder;
    final UUID schedule =UUID.randomUUID(), owner =UUID.randomUUID();
    String endpoint() { return "/api/v1/schedules/" + schedule + "/repair"; }

    @Test void anonymousCannotRepair() throws Exception {
        mvc.perform(post(endpoint()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(reader, generation, schedules);
    }
    @Test void missingAndForeignSchedulesReturn404BeforeSolving() throws Exception {
        when(reader.findSchedule(schedule, owner)).thenReturn(Optional.empty());
        mvc.perform(post(endpoint()).with(jwt().jwt(token -> token.subject(owner.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        verify(reader).findSchedule(schedule, owner);
        verifyNoInteractions(generation, schedules);
    }
    @Test void previewUsesJwtOwnershipAndNeverWritesTheOriginal() throws Exception {
        SavedScheduleDto original =new SavedScheduleDto(schedule, owner, UUID.randomUUID(), "Week", Instant.EPOCH, List.of());
        when(reader.findSchedule(schedule, owner)).thenReturn(Optional.of(original));
        when(generation.repair(eq(original), any())).thenReturn(new RepairResponse(schedule, Status.SOLVED,
                List.of(), new Metrics(1, 0, 0, 1, 1, true)));
        mvc.perform(post(endpoint()).with(jwt().jwt(token -> token.subject(owner.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.originalScheduleId").value(schedule.toString()));
        verify(generation).repair(original, new RepairScheduleRequest(null, null, null, null));
        verifyNoInteractions(schedules);
    }
    @Test void rejectsInvalidLimitsAndNullExclusions() throws Exception {
        for (String body : List.of("{\"maxResults\":6}", "{\"timeLimitMillis\":0}", "{\"excludedSectionIds\":[null]}")) {
            mvc.perform(post(endpoint()).with(jwt().jwt(token -> token.subject(owner.toString()))
                            .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(reader, generation, schedules);
    }
}

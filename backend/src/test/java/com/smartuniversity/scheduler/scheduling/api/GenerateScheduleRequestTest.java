package com.smartuniversity.scheduler.scheduling.api;

import com.smartuniversity.scheduler.scheduling.solver.TimetableModel;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class GenerateScheduleRequestTest {
    @Test
    void mapsSchedulePreferencesIntoTheSolverModel() {
        var preferenceRequest =new GenerateScheduleRequest.PreferenceRequest(
                List.of(), LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(DayOfWeek.FRIDAY),
                4, TimetableModel.TimeOfDay.AFTERNOON,
                new GenerateScheduleRequest.WeightRequest(2, 1, 1, 1, 0.2));
        var request =new GenerateScheduleRequest(UUID.randomUUID(), List.of(UUID.randomUUID()),
                5, 1000L, preferenceRequest);

        TimetableModel.Preferences preferences =request.preferenceModel();

        assertThat(preferences.preferredStart()).isEqualTo(LocalTime.of(8, 0));
        assertThat(preferences.preferredEnd()).isEqualTo(LocalTime.of(18, 0));
        assertThat(preferences.preferredFreeDays()).containsExactly(DayOfWeek.FRIDAY);
        assertThat(preferences.maximumCampusDays()).isEqualTo(4);
        assertThat(preferences.preferredPeriod()).isEqualTo(TimetableModel.TimeOfDay.AFTERNOON);
        assertThat(preferences.weights().gaps()).isEqualTo(2);
    }
}

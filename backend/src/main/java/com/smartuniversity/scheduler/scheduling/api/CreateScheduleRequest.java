package com.smartuniversity.scheduler.scheduling.api;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
public record CreateScheduleRequest(@NotNull UUID semesterId, @NotBlank @Size(max =120) String name,
                                    @NotNull @Size(max =100) List<@NotNull UUID> sectionIds) {}

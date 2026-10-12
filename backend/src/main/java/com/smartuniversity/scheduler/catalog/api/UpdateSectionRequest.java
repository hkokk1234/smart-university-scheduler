package com.smartuniversity.scheduler.catalog.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record UpdateSectionRequest(@NotBlank @Size(max =32) String code, @Positive int capacity,
                                   @NotNull @PositiveOrZero Long version,
                                   @NotEmpty @Size(max =50) List<@NotNull @Valid MeetingRequest> meetings) {}

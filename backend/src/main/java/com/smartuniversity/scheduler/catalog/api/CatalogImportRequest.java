package com.smartuniversity.scheduler.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record CatalogImportRequest(@NotNull UUID semesterId,
        @NotEmpty @Size(max =500) List<@NotNull @Valid Row> rows) {
    // Keep editable strings so validation can return errors on individual cells.
    public record Row(@Size(max =500) String courseCode, @Size(max =500) String title,
            @Size(max =500) String credits, @Size(max =500) String groupCode,
            @Size(max =500) String groupType, @Size(max =500) String sectionCode,
            @Size(max =500) String capacity, @Size(max =500) String weekday,
            @Size(max =500) String startTime, @Size(max =500) String endTime) {}
}

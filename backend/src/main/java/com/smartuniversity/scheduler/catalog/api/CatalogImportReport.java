package com.smartuniversity.scheduler.catalog.api;

import java.util.List;
import java.util.Map;

public record CatalogImportReport(boolean valid, List<RowError> errors, int courseCount, int sectionCount, int meetingCount) {
    public record RowError(int rowIndex, Map<String, String> fields) {}
    public record Imported(int coursesCreated, int groupsCreated, int sectionsCreated, int meetingsCreated) {}
}

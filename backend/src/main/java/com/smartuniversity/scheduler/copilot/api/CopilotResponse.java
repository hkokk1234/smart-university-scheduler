package com.smartuniversity.scheduler.copilot.api;

import com.smartuniversity.scheduler.catalog.api.CourseDto;
import com.smartuniversity.scheduler.scheduling.api.GenerationResponse;
import java.util.List;

public record CopilotResponse(String answer, List<CourseDto> recommendedCourses,
                              GenerationResponse scheduleCheck, boolean catalogTruncated) {}

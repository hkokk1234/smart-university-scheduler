package com.smartuniversity.scheduler.copilot.application;

import com.smartuniversity.scheduler.copilot.api.CopilotRequest;
import java.util.*;

public interface AdviceProvider {
    boolean available();
    Advice advise(String context, CopilotRequest request, Set<UUID> allowedCourses);
    record Advice(String answer, List<UUID> courseIds) {}
}

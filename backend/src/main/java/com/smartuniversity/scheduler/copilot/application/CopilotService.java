package com.smartuniversity.scheduler.copilot.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartuniversity.scheduler.catalog.api.CourseDto;
import com.smartuniversity.scheduler.catalog.application.*;
import com.smartuniversity.scheduler.copilot.api.*;
import com.smartuniversity.scheduler.platform.api.ApiException;
import com.smartuniversity.scheduler.scheduling.api.*;
import com.smartuniversity.scheduler.scheduling.application.ScheduleGenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class CopilotService {
    private final AdviceProvider provider;
    private final CourseService catalog;
    private final SemesterReadService semesters;
    private final ScheduleGenerationService generator;
    private final ObjectMapper mapper;
    private final Semaphore capacity=new Semaphore(2);
    private long windowStarted=System.nanoTime();
    private int requestsInWindow;
    private synchronized boolean withinBudget() {
        long now=System.nanoTime();
        if (now-windowStarted>=60_000_000_000L) { windowStarted=now; requestsInWindow=0; }
        if (requestsInWindow>=20) return false;
        requestsInWindow++; return true;
    }
    public CopilotService(AdviceProvider provider, CourseService catalog, SemesterReadService semesters,
                          ScheduleGenerationService generator, ObjectMapper mapper) {
        this.provider=provider; this.catalog=catalog; this.semesters=semesters; this.generator=generator; this.mapper=mapper;
    }
    public boolean available() { return provider.available(); }
    public CopilotResponse chat(CopilotRequest request) {
        if (!available()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"AI_NOT_CONFIGURED","The AI advisor is not connected yet. Ask your administrator to enable it.");
        if (!capacity.tryAcquire()) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"AI_BUSY","The AI advisor is busy. Please try again shortly.");
        try {
            if (!withinBudget()) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"AI_BUSY","The AI advisor's request limit has been reached. Please try again in a minute.");
            var semester=semesters.get(request.semesterId());
            var page=catalog.search(request.semesterId(),null,null,0,50,"code,asc");
            Map<UUID,CourseDto> courses=new LinkedHashMap<>();
            page.content().forEach(c->courses.put(c.id(),c));
            List<UUID> selected=request.courseIds()==null?List.of():request.courseIds();
            if (new HashSet<>(selected).size()!=selected.size()) throw new IllegalArgumentException("Duplicate courses");
            boolean sectionsPublished=true;
            for (UUID id:selected) {
                var details=catalog.details(id);
                var course=details.course();
                if (!course.semesterId().equals(request.semesterId())) throw new IllegalArgumentException("Cross semester course");
                courses.put(id,course);
                if (details.sectionGroups().isEmpty() || details.sectionGroups().stream().anyMatch(g->g.sections().isEmpty())) sectionsPublished=false;
            }
            var planningRequest=new GenerateScheduleRequest(request.semesterId(),selected,1,1000L,request.preferences());
            planningRequest.preferenceModel();
            GenerationResponse check=selected.isEmpty() || !sectionsPublished?null:generator.generate(planningRequest);
            Map<String,Object> facts=new LinkedHashMap<>();
            facts.put("semester",semester); facts.put("catalog",courses.values());
            facts.put("catalogTruncated",page.totalElements()>50); facts.put("selectedCourseIds",selected);
            if (!sectionsPublished) facts.put("scheduleCheckUnavailableReason","Some selected courses have unpublished or empty section groups.");
            facts.put("preferences",request.preferences());
            if (check!=null) facts.put("scheduleCheck",Map.of("status",check.status(),"complete",check.metrics().complete(),
                    "options",check.schedules().stream().map(s->Map.of("selectedSectionIds",s.selectedSectionIds(),
                        "optimizationScore",s.optimizationScore(),"scoreExplanation",s.scoreExplanation(),
                        "meetings",s.weeklyMeetings().stream().map(m->Map.of("weekday",m.weekday(),
                            "startTime",m.startTime(),"endTime",m.endTime(),"sectionId",m.sectionId())).toList())).toList()));
            String context;
            try { context=mapper.writeValueAsString(facts); } catch (Exception e) { throw new IllegalStateException("Cannot encode catalog context",e); }
            var advice=provider.advise(context,request,Set.copyOf(courses.keySet()));
            // Defense in depth for other future provider implementations.
            if (advice.answer()==null || advice.answer().isBlank() || advice.courseIds().size()>8
                    || !courses.keySet().containsAll(advice.courseIds()))
                throw new ApiException(HttpStatus.BAD_GATEWAY,"AI_UNAVAILABLE","The advisor returned unsupported recommendations.");
            return new CopilotResponse(advice.answer(),advice.courseIds().stream().distinct().map(courses::get).toList(),check,page.totalElements()>50);
        } finally { capacity.release(); }
    }
}

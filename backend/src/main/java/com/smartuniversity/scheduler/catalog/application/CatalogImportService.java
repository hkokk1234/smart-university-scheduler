package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.domain.*;
import com.smartuniversity.scheduler.catalog.infrastructure.*;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.*;
import java.util.*;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class CatalogImportService {
    private final SemesterRepository semesters;
    private final CourseRepository courses;
    private final SectionGroupRepository groups;
    private final SectionRepository sections;
    public CatalogImportService(SemesterRepository semesters, CourseRepository courses,
                                SectionGroupRepository groups, SectionRepository sections) {
        this.semesters =semesters; this.courses =courses; this.groups =groups; this.sections =sections;
    }
    private record GroupKey(String course, String group) {}
    private record SectionKey(String course, String group, String section) {}
    private record Parsed(int index, String code, String title, BigDecimal credits, String group,
                          SectionGroupType type, String section, int capacity, DayOfWeek day, LocalTime start, LocalTime end) {
        GroupKey groupKey() { return new GroupKey(code, group); }
        SectionKey sectionKey() { return new SectionKey(code, group, section); }
    }
    private record Checked(Semester semester, List<Parsed> rows, Map<String, Course> courses,
                           Map<GroupKey, SectionGroup> groups, CatalogImportReport report) {}

    @Transactional(readOnly =true)
    public CatalogImportReport validate(CatalogImportRequest request) { return check(request).report(); }

    @Transactional
    public CatalogImportReport.Imported commit(CatalogImportRequest request) {
        Checked checked =check(request);
        if (!checked.report().valid()) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMPORT",
                "Some rows are invalid or the catalog changed. Validate the preview again before importing.");
        Map<String, Course> courseMap =new HashMap<>(checked.courses());
        Map<GroupKey, SectionGroup> groupMap =new HashMap<>(checked.groups());
        Map<SectionKey, Section> sectionMap =new LinkedHashMap<>();
        int createdCourses =0, createdGroups =0;
        for (Parsed row : checked.rows()) {
            Course course =courseMap.get(row.code());
            if (course ==null) {
                course =courses.save(new Course(checked.semester(), row.code(), row.title(), row.credits()));
                courseMap.put(row.code(), course); createdCourses++;
            }
            SectionGroup group =groupMap.get(row.groupKey());
            if (group ==null) {
                group =groups.save(new SectionGroup(course, row.group(), row.group(), row.type()));
                groupMap.put(row.groupKey(), group); createdGroups++;
            }
            Section section =sectionMap.get(row.sectionKey());
            if (section ==null) {
                section =new Section(group, row.section(), row.capacity());
                sectionMap.put(row.sectionKey(), section);
            }
            section.addMeeting(row.day(), row.start(), row.end(), null, null);
        }
        // Each new section has all its meetings before persistence and commit-time constraints run.
        sections.saveAll(sectionMap.values());
        sections.flush();
        return new CatalogImportReport.Imported(createdCourses, createdGroups, sectionMap.size(), checked.rows().size());
    }

    private Checked check(CatalogImportRequest request) {
        if (request.rows() ==null || request.rows().isEmpty() || request.rows().size() > 500)
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMPORT", "Provide between 1 and 500 meeting rows.");
        Semester semester =semesters.findById(request.semesterId()).orElseThrow(() -> ApiException.notFound("Semester"));
        Map<Integer, Map<String, String>> errors =new TreeMap<>();
        List<Parsed> parsed =new ArrayList<>();
        for (int index =0; index < request.rows().size(); index++) {
            CatalogImportRequest.Row row =request.rows().get(index);
            Map<String, String> fields =new LinkedHashMap<>();
            if (row ==null) { error(errors, index, "courseCode", "A meeting row is required."); continue; }
            String code =text(row.courseCode(), 32, "courseCode", fields);
            String title =text(row.title(), 200, "title", fields);
            String group =text(row.groupCode(), 32, "groupCode", fields);
            String section =text(row.sectionCode(), 32, "sectionCode", fields);
            BigDecimal credits =null; int capacity =0; SectionGroupType type =null; DayOfWeek day =null;
            LocalTime start =null, end =null;
            try {
                credits =new BigDecimal(clean(row.credits()).replace(',', '.')).setScale(1, RoundingMode.UNNECESSARY);
                if (credits.signum() <=0 || credits.compareTo(BigDecimal.valueOf(60)) > 0) throw new IllegalArgumentException();
            } catch (RuntimeException ex) { fields.put("credits", "Use credits greater than 0 and at most 60, with at most one decimal place."); }
            try {
                capacity =Integer.parseInt(clean(row.capacity()));
                if (capacity < 1 || capacity > 100000) throw new IllegalArgumentException();
            } catch (RuntimeException ex) { fields.put("capacity", "Use a whole capacity between 1 and 100000."); }
            try { type =groupType(row.groupType()); } catch (RuntimeException ex) { fields.put("groupType", "Choose LECTURE, LABORATORY, TUTORIAL or SEMINAR."); }
            try { day =weekday(row.weekday()); } catch (RuntimeException ex) { fields.put("weekday", "Use a weekday name (English or Greek), or 1–7 for Monday–Sunday."); }
            try { start =time(row.startTime()); } catch (RuntimeException ex) { fields.put("startTime", "Use a 24-hour time, e.g. 09:00."); }
            try { end =time(row.endTime()); } catch (RuntimeException ex) { fields.put("endTime", "Use a 24-hour time, e.g. 11:00."); }
            if (start !=null && end !=null && !start.isBefore(end)) fields.put("endTime", "End must be after start on the same day.");
            if (!fields.isEmpty()) errors.put(index, fields);
            else parsed.add(new Parsed(index, code, title, credits, group, type, section, capacity, day, start, end));
        }
        Map<String, Parsed> firstCourse =new HashMap<>();
        Map<GroupKey, Parsed> firstGroup =new HashMap<>();
        Map<SectionKey, List<Parsed>> sectionRows =new LinkedHashMap<>();
        for (Parsed row : parsed) {
            Parsed first =firstCourse.putIfAbsent(row.code(), row);
            if (first !=null && (!first.title().equals(row.title()) || first.credits().compareTo(row.credits()) !=0))
                error(errors, row.index(), "title", "All rows for this course must use the same title and credits.");
            first =firstGroup.putIfAbsent(row.groupKey(), row);
            if (first !=null && first.type() !=row.type()) error(errors, row.index(), "groupType", "This group has inconsistent types.");
            List<Parsed> previous =sectionRows.computeIfAbsent(row.sectionKey(), ignored -> new ArrayList<>());
            for (Parsed other : previous) {
                if (row.capacity() !=other.capacity()) error(errors, row.index(), "capacity", "All meetings of a section must use the same capacity.");
                if (row.day() ==other.day() && row.start().isBefore(other.end()) && other.start().isBefore(row.end())) {
                    error(errors, row.index(), "startTime", "Duplicate or overlapping meeting in this section (row " + (other.index() + 1) + ").");
                    error(errors, other.index(), "startTime", "Duplicate or overlapping meeting in this section (row " + (row.index() + 1) + ").");
                }
            }
            previous.add(row);
        }
        Map<String, Course> existingCourses =new HashMap<>();
        Map<GroupKey, SectionGroup> existingGroups =new HashMap<>();
        for (Course course : courses.findBySemesterIdOrderByCodeAsc(request.semesterId())) {
            Parsed first =firstCourse.get(course.getCode());
            if (first ==null) continue;
            existingCourses.put(course.getCode(), course);
            for (Parsed row : parsed) if (row.code().equals(course.getCode()) &&
                    (!course.getTitle().equals(row.title()) || course.getCredits().compareTo(row.credits()) !=0))
                error(errors, row.index(), "title", "An existing course uses this code with different title or credits. Match its metadata or use a new code.");
            for (SectionGroup group : groups.findByCourseIdOrderByCodeAsc(course.getId())) {
                GroupKey key =new GroupKey(course.getCode(), group.getCode());
                if (!firstGroup.containsKey(key)) continue;
                existingGroups.put(key, group);
                Set<String> sectionCodes =new HashSet<>();
                sections.findBySectionGroupIdOrderByCodeAsc(group.getId()).forEach(section -> sectionCodes.add(section.getCode()));
                for (Parsed row : parsed) if (row.groupKey().equals(key)) {
                    if (group.getType() !=row.type()) error(errors, row.index(), "groupType", "This existing group has a different type.");
                    if (sectionCodes.contains(row.section())) error(errors, row.index(), "sectionCode", "This section already exists. Use a new section code or edit it in Administration.");
                }
            }
        }
        List<CatalogImportReport.RowError> rowErrors =errors.entrySet().stream()
                .map(entry -> new CatalogImportReport.RowError(entry.getKey(), Map.copyOf(entry.getValue()))).toList();
        return new Checked(semester, parsed, existingCourses, existingGroups,
                new CatalogImportReport(errors.isEmpty(), rowErrors, firstCourse.size(), sectionRows.size(), request.rows().size()));
    }
    private static void error(Map<Integer, Map<String, String>> errors, int index, String field, String message) {
        errors.computeIfAbsent(index, ignored -> new LinkedHashMap<>()).putIfAbsent(field, message);
    }
    private static String clean(String value) { return value ==null ? "" : value.trim(); }
    private static String text(String value, int max, String field, Map<String, String> errors) {
        String result =clean(value);
        if (result.isEmpty() || result.length() > max) errors.put(field, "Required; maximum " + max + " characters.");
        return result;
    }
    private static String normalized(String value) {
        return Normalizer.normalize(clean(value), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).replace(".", "");
    }
    static DayOfWeek weekday(String value) {
        return switch (normalized(value)) {
            case "1", "MON", "MONDAY", "ΔΕΥ", "ΔΕΥΤΕΡΑ" -> DayOfWeek.MONDAY;
            case "2", "TUE", "TUESDAY", "ΤΡΙ", "ΤΡΙΤΗ" -> DayOfWeek.TUESDAY;
            case "3", "WED", "WEDNESDAY", "ΤΕΤ", "ΤΕΤΑΡΤΗ" -> DayOfWeek.WEDNESDAY;
            case "4", "THU", "THURSDAY", "ΠΕΜ", "ΠΕΜΠΤΗ" -> DayOfWeek.THURSDAY;
            case "5", "FRI", "FRIDAY", "ΠΑΡ", "ΠΑΡΑΣΚΕΥΗ" -> DayOfWeek.FRIDAY;
            case "6", "SAT", "SATURDAY", "ΣΑΒ", "ΣΑΒΒΑΤΟ" -> DayOfWeek.SATURDAY;
            case "7", "SUN", "SUNDAY", "ΚΥΡ", "ΚΥΡΙΑΚΗ" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException();
        };
    }
    static SectionGroupType groupType(String value) {
        return switch (normalized(value)) {
            case "LECTURE", "LEC", "ΘΕΩΡΙΑ", "ΔΙΑΛΕΞΗ" -> SectionGroupType.LECTURE;
            case "LABORATORY", "LAB", "ΕΡΓΑΣΤΗΡΙΟ" -> SectionGroupType.LABORATORY;
            case "TUTORIAL", "TUT", "ΦΡΟΝΤΙΣΤΗΡΙΟ" -> SectionGroupType.TUTORIAL;
            case "SEMINAR", "ΣΕΜΙΝΑΡΙΟ" -> SectionGroupType.SEMINAR;
            default -> throw new IllegalArgumentException();
        };
    }
    static LocalTime time(String value) {
        String input =clean(value).replace('.', ':');
        if (!input.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) throw new IllegalArgumentException();
        if (input.indexOf(':') ==1) input ="0" + input;
        return LocalTime.parse(input);
    }
}

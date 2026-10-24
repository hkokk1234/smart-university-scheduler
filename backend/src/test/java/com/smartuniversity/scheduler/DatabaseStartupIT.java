package com.smartuniversity.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartuniversity.scheduler.catalog.application.CatalogReadService;
import com.smartuniversity.scheduler.catalog.domain.*;
import com.smartuniversity.scheduler.catalog.infrastructure.*;
import com.smartuniversity.scheduler.identity.application.UserDtoMapper;
import com.smartuniversity.scheduler.identity.domain.User;
import com.smartuniversity.scheduler.identity.infrastructure.UserRepository;
import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.scheduling.application.ScheduleReadService;
import com.smartuniversity.scheduler.scheduling.domain.SavedSchedule;
import com.smartuniversity.scheduler.scheduling.infrastructure.*;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties ={
        "logging.level.root=INFO", "debug=false", "spring.config.import=",
        "spring.flyway.locations=classpath:db/migration,classpath:db/demo"
})
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode =org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class DatabaseStartupIT {
    private static final UUID USER =UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER =UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static final UUID SEMESTER =UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID GROUP =UUID.fromString("60000000-0000-4000-8000-000000000001");
    private static final UUID SECTION =UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID ALTERNATIVE =UUID.fromString("70000000-0000-4000-8000-000000000002");
    private static final UUID SCHEDULE =UUID.fromString("90000000-0000-4000-8000-000000000001");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("scheduler_tests").withUsername("scheduler_test").withPassword("test-only");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret-base64", () -> TestSecrets.JWT_KEY);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired SemesterRepository semesters;
    @Autowired CourseRepository courses;
    @Autowired SectionGroupRepository groups;
    @Autowired SectionRepository sections;
    @Autowired MeetingRepository meetings;
    @Autowired InstructorRepository instructors;
    @Autowired RoomRepository rooms;
    @Autowired SavedScheduleRepository schedules;
    @Autowired ScheduleSectionRepository selections;
    @Autowired CatalogReadService catalogReader;
    @Autowired ScheduleReadService scheduleReader;

    @Test
    void databaseIsMigratedAndIncludedInReadiness() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        assertThat(courses.findBySemesterIdOrderByCodeAsc(SEMESTER)).hasSize(3);
        assertThat(groups.findByCourseIdOrderByCodeAsc(UUID.fromString("50000000-0000-4000-8000-000000000001"))).hasSize(2);
        assertThat(sections.findBySectionGroupIdOrderByCodeAsc(GROUP)).hasSize(2);
        assertThat(meetings.findBySectionIdOrderByWeekdayAscStartTimeAsc(SECTION)).hasSize(2);
        assertThat(selections.findBySavedScheduleId(SCHEDULE)).hasSize(5);
    }

    @Test
    @Transactional(propagation =Propagation.NOT_SUPPORTED)
    void productionMigrationsWorkOnAnotherCleanDatabaseWithoutDemoData() throws Exception {
        try (Connection connection =connection(); var statement =connection.createStatement()) {
            statement.execute("CREATE DATABASE scheduler_production_check");
        }
        String url =POSTGRES.getJdbcUrl().replace("/scheduler_tests", "/scheduler_production_check");
        Flyway production =Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load();
<<<<<<< HEAD
        assertThat(production.migrate().migrationsExecuted).isEqualTo(4);
=======
        assertThat(production.migrate().migrationsExecuted).isEqualTo(3);
>>>>>>> origin/main
        assertThat(production.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(production.validateWithResult().validationSuccessful).isTrue();
        assertThat(production.migrate().migrationsExecuted).isZero();
        try (Connection connection =DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement =connection.createStatement();
             var result =statement.executeQuery("SELECT count(*) FROM app_users")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isZero();
        }
    }

    @Test
    void allEntitiesRoundTripWithGeneratedUuidsAndWeeklyMeetings() {
        User user =users.save(new User(" NEW.Student@Example.edu ", "New Student"));
        Semester semester =semesters.save(new Semester("2027-SPRING", "Spring 2027",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30), "Europe/Athens"));
        Course course =courses.save(new Course(semester, "CS310", "Distributed Systems", new BigDecimal("6.0")));
        Instructor instructor =instructors.save(new Instructor("STAFF-NEW", "Dr. Taylor", null));
        Room room =rooms.save(new Room("Engineering", "E-100", 60));
        SectionGroup group =groups.save(new SectionGroup(course, "LEC", "Lectures", SectionGroupType.LECTURE));
        Section section =new Section(group, "A", 50);
        Meeting monday =section.addMeeting(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), instructor, room);
        Meeting thursday =section.addMeeting(DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), null, null);
        sections.saveAndFlush(section);
        SavedSchedule schedule =new SavedSchedule(user, semester, "My first option");
        var selection =schedule.addSection(section);
        assertThatThrownBy(() -> schedule.addSection(section)).isInstanceOf(IllegalArgumentException.class);
        SavedSchedule wrongSemester =new SavedSchedule(user, semesters.findById(SEMESTER).orElseThrow(), "Wrong semester");
        assertThatThrownBy(() -> wrongSemester.addSection(section)).isInstanceOf(IllegalArgumentException.class);
        schedules.saveAndFlush(schedule);
        assertThat(List.of(user, semester, course, instructor, room, group, section, monday, thursday, schedule, selection))
                .extracting(UuidEntity::getId).doesNotContainNull().doesNotHaveDuplicates();
        entityManager.clear();

        var dto =scheduleReader.findSchedule(schedule.getId(), user.getId()).orElseThrow();
        assertThat(dto.sections()).hasSize(1);
        assertThat(dto.sections().getFirst().section().meetings()).extracting(meeting -> meeting.weekday())
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        assertThat(dto.sections().getFirst().section().meetings().getFirst().instructor().displayName()).isEqualTo("Dr. Taylor");
        assertThat(dto.sections().getFirst().section().meetings().getLast().room()).isNull();
        assertThat(UserDtoMapper.toDto(users.findByEmail("new.student@example.edu").orElseThrow()).displayName()).isEqualTo("New Student");
        assertThat(instructors.findByStaffCode("STAFF-NEW")).isPresent();
        assertThat(rooms.findByBuildingAndCode("Engineering", "E-100")).isPresent();
        assertThat(courses.findBySemesterIdAndCode(semester.getId(), "CS310")).isPresent();
        assertThat(semesters.findByCode("2027-SPRING")).isPresent();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
    }

    @Test
    @Transactional(propagation =Propagation.NOT_SUPPORTED)
    void detachedDtosContainAllSelectedMeetingsAndRespectScheduleOwnership() throws Exception {
        var dto =scheduleReader.findSchedule(SCHEDULE, USER).orElseThrow();
        assertThat(dto.sections()).hasSize(5);
        assertThat(dto.sections().stream().mapToInt(selection -> selection.section().meetings().size()).sum()).isEqualTo(8);
        assertThat(json.readTree(json.writeValueAsString(dto)).get("sections").size()).isEqualTo(5);
        assertThat(scheduleReader.findSchedule(SCHEDULE, OTHER_USER)).isEmpty();
        assertThat(catalogReader.findSection(SECTION).orElseThrow().meetings()).hasSize(2);
        assertThat(catalogReader.findSectionAlternatives(GROUP)).hasSize(2);
    }

    @Test
    void databaseRejectsInvalidWeekdaysAndTimes() {
        for (int day : new int[]{0, 8}) {
            rejects("23514", "ck_meeting_weekday", () -> jdbc.update(
                    "INSERT INTO meetings(section_id, weekday, start_time, end_time) VALUES (?, ?, '17:00', '18:00')", SECTION, day));
        }
        for (String[] times : new String[][]{{"12:00", "12:00"}, {"13:00", "12:00"}, {"23:00", "24:00"}}) {
            rejects("23514", "ck_meeting_times", () -> jdbc.update(
                    "INSERT INTO meetings(section_id, weekday, start_time, end_time) VALUES (?, 6, ?::time, ?::time)", SECTION, times[0], times[1]));
        }
    }

    @Test
    void databaseRejectsDuplicateNaturalKeysAndMeetingSlots() {
        rejects("23505", "app_users_email_key", () -> jdbc.update(
                "INSERT INTO app_users(email, display_name) VALUES ('alex.student@example.edu', 'Duplicate')"));
        rejects("23505", "uq_course_semester_code", () -> jdbc.update(
                "INSERT INTO courses(semester_id, code, title, credits) VALUES (?, 'CS101', 'Duplicate', 6)", SEMESTER));
        rejects("23505", "uq_group_course_code", () -> jdbc.update(
                "INSERT INTO section_groups(course_id, semester_id, code, name, type) SELECT course_id, semester_id, code, name, type FROM section_groups WHERE id =?", GROUP));
        rejects("23505", "uq_section_group_code", () -> jdbc.update(
                "INSERT INTO sections(section_group_id, semester_id, code, capacity) VALUES (?, ?, 'A', 25)", GROUP, SEMESTER));
        rejects("23505", "uq_meeting_slot", () -> jdbc.update(
                "INSERT INTO meetings(section_id, weekday, start_time, end_time) VALUES (?, 1, '09:00', '10:30')", SECTION));
        rejects("23505", "uq_schedule_user_semester_name", () -> jdbc.update(
                "INSERT INTO saved_schedules(user_id, semester_id, name) VALUES (?, ?, 'Autumn first choice')", USER, SEMESTER));
        rejects("23514", "ck_user_email_normalized", () -> jdbc.update(
                "INSERT INTO app_users(email, display_name) VALUES ('UPPER@example.edu', 'Unnormalized')"));
    }

    @Test
    void databaseRejectsDuplicateSelectionsAndMultipleAlternatives() {
        for (UUID section : List.of(SECTION, ALTERNATIVE)) {
            rejects("23505", "uq_selection_schedule_group", () -> jdbc.update(
                    "INSERT INTO schedule_sections(saved_schedule_id, section_id, section_group_id, semester_id) VALUES (?, ?, ?, ?)",
                    SCHEDULE, section, GROUP, SEMESTER));
        }
    }

    @Test
    void databaseRejectsCrossSemesterSelectionsAndForgedGroupReferences() {
        rejects("23503", "fk_selection_schedule_semester", () -> {
            UUID otherSemester =createOtherSemester();
            UUID schedule =UUID.randomUUID();
            jdbc.update("INSERT INTO saved_schedules(id, user_id, semester_id, name) VALUES (?, ?, ?, 'Cross semester')", schedule, USER, otherSemester);
            jdbc.update("INSERT INTO schedule_sections(saved_schedule_id, section_id, section_group_id, semester_id) VALUES (?, ?, ?, ?)",
                    schedule, SECTION, GROUP, SEMESTER);
        });
        rejects("23503", "fk_group_course_semester", () -> {
            UUID otherSemester =createOtherSemester();
            jdbc.update("INSERT INTO section_groups(course_id, semester_id, code, name, type) VALUES ('50000000-0000-4000-8000-000000000001', ?, 'BAD', 'Wrong semester', 'LECTURE')",
                    otherSemester);
        });
        rejects("23503", "fk_section_group_semester", () -> {
            UUID otherSemester =createOtherSemester();
            jdbc.update("INSERT INTO sections(section_group_id, semester_id, code, capacity) VALUES (?, ?, 'BAD', 25)", GROUP, otherSemester);
        });
        rejects("23503", "fk_selection_section_group_semester", () -> {
            UUID emptySchedule =UUID.randomUUID();
            jdbc.update("INSERT INTO saved_schedules(id, user_id, semester_id, name) VALUES (?, ?, ?, 'Forged group test')",
                    emptySchedule, USER, SEMESTER);
            jdbc.update("INSERT INTO schedule_sections(saved_schedule_id, section_id, section_group_id, semester_id) "
                            + "VALUES (?, ?, '60000000-0000-4000-8000-000000000005', ?)",
                    emptySchedule, SECTION, SEMESTER);
        });
    }

    @Test
    void aSectionCannotCommitWithoutMeetingsOrLoseItsLastMeeting() {
        rejects("23514", "ck_section_has_meeting", () -> jdbc.update(
                "INSERT INTO sections(section_group_id, semester_id, code, capacity) VALUES (?, ?, 'EMPTY', 25)", GROUP, SEMESTER));
        rejects("23514", "ck_section_has_meeting", () -> jdbc.update("DELETE FROM meetings WHERE section_id =?", SECTION));
        rejects("23514", "ck_section_has_meeting", () -> jdbc.update("UPDATE meetings SET section_id =? WHERE section_id =?", ALTERNATIVE, SECTION));
    }

    @Test
    void deferredConstraintAllowsSectionThenMeetingAndParentDeletion() {
        UUID section =createTemporarySectionWithTwoMeetings();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM meetings WHERE section_id =?", Integer.class, section)).isEqualTo(2);
        inNewTransaction(() -> jdbc.update("DELETE FROM sections WHERE id =?", section));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM meetings WHERE section_id =?", Integer.class, section)).isZero();
    }

    @Test
    @Transactional(propagation =Propagation.NOT_SUPPORTED)
    void concurrentDeletesCannotRemoveAllMeetings() throws Exception {
        UUID section =createTemporarySectionWithTwoMeetings();
        List<UUID> meetingIds =jdbc.queryForList("SELECT id FROM meetings WHERE section_id =?", UUID.class, section);
        var barrier =new CyclicBarrier(2);
        try (var executor =Executors.newVirtualThreadPerTaskExecutor()) {
            var first =executor.submit(() -> deleteMeetingAndCommit(meetingIds.get(0), barrier));
            var second =executor.submit(() -> deleteMeetingAndCommit(meetingIds.get(1), barrier));
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder("COMMITTED", "23514");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM meetings WHERE section_id =?", Integer.class, section)).isEqualTo(1);
        } finally {
            inNewTransaction(() -> jdbc.update("DELETE FROM sections WHERE id =?", section));
        }
    }

    @Test
    void foreignKeysProtectCatalogAndUserDeletionCascadesOnlyPersonalSchedules() {
        rejects("23503", "fk_selection_section_group_semester", () -> jdbc.update("DELETE FROM sections WHERE id =?", SECTION));
        rejects("23503", "meetings_room_id_fkey", () -> jdbc.update("DELETE FROM rooms WHERE id ='40000000-0000-4000-8000-000000000001'"));
        jdbc.update("DELETE FROM app_users WHERE id =?", USER);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_schedules WHERE id =?", Integer.class, SCHEDULE)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM schedule_sections WHERE saved_schedule_id =?", Integer.class, SCHEDULE)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sections WHERE id =?", Integer.class, SECTION)).isEqualTo(1);
    }

    private UUID createOtherSemester() {
        UUID id =UUID.randomUUID();
        jdbc.update("INSERT INTO semesters(id, code, name, starts_on, ends_on, time_zone) VALUES (?, ?, 'Other', '2027-02-01', '2027-06-30', 'Europe/Athens')",
                id, id.toString().substring(0, 8));
        return id;
    }

    private UUID createTemporarySectionWithTwoMeetings() {
        UUID section =UUID.randomUUID();
        inNewTransaction(() -> {
            jdbc.update("INSERT INTO sections(id, section_group_id, semester_id, code, capacity) VALUES (?, ?, ?, ?, 25)",
                    section, GROUP, SEMESTER, section.toString().substring(0, 8));
            jdbc.update("INSERT INTO meetings(section_id, weekday, start_time, end_time) VALUES (?, 6, '09:00', '10:00'), (?, 7, '09:00', '10:00')", section, section);
        });
        return section;
    }

    private String deleteMeetingAndCommit(UUID meetingId, CyclicBarrier barrier) throws Exception {
        try (Connection connection =connection()) {
            connection.setAutoCommit(false);
            try (var statement =connection.prepareStatement("DELETE FROM meetings WHERE id =?")) {
                statement.setObject(1, meetingId);
                statement.executeUpdate();
                barrier.await(15, TimeUnit.SECONDS);
                try {
                    connection.commit();
                    return "COMMITTED";
                } catch (SQLException error) {
                    connection.rollback();
                    return error.getSQLState();
                }
            }
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void inNewTransaction(Runnable operation) {
        var transaction =new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(status -> operation.run());
    }

    private void rejects(String sqlState, String constraint, Runnable operation) {
        assertThatThrownBy(() -> inNewTransaction(operation)).satisfies(error -> {
            Throwable cause =error;
            while (cause !=null && !(cause instanceof PSQLException)) cause =cause.getCause();
            assertThat(cause).as("PostgreSQL constraint violation").isInstanceOf(PSQLException.class);
            PSQLException postgres =(PSQLException) cause;
            assertThat(postgres.getSQLState()).isEqualTo(sqlState);
            assertThat(postgres.getServerErrorMessage()).isNotNull();
            assertThat(postgres.getServerErrorMessage().getConstraint()).isEqualTo(constraint);
        });
    }
}

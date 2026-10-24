package com.smartuniversity.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties ={
        "spring.config.import=", "debug=false", "logging.level.root=INFO", "copilot.enabled=false",
        "spring.flyway.locations=classpath:db/migration,classpath:db/demo",
        "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"
})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode =DirtiesContext.ClassMode.AFTER_CLASS)
class ApiIT {
    private static final String PASSWORD ="Test-only-long-password-2026";
    private static final String SEMESTER ="20000000-0000-4000-8000-000000000001";
    private static final String COURSE ="50000000-0000-4000-8000-000000000001";
    private static final String SECTION ="70000000-0000-4000-8000-000000000001";
    private static final String LAB ="70000000-0000-4000-8000-000000000003";
    @Container
    static final PostgreSQLContainer<?> POSTGRES =new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("api_tests").withUsername("api_test").withPassword("test-only");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret-base64", () -> TestSecrets.JWT_KEY);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtDecoder decoder;

    @Test
    void registrationAndLoginUseBcryptAndIssueValidatedStudentTokens() throws Exception {
        String email =UUID.randomUUID() + "@example.edu";
        var registration =postJson("/api/v1/auth/register", null,
                Map.of("email", email.toUpperCase(java.util.Locale.ROOT), "displayName", "API Student", "password", PASSWORD))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        JsonNode body =body(registration);
        String hash =jdbc.queryForObject("SELECT password_hash FROM app_users WHERE email =?", String.class, email);
        assertThat(hash).isNotEqualTo(PASSWORD).startsWith("$2a$12$");
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        var jwt =decoder.decode(body.get("accessToken").asText());
        assertThat(jwt.getSubject()).isEqualTo(body.get("user").get("id").asText());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        postJson("/api/v1/auth/login", null, Map.of("email", email, "password", PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"));
        postJson("/api/v1/auth/register", null, Map.of("email", email, "displayName", "Duplicate", "password", PASSWORD))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void invalidCredentialsHaveConsistentErrorsAndNoTokens() throws Exception {
        Account account =register();
        for (String email : List.of(account.email(), UUID.randomUUID() + "@example.edu", "alex.student@example.edu")) {
            postJson("/api/v1/auth/login", null, Map.of("email", email, "password", "Incorrect-long-password"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.accessToken").doesNotExist()).andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
        }
    }

    @Test
    void validationRejectsRoleInjectionUnknownFieldsAndOversizedUtf8Passwords() throws Exception {
        postJson("/api/v1/auth/register", null, Map.of("email", "inject@example.edu", "displayName", "Injection", "password", PASSWORD, "role", "ADMIN"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        postJson("/api/v1/auth/register", null, Map.of("email", "invalid", "displayName", "", "password", "short"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        postJson("/api/v1/auth/register", null, Map.of("email", "utf8@example.edu", "displayName", "Unicode", "password", "🙂".repeat(20)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void publicCatalogSupportsFiltersPaginationSortingAndFullMeetingDetails() throws Exception {
        mvc.perform(get("/api/v1/courses").param("semesterId", SEMESTER).param("q", "database").param("size", "1").param("sort", "title,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("CS205"))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.size").value(1));
        mvc.perform(get("/api/v1/courses").param("code", "cs101"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/courses").param("q", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/courses/" + COURSE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionGroups.length()").value(2));
        mvc.perform(get("/api/v1/sections/" + SECTION)).andExpect(status().isOk())
                .andExpect(jsonPath("$.meetings.length()").value(2)).andExpect(jsonPath("$.version").isNumber());
        mvc.perform(get("/api/v1/courses/" + UUID.randomUUID())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void catalogRejectsInvalidPagingSortingAndIdentifierParameters() throws Exception {
        for (String[] parameter : new String[][]{{"size", "101"}, {"size", "0"}, {"page", "-1"}, {"sort", "passwordHash,asc"}, {"semesterId", "invalid"}}) {
            mvc.perform(get("/api/v1/courses").param(parameter[0], parameter[1]))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(get("/api/v1/courses/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void savedSchedulesArePrivateAndAlwaysUseJwtOwnership() throws Exception {
        Account owner =register(), other =register();
        JsonNode created =body(postJson("/api/v1/schedules", owner.token(),
                Map.of("semesterId", SEMESTER, "name", "API selection", "sectionIds", List.of(SECTION, LAB)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(owner.id()))
                .andExpect(jsonPath("$.sections[0].section.meetings.length()").value(2)));
        String id =created.get("id").asText();
        mvc.perform(get("/api/v1/schedules").header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/schedules").header("Authorization", bearer(other.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/schedules/" + id).header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/schedules/" + id).header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/schedules/" + id).header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/schedules/" + id).header("Authorization", bearer(owner.token())))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM schedule_sections WHERE saved_schedule_id =?", Integer.class, UUID.fromString(id))).isZero();
    }

    @Test
    void scheduleWritesRejectDuplicateAlternativesCrossSemesterAndOwnerInjection() throws Exception {
        Account account =register();
        postJson("/api/v1/schedules", account.token(), Map.of("semesterId", SEMESTER, "name", "Two alternatives", "sectionIds",
                List.of(SECTION, "70000000-0000-4000-8000-000000000002"))).andExpect(status().isBadRequest());
        UUID semester =UUID.randomUUID();
        jdbc.update("INSERT INTO semesters(id, code, name, starts_on, ends_on, time_zone) VALUES (?, ?, 'Other', '2027-02-01', '2027-06-30', 'Europe/Athens')",
                semester, semester.toString().substring(0, 8));
        postJson("/api/v1/schedules", account.token(), Map.of("semesterId", semester.toString(), "name", "Cross semester", "sectionIds", List.of(SECTION)))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/schedules", account.token(), Map.of("semesterId", SEMESTER, "name", "Injected owner", "sectionIds", List.of(), "userId", account.id()))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/schedules", account.token(), Map.of("semesterId", SEMESTER, "name", "Draft", "sectionIds", List.of()))
                .andExpect(status().isCreated());
        postJson("/api/v1/schedules", account.token(), Map.of("semesterId", SEMESTER, "name", "Draft", "sectionIds", List.of()))
                .andExpect(status().isConflict());
    }

    @Test
    void adminRoutesRejectAnonymousStudentsAndTamperedTokens() throws Exception {
        Account student =register();
        var request =courseRequest();
        postJson("/api/v1/admin/courses", null, request).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        postJson("/api/v1/admin/courses", student.token(), request).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(put("/api/v1/admin/sections/" + SECTION).header("Authorization", bearer(student.token()))
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        String[] parts =student.token().split("\\.");
        parts[2] =(parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        mvc.perform(get("/api/v1/schedules").header("Authorization", bearer(String.join(".", parts))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/schedules").param("access_token", student.token())).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateCoursesButStillCannotReadAnotherUsersSchedules() throws Exception {
        Account admin =admin();
        var request =courseRequest();
        postJson("/api/v1/admin/courses", admin.token(), request)
                .andExpect(status().isCreated()).andExpect(header().exists("Location"));
        postJson("/api/v1/admin/courses", admin.token(), request).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/schedules/90000000-0000-4000-8000-000000000001").header("Authorization", bearer(admin.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminReplacesMeetingsAtomicallyAndRejectsStaleAndInvalidUpdates() throws Exception {
        Account admin =admin();
        JsonNode current =body(mvc.perform(get("/api/v1/sections/" + SECTION)).andExpect(status().isOk()));
        long version =current.get("version").asLong();
        var meeting =Map.of("weekday", "MONDAY", "startTime", "09:00:00", "endTime", "10:30:00");
        var second =Map.of("weekday", "WEDNESDAY", "startTime", "09:00:00", "endTime", "10:30:00");
        var request =Map.of("code", "A", "capacity", 95, "version", version, "meetings", List.of(meeting, second));
        JsonNode updated =body(putJson(SECTION, admin.token(), request).andExpect(status().isOk())
                .andExpect(jsonPath("$.meetings.length()").value(2)).andExpect(jsonPath("$.capacity").value(95)));
        assertThat(updated.get("version").asLong()).isGreaterThan(version);
        putJson(SECTION, admin.token(), request).andExpect(status().isConflict());
        long latest =updated.get("version").asLong();
        putJson(SECTION, admin.token(), Map.of("code", "A", "capacity", 95, "version", latest, "meetings", List.of()))
                .andExpect(status().isBadRequest());
        putJson(SECTION, admin.token(), Map.of("code", "A", "capacity", 95, "version", latest, "meetings",
                List.of(Map.of("weekday", "MONDAY", "startTime", "11:00", "endTime", "10:00"))))
                .andExpect(status().isBadRequest());
        putJson(SECTION, admin.token(), Map.of("code", "A", "capacity", 95, "version", latest, "meetings", List.of(meeting, meeting)))
                .andExpect(status().isBadRequest());
        putJson(SECTION, admin.token(), Map.of("code", "A", "capacity", 95, "version", latest, "meetings",
                List.of(Map.of("weekday", "MONDAY", "startTime", "09:00", "endTime", "10:30", "roomId", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound());
        JsonNode after =body(mvc.perform(get("/api/v1/sections/" + SECTION)).andExpect(status().isOk()));
        assertThat(after.get("version").asLong()).isEqualTo(latest);
        assertThat(after.get("meetings").size()).isEqualTo(2);
    }

    @Test
    void openApiDescribesEveryRequiredRouteAndBearerSecurity() throws Exception {
        JsonNode schema =body(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()));
        for (String path : List.of("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/courses", "/api/v1/courses/{id}",
                "/api/v1/schedules", "/api/v1/schedules/{id}", "/api/v1/schedules/generate", "/api/v1/admin/courses", "/api/v1/admin/sections/{id}")) {
            assertThat(schema.get("paths").has(path)).isTrue();
        }
        assertThat(schema.get("components").get("securitySchemes").get("bearerAuth").get("scheme").asText()).isEqualTo("bearer");
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void generationUsesCatalogSectionsAndRequiresAuthentication() throws Exception {
        var request =Map.of("semesterId", SEMESTER, "courseIds", List.of(COURSE), "maxResults", 5);
        postJson("/api/v1/schedules/generate", null, request).andExpect(status().isUnauthorized());
        Account student =register();
        JsonNode generated =body(postJson("/api/v1/schedules/generate", student.token(), request)
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("SOLVED")).andExpect(jsonPath("$.metrics.complete").value(true)));
        assertThat(generated.get("schedules").size()).isBetween(1, 5);
        for (JsonNode schedule : generated.get("schedules")) {
            assertThat(schedule.get("selectedSectionIds").size()).isEqualTo(2);
            assertThat(schedule.get("weeklyMeetings").size()).isEqualTo(3);
            assertThat(schedule.get("scoreExplanation").size()).isEqualTo(6);
        }
        postJson("/api/v1/schedules/generate", student.token(), Map.of("semesterId", UUID.randomUUID().toString(), "courseIds", List.of(COURSE)))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/schedules/generate", student.token(), Map.of("semesterId", SEMESTER, "courseIds", List.of(COURSE, COURSE)))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/schedules/generate", student.token(), Map.of("semesterId", SEMESTER, "courseIds", List.of(COURSE),
                "preferences", Map.of("unavailableTimes", List.of(
                        Map.of("weekday", "MONDAY", "startTime", "00:00", "endTime", "23:59:59"),
                        Map.of("weekday", "TUESDAY", "startTime", "00:00", "endTime", "23:59:59"),
                        Map.of("weekday", "WEDNESDAY", "startTime", "00:00", "endTime", "23:59:59"),
                        Map.of("weekday", "THURSDAY", "startTime", "00:00", "endTime", "23:59:59"),
                        Map.of("weekday", "FRIDAY", "startTime", "00:00", "endTime", "23:59:59")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNSAT"));
    }

    @Test
    void copilotRequiresLoginAndClearlyReportsMissingConfiguration() throws Exception {
        mvc.perform(get("/api/v1/copilot/status")).andExpect(status().isUnauthorized());
        Account student=register();
        mvc.perform(get("/api/v1/copilot/status").header("Authorization",bearer(student.token())))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.available").value(false));
        postJson("/api/v1/copilot/chat",student.token(),Map.of("semesterId",SEMESTER,"message","Help me choose courses"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"));
        postJson("/api/v1/copilot/chat",student.token(),Map.of("semesterId",SEMESTER,"message",""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void digitalTwinIsAdminOnlyValidatesChangesAndNeverPersistsThem() throws Exception {
        String path = "/api/v1/admin/digital-twin/simulate";
        var request = Map.of("semesterId", SEMESTER, "changes", List.of());
        postJson(path, null, request).andExpect(status().isUnauthorized());
        postJson(path, register().token(), request).andExpect(status().isForbidden());
        String token = admin().token();
        JsonNode baseline = body(postJson(path, token, request).andExpect(status().isOk()));
        assertThat(baseline.get("baseline")).isEqualTo(baseline.get("scenario"));
        JsonNode meeting = baseline.get("meetings").get(0);
        var change = Map.of("meetingId", meeting.get("id").asText(), "weekday", "SUNDAY", "startTime", "07:00", "endTime", "08:00", "roomId", baseline.get("rooms").get(0).get("id").asText(), "attendance", 10000);
        postJson(path, token, Map.of("semesterId", SEMESTER, "changes", List.of(change)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scenario.alerts[0].type").value("CAPACITY"));
        JsonNode unchanged = body(postJson(path, token, request).andExpect(status().isOk()));
        assertThat(unchanged.get("meetings")).isEqualTo(baseline.get("meetings"));
        assertThat(unchanged.get("baseline")).isEqualTo(baseline.get("baseline"));
        postJson(path, token, Map.of("semesterId", SEMESTER, "changes", List.of(change, change))).andExpect(status().isBadRequest());
        postJson(path, token, Map.of("semesterId", SEMESTER, "changes", List.of(Map.of("meetingId", UUID.randomUUID(), "weekday", "MONDAY", "startTime", "09:00", "endTime", "10:00", "attendance", 10)))).andExpect(status().isBadRequest());
    }

<<<<<<< HEAD
    @Test
    void catalogImportPreviewsWithoutWritesAndPublishesACompleteValidatedBatch() throws Exception {
        Account admin =admin();
        String code ="IMP-" + UUID.randomUUID().toString().substring(0, 8);
        String path ="/api/v1/admin/catalog-import";
        byte[] workbook;
        try (var book =new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var output =new java.io.ByteArrayOutputStream()) {
            var sheet =book.createSheet("Timetable");
            String[][] cells ={{"Code", "Title", "Day", "Start", "End"},
                    {code, "Imported course", "Monday", "09:00", "11:00"}};
            for (int row =0; row < cells.length; row++) {
                var excelRow =sheet.createRow(row);
                for (int column =0; column < cells[row].length; column++) excelRow.createCell(column).setCellValue(cells[row][column]);
            }
            book.write(output); workbook =output.toByteArray();
        }
        mvc.perform(multipart(path + "/preview").file(new org.springframework.mock.web.MockMultipartFile(
                        "file", "timetable.xlsx", "application/octet-stream", workbook)).header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tables[0].rows[1][0]").value(code));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE code =?", Integer.class, code)).isZero();
        var monday =Map.of("courseCode", code, "title", "Imported course", "credits", "6", "groupCode", "LEC",
                "groupType", "LECTURE", "sectionCode", "A", "capacity", "30", "weekday", "Δευτέρα", "startTime", "9.00", "endTime", "11.00");
        var wednesday =new java.util.HashMap<>(monday); wednesday.put("weekday", "Τετάρτη");
        var request =Map.of("semesterId", SEMESTER, "rows", List.of(monday, wednesday));
        postJson(path + "/validate", admin.token(), request).andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE code =?", Integer.class, code)).isZero();
        postJson(path + "/commit", admin.token(), request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.coursesCreated").value(1)).andExpect(jsonPath("$.meetingsCreated").value(2));
        UUID id =jdbc.queryForObject("SELECT id FROM courses WHERE code =?", UUID.class, code);
        mvc.perform(get("/api/v1/courses/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionGroups[0].sections[0].meetings.length()").value(2));
        postJson(path + "/commit", admin.token(), request).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE code =?", Integer.class, code)).isEqualTo(1);
        var invalid =new java.util.HashMap<>(monday); invalid.put("courseCode", code + "-NEW"); invalid.put("startTime", "25:00");
        postJson(path + "/commit", admin.token(), Map.of("semesterId", SEMESTER, "rows", List.of(invalid)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE code =?", Integer.class, code + "-NEW")).isZero();
    }

    @Test
    void sectionHistoryAndNotificationsAreDurableDeduplicatedAndOwnerScoped() throws Exception {
        Account student =register(), stranger =register(), admin =admin();
        for (String name : List.of("History plan one", "History plan two")) postJson("/api/v1/schedules", student.token(),
                Map.of("semesterId", SEMESTER, "name", name, "sectionIds", List.of(SECTION))).andExpect(status().isCreated());
        JsonNode current =body(mvc.perform(get("/api/v1/sections/" + SECTION)).andExpect(status().isOk()));
        long historyBefore =jdbc.queryForObject("SELECT count(*) FROM section_changes WHERE section_id=?", Long.class, UUID.fromString(SECTION));
        var meeting =Map.of("weekday", "FRIDAY", "startTime", "16:00", "endTime", "17:00");
        var update =Map.of("code", current.get("code").asText(), "capacity", current.get("capacity").asInt() + 1,
                "version", current.get("version").asLong(), "meetings", List.of(meeting));
        putJson(SECTION, admin.token(), update).andExpect(status().isOk());
        JsonNode inbox =body(mvc.perform(get("/api/v1/notifications").header("Authorization", bearer(student.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items.totalElements").value(1)));
        String notification =inbox.get("items").get("content").get(0).get("id").asText();
        assertThat(inbox.get("items").get("content").get(0).get("change").get("before").get("capacity").asInt())
                .isEqualTo(current.get("capacity").asInt());
        mvc.perform(get("/api/v1/notifications").header("Authorization", bearer(stranger.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.totalElements").value(0));
        postJson("/api/v1/notifications/" + notification + "/read", stranger.token(), Map.of()).andExpect(status().isNotFound());
        postJson("/api/v1/notifications/" + notification + "/read", student.token(), Map.of()).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications").header("Authorization", bearer(student.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
        mvc.perform(get("/api/v1/admin/sections/" + SECTION + "/history").header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/sections/" + SECTION + "/history").header("Authorization", bearer(student.token())))
                .andExpect(status().isForbidden());
        putJson(SECTION, admin.token(), update).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM section_changes WHERE section_id=?", Long.class, UUID.fromString(SECTION)))
                .isEqualTo(historyBefore + 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_notifications WHERE user_id=?", Long.class, UUID.fromString(student.id()))).isEqualTo(1);
    }

=======
>>>>>>> origin/main
    private Account register() throws Exception {
        String email =UUID.randomUUID() + "@example.edu";
        JsonNode response =body(postJson("/api/v1/auth/register", null, Map.of("email", email, "displayName", "Test Student", "password", PASSWORD))
                .andExpect(status().isCreated()));
        return new Account(email, response.get("user").get("id").asText(), response.get("accessToken").asText());
    }
    private Account admin() throws Exception {
        Account account =register();
        jdbc.update("UPDATE app_users SET role ='ADMIN' WHERE id =?", UUID.fromString(account.id()));
        JsonNode login =body(postJson("/api/v1/auth/login", null, Map.of("email", account.email(), "password", PASSWORD)).andExpect(status().isOk()));
        return new Account(account.email(), account.id(), login.get("accessToken").asText());
    }
    private Map<String, Object> courseRequest() {
        return Map.of("semesterId", SEMESTER, "code", "API-" + UUID.randomUUID().toString().substring(0, 8), "title", "API created course", "credits", 6.0);
    }
    private ResultActions postJson(String path, String token, Object request) throws Exception {
        var builder =post(path).contentType("application/json").content(json.writeValueAsString(request));
        if (token !=null) builder.header("Authorization", bearer(token));
        return mvc.perform(builder);
    }
    private ResultActions putJson(String section, String token, Object request) throws Exception {
        return mvc.perform(put("/api/v1/admin/sections/" + section).header("Authorization", bearer(token))
                .contentType("application/json").content(json.writeValueAsString(request)));
    }
    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
    private String bearer(String token) { return "Bearer " + token; }
    private record Account(String email, String id, String token) {}
}

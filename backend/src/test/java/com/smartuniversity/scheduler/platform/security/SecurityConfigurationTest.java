package com.smartuniversity.scheduler.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.smartuniversity.scheduler.TestSecrets;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes =SecurityConfigurationTest.TestApplication.class, properties ={
        "logging.level.root=INFO",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
        "management.endpoint.health.group.readiness.include=readinessState"
})
@AutoConfigureMockMvc
@ActiveProfiles("security-test")
@org.springframework.test.annotation.DirtiesContext(classMode =org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityConfigurationTest {
    @Configuration(proxyBeanMethods =false)
    @Profile("security-test")
    @EnableAutoConfiguration
    @Import({SecurityConfiguration.class, JwtConfiguration.class})
    static class TestApplication {}

    @DynamicPropertySource
    static void tokenKey(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret-base64", () -> TestSecrets.JWT_KEY);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void healthAndProbesArePublicWithoutExposingDetails() throws Exception {
        for (String endpoint : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mvc.perform(get(endpoint))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
    }

    @Test
    void anonymousApiAccessIsDenied() throws Exception {
        mvc.perform(get("/api/anything")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void authenticatedApiAccessIsDenied() throws Exception {
        mvc.perform(get("/api/anything")).andExpect(status().isForbidden());
    }

    @Test
    void bearerOnlyAuthenticationDoesNotAcceptCookies() throws Exception {
        mvc.perform(get("/api/v1/schedules").cookie(new jakarta.servlet.http.Cookie("access_token", "fake")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(post("/actuator/health")).andExpect(status().isUnauthorized());
    }
}

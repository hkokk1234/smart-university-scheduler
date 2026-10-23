package com.smartuniversity.scheduler.identity.api;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RequestValidationTest {
    @Test
    void rejectsUtf8PasswordsThatWouldExceedBcryptLimit() {
        try (var factory =Validation.buildDefaultValidatorFactory()) {
            var violations =factory.getValidator().validate(new RegisterRequest("valid@example.edu", "Student", "🙂".repeat(20)));
            assertThat(violations).anyMatch(violation -> violation.getMessage().contains("72 UTF-8 bytes"));
        }
    }
}

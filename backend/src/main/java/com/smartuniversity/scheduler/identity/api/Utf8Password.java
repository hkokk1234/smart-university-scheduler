package com.smartuniversity.scheduler.identity.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy =Utf8Password.Validator.class)
public @interface Utf8Password {
    String message() default "password must not exceed 72 UTF-8 bytes";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<Utf8Password, String> {
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value ==null || value.getBytes(StandardCharsets.UTF_8).length <=72;
        }
    }
}

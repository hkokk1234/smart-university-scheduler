package com.smartuniversity.scheduler.platform.persistence;

import java.util.Locale;
import java.util.Objects;

public final class EntityValues {
    private EntityValues() {}

    public static String requiredText(String value, int maxLength, String field) {
        String normalized =Objects.requireNonNull(value, field).strip();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1 to " + maxLength + " characters");
        }
        return normalized;
    }

    public static String email(String value) {
        return requiredText(value, 254, "email").toLowerCase(Locale.ROOT);
    }

    public static int positive(int value, String field) {
        if (value <=0) throw new IllegalArgumentException(field + " must be positive");
        return value;
    }
}

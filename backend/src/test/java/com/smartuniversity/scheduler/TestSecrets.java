package com.smartuniversity.scheduler;

import java.security.SecureRandom;
import java.util.Base64;

public final class TestSecrets {
    public static final String JWT_KEY =generate();
    private TestSecrets() {}
    public static String generate() {
        byte[] bytes =new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}

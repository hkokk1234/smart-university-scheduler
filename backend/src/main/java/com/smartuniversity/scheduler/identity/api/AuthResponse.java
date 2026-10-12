package com.smartuniversity.scheduler.identity.api;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserDto user) {}

package com.smartuniversity.scheduler.catalog.api;

import java.util.UUID;


public record RoomDto(UUID id, String building, String code, int capacity) {
}

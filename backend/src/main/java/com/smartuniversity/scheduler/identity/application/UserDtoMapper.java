package com.smartuniversity.scheduler.identity.application;

import com.smartuniversity.scheduler.identity.api.UserDto;
import com.smartuniversity.scheduler.identity.domain.User;

public final class UserDtoMapper {
    private UserDtoMapper() {}

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt(), user.getRole().name());
    }
}

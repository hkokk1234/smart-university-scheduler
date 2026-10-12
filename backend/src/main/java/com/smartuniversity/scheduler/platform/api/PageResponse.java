package com.smartuniversity.scheduler.platform.api;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public PageResponse { content =List.copyOf(content); }

    public static <E, T> PageResponse<T> from(Page<E> result, Function<E, T> mapper) {
        return new PageResponse<>(result.getContent().stream().map(mapper).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}

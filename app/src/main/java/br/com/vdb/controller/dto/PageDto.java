package br.com.vdb.controller.dto;

import br.com.vdb.core.port.in.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageDto<T> from(PageResult<S> result, Function<S, T> mapper) {
        PageResult<T> mapped = result.map(mapper);
        return new PageDto<>(mapped.content(), mapped.page(), mapped.size(), mapped.totalElements(),
                mapped.totalPages());
    }
}

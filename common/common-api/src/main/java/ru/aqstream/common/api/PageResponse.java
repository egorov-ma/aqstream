package ru.aqstream.common.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Стандартный ответ с пагинацией для REST API.
 * Используется для всех endpoint'ов, возвращающих списки с пагинацией.
 *
 * @param <T> тип элементов в списке
 */
public record PageResponse<T>(
    List<T> data,
    int page,
    int size,
    long totalElements,
    int totalPages,
    @JsonProperty("hasNext") boolean hasNext,
    @JsonProperty("hasPrevious") boolean hasPrevious
) {

    /**
     * Создаёт PageResponse из Spring Data Page.
     *
     * @param page Spring Data Page
     * @param <T>  тип элементов
     * @return PageResponse с данными из Page
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    /**
     * Создаёт PageResponse с маппингом элементов.
     *
     * @param page   Spring Data Page
     * @param mapper функция маппинга элементов
     * @param <T>    исходный тип элементов
     * @param <R>    результирующий тип элементов
     * @return PageResponse с преобразованными данными
     */
    public static <T, R> PageResponse<R> of(
        org.springframework.data.domain.Page<T> page,
        java.util.function.Function<T, R> mapper
    ) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}

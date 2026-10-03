package com.api.medirecord.common.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Representacion estable de una pagina paginada en la API.
 *
 * <p>Se evita exponer {@link Page} directamente para no acoplar el contrato
 * publico al framework de persistencia.</p>
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

	public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
		List<T> content = page.getContent().stream().map(mapper).toList();
		return new PageResponse<>(
				content,
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

}
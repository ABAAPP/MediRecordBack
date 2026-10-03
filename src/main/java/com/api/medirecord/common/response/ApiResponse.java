package com.api.medirecord.common.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envoltura estandar de las respuestas exitosas de la API.
 *
 * <p>Los errores no usan este formato: se devuelven como {@code ProblemDetail}
 * (RFC 9457) desde {@code GlobalExceptionHandler}.</p>
 *
 * @param success    siempre {@code true} en respuestas exitosas
 * @param message    mensaje legible opcional
 * @param data       payload de la respuesta
 * @param timestamp  momento de generacion de la respuesta (UTC)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, null, data, Instant.now());
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, message, data, Instant.now());
	}

	public static ApiResponse<Void> empty() {
		return new ApiResponse<>(true, null, null, Instant.now());
	}

	public static ApiResponse<Void> empty(String message) {
		return new ApiResponse<>(true, message, null, Instant.now());
	}

}
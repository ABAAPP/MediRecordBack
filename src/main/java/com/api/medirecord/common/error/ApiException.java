package com.api.medirecord.common.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Excepcion de negocio checked-free: la capa web la traduce a {@code ProblemDetail}.
 *
 * <p>Usar solo para condiciones esperadas y conocidas por el dominio. Los errores
 * inesperados deben propagarse como {@link RuntimeException} para que
 * {@code GlobalExceptionHandler} los reporte como {@code INTERNAL_ERROR}.</p>
 */
public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient ErrorCode errorCode;
	private final transient Map<String, Object> details = new LinkedHashMap<>();

	public ApiException(ErrorCode errorCode) {
		this(errorCode, errorCode.getDefaultMessage());
	}

	public ApiException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode no puede ser null");
	}

	public ApiException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode no puede ser null");
	}

	/** Agrega contexto al error (por ejemplo el campo que lo disparo). */
	public ApiException withDetail(String key, Object value) {
		this.details.put(key, value);
		return this;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public Map<String, Object> getDetails() {
		return Collections.unmodifiableMap(details);
	}

}
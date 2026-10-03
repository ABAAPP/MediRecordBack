package com.api.medirecord.common.error;

import org.springframework.http.HttpStatus;

/**
 * Catalogo de errores de la aplicacion.
 *
 * <p>El nombre del enum viaja al cliente dentro del {@code ProblemDetail}
 * ({@code type}, {@code title} y propiedad {@code code}), por lo que es parte
 * del contrato de la API: no renombrar sin considerarlo un breaking change.</p>
 */
public enum ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos"),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido"),
	MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "Falta un parametro requerido"),
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "El tipo de un parametro es incorrecto"),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Credenciales ausentes o invalidas"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "No cuenta con permisos para esta operacion"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "El recurso solicitado no existe"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no soportado"),
	CONFLICT(HttpStatus.CONFLICT, "La operacion entra en conflicto con el estado actual"),
	DUPLICATED_RESOURCE(HttpStatus.CONFLICT, "El recurso ya existe"),
	DATA_INTEGRITY_ERROR(HttpStatus.CONFLICT, "La operacion viola una restriccion de integridad"),
	CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "El recurso fue modificado por otra transaccion"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno inesperado"),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible temporalmente");

	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String defaultMessage) {
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}

	/** Identificador estable del error dentro del contrato de la API. */
	public String getCode() {
		return name();
	}

}
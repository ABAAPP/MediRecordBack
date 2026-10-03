package com.api.medirecord.common.error;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * Traduce excepciones a respuestas {@code ProblemDetail} (RFC 9457).
 *
 * <p>Contrato de errores de la API:
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "NOT_FOUND",
 *   "status": 404,
 *   "detail": "No se encontro el paciente con id 123",
 *   "instance": "/api/v1/pacientes/123",
 *   "code": "NOT_FOUND",
 *   "timestamp": "2026-01-01T12:00:00Z",
 *   "traceId": "8f2a...",
 *   "errors": [ { "field": "email", "message": "..." } ]
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApiException(ApiException ex) {
		log.debug("Error de negocio: {}", ex.getMessage());
		ProblemDetail detail = problem(ex.getErrorCode(), ex.getMessage());
		ex.getDetails().forEach(detail::setProperty);
		return detail;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toViolation)
				.toList();
		ProblemDetail detail = problem(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage());
		detail.setProperty("errors", violations);
		return detail;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex) {
		List<FieldViolation> violations = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldViolation(
								result.getMethodParameter().getParameterName(),
								error.getDefaultMessage(),
								result.getArgument())))
				.toList();
		ProblemDetail detail = problem(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage());
		detail.setProperty("errors", violations);
		return detail;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		List<FieldViolation> violations = ex.getConstraintViolations().stream()
				.map(violation -> new FieldViolation(
						String.valueOf(violation.getPropertyPath()),
						violation.getMessage(),
						violation.getInvalidValue()))
				.toList();
		ProblemDetail detail = problem(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage());
		detail.setProperty("errors", violations);
		return detail;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
		log.debug("Cuerpo de peticion ilegible: {}", ex.getMessage());
		return problem(ErrorCode.MALFORMED_REQUEST, "Revise el cuerpo de la peticion: " + ex.getMostSpecificCause().getMessage());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
		return problem(ErrorCode.MISSING_PARAMETER, "El parametro '" + ex.getParameterName() + "' es obligatorio");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return problem(ErrorCode.TYPE_MISMATCH,
				"El parametro '" + ex.getName() + "' espera el tipo " + ex.getRequiredType().getSimpleName());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
		return problem(ErrorCode.NOT_FOUND, "No existe el recurso " + ex.getResourcePath());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail handleAuthentication(AuthenticationException ex) {
		return problem(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.getDefaultMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
		return problem(ErrorCode.DATA_INTEGRITY_ERROR, ErrorCode.DATA_INTEGRITY_ERROR.getDefaultMessage());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
		return problem(ErrorCode.CONCURRENT_MODIFICATION, ErrorCode.CONCURRENT_MODIFICATION.getDefaultMessage());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Error no controlado", ex);
		return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage());
	}

	private FieldViolation toViolation(FieldError fieldError) {
		return new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());
	}

	private ProblemDetail problem(ErrorCode errorCode, String message) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), message);
		detail.setTitle(errorCode.getCode());
		detail.setProperty("code", errorCode.getCode());
		detail.setProperty("timestamp", Instant.now());
		detail.setProperty("traceId", MDC.get("traceId"));
		return detail;
	}

	/** Violacion de validacion de un campo concreto. */
	public record FieldViolation(String field, String message, Object rejectedValue) {
	}

}
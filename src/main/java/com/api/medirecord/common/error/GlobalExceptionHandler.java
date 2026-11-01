package com.api.medirecord.common.error;

import java.net.URI;
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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
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
	public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
		log.debug("Error de negocio: {}", ex.getMessage());
		ProblemDetail detail = problem(ex.getErrorCode(), ex.getMessage(), request);
		ex.getDetails().forEach(detail::setProperty);
		return detail;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toViolation)
				.toList();
		ProblemDetail detail = problem(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request);
		detail.setProperty("errors", violations);
		return detail;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldViolation(
								result.getMethodParameter().getParameterName(),
								error.getDefaultMessage(),
								result.getArgument())))
				.toList();
		ProblemDetail detail = problem(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request);
		detail.setProperty("errors", violations);
		return detail;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getConstraintViolations().stream()
				.map(violation -> new FieldViolation(
						String.valueOf(violation.getPropertyPath()),
						violation.getMessage(),
						violation.getInvalidValue()))
				.toList();
		ProblemDetail detail = problem(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request);
		detail.setProperty("errors", violations);
		return detail;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		log.debug("Cuerpo de peticion ilegible: {}", ex.getMessage());
		return problem(ErrorCode.MALFORMED_REQUEST, "Revise el cuerpo de la peticion: " + ex.getMostSpecificCause().getMessage(), request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
		return problem(ErrorCode.MISSING_PARAMETER, "El parametro '" + ex.getParameterName() + "' es obligatorio", request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return problem(ErrorCode.TYPE_MISMATCH,
				"El parametro '" + ex.getName() + "' espera el tipo " + ex.getRequiredType().getSimpleName(), request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
		return problem(ErrorCode.NOT_FOUND, "No existe el recurso " + ex.getResourcePath(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return problem(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.getDefaultMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return problem(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("Violacion de integridad: {}", ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
		return problem(ErrorCode.DATA_INTEGRITY_ERROR, ErrorCode.DATA_INTEGRITY_ERROR.getDefaultMessage(), request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
		return problem(ErrorCode.CONCURRENT_MODIFICATION, ErrorCode.CONCURRENT_MODIFICATION.getDefaultMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Error no controlado", ex);
		return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request);
	}

	private FieldViolation toViolation(FieldError fieldError) {
		return new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());
	}

	public ProblemDetail problem(ErrorCode errorCode, String message) {
		return problem(errorCode, message, null);
	}

	private ProblemDetail problem(ErrorCode errorCode, String message, HttpServletRequest request) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), message);
		detail.setTitle(errorCode.getCode());
		if (request != null) {
			detail.setInstance(URI.create(request.getRequestURI()));
		}
		detail.setProperty("code", errorCode.getCode());
		detail.setProperty("timestamp", Instant.now());
		detail.setProperty("traceId", MDC.get("traceId"));
		return detail;
	}

	/** Violacion de validacion de un campo concreto. */
	public record FieldViolation(String field, String message, Object rejectedValue) {
	}

}
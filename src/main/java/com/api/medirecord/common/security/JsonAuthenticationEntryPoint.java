package com.api.medirecord.common.security;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.api.medirecord.common.error.ErrorCode;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde 401 en formato {@code ProblemDetail} cuando el token es ausente, invalido
 * o expira. Sustituye al {@code BearerTokenAuthenticationEntryPoint} por defecto para
 * que el formato de error sea homogeneo en toda la API.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final JsonMapper objectMapper;

	public JsonAuthenticationEntryPoint(JsonMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ErrorCode.UNAUTHENTICATED.getStatus(),
				ErrorCode.UNAUTHENTICATED.getDefaultMessage());
		problem.setTitle(ErrorCode.UNAUTHENTICATED.getCode());
		problem.setInstance(java.net.URI.create(request.getRequestURI()));
		problem.setProperty("code", ErrorCode.UNAUTHENTICATED.getCode());
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("traceId", MDC.get("traceId"));

		response.setStatus(ErrorCode.UNAUTHENTICATED.getStatus().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}

}
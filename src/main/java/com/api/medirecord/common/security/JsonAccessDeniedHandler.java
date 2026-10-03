package com.api.medirecord.common.security;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.api.medirecord.common.error.ErrorCode;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Responde 403 en formato {@code ProblemDetail} cuando el usuario no tiene permisos. */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final JsonMapper objectMapper;

	public JsonAccessDeniedHandler(JsonMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ErrorCode.FORBIDDEN.getStatus(),
				ErrorCode.FORBIDDEN.getDefaultMessage());
		problem.setTitle(ErrorCode.FORBIDDEN.getCode());
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", ErrorCode.FORBIDDEN.getCode());
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("traceId", MDC.get("traceId"));

		response.setStatus(ErrorCode.FORBIDDEN.getStatus().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}

}
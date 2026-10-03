package com.api.medirecord.common.security;

import java.util.Collection;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Acceso al contexto de seguridad desde cualquier capa sin depender de Spring Security
 * en las firmas (util para auditoria y servicios de dominio).
 */
public final class SecurityUtils {

	public static final String SYSTEM_AUDITOR = "system";
	private static final String ROLE_PREFIX = "ROLE_";

	private SecurityUtils() {
	}

	public static Optional<Authentication> currentAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		return Optional.of(authentication);
	}

	/** Identificador del usuario autenticado (claim {@code sub} del JWT) o vacio. */
	public static Optional<String> currentUsername() {
		return currentAuthentication().map(SecurityUtils::extractUsername);
	}

	/** Identificador del usuario autenticado o {@value #SYSTEM_AUDITOR}. */
	public static String currentUsernameOrSystem() {
		return currentUsername().orElse(SYSTEM_AUDITOR);
	}

	public static boolean hasRole(String role) {
		String required = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
		return currentAuthentication()
				.map(Authentication::getAuthorities)
				.stream()
				.flatMap(Collection::stream)
				.map(GrantedAuthority::getAuthority)
				.anyMatch(required::equals);
	}

	private static String extractUsername(Authentication authentication) {
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			var token = jwtAuthentication.getToken();
			return token.getSubject() != null ? token.getSubject() : jwtAuthentication.getName();
		}
		return authentication.getName();
	}

}
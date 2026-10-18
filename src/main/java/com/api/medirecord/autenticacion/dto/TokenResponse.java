package com.api.medirecord.autenticacion.dto;

import com.api.medirecord.autenticacion.keycloak.dto.KeycloakToken;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tokens de sesion devueltos por la API.
 *
 * <p>El access token debe enviarse como {@code Authorization: Bearer <token>} en
 * cualquier peticion protegida; el refresh token solo sirve para renovar la sesion
 * y no debe exponerse en almacenamiento del navegador junto al access token.</p>
 */
@Schema(name = "TokenResponse", description = "Tokens de acceso y renovacion de la sesion")
public record TokenResponse(

		@Schema(description = "Token de acceso (JWT)", example = "eyJhbGciOiJSUzI1NiJ9...")
		String accessToken,

		@Schema(example = "Bearer")
		String tokenType,

		@Schema(description = "Vida util del access token en segundos", example = "1800")
		long expiresIn,

		@Schema(description = "Vida util del refresh token en segundos", example = "1800")
		long refreshExpiresIn,

		@Schema(description = "Token para renovar la sesion", example = "eyJhbGciOiJSUzI1NiJ9...")
		String refreshToken) {

	public static TokenResponse from(KeycloakToken token) {
		String tipo = token.tokenType() == null || token.tokenType().isBlank()
				? "Bearer"
				: token.tokenType();
		return new TokenResponse(token.accessToken(), tipo, token.expiresIn(),
				token.refreshExpiresIn(), token.refreshToken());
	}

}

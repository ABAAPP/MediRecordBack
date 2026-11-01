package com.api.medirecord.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del inicio de sesion (B-12).
 */
@Schema(name = "LoginResponse", description = "Respuesta del inicio de sesion con token JWT")
public record LoginResponse(

		@Schema(description = "Indica si la autenticacion fue exitosa", example = "true")
		boolean success,

		@Schema(description = "Mensaje informativo del resultado", example = "Inicio de sesion exitoso")
		String message,

		@Schema(description = "Token de acceso Bearer JWT", example = "eyJhbGciOi...", nullable = true)
		String token,

		@Schema(description = "Correo del usuario autenticado", example = "admin@medirecord.local", nullable = true)
		String email) {

	public static LoginResponse ok(String token, String email) {
		return new LoginResponse(true, "Inicio de sesion exitoso", token, email);
	}

	public static LoginResponse failure(String message) {
		return new LoginResponse(false, message, null, null);
	}
}

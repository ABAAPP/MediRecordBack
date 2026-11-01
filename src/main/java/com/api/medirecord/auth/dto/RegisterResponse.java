package com.api.medirecord.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del endpoint de registro de usuario (B-12).
 */
@Schema(name = "RegisterResponse", description = "Respuesta del registro de nuevo usuario")
public record RegisterResponse(

		@Schema(description = "Indica si el registro fue exitoso", example = "true")
		boolean success,

		@Schema(description = "Mensaje descriptivo del resultado", example = "Usuario registrado exitosamente")
		String message) {

	public static RegisterResponse ok() {
		return new RegisterResponse(true, "Usuario registrado exitosamente");
	}

	public static RegisterResponse failure(String message) {
		return new RegisterResponse(false, message);
	}
}

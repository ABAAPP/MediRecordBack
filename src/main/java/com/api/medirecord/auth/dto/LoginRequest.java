package com.api.medirecord.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload de autenticacion para inicio de sesion (B-12).
 */
@Schema(name = "LoginRequest", description = "Credenciales para iniciar sesion desde la aplicacion movil o cliente web")
public record LoginRequest(

		@Schema(description = "Correo electronico del usuario", example = "admin@medirecord.local")
		@NotBlank(message = "El correo electronico es obligatorio")
		@Email(message = "El formato del correo electronico no es valido")
		String email,

		@Schema(description = "Contrasena de la cuenta", example = "admin123")
		@NotBlank(message = "La contrasena es obligatoria")
		String password) {
}

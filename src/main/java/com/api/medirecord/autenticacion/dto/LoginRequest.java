package com.api.medirecord.autenticacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload de entrada para iniciar sesion con correo y contrasena. */
@Schema(name = "LoginRequest", description = "Credenciales de acceso")
public record LoginRequest(

		@Schema(description = "Correo electronico registrado", example = "ana.gomez@correo.com")
		@NotBlank(message = "El correo electronico es obligatorio")
		@Email(message = "El correo electronico no tiene un formato valido")
		@Size(max = 150, message = "El correo electronico no debe exceder 150 caracteres")
		String email,

		@Schema(description = "Contrasena de acceso", example = "Secreto123")
		@NotBlank(message = "La contrasena es obligatoria")
		@Size(max = 128, message = "La contrasena no debe exceder 128 caracteres")
		String password) {
}

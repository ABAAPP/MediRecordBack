package com.api.medirecord.autenticacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload de entrada para renovar la sesion con un refresh token vigente. */
@Schema(name = "RefreshRequest", description = "Refresh token emitido en el inicio de sesion")
public record RefreshRequest(

		@Schema(description = "Refresh token", example = "eyJhbGciOi...")
		@NotBlank(message = "El refresh token es obligatorio")
		@Size(max = 8192, message = "El refresh token no es valido")
		String refreshToken) {
}

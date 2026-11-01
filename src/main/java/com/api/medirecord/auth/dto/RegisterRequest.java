package com.api.medirecord.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para registro de un nuevo usuario desde la aplicacion movil o cliente web (B-12).
 */
@Schema(name = "RegisterRequest", description = "Datos requeridos para el registro de un nuevo usuario")
public record RegisterRequest(

		@Schema(description = "Nombre completo del usuario", example = "Carlos Morales")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
		String name,

		@Schema(description = "Correo electronico unico", example = "carlos.morales@correo.com")
		@NotBlank(message = "El correo electronico es obligatorio")
		@Email(message = "El formato del correo electronico no es valido")
		String email,

		@Schema(description = "Contrasena de acceso", example = "ClaveSegura123")
		@NotBlank(message = "La contrasena es obligatoria")
		@Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
		String password) {
}

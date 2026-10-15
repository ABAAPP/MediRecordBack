package com.api.medirecord.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para crear o actualizar un usuario.
 *
 * <p>Las validaciones viven aqui (no en la entidad): la entidad representa el
 * modelo de datos, el DTO el contrato de entrada. {@code keycloakId} no forma
 * parte del contrato publico: lo gestiona la integracion con el proveedor de
 * identidad, no el cliente.</p>
 */
@Schema(name = "UsuarioRequest", description = "Datos para crear o actualizar un usuario")
public record UsuarioRequest(

		@Schema(description = "Nombre(s) del usuario", example = "Ana")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
		String nombre,

		@Schema(description = "Apellido paterno", example = "Gomez")
		@NotBlank(message = "El apellido paterno es obligatorio")
		@Size(max = 100, message = "El apellido paterno no debe exceder 100 caracteres")
		String apellidoPaterno,

		@Schema(description = "Apellido materno", example = "Lopez", nullable = true)
		@Size(max = 100, message = "El apellido materno no debe exceder 100 caracteres")
		String apellidoMaterno,

		@Schema(description = "Correo electronico", example = "ana.gomez@correo.com")
		@NotBlank(message = "El correo electronico es obligatorio")
		@Email(message = "El correo electronico no tiene un formato valido")
		@Size(max = 150, message = "El correo electronico no debe exceder 150 caracteres")
		String email,

		@Schema(description = "Telefono de contacto", example = "+52 664 123 4567")
		@Pattern(regexp = "^\\+?[0-9][0-9\\s-]{6,19}$", message = "El telefono no tiene un formato valido")
		String telefono) {
}

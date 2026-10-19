package com.api.medirecord.autenticacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para crear una cuenta de usuario.
 *
 * <p>Combinia los datos de perfil de {@code UsuarioRequest} con la contrasena de
 * acceso. La contrasena <b>nunca</b> se persiste en MediRecord: viaja al proveedor
 * de identidad (Keycloak), que es quien la almacena hasheada.</p>
 */
@Schema(name = "RegistroRequest", description = "Datos para registrar una cuenta de usuario")
public record RegistroRequest(

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

		@Schema(description = "Correo electronico (tambien es el usuario de acceso)", example = "ana.gomez@correo.com")
		@NotBlank(message = "El correo electronico es obligatorio")
		@Email(message = "El correo electronico no tiene un formato valido")
		@Size(max = 150, message = "El correo electronico no debe exceder 150 caracteres")
		String email,

		@Schema(description = "Telefono de contacto", example = "+52 664 123 4567", nullable = true)
		@Pattern(regexp = "^\\+?[0-9][0-9\\s-]{6,19}$", message = "El telefono no tiene un formato valido")
		String telefono,

		@Schema(description = "Contrasena de acceso", example = "Secreto123")
		@NotBlank(message = "La contrasena es obligatoria")
		@Size(min = 8, max = 128, message = "La contrasena debe tener entre 8 y 128 caracteres")
		String password) {
}

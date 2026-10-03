package com.api.medirecord.paciente.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para crear/actualizar un paciente.
 *
 * <p>Las validaciones viven aqui (no en la entidad): la entidad representa el
 * modelo de datos, el DTO el contrato de entrada.</p>
 */
@Schema(name = "PacienteRequest", description = "Datos para crear o actualizar un paciente")
public record PacienteRequest(

		@Schema(description = "Nombre(s) del paciente", example = "Ana")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 80, message = "El nombre no debe exceder 80 caracteres")
		String nombre,

		@Schema(description = "Apellido paterno", example = "Gomez")
		@NotBlank(message = "El apellido paterno es obligatorio")
		@Size(max = 80, message = "El apellido paterno no debe exceder 80 caracteres")
		String apellidoPaterno,

		@Schema(description = "Apellido materno", example = "Lopez", nullable = true)
		@Size(max = 80, message = "El apellido materno no debe exceder 80 caracteres")
		String apellidoMaterno,

		@Schema(description = "Fecha de nacimiento (ISO-8601)", example = "1990-05-14")
		@NotNull(message = "La fecha de nacimiento es obligatoria")
		@Past(message = "La fecha de nacimiento debe ser en el pasado")
		LocalDate fechaNacimiento,

		@Schema(description = "Correo electronico", example = "ana.gomez@correo.com")
		@NotBlank(message = "El correo electronico es obligatorio")
		@Email(message = "El correo electronico no tiene un formato valido")
		@Size(max = 120, message = "El correo electronico no debe exceder 120 caracteres")
		String email,

		@Schema(description = "Telefono de contacto", example = "+52 664 123 4567")
		@Pattern(regexp = "^\\+?[0-9][0-9\\s-]{6,19}$", message = "El telefono no tiene un formato valido")
		String telefono) {
}
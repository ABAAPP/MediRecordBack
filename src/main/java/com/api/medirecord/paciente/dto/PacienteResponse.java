package com.api.medirecord.paciente.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.api.medirecord.paciente.Paciente;

import io.swagger.v3.oas.annotations.media.Schema;

/** Representacion de salida de un paciente (nunca se expone la entidad). */
@Schema(name = "PacienteResponse", description = "Datos de un paciente registrado")
public record PacienteResponse(

		@Schema(example = "3f7c8b2e-1d4a-4c9e-9f1b-7a2c3d4e5f60") UUID id,
		@Schema(example = "Ana") String nombre,
		@Schema(example = "Gomez") String apellidoPaterno,
		@Schema(example = "Lopez") String apellidoMaterno,
		@Schema(example = "Ana Gomez Lopez") String nombreCompleto,
		@Schema(example = "1990-05-14") LocalDate fechaNacimiento,
		@Schema(example = "35") Integer edad,
		@Schema(example = "ana.gomez@correo.com") String email,
		@Schema(example = "+52 664 123 4567") String telefono,
		@Schema(example = "true") boolean activo,
		Instant createdAt,
		Instant updatedAt) {

	public static PacienteResponse from(Paciente paciente) {
		return new PacienteResponse(
				paciente.getId(),
				paciente.getNombre(),
				paciente.getApellidoPaterno(),
				paciente.getApellidoMaterno(),
				nombreCompleto(paciente),
				paciente.getFechaNacimiento(),
				edad(paciente.getFechaNacimiento()),
				paciente.getEmail(),
				paciente.getTelefono(),
				paciente.isActivo(),
				paciente.getCreatedAt(),
				paciente.getUpdatedAt());
	}

	private static String nombreCompleto(Paciente paciente) {
		return Stream.of(paciente.getNombre(), paciente.getApellidoPaterno(), paciente.getApellidoMaterno())
				.filter(part -> part != null && !part.isBlank())
				.collect(Collectors.joining(" "));
	}

	private static Integer edad(LocalDate fechaNacimiento) {
		if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
			return null;
		}
		return Period.between(fechaNacimiento, LocalDate.now()).getYears();
	}
}
package com.api.medirecord.toma.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Peticion para registrar o marcar una toma de medicamento como realizada (B-09).
 */
@Schema(name = "RegistrarTomaRequest", description = "Datos para registrar una toma realizada")
public record RegistrarTomaRequest(

		@Schema(description = "ID del medicamento correspondiente", example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d")
		@NotNull(message = "El id del medicamento es obligatorio")
		UUID medicamentoId,

		@Schema(description = "ID del horario correspondiente (opcional)", example = "9b2e4d6f-1a3c-5e7b-8d9f-0a1b2c3d4e5f", nullable = true)
		UUID horarioId,

		@Schema(description = "Fecha de la toma (por defecto hoy)", example = "2026-10-31", nullable = true)
		LocalDate fechaToma,

		@Schema(description = "Hora de la toma realizada", example = "08:05 AM", nullable = true)
		@Size(max = 20, message = "La hora de la toma no debe exceder 20 caracteres")
		String horaToma,

		@Schema(description = "Indica si la toma fue realizada (true) o revertida a pendiente (false)", example = "true", nullable = true)
		Boolean realizada,

		@Schema(description = "Notas u observaciones de la toma", example = "Tomado con abundante agua", nullable = true)
		@Size(max = 500, message = "Las notas no deben exceder 500 caracteres")
		String notas) {
}

package com.api.medirecord.horario.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para establecer o actualizar el horario y frecuencia de un
 * medicamento (B-07).
 */
@Schema(name = "HorarioRequest", description = "Datos para establecer el horario y frecuencia de un medicamento")
public record HorarioRequest(

		@Schema(description = "Identificador del medicamento al que pertenece el horario", example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d")
		@JsonAlias("medicationId")
		@NotNull(message = "El id del medicamento es obligatorio")
		UUID medicamentoId,

		@Schema(description = "Hora de administracion del medicamento", example = "08:00 AM")
		@JsonAlias("scheduledTime")
		@NotBlank(message = "La hora de administracion es obligatoria")
		@Pattern(
				regexp = "^([01]?\\d|2[0-3]):[0-5]\\d(\\s?(?i)(AM|PM))?$",
				message = "El formato de la hora de administracion no es valido (ejemplo: 08:00 AM o 14:30)")
		@Size(max = 20, message = "La hora de administracion no debe exceder 20 caracteres")
		String horaAdministracion,

		@Schema(description = "Frecuencia de administracion", example = "Cada 8 horas (3 veces al día)")
		@JsonAlias("frequency")
		@NotBlank(message = "La frecuencia es obligatoria")
		@Size(max = 100, message = "La frecuencia no debe exceder 100 caracteres")
		String frecuencia,

		@Schema(description = "Dias de la semana seleccionados (si se omite, aplica todos los dias)",
				example = "[\"Lunes\", \"Miércoles\", \"Viernes\"]", nullable = true)
		@JsonAlias("selectedDays")
		List<String> diasSemana,

		@Schema(description = "Indica si el recordatorio esta habilitado para este horario", example = "true", nullable = true)
		@JsonAlias("reminderEnabled")
		Boolean recordatorioActivo,

		@Schema(description = "Notas o indicaciones adicionales del horario", example = "Tomar con un vaso lleno de agua", nullable = true)
		@JsonAlias("notes")
		@Size(max = 500, message = "Las notas no deben exceder 500 caracteres")
		String notas) {
}

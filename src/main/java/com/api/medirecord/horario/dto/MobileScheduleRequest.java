package com.api.medirecord.horario.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada enviado por la aplicacion movil MediRecordFront
 * ({@code POST /api/schedules}).
 */
@Schema(name = "MobileScheduleRequest", description = "Configuracion de horario enviada desde la app movil")
public record MobileScheduleRequest(

		@Schema(description = "ID del medicamento existente", example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d")
		@NotBlank(message = "El id del medicamento es obligatorio")
		String medicationId,

		@Schema(description = "Nombre del medicamento", example = "Paracetamol", nullable = true)
		String medicationName,

		@Schema(description = "Hora programada de administracion", example = "08:00 AM")
		@NotBlank(message = "Debes indicar la hora programada")
		@Pattern(
				regexp = "^([01]?\\d|2[0-3]):[0-5]\\d(\\s?(?i)(AM|PM))?$",
				message = "El formato de la hora no es valido (ejemplo: 08:00 AM)")
		String scheduledTime,

		@Schema(description = "Frecuencia seleccionada", example = "Cada 8 horas (3 veces al día)")
		@NotBlank(message = "Debes seleccionar una frecuencia")
		@Size(max = 100, message = "La frecuencia no debe exceder 100 caracteres")
		String frequency,

		@Schema(description = "Dias de la semana seleccionados", nullable = true)
		List<String> selectedDays,

		@Schema(description = "Activar recordatorios", example = "true", nullable = true)
		Boolean reminderEnabled,

		@Schema(description = "Notas adicionales", example = "Con alimentos", nullable = true)
		@Size(max = 500, message = "Las notas no deben exceder 500 caracteres")
		String notes) {
}

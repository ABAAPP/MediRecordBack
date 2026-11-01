package com.api.medirecord.toma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Peticion enviada por la aplicacion movil MediRecordFront para marcar
 * un recordatorio como completado ({@code POST /api/reminders/{id}/complete}).
 */
@Schema(name = "CompleteReminderRequest", description = "Datos para marcar un recordatorio como completado en la app")
public record CompleteReminderRequest(
		@Schema(description = "ID del recordatorio", example = "1")
		String reminderId,

		@Schema(description = "Hora de cumplimiento de la toma", example = "08:05 AM")
		@NotBlank(message = "La hora de realizacion es obligatoria")
		String completedAt,

		@Schema(description = "Correo o identificador del usuario opcional", example = "usuario@correo.com", nullable = true)
		String userEmail) {
}

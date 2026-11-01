package com.api.medirecord.toma.dto;

import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta devuelta a la aplicacion movil MediRecordFront al marcar un
 * recordatorio como completado.
 */
@Schema(name = "CompleteReminderResponse", description = "Respuesta de confirmacion de recordatorio completado")
public record CompleteReminderResponse(
		boolean success,
		String message,
		String reminderId,
		String completedAt,
		MobileMedicationReminderDto updatedReminder) {
}

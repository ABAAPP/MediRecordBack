package com.api.medirecord.recordatorio.dto;

import java.util.List;
import java.util.UUID;

import com.api.medirecord.horario.Horario;
import com.api.medirecord.medicamento.Medicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida consumida por la aplicacion movil MediRecordFront
 * ({@code GET /api/reminders}).
 */
@Schema(name = "MobileMedicationReminderDto", description = "Recordatorio de medicamento consumido por la app movil")
public record MobileMedicationReminderDto(
		String id,
		String medicationId,
		String medicationName,
		String dosage,
		String scheduledTime,
		String frequency,
		List<String> selectedDays,
		String instructions,
		boolean isCompleted,
		String userEmail,
		String completedAt) {

	public static MobileMedicationReminderDto from(Horario horario) {
		Medicamento med = horario.getMedicamento();
		String id = horario.getId() != null
				? horario.getId().toString()
				: (med != null && med.getId() != null ? med.getId().toString() : UUID.randomUUID().toString());
		String medId = (med != null && med.getId() != null)
				? med.getId().toString()
				: id;
		String instrucciones = (horario.getNotas() != null && !horario.getNotas().isBlank())
				? horario.getNotas()
				: (med != null ? med.getInstrucciones() : null);

		return new MobileMedicationReminderDto(
				id,
				medId,
				med != null ? med.getNombre() : "",
				med != null && med.getDosis() != null ? med.getDosis() : "",
				horario.getHoraAdministracion(),
				horario.getFrecuencia(),
				horario.getDiasSeleccionados(),
				instrucciones,
				false,
				med != null ? med.getUsuarioId() : null,
				null);
	}

}

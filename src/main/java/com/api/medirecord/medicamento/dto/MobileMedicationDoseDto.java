package com.api.medirecord.medicamento.dto;

import java.util.List;

import com.api.medirecord.medicamento.Medicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de una dosis/medicamento para la aplicacion movil MediRecordFront.
 */
@Schema(name = "MobileMedicationDoseDto", description = "Detalle del medicamento registrado para la app movil")
public record MobileMedicationDoseDto(
		String id,
		String name,
		String dosage,
		String scheduledTime,
		boolean isCompleted,
		String instructions,
		String frequency,
		List<String> selectedDays,
		String userEmail,
		String completedAt,
		String scheduledDate) {

	private static final List<String> ALL_WEEK_DAYS = List.of(
			"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

	public static MobileMedicationDoseDto from(Medicamento medicamento) {
		return new MobileMedicationDoseDto(
				medicamento.getId() != null ? medicamento.getId().toString() : null,
				medicamento.getNombre(),
				medicamento.getDosis() != null ? medicamento.getDosis() : "",
				medicamento.getHoraProgramada() != null ? medicamento.getHoraProgramada() : "08:00 AM",
				false,
				medicamento.getInstrucciones(),
				medicamento.getFrecuencia(),
				ALL_WEEK_DAYS,
				medicamento.getUsuarioId(),
				null,
				medicamento.getFechaInicio() != null ? medicamento.getFechaInicio().toString() : null);
	}

}

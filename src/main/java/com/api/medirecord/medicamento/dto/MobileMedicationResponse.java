package com.api.medirecord.medicamento.dto;

import com.api.medirecord.medicamento.Medicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta devuelta al cliente movil MediRecordFront tras registrar un medicamento.
 */
@Schema(name = "MobileMedicationResponse", description = "Respuesta de registro exitoso de medicamento para la app movil")
public record MobileMedicationResponse(
		boolean success,
		String message,
		String medicationId,
		MobileMedicationDoseDto medication) {

	public static MobileMedicationResponse from(Medicamento medicamento) {
		String id = medicamento.getId() != null ? medicamento.getId().toString() : null;
		return new MobileMedicationResponse(
				true,
				"Medicamento '" + medicamento.getNombre() + "' registrado exitosamente",
				id,
				MobileMedicationDoseDto.from(medicamento));
	}

}

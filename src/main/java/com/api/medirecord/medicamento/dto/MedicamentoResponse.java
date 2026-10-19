package com.api.medirecord.medicamento.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.api.medirecord.medicamento.Medicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/** Representacion de salida de un medicamento (nunca se expone la entidad). */
@Schema(name = "MedicamentoResponse", description = "Medicamento registrado por el usuario autenticado")
public record MedicamentoResponse(

		@Schema(example = "8f2c1a94-6b3e-4d7a-a1c5-9e0b2f4d6c81") UUID id,
		@Schema(example = "Paracetamol") String nombre,
		@Schema(example = "500 mg") String dosis,
		@Schema(example = "Cada 8 horas") String frecuencia,
		@Schema(example = "2026-10-01") LocalDate fechaInicio,
		@Schema(example = "2026-10-15") LocalDate fechaFin,
		@Schema(example = "true") boolean enTratamiento,
		@Schema(example = "true") boolean activo,
		Instant createdAt,
		Instant updatedAt) {

	public static MedicamentoResponse from(Medicamento medicamento) {
		return new MedicamentoResponse(
				medicamento.getId(),
				medicamento.getNombre(),
				medicamento.getDosis(),
				medicamento.getFrecuencia(),
				medicamento.getFechaInicio(),
				medicamento.getFechaFin(),
				medicamento.getFechaFin() == null,
				medicamento.isActivo(),
				medicamento.getCreatedAt(),
				medicamento.getUpdatedAt());
	}
}

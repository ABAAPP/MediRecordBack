package com.api.medirecord.toma.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.api.medirecord.toma.TomaMedicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida del registro de una toma de medicamento (B-09).
 */
@Schema(name = "TomaResponse", description = "Datos de una toma de medicamento registrada")
public record TomaResponse(
		@Schema(example = "3c5e7b8d-9f0a-1b2c-3d4e-5f6a7b8c9d0e") UUID id,
		@Schema(example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d") UUID medicamentoId,
		@Schema(example = "Paracetamol") String medicamentoNombre,
		@Schema(example = "9b2e4d6f-1a3c-5e7b-8d9f-0a1b2c3d4e5f", nullable = true) UUID horarioId,
		@Schema(example = "usuario-123") String usuarioId,
		@Schema(example = "2026-10-31") LocalDate fechaToma,
		@Schema(example = "08:05 AM") String horaToma,
		@Schema(example = "true") boolean realizada,
		@Schema(example = "Tomado con alimentos", nullable = true) String notas,
		Instant createdAt) {

	public static TomaResponse from(TomaMedicamento toma) {
		return new TomaResponse(
				toma.getId(),
				toma.getMedicamento() != null ? toma.getMedicamento().getId() : null,
				toma.getMedicamento() != null ? toma.getMedicamento().getNombre() : null,
				toma.getHorario() != null ? toma.getHorario().getId() : null,
				toma.getUsuarioId(),
				toma.getFechaToma(),
				toma.getHoraToma(),
				toma.isRealizada(),
				toma.getNotas(),
				toma.getCreatedAt());
	}

}

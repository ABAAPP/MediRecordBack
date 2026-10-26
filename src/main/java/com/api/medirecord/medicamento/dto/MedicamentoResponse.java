package com.api.medirecord.medicamento.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.api.medirecord.medicamento.Medicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/** Representacion de salida de un medicamento registrado por el usuario. */
@Schema(name = "MedicamentoResponse", description = "Datos de un medicamento asociado a la cuenta del usuario")
public record MedicamentoResponse(

		@Schema(example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d") UUID id,
		@Schema(description = "Identificador del usuario propietario", example = "c28dc3d0-56ca-4fda-99f5-05afbe24ae4c") String usuarioId,
		@Schema(example = "Paracetamol") String nombre,
		@Schema(example = "500 mg - 1 tableta") String dosis,
		@Schema(example = "Cada 8 horas") String frecuencia,
		@Schema(example = "Tomar con alimentos") String instrucciones,
		@Schema(example = "2026-10-25") LocalDate fechaInicio,
		@Schema(example = "2026-11-01", nullable = true) LocalDate fechaFin,
		@Schema(description = "Indica si el tratamiento sigue vigente en la fecha actual", example = "true") boolean vigente,
		@Schema(example = "true") boolean activo,
		Instant createdAt,
		Instant updatedAt) {

	public static MedicamentoResponse from(Medicamento medicamento) {
		return new MedicamentoResponse(
				medicamento.getId(),
				medicamento.getUsuarioId(),
				medicamento.getNombre(),
				medicamento.getDosis(),
				medicamento.getFrecuencia(),
				medicamento.getInstrucciones(),
				medicamento.getFechaInicio(),
				medicamento.getFechaFin(),
				esVigente(medicamento),
				medicamento.isActivo(),
				medicamento.getCreatedAt(),
				medicamento.getUpdatedAt());
	}

	private static boolean esVigente(Medicamento medicamento) {
		if (!medicamento.isActivo() || medicamento.getFechaInicio() == null) {
			return false;
		}
		LocalDate hoy = LocalDate.now();
		return medicamento.getFechaFin() == null || !medicamento.getFechaFin().isBefore(hoy);
	}

}

package com.api.medirecord.horario.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.api.medirecord.horario.Horario;
import com.api.medirecord.medicamento.Medicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida de un horario de administracion, lista para ser
 * consumida por el cliente o por el servicio de recordatorios (B-07).
 */
@Schema(name = "HorarioResponse", description = "Datos de un horario y frecuencia asociados a un medicamento")
public record HorarioResponse(

		@Schema(example = "9b2e4d6f-1a3c-5e7b-8d9f-0a1b2c3d4e5f") UUID id,
		@Schema(example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d") UUID medicamentoId,
		@Schema(example = "Paracetamol") String medicamentoNombre,
		@Schema(example = "500 mg - 1 tableta") String medicamentoDosis,
		@Schema(example = "08:00 AM") String horaAdministracion,
		@Schema(example = "Cada 8 horas (3 veces al día)") String frecuencia,
		@Schema(example = "[\"Lunes\", \"Martes\", \"Miércoles\", \"Jueves\", \"Viernes\", \"Sábado\", \"Domingo\"]") List<String> diasSemana,
		@Schema(example = "true") boolean recordatorioActivo,
		@Schema(example = "Tomar con alimentos", nullable = true) String notas,
		@Schema(example = "true") boolean activo,
		Instant createdAt,
		Instant updatedAt) {

	public static HorarioResponse from(Horario horario) {
		Medicamento med = horario.getMedicamento();
		return new HorarioResponse(
				horario.getId(),
				med != null ? med.getId() : null,
				med != null ? med.getNombre() : null,
				med != null ? med.getDosis() : null,
				horario.getHoraAdministracion(),
				horario.getFrecuencia(),
				horario.getDiasSeleccionados(),
				horario.isRecordatorioActivo(),
				horario.getNotas(),
				horario.isActivo(),
				horario.getCreatedAt(),
				horario.getUpdatedAt());
	}

}

package com.api.medirecord.recordatorio.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.api.medirecord.horario.Horario;
import com.api.medirecord.medicamento.Medicamento;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida de un recordatorio para el usuario autenticado (B-08).
 *
 * <p>Consolida la informacion del medicamento y el horario aplicable para una
 * fecha determinada.</p>
 */
@Schema(name = "RecordatorioResponse", description = "Datos del recordatorio de toma de medicamento")
public record RecordatorioResponse(

		@Schema(example = "9b2e4d6f-1a3c-5e7b-8d9f-0a1b2c3d4e5f") UUID horarioId,
		@Schema(example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d") UUID medicamentoId,
		@Schema(example = "Paracetamol") String medicamentoNombre,
		@Schema(example = "500 mg - 1 tableta") String dosis,
		@Schema(example = "08:00 AM") String horaAdministracion,
		@Schema(example = "Cada 8 horas (3 veces al día)") String frecuencia,
		@Schema(example = "[\"Lunes\", \"Martes\", \"Miércoles\", \"Jueves\", \"Viernes\", \"Sábado\", \"Domingo\"]") List<String> diasSemana,
		@Schema(example = "Tomar con alimentos", nullable = true) String instrucciones,
		@Schema(example = "2026-10-31") LocalDate fechaConsultada,
		@Schema(example = "true") boolean recordatorioActivo,
		@Schema(example = "usuario-123") String usuarioId) {

	public static RecordatorioResponse from(Horario horario, LocalDate fechaConsultada) {
		Medicamento med = horario.getMedicamento();
		String instrucciones = (horario.getNotas() != null && !horario.getNotas().isBlank())
				? horario.getNotas()
				: (med != null ? med.getInstrucciones() : null);

		return new RecordatorioResponse(
				horario.getId(),
				med != null ? med.getId() : null,
				med != null ? med.getNombre() : null,
				med != null ? med.getDosis() : null,
				horario.getHoraAdministracion(),
				horario.getFrecuencia(),
				horario.getDiasSeleccionados(),
				instrucciones,
				fechaConsultada,
				horario.isRecordatorioActivo(),
				med != null ? med.getUsuarioId() : null);
	}

}

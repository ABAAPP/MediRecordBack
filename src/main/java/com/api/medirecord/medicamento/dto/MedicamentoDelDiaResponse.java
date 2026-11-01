package com.api.medirecord.medicamento.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida para los medicamentos programados del dia (B-10).
 *
 * <p>Muestra el medicamento asociado a su horario y especifica si la toma se encuentra
 * en estado pendiente o realizada con su respectiva hora de ejecucion.</p>
 */
@Schema(name = "MedicamentoDelDiaResponse", description = "Detalle de un medicamento programado para el dia actual con su estado de toma")
public record MedicamentoDelDiaResponse(
		@Schema(example = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d") UUID medicamentoId,
		@Schema(example = "Paracetamol") String nombre,
		@Schema(example = "500 mg - 1 tableta") String dosis,
		@Schema(example = "9b2e4d6f-1a3c-5e7b-8d9f-0a1b2c3d4e5f", nullable = true) UUID horarioId,
		@Schema(example = "08:00 AM") String horaProgramada,
		@Schema(example = "Cada 8 horas (3 veces al día)") String frecuencia,
		@Schema(example = "[\"Lunes\", \"Martes\", \"Miércoles\", \"Jueves\", \"Viernes\", \"Sábado\", \"Domingo\"]") List<String> diasSemana,
		@Schema(example = "Tomar con alimentos", nullable = true) String instrucciones,
		@Schema(example = "2026-10-31") LocalDate fecha,
		@Schema(example = "true") boolean realizada,
		@Schema(example = "08:05 AM", nullable = true) String horaRealizada,
		@Schema(example = "REALIZADA", allowableValues = { "REALIZADA", "PENDIENTE" }) String estado,
		@Schema(example = "usuario-123") String usuarioId) {

	public static MedicamentoDelDiaResponse de(
			UUID medicamentoId,
			String nombre,
			String dosis,
			UUID horarioId,
			String horaProgramada,
			String frecuencia,
			List<String> diasSemana,
			String instrucciones,
			LocalDate fecha,
			boolean realizada,
			String horaRealizada,
			String usuarioId) {
		String estado = realizada ? "REALIZADA" : "PENDIENTE";
		return new MedicamentoDelDiaResponse(
				medicamentoId,
				nombre,
				dosis,
				horarioId,
				horaProgramada,
				frecuencia,
				diasSemana,
				instrucciones,
				fecha,
				realizada,
				horaRealizada,
				estado,
				usuarioId);
	}

}

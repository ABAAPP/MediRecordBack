package com.api.medirecord.medicamento.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para registrar o actualizar un medicamento del usuario.
 */
@Schema(name = "MedicamentoRequest", description = "Datos para registrar o actualizar un medicamento")
public record MedicamentoRequest(

		@Schema(description = "Nombre del medicamento", example = "Paracetamol")
		@NotBlank(message = "El nombre del medicamento es obligatorio")
		@Size(max = 120, message = "El nombre del medicamento no debe exceder 120 caracteres")
		String nombre,

		@Schema(description = "Dosis indicada", example = "500 mg - 1 tableta", nullable = true)
		@Size(max = 100, message = "La dosis no debe exceder 100 caracteres")
		String dosis,

		@Schema(description = "Frecuencia de toma", example = "Cada 8 horas", nullable = true)
		@Size(max = 100, message = "La frecuencia no debe exceder 100 caracteres")
		String frecuencia,

		@Schema(description = "Instrucciones adicionales del tratamiento", example = "Tomar con alimentos", nullable = true)
		@Size(max = 500, message = "Las instrucciones no deben exceder 500 caracteres")
		String instrucciones,

		@Schema(description = "Fecha de inicio del tratamiento (ISO-8601)", example = "2026-10-25")
		@NotNull(message = "La fecha de inicio es obligatoria")
		LocalDate fechaInicio,

		@Schema(description = "Fecha de finalizacion del tratamiento (opcional, ISO-8601)", example = "2026-11-01", nullable = true)
		LocalDate fechaFin) {
}

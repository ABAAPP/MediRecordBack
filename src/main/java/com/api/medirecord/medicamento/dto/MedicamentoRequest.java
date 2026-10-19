package com.api.medirecord.medicamento.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para crear/actualizar un medicamento.
 *
 * <p>El usuario que lo recibe NO viaja en el body: se resuelve del token JWT,
 * de modo que nadie puede registrar medicamentos a nombre de otra persona. Las
 * validaciones viven aqui (no en la entidad): la entidad representa el modelo
 * de datos, el DTO el contrato de entrada.</p>
 */
@Schema(name = "MedicamentoRequest", description = "Datos para crear o actualizar un medicamento propio")
public record MedicamentoRequest(

		@Schema(description = "Nombre del medicamento", example = "Paracetamol")
		@NotBlank(message = "El nombre del medicamento es obligatorio")
		@Size(max = 120, message = "El nombre del medicamento no debe exceder 120 caracteres")
		String nombre,

		@Schema(description = "Dosis", example = "500 mg", nullable = true)
		@Size(max = 80, message = "La dosis no debe exceder 80 caracteres")
		String dosis,

		@Schema(description = "Frecuencia de administracion", example = "Cada 8 horas", nullable = true)
		@Size(max = 80, message = "La frecuencia no debe exceder 80 caracteres")
		String frecuencia,

		@Schema(description = "Fecha de inicio del tratamiento (ISO-8601)", example = "2026-10-01")
		@NotNull(message = "La fecha de inicio es obligatoria")
		LocalDate fechaInicio,

		@Schema(description = "Fecha de finalizacion del tratamiento; omitir si continua vigente (ISO-8601)",
				example = "2026-10-15", nullable = true)
		LocalDate fechaFin) {
}

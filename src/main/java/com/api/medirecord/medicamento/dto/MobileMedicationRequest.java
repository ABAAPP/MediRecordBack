package com.api.medirecord.medicamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada compatible con el cliente movil MediRecordFront
 * ({@code POST /api/medications}).
 */
@Schema(name = "MobileMedicationRequest", description = "Datos enviados por la aplicacion movil para registrar un medicamento")
public record MobileMedicationRequest(

		@Schema(description = "Nombre del medicamento", example = "Paracetamol")
		@NotBlank(message = "El nombre del medicamento es obligatorio")
		@Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
		String name,

		@Schema(description = "Dosis o presentacion", example = "500 mg - 1 tableta")
		@NotBlank(message = "La dosis o presentacion es obligatoria")
		@Size(max = 100, message = "La dosis no debe exceder 100 caracteres")
		String dosage,

		@Schema(description = "Fecha de inicio (ISO yyyy-MM-dd o dd/MM/yyyy)", example = "25/10/2026")
		@NotBlank(message = "La fecha de inicio es obligatoria")
		String startDate,

		@Schema(description = "Fecha de finalizacion opcional (ISO yyyy-MM-dd o dd/MM/yyyy)", example = "01/11/2026", nullable = true)
		String endDate,

		@Schema(description = "Frecuencia de horario", example = "Cada 8 horas (3 veces al día)")
		@NotBlank(message = "Debes seleccionar una frecuencia de horario")
		@Size(max = 100, message = "La frecuencia no debe exceder 100 caracteres")
		String frequency,

		@Schema(description = "Hora programada de la toma", example = "08:00 AM")
		@NotBlank(message = "Debes indicar la hora de la toma")
		@Pattern(
				regexp = "^([01]?\\d|2[0-3]):[0-5]\\d(\\s?(?i)(AM|PM))?$",
				message = "El formato de la hora no es valido (ejemplo: 08:00 AM)")
		String scheduledTime,

		@Schema(description = "Instrucciones adicionales", example = "Con alimentos", nullable = true)
		@Size(max = 500, message = "Las instrucciones no deben exceder 500 caracteres")
		String instructions,

		@Schema(description = "Correo del usuario (opcional, se prioriza el token JWT)", example = "ana.gomez@correo.com", nullable = true)
		String userEmail) {
}

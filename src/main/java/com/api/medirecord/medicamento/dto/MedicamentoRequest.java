package com.api.medirecord.medicamento.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para registrar o actualizar un medicamento del usuario (B-06).
 *
 * <p>Incluye validaciones de formato, longitud y coherencia entre la fecha de
 * inicio y la fecha de finalizacion.</p>
 */
@Schema(name = "MedicamentoRequest", description = "Datos para registrar o actualizar un medicamento desde la aplicacion")
public record MedicamentoRequest(

		@Schema(description = "Nombre del medicamento", example = "Paracetamol")
		@JsonAlias("name")
		@NotBlank(message = "El nombre del medicamento es obligatorio")
		@Size(min = 2, max = 120, message = "El nombre del medicamento debe tener entre 2 y 120 caracteres")
		String nombre,

		@Schema(description = "Dosis o presentacion indicada", example = "500 mg - 1 tableta", nullable = true)
		@JsonAlias("dosage")
		@Size(max = 100, message = "La dosis no debe exceder 100 caracteres")
		String dosis,

		@Schema(description = "Frecuencia de toma", example = "Cada 8 horas", nullable = true)
		@JsonAlias("frequency")
		@Size(max = 100, message = "La frecuencia no debe exceder 100 caracteres")
		String frecuencia,

		@Schema(description = "Hora programada de la toma (formato 24h o 12h AM/PM)", example = "08:00 AM", nullable = true)
		@JsonAlias("scheduledTime")
		@Pattern(
				regexp = "^$|^([01]?\\d|2[0-3]):[0-5]\\d(\\s?(?i)(AM|PM))?$",
				message = "El formato de la hora programada no es valido (ejemplo: 08:00 AM o 14:30)")
		@Size(max = 20, message = "La hora programada no debe exceder 20 caracteres")
		String horaProgramada,

		@Schema(description = "Instrucciones adicionales del tratamiento", example = "Tomar con alimentos", nullable = true)
		@JsonAlias("instructions")
		@Size(max = 500, message = "Las instrucciones no deben exceder 500 caracteres")
		String instrucciones,

		@Schema(description = "Fecha de inicio del tratamiento (ISO-8601)", example = "2026-10-25")
		@JsonAlias("startDate")
		@NotNull(message = "La fecha de inicio es obligatoria")
		LocalDate fechaInicio,

		@Schema(description = "Fecha de finalizacion del tratamiento (opcional, ISO-8601)", example = "2026-11-01", nullable = true)
		@JsonAlias("endDate")
		LocalDate fechaFin) {

	public MedicamentoRequest(
			String nombre,
			String dosis,
			String frecuencia,
			String instrucciones,
			LocalDate fechaInicio,
			LocalDate fechaFin) {
		this(nombre, dosis, frecuencia, null, instrucciones, fechaInicio, fechaFin);
	}

	@JsonIgnore
	@AssertTrue(message = "La fecha de finalizacion no puede ser anterior a la fecha de inicio")
	public boolean isFechaFinValida() {
		if (fechaInicio == null || fechaFin == null) {
			return true;
		}
		return !fechaFin.isBefore(fechaInicio);
	}

}

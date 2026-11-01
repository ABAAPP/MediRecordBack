package com.api.medirecord.recordatorio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints consumidos directamente por la aplicacion movil MediRecordFront
 * para consultar recordatorios y medicamentos programados (B-08).
 */
@RestController
@Tag(name = "Recordatorios (App Movil)", description = "Endpoints consumidos por la app movil para alarmas y recordatorios")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class MobileReminderController {

	private static final DateTimeFormatter FORMATO_DIA_MES_ANIO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final RecordatorioService service;

	public MobileReminderController(RecordatorioService service) {
		this.service = service;
	}

	@Operation(
			summary = "Consulta los recordatorios del usuario para alarmas de la app movil",
			description = "Devuelve los recordatorios con hora de toma, dias configurados y notas para programar notificaciones.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de recordatorios obtenida"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping("/api/reminders")
	public List<MobileMedicationReminderDto> getReminders(
			@RequestParam(required = false) String userEmail) {
		return service.consultarRecordatoriosMovil(userEmail);
	}

	@Operation(
			summary = "Consulta las tomas programadas para hoy o una fecha especifica en la app movil",
			description = "Filtra medicamentos vigentes y dias seleccionados para la fecha indicada.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de dosis obtenida"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping("/api/medications/today")
	public List<MobileMedicationDoseDto> getTodayMedications(
			@RequestParam(required = false) String date,
			@RequestParam(required = false) String userEmail) {
		LocalDate fecha = parsearFechaOpcional(date);
		return service.consultarDosisFechaMovil(fecha, userEmail);
	}

	private static LocalDate parsearFechaOpcional(String valor) {
		if (valor == null || valor.isBlank()) {
			return LocalDate.now();
		}
		String limpio = valor.trim();
		try {
			if (limpio.contains("/")) {
				return LocalDate.parse(limpio, FORMATO_DIA_MES_ANIO);
			}
			return LocalDate.parse(limpio);
		} catch (DateTimeParseException ex) {
			return LocalDate.now();
		}
	}

}

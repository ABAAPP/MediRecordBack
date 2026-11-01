package com.api.medirecord.recordatorio;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;
import com.api.medirecord.toma.TomaService;
import com.api.medirecord.toma.dto.CompleteReminderRequest;
import com.api.medirecord.toma.dto.CompleteReminderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints consumidos directamente por la aplicacion movil MediRecordFront
 * bajo {@code /api/reminders/**} (B-08 y B-09).
 */
@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Recordatorios (App Movil)", description = "Endpoints consumidos por la app movil para alarmas y cumplimiento de tomas")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class MobileReminderController {

	private final RecordatorioService recordatorioService;
	private final TomaService tomaService;

	public MobileReminderController(RecordatorioService recordatorioService, TomaService tomaService) {
		this.recordatorioService = recordatorioService;
		this.tomaService = tomaService;
	}

	@Operation(
			summary = "Consulta los recordatorios del usuario para alarmas de la app movil (B-08)",
			description = "Devuelve los recordatorios con hora de toma, dias configurados y estado de cumplimiento.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de recordatorios obtenida"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping
	public List<MobileMedicationReminderDto> getReminders(
			@RequestParam(required = false) String userEmail) {
		return recordatorioService.consultarRecordatoriosMovil(userEmail);
	}

	@Operation(
			summary = "Marca un recordatorio de toma como realizado (B-09)",
			description = "Registra que el usuario tomo su medicamento a la hora especificada.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Recordatorio marcado como realizado"),
			@ApiResponse(responseCode = "401", description = "No autenticado"),
			@ApiResponse(responseCode = "403", description = "No tiene permiso para modificar medicamentos de otro usuario")
	})
	@PostMapping("/{id}/complete")
	public CompleteReminderResponse markReminderAsCompleted(
			@PathVariable("id") String reminderId,
			@Valid @RequestBody(required = false) CompleteReminderRequest request) {
		return tomaService.marcarRecordatorioCompletado(reminderId, request);
	}

}

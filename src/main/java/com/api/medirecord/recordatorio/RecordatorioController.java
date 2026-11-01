package com.api.medirecord.recordatorio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.medirecord.common.response.ApiResponse;
import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.recordatorio.dto.RecordatorioResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints REST para consultar recordatorios correspondientes a los horarios
 * y tratamientos del usuario autenticado (B-08).
 */
@RestController
@RequestMapping("/api/v1/recordatorios")
@Tag(name = "Recordatorios", description = "Consulta y planificacion de recordatorios para toma de medicamentos")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class RecordatorioController {

	private final RecordatorioService service;

	public RecordatorioController(RecordatorioService service) {
		this.service = service;
	}

	@Operation(
			summary = "Consulta los recordatorios correspondientes a la fecha indicada",
			description = "Filtra por los horarios registrados del usuario, validando vigencia del tratamiento y dia de la semana.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de recordatorios obtenida exitosamente"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping
	public ApiResponse<List<RecordatorioResponse>> listar(
			@Parameter(description = "Fecha a consultar (formato ISO YYYY-MM-DD). Si se omite, se usa la fecha actual.")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return ApiResponse.ok(service.consultarRecordatorios(fecha));
	}

	@Operation(summary = "Obtiene los recordatorios correspondientes al dia de hoy")
	@GetMapping("/hoy")
	public ApiResponse<List<RecordatorioResponse>> listarHoy() {
		return ApiResponse.ok(service.consultarRecordatorios(LocalDate.now()));
	}

}

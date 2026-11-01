package com.api.medirecord.toma;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.api.medirecord.common.response.ApiResponse;
import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.toma.dto.RegistrarTomaRequest;
import com.api.medirecord.toma.dto.TomaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints REST para el registro y consulta de tomas realizadas (B-09).
 */
@RestController
@RequestMapping("/api/v1/tomas")
@Tag(name = "Tomas de Medicamentos", description = "Registro y administracion de tomas realizadas y pendientes")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class TomaController {

	private static final String MODULO = "Toma de medicamento";

	private final TomaService tomaService;

	public TomaController(TomaService tomaService) {
		this.tomaService = tomaService;
	}

	@Operation(
			summary = "Registra una toma realizada de medicamento (B-09)",
			description = "Registra o actualiza el estado de una toma como realizada, asociandola al usuario autenticado.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Toma registrada exitosamente"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de toma invalidos"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permiso para modificar medicamentos de otro usuario"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Medicamento no encontrado")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<TomaResponse>> registrarToma(@Valid @RequestBody RegistrarTomaRequest request) {
		TomaResponse response = tomaService.registrarToma(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(ApiResponse.ok(MODULO + " registrada exitosamente", response));
	}

	@Operation(summary = "Consulta las tomas registradas para una fecha determinada")
	@GetMapping
	public ApiResponse<List<TomaResponse>> consultarTomas(
			@Parameter(description = "Fecha a consultar (formato ISO YYYY-MM-DD). Si se omite, se usa la fecha actual.")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return ApiResponse.ok(tomaService.consultarTomasFecha(fecha));
	}

	@Operation(summary = "Alterna el estado de una toma (realizada <-> pendiente) para la fecha actual")
	@PostMapping("/{id}/alternar")
	public ApiResponse<MobileMedicationDoseDto> alternarEstado(@PathVariable String id) {
		return ApiResponse.ok("Estado de toma actualizado", tomaService.alternarEstadoToma(id));
	}

}

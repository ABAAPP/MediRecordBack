package com.api.medirecord.horario;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.api.medirecord.common.response.ApiResponse;
import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.horario.dto.HorarioRequest;
import com.api.medirecord.horario.dto.HorarioResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints REST para establecer y consultar horarios y frecuencias de
 * medicamentos (B-07).
 */
@RestController
@RequestMapping("/api/v1/horarios")
@Tag(name = "Horarios", description = "Configuracion de horarios y frecuencias de administracion de medicamentos")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class HorarioController {

	private static final String MODULO = "Horario";

	private final HorarioService service;

	public HorarioController(HorarioService service) {
		this.service = service;
	}

	@Operation(summary = "Registra un horario y frecuencia para un medicamento del usuario")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Horario registrado"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos del horario invalidos"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El medicamento pertenece a otro usuario"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El medicamento indicado no existe")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<HorarioResponse>> crear(@Valid @RequestBody HorarioRequest request) {
		HorarioResponse created = service.crear(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(ApiResponse.ok(MODULO + " registrado", created));
	}

	@Operation(summary = "Lista los horarios activos de un medicamento especifico")
	@GetMapping
	public ApiResponse<List<HorarioResponse>> listarPorMedicamento(
			@Parameter(description = "ID del medicamento a consultar", required = true)
			@RequestParam UUID medicamentoId) {
		return ApiResponse.ok(service.listarPorMedicamento(medicamentoId));
	}

	@Operation(summary = "Obtiene todos los horarios activos disponibles para el servicio de recordatorios")
	@GetMapping("/recordatorios")
	public ApiResponse<List<HorarioResponse>> listarParaRecordatorios() {
		return ApiResponse.ok(service.obtenerHorariosParaRecordatorios());
	}

	@Operation(summary = "Obtiene un horario por su identificador")
	@GetMapping("/{id}")
	public ApiResponse<HorarioResponse> obtener(@PathVariable UUID id) {
		return ApiResponse.ok(service.obtener(id));
	}

	@Operation(summary = "Actualiza la hora, frecuencia o dias de un horario")
	@PutMapping("/{id}")
	public ApiResponse<HorarioResponse> actualizar(@PathVariable UUID id,
			@Valid @RequestBody HorarioRequest request) {
		return ApiResponse.ok(MODULO + " actualizado", service.actualizar(id, request));
	}

	@Operation(summary = "Desactiva un horario (baja logica)")
	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

	@Operation(summary = "Elimina un horario (alias de baja logica)")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

}

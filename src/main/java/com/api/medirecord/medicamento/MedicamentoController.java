package com.api.medirecord.medicamento;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints para administrar y consultar los medicamentos asociados a la cuenta
 * del usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/medicamentos")
@Tag(name = "Medicamentos", description = "Administracion y consulta de medicamentos del usuario autenticado")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class MedicamentoController {

	private static final String MODULO = "Medicamento";

	private final MedicamentoService service;

	public MedicamentoController(MedicamentoService service) {
		this.service = service;
	}

	@Operation(summary = "Registra un medicamento asociado al usuario autenticado")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Medicamento registrado"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos o rango de fechas incoherente"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<MedicamentoResponse>> crear(@Valid @RequestBody MedicamentoRequest request) {
		MedicamentoResponse created = service.crear(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(ApiResponse.ok(MODULO + " registrado", created));
	}

	@Operation(summary = "Lista los medicamentos del usuario autenticado con filtros y paginacion")
	@GetMapping
	public ApiResponse<PageResponse<MedicamentoResponse>> listar(
			@Parameter(description = "Termino de busqueda en nombre, dosis, frecuencia o instrucciones")
			@RequestParam(required = false) String search,
			@Parameter(description = "Incluir medicamentos dados de baja")
			@RequestParam(defaultValue = "false") boolean incluirInactivos,
			@PageableDefault(size = 20, sort = "fechaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(service.buscar(search, incluirInactivos, pageable));
	}

	@Operation(summary = "Obtiene un medicamento por id (solo si pertenece al usuario autenticado)")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medicamento encontrado"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El medicamento pertenece a otro usuario"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Medicamento no encontrado")
	})
	@GetMapping("/{id}")
	public ApiResponse<MedicamentoResponse> obtener(@PathVariable UUID id) {
		return ApiResponse.ok(service.obtener(id));
	}

	@Operation(summary = "Actualiza un medicamento del usuario autenticado")
	@PutMapping("/{id}")
	public ApiResponse<MedicamentoResponse> actualizar(@PathVariable UUID id,
			@Valid @RequestBody MedicamentoRequest request) {
		return ApiResponse.ok(MODULO + " actualizado", service.actualizar(id, request));
	}

	@Operation(summary = "Da de baja un medicamento (baja logica)")
	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

	@Operation(summary = "Elimina un medicamento (alias de baja logica)")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

}

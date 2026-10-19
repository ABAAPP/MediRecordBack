package com.api.medirecord.medicamento;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints de medicamentos del usuario autenticado. El recurso es personal:
 * no recibe id de usuario porque se resuelve del token JWT, lo que impide que
 * un usuario lea o administre medicamentos de otro.
 */
@RestController
@RequestMapping("/api/v1/medicamentos")
@Tag(name = "Medicamentos", description = "Administracion de los medicamentos del usuario autenticado")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class MedicamentoController {

	private static final String MODULO = "Medicamento";

	private final MedicamentoService service;

	public MedicamentoController(MedicamentoService service) {
		this.service = service;
	}

	@Operation(summary = "Registra un medicamento propio")
	@PostMapping
	public ResponseEntity<ApiResponse<MedicamentoResponse>> crear(@Valid @RequestBody MedicamentoRequest request) {
		MedicamentoResponse created = service.crear(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(ApiResponse.ok(MODULO + " registrado", created));
	}

	@Operation(summary = "Lista los medicamentos del usuario autenticado")
	@GetMapping
	public ApiResponse<PageResponse<MedicamentoResponse>> listar(
			@Parameter(description = "Termino de busqueda en nombre, dosis o frecuencia")
			@RequestParam(required = false) String search,
			@Parameter(description = "Incluir medicamentos dados de baja")
			@RequestParam(defaultValue = "false") boolean incluirInactivos,
			@PageableDefault(size = 20, sort = "fechaInicio,desc") Pageable pageable) {
		return ApiResponse.ok(service.listar(search, incluirInactivos, pageable));
	}

	@Operation(summary = "Obtiene un medicamento propio por id")
	@GetMapping("/{id}")
	public ApiResponse<MedicamentoResponse> obtener(@PathVariable UUID id) {
		return ApiResponse.ok(service.obtener(id));
	}

	@Operation(summary = "Actualiza un medicamento propio")
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

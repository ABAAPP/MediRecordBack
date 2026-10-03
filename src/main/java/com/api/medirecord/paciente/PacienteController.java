package com.api.medirecord.paciente;

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
import com.api.medirecord.paciente.dto.PacienteRequest;
import com.api.medirecord.paciente.dto.PacienteResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints de pacientes. Plantilla de referencia del patron usado por cada modulo
 * de la aplicacion (controller delgado: solo validacion, delegacion y status HTTP).
 */
@RestController
@RequestMapping("/api/v1/pacientes")
@Tag(name = "Pacientes", description = "Gestion del padron de pacientes")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class PacienteController {

	private static final String MODULO = "Pacientes";

	private final PacienteService service;

	public PacienteController(PacienteService service) {
		this.service = service;
	}

	@Operation(summary = "Registra un paciente")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Paciente creado"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Correo ya registrado")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<PacienteResponse>> crear(@Valid @RequestBody PacienteRequest request) {
		PacienteResponse created = service.crear(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(ApiResponse.ok(MODULO + " registrado", created));
	}

	@Operation(summary = "Lista pacientes con filtros y paginacion")
	@GetMapping
	public ApiResponse<PageResponse<PacienteResponse>> listar(
			@Parameter(description = "Termino de busqueda en nombre, apellidos o correo")
			@RequestParam(required = false) String search,
			@Parameter(description = "Incluir pacientes dados de baja")
			@RequestParam(defaultValue = "false") boolean incluirInactivos,
			@PageableDefault(size = 20, sort = "apellidoPaterno") Pageable pageable) {
		return ApiResponse.ok(service.buscar(search, incluirInactivos, pageable));
	}

	@Operation(summary = "Obtiene un paciente por id")
	@GetMapping("/{id}")
	public ApiResponse<PacienteResponse> obtener(@PathVariable UUID id) {
		return ApiResponse.ok(service.obtener(id));
	}

	@Operation(summary = "Actualiza un paciente")
	@PutMapping("/{id}")
	public ApiResponse<PacienteResponse> actualizar(@PathVariable UUID id,
			@Valid @RequestBody PacienteRequest request) {
		return ApiResponse.ok(MODULO + " actualizado", service.actualizar(id, request));
	}

	@Operation(summary = "Da de baja un paciente (baja logica)")
	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

	@Operation(summary = "Elimina un paciente (alias de baja logica)")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

}
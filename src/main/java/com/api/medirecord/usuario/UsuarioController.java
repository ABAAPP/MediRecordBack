package com.api.medirecord.usuario;

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
import com.api.medirecord.usuario.dto.UsuarioRequest;
import com.api.medirecord.usuario.dto.UsuarioResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints de usuarios. Controller delgado: solo validacion, delegacion y status
 * HTTP; la logica vive en {@link UsuarioService}.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestion de cuentas de usuario y sus datos de contacto")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class UsuarioController {

	private static final String MODULO = "Usuarios";

	private final UsuarioService service;

	public UsuarioController(UsuarioService service) {
		this.service = service;
	}

	@Operation(summary = "Registra un usuario")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuario creado"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Correo ya registrado")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<UsuarioResponse>> crear(@Valid @RequestBody UsuarioRequest request) {
		UsuarioResponse created = service.crear(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(ApiResponse.ok(MODULO + " registrado", created));
	}

	@Operation(summary = "Lista usuarios con filtros y paginacion")
	@GetMapping
	public ApiResponse<PageResponse<UsuarioResponse>> listar(
			@Parameter(description = "Termino de busqueda en nombre, apellidos o correo")
			@RequestParam(required = false) String search,
			@Parameter(description = "Incluir usuarios dados de baja")
			@RequestParam(defaultValue = "false") boolean incluirInactivos,
			@PageableDefault(size = 20, sort = "apellidoPaterno") Pageable pageable) {
		return ApiResponse.ok(service.buscar(search, incluirInactivos, pageable));
	}

	@Operation(summary = "Obtiene un usuario por id")
	@GetMapping("/{id}")
	public ApiResponse<UsuarioResponse> obtener(@PathVariable UUID id) {
		return ApiResponse.ok(service.obtener(id));
	}

	@Operation(summary = "Actualiza un usuario")
	@PutMapping("/{id}")
	public ApiResponse<UsuarioResponse> actualizar(@PathVariable UUID id,
			@Valid @RequestBody UsuarioRequest request) {
		return ApiResponse.ok(MODULO + " actualizado", service.actualizar(id, request));
	}

	@Operation(summary = "Da de baja un usuario (baja logica)")
	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

	@Operation(summary = "Elimina un usuario (alias de baja logica)")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
		service.desactivar(id);
		return ResponseEntity.ok().body(ApiResponse.empty(MODULO + " dado de baja"));
	}

}

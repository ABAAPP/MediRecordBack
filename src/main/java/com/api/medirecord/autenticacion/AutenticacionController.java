package com.api.medirecord.autenticacion;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.api.medirecord.autenticacion.dto.LoginRequest;
import com.api.medirecord.autenticacion.dto.LoginResponse;
import com.api.medirecord.autenticacion.dto.RefreshRequest;
import com.api.medirecord.autenticacion.dto.RegistroRequest;
import com.api.medirecord.autenticacion.dto.TokenResponse;
import com.api.medirecord.common.response.ApiResponse;
import com.api.medirecord.usuario.dto.UsuarioResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints publicos de autenticacion: registro, inicio de sesion y renovacion.
 *
 * <p>Son la unica via de la API para obtener credenciales: no requieren token porque
 * de ello se encarga la propia autenticacion. El resto de la API sigue exigiendo
 * {@code Authorization: Bearer}.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Registro e inicio de sesion en MediRecord")
public class AutenticacionController {

	private final AutenticacionService service;

	public AutenticacionController(AutenticacionService service) {
		this.service = service;
	}

	@Operation(summary = "Registra una cuenta nueva",
			description = "Crea la cuenta en el proveedor de identidad y su proyeccion local. "
					+ "La contrasena no se almacena en MediRecord.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
					description = "Cuenta creada; el Location apunta al usuario"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "Datos invalidos"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
					description = "Correo ya registrado"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
					description = "Servicio de autenticacion no disponible")
	})
	@PostMapping("/registro")
	public ResponseEntity<ApiResponse<UsuarioResponse>> registrar(@Valid @RequestBody RegistroRequest request) {
		UsuarioResponse creada = service.registrar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/usuarios/{id}")
				.buildAndExpand(creada.id())
				.toUri();
		return ResponseEntity.created(location).body(ApiResponse.ok("Cuenta registrada", creada));
	}

	@Operation(summary = "Inicia sesion",
			description = "Verifica las credenciales y devuelve los tokens de la sesion.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
					description = "Sesion iniciada"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "Datos invalidos"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
					description = "Credenciales invalidas o cuenta no vinculada"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
					description = "Cuenta desactivada"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
					description = "Servicio de autenticacion no disponible")
	})
	@PostMapping("/login")
	public ApiResponse<LoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok("Sesion iniciada", service.iniciarSesion(request));
	}

	@Operation(summary = "Renueva la sesion",
			description = "Emite tokens nuevos a partir de un refresh token vigente.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
					description = "Sesion renovada"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "Datos invalidos"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
					description = "Refresh token invalido o cuenta no vinculada"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
					description = "Cuenta desactivada"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
					description = "Servicio de autenticacion no disponible")
	})
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> renovarSesion(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.ok("Sesion renovada", service.renovarSesion(request));
	}

}

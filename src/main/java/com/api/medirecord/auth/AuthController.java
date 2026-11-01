package com.api.medirecord.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.medirecord.auth.dto.LoginRequest;
import com.api.medirecord.auth.dto.LoginResponse;
import com.api.medirecord.auth.dto.RegisterRequest;
import com.api.medirecord.auth.dto.RegisterResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints publicos de registro e inicio de sesion (B-12).
 * Compatible con la aplicacion movil MediRecordFront (/api/auth/**).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints de registro e inicio de sesion consumidos por la app movil y clientes API")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "Registra un nuevo usuario para la aplicacion movil o cliente web")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
			@ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
			@ApiResponse(responseCode = "409", description = "El correo electronico ya esta registrado")
	})
	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		RegisterResponse response = authService.register(request);
		if (!response.success()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Inicia sesion devolviendo un token de acceso")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Autenticacion exitosa"),
			@ApiResponse(responseCode = "400", description = "Credenciales incompletas o formato invalido"),
			@ApiResponse(responseCode = "401", description = "Credenciales invalidas")
	})
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginResponse response = authService.login(request);
		if (!response.success()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		return ResponseEntity.ok(response);
	}

}

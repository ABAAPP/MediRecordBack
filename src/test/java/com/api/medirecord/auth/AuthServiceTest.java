package com.api.medirecord.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.api.medirecord.auth.dto.LoginRequest;
import com.api.medirecord.auth.dto.LoginResponse;
import com.api.medirecord.auth.dto.RegisterRequest;
import com.api.medirecord.auth.dto.RegisterResponse;
import com.api.medirecord.paciente.Paciente;
import com.api.medirecord.paciente.PacienteRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Pruebas unitarias para {@link AuthService} (B-12).
 */
class AuthServiceTest {

	private PacienteRepository pacienteRepository;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		pacienteRepository = mock(PacienteRepository.class);
		JsonMapper objectMapper = JsonMapper.builder().build();
		authService = new AuthService(pacienteRepository, objectMapper, "http://localhost:8081/realms/medirecord");
	}

	@Test
	@DisplayName("Registro de nuevo usuario con datos validos resulta exitoso")
	void registerNuevoUsuarioExitoso() {
		given(pacienteRepository.existsByEmailIgnoreCase(anyString())).willReturn(false);

		RegisterRequest request = new RegisterRequest("Carlos Lopez", "carlos@correo.com", "ClaveSegura123");
		RegisterResponse response = authService.register(request);

		assertThat(response.success()).isTrue();
		assertThat(response.message()).contains("exitosamente");
		assertThat(authService.isUserRegistered("carlos@correo.com")).isTrue();
	}

	@Test
	@DisplayName("Registro con correo ya existente en pacientes es rechazado")
	void registerCorreoDuplicadoEnPacientesEsRechazado() {
		given(pacienteRepository.existsByEmailIgnoreCase("paciente@correo.com"))
				.willReturn(true);

		RegisterRequest request = new RegisterRequest("Ana Lopez", "paciente@correo.com", "ClaveSegura123");
		RegisterResponse response = authService.register(request);

		assertThat(response.success()).isFalse();
		assertThat(response.message()).contains("ya esta registrado");
	}

	@Test
	@DisplayName("Registro con correo previamente registrado es rechazado")
	void registerCorreoDuplicadoLocalmenteEsRechazado() {
		given(pacienteRepository.existsByEmailIgnoreCase(anyString())).willReturn(false);

		RegisterRequest request1 = new RegisterRequest("Pedro Garcia", "pedro@correo.com", "ClaveSegura123");
		authService.register(request1);

		RegisterRequest request2 = new RegisterRequest("Pedro Duplicado", "pedro@correo.com", "OtraClave456");
		RegisterResponse response2 = authService.register(request2);

		assertThat(response2.success()).isFalse();
		assertThat(response2.message()).contains("ya esta registrado");
	}

	@Test
	@DisplayName("Login con credenciales correctas de usuario registrado genera token")
	void loginExitosoUsuarioRegistrado() {
		given(pacienteRepository.existsByEmailIgnoreCase(anyString())).willReturn(false);

		authService.register(new RegisterRequest("Sofia Ruiz", "sofia@correo.com", "Password123"));

		LoginResponse response = authService.login(new LoginRequest("sofia@correo.com", "Password123"));

		assertThat(response.success()).isTrue();
		assertThat(response.token()).isNotNull().isNotBlank();
		assertThat(response.email()).isEqualTo("sofia@correo.com");
	}

	@Test
	@DisplayName("Login con contrasena incorrecta devuelve respuesta de fallo")
	void loginConPasswordIncorrectoFalla() {
		given(pacienteRepository.existsByEmailIgnoreCase(anyString())).willReturn(false);

		authService.register(new RegisterRequest("Sofia Ruiz", "sofia@correo.com", "Password123"));

		LoginResponse response = authService.login(new LoginRequest("sofia@correo.com", "PasswordErroneo"));

		assertThat(response.success()).isFalse();
		assertThat(response.token()).isNull();
		assertThat(response.message()).contains("Credenciales invalidas");
	}

	@Test
	@DisplayName("Login con cuenta demo admin por defecto es exitoso")
	void loginCuentaDemoAdminEsExitoso() {
		LoginResponse response = authService.login(new LoginRequest("admin@medirecord.local", "admin123"));

		assertThat(response.success()).isTrue();
		assertThat(response.token()).isNotNull();
	}

}

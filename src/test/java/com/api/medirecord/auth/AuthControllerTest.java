package com.api.medirecord.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.api.medirecord.auth.dto.LoginRequest;
import com.api.medirecord.auth.dto.LoginResponse;
import com.api.medirecord.auth.dto.RegisterRequest;
import com.api.medirecord.auth.dto.RegisterResponse;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;

/**
 * Pruebas del controlador de autenticacion y registro (B-12: /api/auth/**).
 *
 * <p>Verifica:
 * <ul>
 *   <li>Registro exitoso (201) y con datos invalidos (400)</li>
 *   <li>Registro con correo duplicado (409)</li>
 *   <li>Inicio de sesion exitoso con token JWT (200)</li>
 *   <li>Inicio de sesion con credenciales invalidas (401)</li>
 *   <li>Inicio de sesion con datos invalidos (400)</li>
 *   <li>Acceso permitido sin token previo (endpoints publicos)</li>
 * </ul>
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, CorsConfig.class, AuthControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class AuthControllerTest {

	@TestConfiguration(proxyBeanMethods = false)
	static class TestCorsProperties {

		@Bean
		CorsProperties corsProperties() {
			return new CorsProperties(List.of("http://localhost:3000"),
					List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
					List.of("Authorization", "Content-Type"),
					List.of("Location"),
					true,
					java.time.Duration.ofHours(1));
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	@DisplayName("Registro exitoso devuelve HTTP 201 y mensaje satisfactorio")
	void registroExitosoDevuelve201() throws Exception {
		given(authService.register(any(RegisterRequest.class)))
				.willReturn(RegisterResponse.ok());

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Maria Gonzalez",
								  "email": "maria.gonzalez@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"));
	}

	@Test
	@DisplayName("Registro con datos invalidos devuelve HTTP 400")
	void registroConDatosInvalidosDevuelve400() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "",
								  "email": "formato-invalido",
								  "password": "123"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors").isArray());
	}

	@Test
	@DisplayName("Registro con correo duplicado devuelve HTTP 409")
	void registroConCorreoDuplicadoDevuelve409() throws Exception {
		given(authService.register(any(RegisterRequest.class)))
				.willReturn(RegisterResponse.failure("El correo electronico ya esta registrado"));

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Maria Gonzalez",
								  "email": "maria.duplicada@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("El correo electronico ya esta registrado"));
	}

	@Test
	@DisplayName("Inicio de sesion exitoso devuelve HTTP 200 y token de acceso")
	void loginExitosoDevuelve200ConToken() throws Exception {
		given(authService.login(any(LoginRequest.class)))
				.willReturn(LoginResponse.ok("jwt_mock_token_abc123", "maria@correo.com"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "maria@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.token").value("jwt_mock_token_abc123"))
				.andExpect(jsonPath("$.email").value("maria@correo.com"));
	}

	@Test
	@DisplayName("Inicio de sesion con credenciales invalidas devuelve HTTP 401")
	void loginConCredencialesInvalidasDevuelve401() throws Exception {
		given(authService.login(any(LoginRequest.class)))
				.willReturn(LoginResponse.failure("Credenciales invalidas. Verifica tu correo y contrasena."));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "maria@correo.com",
								  "password": "PasswordErroneo"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Credenciales invalidas. Verifica tu correo y contrasena."));
	}

	@Test
	@DisplayName("Inicio de sesion con campos invalidos devuelve HTTP 400")
	void loginConCamposInvalidosDevuelve400() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "correo-no-valido",
								  "password": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("Los endpoints de registro y login son publicos y no requieren cabecera de autorizacion")
	void endpointsPublicosSinTokenNoDevuelven401DeSeguridad() throws Exception {
		given(authService.login(any(LoginRequest.class)))
				.willReturn(LoginResponse.ok("token_ok", "test@correo.com"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "test@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isOk());
	}

}

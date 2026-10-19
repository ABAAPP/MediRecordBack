package com.api.medirecord.autenticacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

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

import com.api.medirecord.autenticacion.dto.LoginRequest;
import com.api.medirecord.autenticacion.dto.LoginResponse;
import com.api.medirecord.autenticacion.dto.RefreshRequest;
import com.api.medirecord.autenticacion.dto.RegistroRequest;
import com.api.medirecord.autenticacion.dto.TokenResponse;
import com.api.medirecord.autenticacion.exception.CredencialesInvalidasException;
import com.api.medirecord.autenticacion.exception.CuentaDesactivadaException;
import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.usuario.dto.UsuarioResponse;
import com.api.medirecord.usuario.exception.UsuarioEmailDuplicadoException;

/**
 * Test de slice del controller de autenticacion: valida que los endpoints de sesion sean
 * publicos, que la validacion y los errores lleguen en el formato ProblemDetail y que el
 * resto de la API siga exigiendo token.
 */
@WebMvcTest(AutenticacionController.class)
@Import({ SecurityConfig.class, CorsConfig.class, AutenticacionControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class AutenticacionControllerTest {

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
	private AutenticacionService service;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final UUID USUARIO_ID = UUID.fromString("3f7c8b2e-1d4a-4c9e-9f1b-7a2c3d4e5f60");

	private static UsuarioResponse usuario() {
		return new UsuarioResponse(USUARIO_ID, "sub-keycloak-123", "Ana", "Gomez", "Lopez",
				"Ana Gomez Lopez", "ana.gomez@correo.com", "+52 664 123 4567", true, null, null);
	}

	private static String registroJson() {
		return """
				{
				  "nombre": "Ana",
				  "apellidoPaterno": "Gomez",
				  "apellidoMaterno": "Lopez",
				  "email": "ana.gomez@correo.com",
				  "telefono": "+52 664 123 4567",
				  "password": "Secreto123"
				}
				""";
	}

	@Test
	@DisplayName("El registro es publico: no requiere token y responde 201 con Location")
	void registroEsPublico() throws Exception {
		given(service.registrar(any(RegistroRequest.class))).willReturn(usuario());

		mockMvc.perform(post("/api/v1/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registroJson()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						"http://localhost/api/v1/usuarios/" + USUARIO_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(USUARIO_ID.toString()))
				.andExpect(jsonPath("$.data.email").value("ana.gomez@correo.com"));
	}

	@Test
	@DisplayName("El registro valida los datos y no invoca el servicio")
	void registroDatosInvalidos() throws Exception {
		mockMvc.perform(post("/api/v1/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "",
								  "apellidoPaterno": "Gomez",
								  "email": "no-es-correo",
								  "password": "123"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.errors[*].field",
						org.hamcrest.Matchers.hasItems("nombre", "email", "password")))
				.andExpect(jsonPath("$.errors[*].field",
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("passwordRetry"))));

		verifyNoInteractions(service);
	}

	@Test
	@DisplayName("Correo ya registrado responde 409 con el codigo del catalogo")
	void registroDuplicado() throws Exception {
		given(service.registrar(any(RegistroRequest.class)))
				.willThrow(new UsuarioEmailDuplicadoException("ana.gomez@correo.com"));

		mockMvc.perform(post("/api/v1/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registroJson()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATED_RESOURCE.getCode()));
	}

	@Test
	@DisplayName("El login es publico y devuelve los tokens envueltos en ApiResponse")
	void loginEsPublico() throws Exception {
		given(service.iniciarSesion(any(LoginRequest.class)))
				.willReturn(new LoginResponse(
						new TokenResponse("access-1", "Bearer", 1800, 1800, "refresh-1"),
						usuario()));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana.gomez@correo.com", "password": "Secreto123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Sesion iniciada"))
				.andExpect(jsonPath("$.data.tokens.accessToken").value("access-1"))
				.andExpect(jsonPath("$.data.tokens.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.usuario.email").value("ana.gomez@correo.com"));
	}

	@Test
	@DisplayName("Credenciales invalidas responden 401 en formato ProblemDetail")
	void loginCredencialesInvalidas() throws Exception {
		given(service.iniciarSesion(any(LoginRequest.class)))
				.willThrow(new CredencialesInvalidasException());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana.gomez@correo.com", "password": "mala"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()))
				.andExpect(jsonPath("$.detail").value("Correo o contrasena invalidos"));
	}

	@Test
	@DisplayName("Cuenta desactivada responde 403")
	void loginCuentaDesactivada() throws Exception {
		given(service.iniciarSesion(any(LoginRequest.class)))
				.willThrow(new CuentaDesactivadaException());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana.gomez@correo.com", "password": "Secreto123"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));
	}

	@Test
	@DisplayName("El refresh es publico y devuelve los tokens nuevos")
	void refreshEsPublico() throws Exception {
		given(service.renovarSesion(any(RefreshRequest.class)))
				.willReturn(new TokenResponse("access-2", "Bearer", 1800, 1800, "refresh-2"));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken": "refresh-1"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("access-2"));
	}

	@Test
	@DisplayName("Las demas rutas de la API siguen exigiendo autenticacion")
	void rutasProtegidasExigenToken() throws Exception {
		mockMvc.perform(get("/api/v1/usuarios"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
	}

	@Test
	@DisplayName("El login no esta sujeto a la regla de admin ni al token")
	void loginSinSesion() throws Exception {
		given(service.iniciarSesion(any(LoginRequest.class)))
				.willReturn(new LoginResponse(
						new TokenResponse("access-1", "Bearer", 1800, 1800, "refresh-1"),
						usuario()));

		mockMvc.perform(post("/api/v1/auth/login")
						.with(user("anon"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana.gomez@correo.com", "password": "Secreto123"}
								"""))
				.andExpect(status().isOk());
	}

}

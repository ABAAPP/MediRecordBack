package com.api.medirecord.toma;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
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

import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.toma.dto.RegistrarTomaRequest;
import com.api.medirecord.toma.dto.TomaResponse;

/**
 * Tests de la API REST de tomas de medicamentos (B-09: /api/v1/tomas).
 */
@WebMvcTest(TomaController.class)
@Import({ SecurityConfig.class, CorsConfig.class, TomaControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class TomaControllerTest {

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
	private TomaService service;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final UUID TOMA_ID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d");
	private static final UUID MED_ID = UUID.fromString("7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d");

	private static TomaResponse mockResponse() {
		return new TomaResponse(
				TOMA_ID,
				MED_ID,
				"Paracetamol",
				null,
				"usuario-123",
				LocalDate.of(2026, 10, 31),
				"08:15 AM",
				true,
				"Con comida",
				Instant.now());
	}

	@Test
	@DisplayName("POST /api/v1/tomas sin token devuelve 401 Unauthorized")
	void sinTokenDevuelve401() throws Exception {
		mockMvc.perform(post("/api/v1/tomas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
	}

	@Test
	@DisplayName("POST /api/v1/tomas con token registra toma y devuelve 201 Created")
	void registrarTomaConToken() throws Exception {
		given(service.registrarToma(any(RegistrarTomaRequest.class))).willReturn(mockResponse());

		mockMvc.perform(post("/api/v1/tomas")
						.with(user("usuario-123").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicamentoId": "%s",
								  "fechaToma": "2026-10-31",
								  "horaToma": "08:15 AM",
								  "realizada": true,
								  "notas": "Con comida"
								}
								""".formatted(MED_ID)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/tomas/" + TOMA_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(TOMA_ID.toString()))
				.andExpect(jsonPath("$.data.realizada").value(true));
	}

	@Test
	@DisplayName("Intentar registrar toma para un medicamento de otro usuario devuelve 403 Forbidden")
	void bloquearTomaDeOtroUsuario() throws Exception {
		given(service.registrarToma(any(RegistrarTomaRequest.class)))
				.willThrow(new MedicamentoAccesoDenegadoException(MED_ID));

		mockMvc.perform(post("/api/v1/tomas")
						.with(user("usuario-123").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicamentoId": "%s",
								  "realizada": true
								}
								""".formatted(MED_ID)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));
	}

	@Test
	@DisplayName("GET /api/v1/tomas consulta las tomas registradas")
	void consultarTomas() throws Exception {
		given(service.consultarTomasFecha(any(LocalDate.class))).willReturn(List.of(mockResponse()));

		mockMvc.perform(get("/api/v1/tomas")
						.with(user("usuario-123").roles("MEDICO"))
						.param("fecha", "2026-10-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].medicamentoNombre").value("Paracetamol"));
	}

}

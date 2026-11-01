package com.api.medirecord.recordatorio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.api.medirecord.recordatorio.dto.RecordatorioResponse;

/**
 * Tests de la API REST de recordatorios (B-08: /api/v1/recordatorios).
 */
@WebMvcTest(RecordatorioController.class)
@Import({ SecurityConfig.class, CorsConfig.class, RecordatorioControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class RecordatorioControllerTest {

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
	private RecordatorioService service;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static RecordatorioResponse mockResponse() {
		return new RecordatorioResponse(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"Paracetamol",
				"500 mg",
				"08:00 AM",
				"Cada 8 horas",
				List.of("Lunes", "Miércoles", "Viernes"),
				"Tomar con agua",
				LocalDate.of(2026, 10, 31),
				true,
				"usuario-123");
	}

	@Test
	@DisplayName("Sin token devuelve 401 Unauthorized")
	void sinTokenDevuelve401() throws Exception {
		mockMvc.perform(get("/api/v1/recordatorios"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
	}

	@Test
	@DisplayName("Consulta recordatorios filtrados por fecha con usuario autenticado")
	void consultaRecordatoriosPorFecha() throws Exception {
		given(service.consultarRecordatorios(any(LocalDate.class))).willReturn(List.of(mockResponse()));

		mockMvc.perform(get("/api/v1/recordatorios")
						.with(user("usuario-123").roles("MEDICO"))
						.param("fecha", "2026-10-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].medicamentoNombre").value("Paracetamol"))
				.andExpect(jsonPath("$.data[0].horaAdministracion").value("08:00 AM"));
	}

	@Test
	@DisplayName("Consulta recordatorios de hoy devuelve 200 OK")
	void consultaRecordatoriosHoy() throws Exception {
		given(service.consultarRecordatorios(any(LocalDate.class))).willReturn(List.of(mockResponse()));

		mockMvc.perform(get("/api/v1/recordatorios/hoy")
						.with(user("usuario-123").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].medicamentoNombre").value("Paracetamol"));
	}

}

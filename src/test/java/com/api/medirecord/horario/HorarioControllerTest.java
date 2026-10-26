package com.api.medirecord.horario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.horario.dto.HorarioRequest;
import com.api.medirecord.horario.dto.HorarioResponse;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

/**
 * Tests del controlador REST de horarios (B-07).
 */
@WebMvcTest(HorarioController.class)
@Import({ SecurityConfig.class, CorsConfig.class, HorarioControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class HorarioControllerTest {

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
	private HorarioService service;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final UUID HORARIO_ID = UUID.fromString("9b2e4d6f-1a3c-5e7b-8d9f-0a1b2c3d4e5f");
	private static final UUID MEDICAMENTO_ID = UUID.fromString("7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d");

	private static HorarioResponse response() {
		return new HorarioResponse(
				HORARIO_ID,
				MEDICAMENTO_ID,
				"Paracetamol",
				"500 mg - 1 tableta",
				"08:00 AM",
				"Cada 8 horas",
				List.of("Lunes", "Miércoles", "Viernes"),
				true,
				"Con alimentos",
				true,
				null,
				null);
	}

	@Test
	@DisplayName("Registra un horario y devuelve 201 Created con cabecera Location")
	void crearHorario() throws Exception {
		given(service.crear(any(HorarioRequest.class))).willReturn(response());

		mockMvc.perform(post("/api/v1/horarios")
						.with(user("usuario-123").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicamentoId": "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
								  "horaAdministracion": "08:00 AM",
								  "frecuencia": "Cada 8 horas",
								  "diasSemana": ["Lunes", "Miércoles", "Viernes"],
								  "recordatorioActivo": true,
								  "notas": "Con alimentos"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/horarios/" + HORARIO_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(HORARIO_ID.toString()))
				.andExpect(jsonPath("$.data.medicamentoId").value(MEDICAMENTO_ID.toString()))
				.andExpect(jsonPath("$.data.horaAdministracion").value("08:00 AM"))
				.andExpect(jsonPath("$.data.frecuencia").value("Cada 8 horas"));
	}

	@Test
	@DisplayName("Asociar un horario a un medicamento inexistente devuelve 404 NOT_FOUND")
	void medicamentoInexistenteDevuelve404() throws Exception {
		UUID idInexistente = UUID.randomUUID();
		given(service.crear(any(HorarioRequest.class)))
				.willThrow(new MedicamentoNotFoundException(idInexistente));

		mockMvc.perform(post("/api/v1/horarios")
						.with(user("usuario-123").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicamentoId": "%s",
								  "horaAdministracion": "08:00 AM",
								  "frecuencia": "Cada 8 horas"
								}
								""".formatted(idInexistente)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()));
	}

	@Test
	@DisplayName("Consulta los horarios disponibles para el servicio de recordatorios")
	void listarHorariosParaRecordatorios() throws Exception {
		given(service.obtenerHorariosParaRecordatorios()).willReturn(List.of(response()));

		mockMvc.perform(get("/api/v1/horarios/recordatorios")
						.with(user("usuario-123").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].id").value(HORARIO_ID.toString()))
				.andExpect(jsonPath("$.data[0].medicamentoNombre").value("Paracetamol"))
				.andExpect(jsonPath("$.data[0].horaAdministracion").value("08:00 AM"));
	}

}

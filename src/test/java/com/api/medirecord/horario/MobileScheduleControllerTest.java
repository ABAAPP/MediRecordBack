package com.api.medirecord.horario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.api.medirecord.horario.dto.MobileScheduleRequest;
import com.api.medirecord.horario.dto.MobileScheduleResponse;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

/**
 * Tests del controlador de horarios para la aplicacion movil (B-07: /api/schedules).
 */
@WebMvcTest(MobileScheduleController.class)
@Import({ SecurityConfig.class, CorsConfig.class, MobileScheduleControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class MobileScheduleControllerTest {

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

	private static final UUID SCHEDULE_ID = UUID.fromString("6a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d");
	private static final UUID MED_ID = UUID.fromString("7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d");

	@Test
	@DisplayName("Guardar horario desde app movil sin token responde 401 ProblemDetail")
	void guardarHorarioSinAutenticacionDevuelve401() throws Exception {
		mockMvc.perform(post("/api/schedules")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicationId": "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
								  "scheduledTime": "08:00 AM",
								  "frequency": "Cada 8 horas",
								  "selectedDays": ["Lunes", "Miercoles"]
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
	}

	@Test
	@DisplayName("Guardar horario desde app movil con datos validos devuelve 201 y payload de respuesta")
	void guardarHorarioDesdeAppExitosoDevuelve201() throws Exception {
		MobileMedicationDoseDto doseDto = new MobileMedicationDoseDto(
				MED_ID.toString(),
				"Paracetamol",
				"500 mg",
				"08:00 AM",
				false,
				"Tomar con desayuno",
				"Cada 8 horas (3 veces al día)",
				List.of("Lunes", "Miércoles", "Viernes"),
				"paciente_test",
				null,
				"2026-11-01");

		MobileScheduleResponse response = new MobileScheduleResponse(
				true,
				"Horario configurado correctamente para 'Paracetamol' a las 08:00 AM",
				SCHEDULE_ID.toString(),
				doseDto);

		given(service.guardarDesdeApp(any(MobileScheduleRequest.class))).willReturn(response);

		mockMvc.perform(post("/api/schedules")
						.with(user("paciente_test").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicationId": "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
								  "medicationName": "Paracetamol",
								  "scheduledTime": "08:00 AM",
								  "frequency": "Cada 8 horas (3 veces al día)",
								  "selectedDays": ["Lunes", "Miércoles", "Viernes"],
								  "reminderEnabled": true,
								  "notes": "Tomar con desayuno"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/horarios/" + SCHEDULE_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID.toString()))
				.andExpect(jsonPath("$.updatedMedication.name").value("Paracetamol"))
				.andExpect(jsonPath("$.updatedMedication.scheduledTime").value("08:00 AM"));
	}

	@Test
	@DisplayName("Guardar horario desde app movil con datos invalidos devuelve 400 ProblemDetail")
	void guardarHorarioConDatosInvalidosDevuelve400() throws Exception {
		mockMvc.perform(post("/api/schedules")
						.with(user("paciente_test").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicationId": "",
								  "scheduledTime": "hora_invalida",
								  "frequency": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.errors").isArray());
	}

	@Test
	@DisplayName("Guardar horario para medicamento inexistente devuelve 404 ProblemDetail")
	void guardarHorarioMedicamentoInexistenteDevuelve404() throws Exception {
		given(service.guardarDesdeApp(any(MobileScheduleRequest.class)))
				.willThrow(new MedicamentoNotFoundException(MED_ID));

		mockMvc.perform(post("/api/schedules")
						.with(user("paciente_test").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicationId": "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
								  "scheduledTime": "08:00 AM",
								  "frequency": "Cada 8 horas"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()));
	}

}

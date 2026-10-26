package com.api.medirecord.medicamento;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.medicamento.dto.MobileMedicationRequest;
import com.api.medirecord.medicamento.dto.MobileMedicationResponse;

/**
 * Tests del endpoint movil de registro de medicamentos (B-06: POST /api/medications).
 */
@WebMvcTest(MobileMedicationController.class)
@Import({ SecurityConfig.class, CorsConfig.class, MobileMedicationControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class MobileMedicationControllerTest {

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
	private MedicamentoService service;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final String MEDICATION_ID = "7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d";
	private static final String USUARIO_ID = "usuario-autenticado";

	@Test
	@DisplayName("El endpoint POST /api/medications requiere autenticacion (401 sin token)")
	void requiereAutenticacion() throws Exception {
		mockMvc.perform(post("/api/medications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
	}

	@Test
	@DisplayName("Registra medicamento exitosamente desde la aplicacion movil y devuelve 201 Created")
	void registraMedicamentoExitosamente() throws Exception {
		var doseDto = new MobileMedicationDoseDto(
				MEDICATION_ID,
				"Paracetamol",
				"500 mg - 1 tableta",
				"08:00 AM",
				false,
				"Con alimentos",
				"Cada 8 horas",
				List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"),
				USUARIO_ID,
				null,
				"2026-10-25");
		var response = new MobileMedicationResponse(
				true,
				"Medicamento 'Paracetamol' registrado exitosamente",
				MEDICATION_ID,
				doseDto);

		given(service.registrarDesdeApp(any(MobileMedicationRequest.class))).willReturn(response);

		mockMvc.perform(post("/api/medications")
						.with(user(USUARIO_ID).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Paracetamol",
								  "dosage": "500 mg - 1 tableta",
								  "startDate": "25/10/2026",
								  "endDate": "01/11/2026",
								  "frequency": "Cada 8 horas",
								  "scheduledTime": "08:00 AM",
								  "instructions": "Con alimentos"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/medicamentos/" + MEDICATION_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.medicationId").value(MEDICATION_ID))
				.andExpect(jsonPath("$.medication.name").value("Paracetamol"))
				.andExpect(jsonPath("$.medication.userEmail").value(USUARIO_ID));
	}

	@Test
	@DisplayName("Devuelve 400 Bad Request con detalle de validacion cuando los datos del medicamento son invalidos")
	void devuelve400CuandoDatosSonInvalidos() throws Exception {
		mockMvc.perform(post("/api/medications")
						.with(user(USUARIO_ID).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "A",
								  "dosage": "",
								  "startDate": "",
								  "frequency": "",
								  "scheduledTime": "hora-invalida"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.errors[*].field",
						org.hamcrest.Matchers.hasItems("name", "dosage", "startDate", "frequency", "scheduledTime")));
	}

}

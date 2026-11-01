package com.api.medirecord.recordatorio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;
import com.api.medirecord.toma.TomaService;
import com.api.medirecord.toma.dto.CompleteReminderRequest;
import com.api.medirecord.toma.dto.CompleteReminderResponse;

/**
 * Tests de los endpoints consumidos por la app movil (B-08 y B-09).
 */
@WebMvcTest(MobileReminderController.class)
@Import({ SecurityConfig.class, CorsConfig.class, MobileReminderControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class MobileReminderControllerTest {

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
	private RecordatorioService recordatorioService;

	@MockitoBean
	private TomaService tomaService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	@DisplayName("GET /api/reminders sin token responde 401 Unauthorized")
	void sinTokenRemindersDevuelve401() throws Exception {
		mockMvc.perform(get("/api/reminders"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
	}

	@Test
	@DisplayName("GET /api/reminders con token devuelve la lista de recordatorios para la app")
	void getRemindersConToken() throws Exception {
		var reminder = new MobileMedicationReminderDto(
				"1",
				"med-1",
				"Paracetamol",
				"500 mg",
				"08:00 AM",
				"Cada 8 horas",
				List.of("Lunes", "Martes", "Miércoles"),
				"Con comida",
				false,
				"usuario-123",
				null);

		given(recordatorioService.consultarRecordatoriosMovil(anyString())).willReturn(List.of(reminder));

		mockMvc.perform(get("/api/reminders")
						.with(user("usuario-123").roles("MEDICO"))
						.param("userEmail", "usuario-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].medicationName").value("Paracetamol"))
				.andExpect(jsonPath("$[0].scheduledTime").value("08:00 AM"));
	}

	@Test
	@DisplayName("GET /api/medications/today con token devuelve las dosis del dia")
	void getTodayMedicationsConToken() throws Exception {
		var dose = new MobileMedicationDoseDto(
				"1",
				"Amoxicilina",
				"500 mg",
				"09:00 AM",
				false,
				"Tomar con agua",
				"Diario",
				List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"),
				"usuario-123",
				null,
				"2026-10-31");

		given(recordatorioService.consultarDosisFechaMovil(any(LocalDate.class), anyString()))
				.willReturn(List.of(dose));

		mockMvc.perform(get("/api/medications/today")
						.with(user("usuario-123").roles("MEDICO"))
						.param("date", "2026-10-31")
						.param("userEmail", "usuario-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Amoxicilina"))
				.andExpect(jsonPath("$[0].scheduledTime").value("09:00 AM"));
	}

	@Test
	@DisplayName("POST /api/reminders/{id}/complete marca el recordatorio como realizado")
	void completeReminder() throws Exception {
		var reminder = new MobileMedicationReminderDto(
				"1",
				"med-1",
				"Paracetamol",
				"500 mg",
				"08:00 AM",
				"Cada 8 horas",
				List.of("Lunes", "Martes"),
				null,
				true,
				"usuario-123",
				"08:05 AM");

		var response = new CompleteReminderResponse(
				true,
				"Toma de 'Paracetamol' marcada como realizada",
				"1",
				"08:05 AM",
				reminder);

		given(tomaService.marcarRecordatorioCompletado(anyString(), any(CompleteReminderRequest.class)))
				.willReturn(response);

		mockMvc.perform(post("/api/reminders/1/complete")
						.with(user("usuario-123").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reminderId": "1",
								  "completedAt": "08:05 AM"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.completedAt").value("08:05 AM"))
				.andExpect(jsonPath("$.updatedReminder.isCompleted").value(true));
	}

	@Test
	@DisplayName("POST /api/medications/{id}/toggle alterna el estado de la dosis")
	void toggleDoseStatus() throws Exception {
		var dose = new MobileMedicationDoseDto(
				"1",
				"Paracetamol",
				"500 mg",
				"08:00 AM",
				true,
				null,
				"Diario",
				List.of("Lunes"),
				"usuario-123",
				"08:05 AM",
				"2026-10-31");

		given(tomaService.alternarEstadoToma("1")).willReturn(dose);

		mockMvc.perform(post("/api/medications/1/toggle")
						.with(user("usuario-123").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isCompleted").value(true))
				.andExpect(jsonPath("$.completedAt").value("08:05 AM"));
	}

}

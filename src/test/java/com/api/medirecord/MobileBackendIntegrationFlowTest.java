package com.api.medirecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.springframework.test.web.servlet.MvcResult;

import com.api.medirecord.auth.AuthController;
import com.api.medirecord.auth.AuthService;
import com.api.medirecord.auth.dto.LoginRequest;
import com.api.medirecord.auth.dto.LoginResponse;
import com.api.medirecord.auth.dto.RegisterRequest;
import com.api.medirecord.auth.dto.RegisterResponse;
import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.horario.HorarioService;
import com.api.medirecord.horario.MobileScheduleController;
import com.api.medirecord.horario.dto.MobileScheduleRequest;
import com.api.medirecord.horario.dto.MobileScheduleResponse;
import com.api.medirecord.medicamento.MedicamentoService;
import com.api.medirecord.medicamento.MobileMedicationController;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.medicamento.dto.MobileMedicationRequest;
import com.api.medirecord.medicamento.dto.MobileMedicationResponse;
import com.api.medirecord.recordatorio.MobileReminderController;
import com.api.medirecord.recordatorio.RecordatorioService;
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;
import com.api.medirecord.toma.TomaService;
import com.api.medirecord.toma.dto.CompleteReminderRequest;
import com.api.medirecord.toma.dto.CompleteReminderResponse;

/**
 * Suite de pruebas de integracion entre la aplicacion movil y el backend (Requerimiento B-13).
 *
 * <p>Verifica el cumplimiento de los 11 criterios de aceptacion del requerimiento:
 * <ul>
 *   <li>1. La aplicacion movil puede comunicarse con la API REST.</li>
 *   <li>2. La autenticacion mediante JWT funciona desde la aplicacion movil.</li>
 *   <li>3. El usuario puede registrarse desde la aplicacion.</li>
 *   <li>4. El usuario puede iniciar sesion desde la aplicacion.</li>
 *   <li>5. El usuario puede registrar medicamentos desde la aplicacion.</li>
 *   <li>6. El usuario puede consultar sus medicamentos y horarios.</li>
 *   <li>7. El usuario puede consultar los medicamentos del dia.</li>
 *   <li>8. El usuario puede marcar una toma como realizada.</li>
 *   <li>9. La informacion enviada desde la aplicacion se procesa y persiste correctamente.</li>
 *   <li>10. Los errores del backend son manejados y devueltos con formato estructurado (ProblemDetail).</li>
 *   <li>11. El flujo principal del PMV puede ejecutarse de principio a fin.</li>
 * </ul>
 */
@WebMvcTest(controllers = {
		AuthController.class,
		MobileMedicationController.class,
		MobileScheduleController.class,
		MobileReminderController.class
})
@Import({ SecurityConfig.class, CorsConfig.class, MobileBackendIntegrationFlowTest.TestCorsProperties.class })
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MobileBackendIntegrationFlowTest {

	@TestConfiguration(proxyBeanMethods = false)
	static class TestCorsProperties {

		@Bean
		CorsProperties corsProperties() {
			return new CorsProperties(List.of("http://localhost:3000", "http://localhost:5173"),
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
	private MedicamentoService medicamentoService;

	@MockitoBean
	private HorarioService horarioService;

	@MockitoBean
	private RecordatorioService recordatorioService;

	@MockitoBean
	private TomaService tomaService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final String TEST_USER = "paciente_movil@correo.com";
	private static final String MOCK_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYWNpZW50ZV9tb3ZpbCJ9";
	private static final UUID MED_ID = UUID.fromString("44444444-5555-6666-7777-888888888888");
	private static final UUID SCHEDULE_ID = UUID.fromString("55555555-6666-7777-8888-999999999999");

	@Test
	@Order(1)
	@DisplayName("Criterio 1 y 3: El usuario puede registrarse desde la aplicacion movil (POST /api/auth/register)")
	void criterio3_registroUsuarioDesdeAppMovil() throws Exception {
		given(authService.register(any(RegisterRequest.class))).willReturn(RegisterResponse.ok());

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Carlos Gomez",
								  "email": "paciente_movil@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"));
	}

	@Test
	@Order(2)
	@DisplayName("Criterio 2 y 4: El usuario puede iniciar sesion y obtener token JWT (POST /api/auth/login)")
	void criterio4_loginUsuarioDesdeAppMovil() throws Exception {
		given(authService.login(any(LoginRequest.class)))
				.willReturn(LoginResponse.ok(MOCK_JWT, TEST_USER));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "paciente_movil@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.token").value(MOCK_JWT))
				.andExpect(jsonPath("$.email").value(TEST_USER));
	}

	@Test
	@Order(3)
	@DisplayName("Criterio 5 y 9: El usuario puede registrar medicamentos desde la aplicacion movil (POST /api/medications)")
	void criterio5_registrarMedicamentoDesdeApp() throws Exception {
		MobileMedicationDoseDto dose = new MobileMedicationDoseDto(
				MED_ID.toString(), "Losartan", "50 mg", "08:00 AM", false,
				"Tomar por la mañana", "Cada 24 horas", List.of("Todos los días"),
				TEST_USER, null, "2026-11-02");

		MobileMedicationResponse resp = new MobileMedicationResponse(
				true, "Medicamento 'Losartan' registrado exitosamente",
				MED_ID.toString(), dose);

		given(medicamentoService.registrarDesdeApp(any(MobileMedicationRequest.class))).willReturn(resp);

		mockMvc.perform(post("/api/medications")
						.with(user(TEST_USER).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Losartan",
								  "dosage": "50 mg",
								  "startDate": "2026-11-02",
								  "frequency": "Cada 24 horas",
								  "scheduledTime": "08:00 AM",
								  "instructions": "Tomar por la mañana"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/medicamentos/" + MED_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.medicationId").value(MED_ID.toString()))
				.andExpect(jsonPath("$.medication.name").value("Losartan"));
	}

	@Test
	@Order(4)
	@DisplayName("Criterio 6: El usuario puede configurar horarios para su medicamento (POST /api/schedules)")
	void criterio6_configurarHorarioMedicamento() throws Exception {
		MobileMedicationDoseDto dose = new MobileMedicationDoseDto(
				MED_ID.toString(), "Losartan", "50 mg", "08:00 AM", false,
				"Tomar con agua", "Cada 24 horas", List.of("Lunes", "Martes", "Miércoles"),
				TEST_USER, null, "2026-11-02");

		MobileScheduleResponse resp = new MobileScheduleResponse(
				true, "Horario configurado correctamente para 'Losartan' a las 08:00 AM",
				SCHEDULE_ID.toString(), dose);

		given(horarioService.guardarDesdeApp(any(MobileScheduleRequest.class))).willReturn(resp);

		mockMvc.perform(post("/api/schedules")
						.with(user(TEST_USER).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "medicationId": "44444444-5555-6666-7777-888888888888",
								  "medicationName": "Losartan",
								  "scheduledTime": "08:00 AM",
								  "frequency": "Cada 24 horas",
								  "selectedDays": ["Lunes", "Martes", "Miércoles"],
								  "reminderEnabled": true,
								  "notes": "Tomar con agua"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID.toString()));
	}

	@Test
	@Order(5)
	@DisplayName("Criterio 6: El usuario puede consultar sus recordatorios y horarios activos (GET /api/reminders)")
	void criterio6_consultarRecordatoriosYHorarios() throws Exception {
		MobileMedicationReminderDto rem = new MobileMedicationReminderDto(
				SCHEDULE_ID.toString(), MED_ID.toString(), "Losartan", "50 mg",
				"08:00 AM", "Cada 24 horas", List.of("Lunes", "Martes", "Miércoles"),
				"Tomar con agua", false, TEST_USER, null);

		given(recordatorioService.consultarRecordatoriosMovil(anyString())).willReturn(List.of(rem));

		mockMvc.perform(get("/api/reminders?userEmail=" + TEST_USER)
						.with(user(TEST_USER).roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(SCHEDULE_ID.toString()))
				.andExpect(jsonPath("$[0].medicationName").value("Losartan"))
				.andExpect(jsonPath("$[0].scheduledTime").value("08:00 AM"))
				.andExpect(jsonPath("$[0].isCompleted").value(false));
	}

	@Test
	@Order(6)
	@DisplayName("Criterio 7: El usuario puede consultar los medicamentos del dia (GET /api/medications/today)")
	void criterio7_consultarMedicamentosDelDia() throws Exception {
		MobileMedicationDoseDto dose = new MobileMedicationDoseDto(
				SCHEDULE_ID.toString(), "Losartan", "50 mg", "08:00 AM", false,
				"Tomar con agua", "Cada 24 horas", List.of("Lunes"),
				TEST_USER, null, "2026-11-02");

		given(medicamentoService.consultarDosisHoyMovil(any(), anyString()))
				.willReturn(List.of(dose));

		mockMvc.perform(get("/api/medications/today?date=2026-11-02&userEmail=" + TEST_USER)
						.with(user(TEST_USER).roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Losartan"))
				.andExpect(jsonPath("$[0].scheduledTime").value("08:00 AM"))
				.andExpect(jsonPath("$[0].isCompleted").value(false));
	}

	@Test
	@Order(7)
	@DisplayName("Criterio 8: El usuario puede marcar una toma como realizada desde la app (POST /api/reminders/{id}/complete)")
	void criterio8_marcarTomaComoRealizada() throws Exception {
		MobileMedicationReminderDto updatedReminder = new MobileMedicationReminderDto(
				SCHEDULE_ID.toString(), MED_ID.toString(), "Losartan", "50 mg",
				"08:00 AM", "Cada 24 horas", List.of("Lunes"), "Tomar con agua",
				true, TEST_USER, "08:10 AM");

		CompleteReminderResponse resp = new CompleteReminderResponse(
				true, "Toma de 'Losartan' marcada como realizada",
				SCHEDULE_ID.toString(), "08:10 AM", updatedReminder);

		given(tomaService.marcarRecordatorioCompletado(eq(SCHEDULE_ID.toString()), any(CompleteReminderRequest.class)))
				.willReturn(resp);

		mockMvc.perform(post("/api/reminders/{id}/complete", SCHEDULE_ID.toString())
						.with(user(TEST_USER).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reminderId": "55555555-6666-7777-8888-999999999999",
								  "completedAt": "08:10 AM"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.completedAt").value("08:10 AM"))
				.andExpect(jsonPath("$.updatedReminder.isCompleted").value(true));
	}

	@Test
	@Order(8)
	@DisplayName("Criterio 8: Alternar estado de toma desde la app (POST /api/medications/{id}/toggle)")
	void criterio8_alternarEstadoToma() throws Exception {
		MobileMedicationDoseDto toggledDose = new MobileMedicationDoseDto(
				SCHEDULE_ID.toString(), "Losartan", "50 mg", "08:00 AM", true,
				"Tomar con agua", "Cada 24 horas", List.of("Lunes"),
				TEST_USER, "08:10 AM", "2026-11-02");

		given(tomaService.alternarEstadoToma(eq(SCHEDULE_ID.toString()))).willReturn(toggledDose);

		mockMvc.perform(post("/api/medications/{id}/toggle", SCHEDULE_ID.toString())
						.with(user(TEST_USER).roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isCompleted").value(true))
				.andExpect(jsonPath("$.completedAt").value("08:10 AM"));
	}

	@Test
	@Order(9)
	@DisplayName("Criterio 10: Los errores del backend son manejados con ProblemDetail (400 datos invalidos)")
	void criterio10_erroresManejadosCorrectamente() throws Exception {
		mockMvc.perform(post("/api/medications")
						.with(user(TEST_USER).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "",
								  "dosage": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.errors").isArray());
	}

	@Test
	@Order(10)
	@DisplayName("Criterio 2: Solicitudes sin token JWT son rechazadas con 401 UNAUTHENTICATED")
	void criterio2_solicitudesSinTokenRechazadasCon401() throws Exception {
		mockMvc.perform(get("/api/medications/today"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
	}

	@Test
	@Order(11)
	@DisplayName("Criterio 11: Ejecucion completa del flujo principal del PMV de principio a fin")
	void criterio11_flujoPrincipalPmvCompleto() throws Exception {
		// Paso 1: Registro
		given(authService.register(any(RegisterRequest.class))).willReturn(RegisterResponse.ok());
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Maria Lopez",
								  "email": "maria.pmv@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true));

		// Paso 2: Login
		given(authService.login(any(LoginRequest.class)))
				.willReturn(LoginResponse.ok(MOCK_JWT, "maria.pmv@correo.com"));
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "maria.pmv@correo.com",
								  "password": "Password123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		// Paso 3: Registro de medicamento
		MobileMedicationDoseDto dose = new MobileMedicationDoseDto(
				MED_ID.toString(), "Metformina", "850 mg", "07:00 AM", false,
				null, "Cada 12 horas", List.of("Todos los días"),
				"maria.pmv@correo.com", null, "2026-11-02");
		given(medicamentoService.registrarDesdeApp(any(MobileMedicationRequest.class)))
				.willReturn(new MobileMedicationResponse(true, "Medicamento registrado", MED_ID.toString(), dose));

		mockMvc.perform(post("/api/medications")
						.with(user("maria.pmv@correo.com").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Metformina",
								  "dosage": "850 mg",
								  "startDate": "2026-11-02",
								  "frequency": "Cada 12 horas",
								  "scheduledTime": "07:00 AM"
								}
								"""))
				.andExpect(status().isCreated());

		// Paso 4: Consulta de medicamentos de hoy
		given(medicamentoService.consultarDosisHoyMovil(any(), anyString()))
				.willReturn(List.of(dose));
		mockMvc.perform(get("/api/medications/today?date=2026-11-02&userEmail=maria.pmv@correo.com")
						.with(user("maria.pmv@correo.com").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Metformina"));

		// Paso 5: Marcar toma como realizada
		given(tomaService.marcarRecordatorioCompletado(eq(MED_ID.toString()), any()))
				.willReturn(new CompleteReminderResponse(true, "Completado", MED_ID.toString(), "07:05 AM", null));
		mockMvc.perform(post("/api/reminders/{id}/complete", MED_ID.toString())
						.with(user("maria.pmv@correo.com").roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "completedAt": "07:05 AM"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.completedAt").value("07:05 AM"));
	}

}

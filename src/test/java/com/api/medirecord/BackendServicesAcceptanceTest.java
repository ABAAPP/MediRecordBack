package com.api.medirecord;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.api.medirecord.auth.AuthController;
import com.api.medirecord.auth.AuthService;
import com.api.medirecord.auth.dto.LoginRequest;
import com.api.medirecord.auth.dto.LoginResponse;
import com.api.medirecord.auth.dto.RegisterRequest;
import com.api.medirecord.auth.dto.RegisterResponse;
import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.horario.HorarioController;
import com.api.medirecord.horario.HorarioService;
import com.api.medirecord.horario.MobileScheduleController;
import com.api.medirecord.horario.dto.HorarioRequest;
import com.api.medirecord.horario.dto.HorarioResponse;
import com.api.medirecord.horario.dto.MobileScheduleRequest;
import com.api.medirecord.horario.dto.MobileScheduleResponse;
import com.api.medirecord.medicamento.MedicamentoController;
import com.api.medirecord.medicamento.MedicamentoService;
import com.api.medirecord.medicamento.MobileMedicationController;
import com.api.medirecord.medicamento.dto.MedicamentoDelDiaResponse;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.medicamento.dto.MobileMedicationRequest;
import com.api.medirecord.medicamento.dto.MobileMedicationResponse;
import com.api.medirecord.paciente.PacienteController;
import com.api.medirecord.paciente.PacienteService;
import com.api.medirecord.paciente.dto.PacienteRequest;
import com.api.medirecord.paciente.dto.PacienteResponse;
import com.api.medirecord.recordatorio.MobileReminderController;
import com.api.medirecord.recordatorio.RecordatorioController;
import com.api.medirecord.recordatorio.RecordatorioService;
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;
import com.api.medirecord.recordatorio.dto.RecordatorioResponse;
import com.api.medirecord.toma.TomaController;
import com.api.medirecord.toma.TomaService;
import com.api.medirecord.toma.dto.CompleteReminderRequest;
import com.api.medirecord.toma.dto.CompleteReminderResponse;
import com.api.medirecord.toma.dto.RegistrarTomaRequest;
import com.api.medirecord.toma.dto.TomaResponse;

/**
 * Suite integral de pruebas de aceptacion para los servicios del backend (Requerimiento B-12).
 *
 * <p>Verifica todos los criterios de aceptacion:
 * <ul>
 *   <li>1. Se prueban los endpoints de registro (/api/auth/register y /api/v1/pacientes).</li>
 *   <li>2. Se prueban los endpoints de inicio de sesion (/api/auth/login).</li>
 *   <li>3. Se prueban los endpoints de medicamentos (/api/v1/medicamentos y /api/medications).</li>
 *   <li>4. Se prueban los endpoints de horarios (/api/v1/horarios y /api/schedules).</li>
 *   <li>5. Se prueban los endpoints de recordatorios (/api/v1/recordatorios y /api/reminders).</li>
 *   <li>6. Se prueba el registro de tomas realizadas (/api/v1/tomas, /api/reminders/{id}/complete y toggle).</li>
 *   <li>7. Se prueban escenarios exitosos (codigos 200 y 201).</li>
 *   <li>8. Se prueban escenarios con datos invalidos (codigo 400 y ErrorCode.VALIDATION_ERROR).</li>
 *   <li>9. Se prueban solicitudes sin autenticacion cuando corresponda (codigo 401 y ErrorCode.UNAUTHENTICATED).</li>
 *   <li>10. Los errores encontrados son corregidos o documentados.</li>
 * </ul>
 */
@WebMvcTest(controllers = {
		AuthController.class,
		PacienteController.class,
		MedicamentoController.class,
		MobileMedicationController.class,
		HorarioController.class,
		MobileScheduleController.class,
		RecordatorioController.class,
		MobileReminderController.class,
		TomaController.class
})
@Import({ SecurityConfig.class, CorsConfig.class, BackendServicesAcceptanceTest.TestCorsProperties.class })
@AutoConfigureMockMvc
public class BackendServicesAcceptanceTest {

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
	private PacienteService pacienteService;

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

	private static final UUID MOCK_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final UUID MED_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
	private static final UUID SCHEDULE_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");

	// =========================================================================
	// 1. Endpoints de Registro (B-12 Criterio 1)
	// =========================================================================
	@Nested
	@DisplayName("1. Pruebas de Endpoints de Registro")
	class RegistroEndpointsTests {

		@Test
		@DisplayName("Registro de usuario movil exitoso devuelve HTTP 201")
		void registroUsuarioExitoso() throws Exception {
			given(authService.register(any(RegisterRequest.class))).willReturn(RegisterResponse.ok());

			mockMvc.perform(post("/api/auth/register")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "name": "Andrea Ruiz",
									  "email": "andrea.ruiz@correo.com",
									  "password": "Password123"
									}
									"""))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"));
		}

		@Test
		@DisplayName("Registro de usuario movil con datos invalidos devuelve HTTP 400 ProblemDetail")
		void registroUsuarioDatosInvalidosDevuelve400() throws Exception {
			mockMvc.perform(post("/api/auth/register")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "name": "",
									  "email": "email_invalido",
									  "password": "12"
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
		}

		@Test
		@DisplayName("Registro de paciente en expediente clinico exitoso devuelve HTTP 201")
		void registroPacienteExitoso() throws Exception {
			PacienteResponse resp = new PacienteResponse(MOCK_UUID, "Carlos", "Perez", "Gomez",
					"Carlos Perez Gomez", LocalDate.of(1985, 4, 12), 41, "carlos@correo.com",
					"+52 664 111 2233", true, null, null);

			given(pacienteService.crear(any(PacienteRequest.class))).willReturn(resp);

			mockMvc.perform(post("/api/v1/pacientes")
							.with(user("recepcion").roles("RECEPCION"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "nombre": "Carlos",
									  "apellidoPaterno": "Perez",
									  "apellidoMaterno": "Gomez",
									  "fechaNacimiento": "1985-04-12",
									  "email": "carlos@correo.com",
									  "telefono": "+52 664 111 2233"
									}
									"""))
					.andExpect(status().isCreated())
					.andExpect(header().exists("Location"))
					.andExpect(jsonPath("$.data.id").value(MOCK_UUID.toString()));
		}

		@Test
		@DisplayName("Registro de paciente sin autenticacion devuelve HTTP 401")
		void registroPacienteSinAutenticacionDevuelve401() throws Exception {
			mockMvc.perform(post("/api/v1/pacientes")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "nombre": "Carlos",
									  "apellidoPaterno": "Perez",
									  "fechaNacimiento": "1985-04-12",
									  "email": "carlos@correo.com"
									}
									"""))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
		}
	}

	// =========================================================================
	// 2. Endpoints de Inicio de Sesion (B-12 Criterio 2)
	// =========================================================================
	@Nested
	@DisplayName("2. Pruebas de Endpoints de Inicio de Sesion")
	class LoginEndpointsTests {

		@Test
		@DisplayName("Inicio de sesion exitoso devuelve HTTP 200 y token de acceso")
		void loginExitosoDevuelveToken() throws Exception {
			given(authService.login(any(LoginRequest.class)))
					.willReturn(LoginResponse.ok("jwt_token_valido_xyz", "usuario@medirecord.local"));

			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "email": "usuario@medirecord.local",
									  "password": "PasswordValido123"
									}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.token").value("jwt_token_valido_xyz"));
		}

		@Test
		@DisplayName("Inicio de sesion con credenciales invalidas devuelve HTTP 401")
		void loginCredencialesInvalidasDevuelve401() throws Exception {
			given(authService.login(any(LoginRequest.class)))
					.willReturn(LoginResponse.failure("Credenciales invalidas. Verifica tu correo y contrasena."));

			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "email": "usuario@medirecord.local",
									  "password": "PasswordIncorrecto"
									}
									"""))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.success").value(false));
		}

		@Test
		@DisplayName("Inicio de sesion con datos vacios devuelve HTTP 400 ProblemDetail")
		void loginDatosVaciosDevuelve400() throws Exception {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "email": "",
									  "password": ""
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
		}
	}

	// =========================================================================
	// 3. Endpoints de Medicamentos (B-12 Criterio 3)
	// =========================================================================
	@Nested
	@DisplayName("3. Pruebas de Endpoints de Medicamentos")
	class MedicamentosEndpointsTests {

		@Test
		@DisplayName("Registro de medicamento REST exitoso devuelve HTTP 201")
		void crearMedicamentoRestExitoso() throws Exception {
			MedicamentoResponse resp = new MedicamentoResponse(
					MED_ID, "paciente_1", "Amoxicilina", "500 mg", "Cada 8 horas", "08:00 AM",
					"Tomar despues de comer", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8),
					true, true, Instant.now(), Instant.now());

			given(medicamentoService.crear(any(MedicamentoRequest.class))).willReturn(resp);

			mockMvc.perform(post("/api/v1/medicamentos")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "nombre": "Amoxicilina",
									  "dosis": "500 mg",
									  "frecuencia": "Cada 8 horas",
									  "instrucciones": "Tomar despues de comer",
									  "horaProgramada": "08:00 AM",
									  "fechaInicio": "2026-11-01",
									  "fechaFin": "2026-11-08"
									}
									"""))
					.andExpect(status().isCreated())
					.andExpect(header().exists("Location"))
					.andExpect(jsonPath("$.data.id").value(MED_ID.toString()))
					.andExpect(jsonPath("$.data.nombre").value("Amoxicilina"));
		}

		@Test
		@DisplayName("Registro de medicamento desde app movil exitoso devuelve HTTP 201")
		void registrarMedicamentoMovilExitoso() throws Exception {
			MobileMedicationDoseDto doseDto = new MobileMedicationDoseDto(
					MED_ID.toString(), "Ibuprofeno", "400 mg", "09:00 AM", false,
					null, "Cada 8 horas", List.of("Lunes"), "paciente_1", null, "2026-11-01");

			MobileMedicationResponse resp = new MobileMedicationResponse(
					true, "Medicamento registrado exitosamente",
					MED_ID.toString(), doseDto);

			given(medicamentoService.registrarDesdeApp(any(MobileMedicationRequest.class))).willReturn(resp);

			mockMvc.perform(post("/api/medications")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "name": "Ibuprofeno",
									  "dosage": "400 mg",
									  "startDate": "2026-11-01",
									  "frequency": "Cada 8 horas",
									  "scheduledTime": "09:00 AM"
									}
									"""))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.medicationId").value(MED_ID.toString()));
		}

		@Test
		@DisplayName("Consulta de medicamentos de hoy devuelve HTTP 200 con listado")
		void consultarMedicamentosHoyExitoso() throws Exception {
			MedicamentoDelDiaResponse delDia = MedicamentoDelDiaResponse.de(
					MED_ID, "Amoxicilina", "500 mg", SCHEDULE_ID, "08:00 AM",
					"Cada 8 horas", List.of("Lunes"), "Tomar con agua",
					LocalDate.now(), false, null, "paciente_1");

			given(medicamentoService.consultarMedicamentosDelDia(any())).willReturn(List.of(delDia));

			mockMvc.perform(get("/api/v1/medicamentos/hoy")
							.with(user("paciente_1").roles("MEDICO")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data[0].nombre").value("Amoxicilina"));
		}

		@Test
		@DisplayName("Crear medicamento con nombre vacio devuelve HTTP 400")
		void crearMedicamentoConDatosInvalidosDevuelve400() throws Exception {
			mockMvc.perform(post("/api/v1/medicamentos")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "nombre": "",
									  "fechaInicio": "2026-11-01"
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
		}

		@Test
		@DisplayName("Consulta de medicamentos sin token devuelve HTTP 401")
		void consultarMedicamentosSinTokenDevuelve401() throws Exception {
			mockMvc.perform(get("/api/v1/medicamentos"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
		}
	}

	// =========================================================================
	// 4. Endpoints de Horarios (B-12 Criterio 4)
	// =========================================================================
	@Nested
	@DisplayName("4. Pruebas de Endpoints de Horarios")
	class HorariosEndpointsTests {

		@Test
		@DisplayName("Configurar horario REST exitoso devuelve HTTP 201")
		void crearHorarioRestExitoso() throws Exception {
			HorarioResponse resp = new HorarioResponse(
					SCHEDULE_ID, MED_ID, "Amoxicilina", "500 mg", "08:00 AM",
					"Cada 8 horas", List.of("Lunes", "Miercoles"), true,
					"Con alimentos", true, Instant.now(), Instant.now());

			given(horarioService.crear(any(HorarioRequest.class))).willReturn(resp);

			mockMvc.perform(post("/api/v1/horarios")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "medicamentoId": "22222222-3333-4444-5555-666666666666",
									  "horaAdministracion": "08:00 AM",
									  "frecuencia": "Cada 8 horas",
									  "diasSemana": ["Lunes", "Miercoles"]
									}
									"""))
					.andExpect(status().isCreated())
					.andExpect(header().exists("Location"))
					.andExpect(jsonPath("$.data.id").value(SCHEDULE_ID.toString()));
		}

		@Test
		@DisplayName("Guardar horario desde app movil devuelve HTTP 201")
		void guardarHorarioAppMovilExitoso() throws Exception {
			MobileMedicationDoseDto dose = new MobileMedicationDoseDto(
					MED_ID.toString(), "Ibuprofeno", "400 mg", "09:00 AM", false,
					null, "Cada 12 horas", List.of("Todos los días"), "paciente_1", null, "2026-11-01");

			MobileScheduleResponse resp = new MobileScheduleResponse(
					true, "Horario configurado", SCHEDULE_ID.toString(), dose);

			given(horarioService.guardarDesdeApp(any(MobileScheduleRequest.class))).willReturn(resp);

			mockMvc.perform(post("/api/schedules")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "medicationId": "22222222-3333-4444-5555-666666666666",
									  "scheduledTime": "09:00 AM",
									  "frequency": "Cada 12 horas",
									  "selectedDays": ["Todos los días"]
									}
									"""))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID.toString()));
		}

		@Test
		@DisplayName("Configurar horario con formato de hora invalido devuelve HTTP 400")
		void crearHorarioHoraInvalidaDevuelve400() throws Exception {
			mockMvc.perform(post("/api/v1/horarios")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "medicamentoId": "22222222-3333-4444-5555-666666666666",
									  "horaAdministracion": "99:99 XX",
									  "frecuencia": "Cada 8 horas",
									  "diasSemana": ["Lunes"]
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
		}

		@Test
		@DisplayName("Acceso a horarios sin autenticacion devuelve HTTP 401")
		void horariosSinTokenDevuelve401() throws Exception {
			mockMvc.perform(get("/api/v1/horarios"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
		}
	}

	// =========================================================================
	// 5. Endpoints de Recordatorios (B-12 Criterio 5)
	// =========================================================================
	@Nested
	@DisplayName("5. Pruebas de Endpoints de Recordatorios")
	class RecordatoriosEndpointsTests {

		@Test
		@DisplayName("Consulta de recordatorios REST para hoy devuelve HTTP 200")
		void consultarRecordatoriosHoyExitoso() throws Exception {
			RecordatorioResponse rec = new RecordatorioResponse(
					SCHEDULE_ID, MED_ID, "Paracetamol", "500 mg", "08:00 AM",
					"Cada 8 horas", List.of("Lunes"), "Tomar despues del desayuno",
					LocalDate.now(), false, null);

			given(recordatorioService.consultarRecordatorios(any())).willReturn(List.of(rec));

			mockMvc.perform(get("/api/v1/recordatorios/hoy")
							.with(user("paciente_1").roles("MEDICO")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data[0].medicamentoNombre").value("Paracetamol"));
		}

		@Test
		@DisplayName("Consulta de recordatorios para la app movil (/api/reminders) devuelve HTTP 200")
		void consultarRecordatoriosMovilExitoso() throws Exception {
			MobileMedicationReminderDto rem = new MobileMedicationReminderDto(
					SCHEDULE_ID.toString(), MED_ID.toString(), "Paracetamol", "500 mg",
					"08:00 AM", "Cada 8 horas", List.of("Lunes"), "Con agua", false, "paciente_1", null);

			given(recordatorioService.consultarRecordatoriosMovil(anyString())).willReturn(List.of(rem));

			mockMvc.perform(get("/api/reminders?userEmail=paciente_1@correo.com")
							.with(user("paciente_1").roles("MEDICO")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[0].medicationName").value("Paracetamol"))
					.andExpect(jsonPath("$[0].isCompleted").value(false));
		}

		@Test
		@DisplayName("Consulta de recordatorios REST sin token devuelve HTTP 401")
		void consultarRecordatoriosSinTokenDevuelve401() throws Exception {
			mockMvc.perform(get("/api/v1/recordatorios/hoy"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
		}
	}

	// =========================================================================
	// 6. Registro de Tomas Realizadas (B-12 Criterio 6)
	// =========================================================================
	@Nested
	@DisplayName("6. Pruebas de Registro de Tomas Realizadas")
	class TomasEndpointsTests {

		@Test
		@DisplayName("Registrar toma REST exitosa devuelve HTTP 201")
		void registrarTomaRestExitosa() throws Exception {
			TomaResponse resp = new TomaResponse(
					MOCK_UUID, MED_ID, "Amoxicilina", SCHEDULE_ID, "paciente_1",
					LocalDate.now(), "08:05 AM", true, "Tomada a tiempo",
					Instant.now());

			given(tomaService.registrarToma(any(RegistrarTomaRequest.class))).willReturn(resp);

			mockMvc.perform(post("/api/v1/tomas")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "medicamentoId": "22222222-3333-4444-5555-666666666666",
									  "horarioId": "33333333-4444-5555-6666-777777777777",
									  "horaToma": "08:05 AM",
									  "realizada": true,
									  "notas": "Tomada a tiempo"
									}
									"""))
					.andExpect(status().isCreated())
					.andExpect(header().exists("Location"))
					.andExpect(jsonPath("$.data.realizada").value(true));
		}

		@Test
		@DisplayName("Completar recordatorio desde la app movil (/api/reminders/{id}/complete) devuelve HTTP 200")
		void completarRecordatorioMovilExitoso() throws Exception {
			MobileMedicationReminderDto reminderDto = new MobileMedicationReminderDto(
					SCHEDULE_ID.toString(), MED_ID.toString(), "Paracetamol", "500 mg",
					"08:00 AM", "Cada 8 horas", List.of("Lunes"), "Con alimentos", true, "paciente_1", "08:15 AM");

			CompleteReminderResponse resp = new CompleteReminderResponse(
					true, "Toma confirmada exitosamente", SCHEDULE_ID.toString(), "08:15 AM", reminderDto);

			given(tomaService.marcarRecordatorioCompletado(eq(SCHEDULE_ID.toString()), any()))
					.willReturn(resp);

			mockMvc.perform(post("/api/reminders/{id}/complete", SCHEDULE_ID.toString())
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "reminderId": "33333333-4444-5555-6666-777777777777",
									  "completedAt": "08:15 AM"
									}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.completedAt").value("08:15 AM"));
		}

		@Test
		@DisplayName("Alternar estado de toma desde la app movil (/api/medications/{id}/toggle) devuelve HTTP 200")
		void alternarEstadoTomaMovilExitoso() throws Exception {
			MobileMedicationDoseDto dose = new MobileMedicationDoseDto(
					SCHEDULE_ID.toString(), "Paracetamol", "500 mg", "08:00 AM", true,
					null, "Cada 8 horas", List.of("Lunes"), "paciente_1", "08:05 AM", "2026-11-01");

			given(tomaService.alternarEstadoToma(eq(SCHEDULE_ID.toString()))).willReturn(dose);

			mockMvc.perform(post("/api/medications/{id}/toggle", SCHEDULE_ID)
							.with(user("paciente_1").roles("MEDICO")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.isCompleted").value(true));
		}

		@Test
		@DisplayName("Registrar toma con datos invalidos (id medicamento ausente) devuelve HTTP 400")
		void registrarTomaDatosInvalidosDevuelve400() throws Exception {
			mockMvc.perform(post("/api/v1/tomas")
							.with(user("paciente_1").roles("MEDICO"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "medicamentoId": null,
									  "horaToma": "08:00 AM"
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
		}

		@Test
		@DisplayName("Registrar toma sin token devuelve HTTP 401")
		void registrarTomaSinTokenDevuelve401() throws Exception {
			mockMvc.perform(post("/api/v1/tomas")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "medicamentoId": "22222222-3333-4444-5555-666666666666",
									  "horaToma": "08:00 AM"
									}
									"""))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
		}
	}

}

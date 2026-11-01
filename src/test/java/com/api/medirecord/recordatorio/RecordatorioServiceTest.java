package com.api.medirecord.recordatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.api.medirecord.horario.Horario;
import com.api.medirecord.horario.HorarioRepository;
import com.api.medirecord.medicamento.Medicamento;
import com.api.medirecord.medicamento.MedicamentoRepository;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;
import com.api.medirecord.recordatorio.dto.RecordatorioResponse;
import com.api.medirecord.toma.TomaMedicamento;
import com.api.medirecord.toma.TomaMedicamentoRepository;

/**
 * Tests unitarios del servicio de recordatorios (B-08 y B-09).
 */
@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

	private static final String USUARIO_PROPIETARIO = "usuario-123";

	@Mock
	private HorarioRepository horarioRepository;

	@Mock
	private MedicamentoRepository medicamentoRepository;

	@Mock
	private TomaMedicamentoRepository tomaRepository;

	@InjectMocks
	private RecordatorioService service;

	@BeforeEach
	void autenticar() {
		var auth = new UsernamePasswordAuthenticationToken(
				USUARIO_PROPIETARIO,
				"N/A",
				List.of(new SimpleGrantedAuthority("ROLE_MEDICO")));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	private static Medicamento crearMedicamento(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
		Medicamento m = new Medicamento(
				USUARIO_PROPIETARIO,
				nombre,
				"500 mg",
				"Cada 8 horas",
				"Con alimentos",
				fechaInicio,
				fechaFin);
		ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
		return m;
	}

	private static Horario crearHorario(Medicamento med, String hora, String frecuencia, List<String> dias, boolean activo, String notas) {
		Horario h = new Horario(med, hora, frecuencia, dias, activo, notas);
		ReflectionTestUtils.setField(h, "id", UUID.randomUUID());
		return h;
	}

	@Test
	@DisplayName("El servicio consulta horarios registrados y los relaciona correctamente con los medicamentos")
	void consultaHorariosYRelacionaConMedicamentos() {
		// 2026-10-26 fue Lunes
		LocalDate fechaLunes = LocalDate.of(2026, 10, 26);
		Medicamento med = crearMedicamento("Paracetamol", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 20));

		Horario horario = crearHorario(
				med,
				"08:00 AM",
				"Lunes, Miércoles, Viernes",
				List.of("Lunes", "Miércoles", "Viernes"),
				true,
				"Tomar con agua");

		when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
				.thenReturn(List.of(horario));
		when(medicamentoRepository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(USUARIO_PROPIETARIO))
				.thenReturn(List.of(med));

		List<RecordatorioResponse> recordatorios = service.consultarRecordatorios(fechaLunes);

		assertThat(recordatorios).hasSize(1);
		RecordatorioResponse resp = recordatorios.getFirst();
		assertThat(resp.medicamentoNombre()).isEqualTo("Paracetamol");
		assertThat(resp.dosis()).isEqualTo("500 mg");
		assertThat(resp.horaAdministracion()).isEqualTo("08:00 AM");
		assertThat(resp.diasSemana()).containsExactly("Lunes", "Miércoles", "Viernes");
		assertThat(resp.usuarioId()).isEqualTo(USUARIO_PROPIETARIO);
		assertThat(resp.fechaConsultada()).isEqualTo(fechaLunes);
	}

	@Test
	@DisplayName("Identifica y filtra recordatorios segun los dias configurados en el horario")
	void filtraPorDiasConfigurados() {
		// 2026-10-27 fue Martes
		LocalDate fechaMartes = LocalDate.of(2026, 10, 27);
		Medicamento med = crearMedicamento("Amoxicilina", LocalDate.of(2026, 10, 20), null);

		// Horario configurado solo para Lunes y Viernes
		Horario horario = crearHorario(
				med,
				"08:00 AM",
				"Días específicos",
				List.of("Lunes", "Viernes"),
				true,
				null);

		when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
				.thenReturn(List.of(horario));
		when(medicamentoRepository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(USUARIO_PROPIETARIO))
				.thenReturn(List.of(med));

		List<RecordatorioResponse> recordatorios = service.consultarRecordatorios(fechaMartes);
		assertThat(recordatorios).isEmpty();
	}

	@Test
	@DisplayName("Descarta recordatorios cuando la fecha consultada esta fuera de la vigencia del tratamiento")
	void descartaFueraDeVigencia() {
		// Fecha posterior al fin del tratamiento
		LocalDate fechaExpirada = LocalDate.of(2026, 11, 15);
		Medicamento medFinalizado = crearMedicamento("Ibuprofeno", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

		Horario horario = crearHorario(
				medFinalizado,
				"09:00 AM",
				"Todos los días (Diario)",
				Horario.TODOS_LOS_DIAS,
				true,
				null);

		when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
				.thenReturn(List.of(horario));
		when(medicamentoRepository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(USUARIO_PROPIETARIO))
				.thenReturn(List.of(medFinalizado));

		List<RecordatorioResponse> recordatorios = service.consultarRecordatorios(fechaExpirada);
		assertThat(recordatorios).isEmpty();
	}

	@Test
	@DisplayName("Ordena cronologicamente los recordatorios por hora")
	void ordenaCronologicamente() {
		// 2026-10-26 es Lunes
		LocalDate fecha = LocalDate.of(2026, 10, 26);
		Medicamento med1 = crearMedicamento("Noche", LocalDate.of(2026, 10, 1), null);
		Medicamento med2 = crearMedicamento("Manana", LocalDate.of(2026, 10, 1), null);

		Horario h1 = crearHorario(med1, "08:00 PM", "Diario", Horario.TODOS_LOS_DIAS, true, null);
		Horario h2 = crearHorario(med2, "08:00 AM", "Diario", Horario.TODOS_LOS_DIAS, true, null);

		when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
				.thenReturn(List.of(h1, h2));
		when(medicamentoRepository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(USUARIO_PROPIETARIO))
				.thenReturn(List.of(med1, med2));

		List<RecordatorioResponse> recordatorios = service.consultarRecordatorios(fecha);
		assertThat(recordatorios).hasSize(2);
		assertThat(recordatorios.get(0).medicamentoNombre()).isEqualTo("Manana");
		assertThat(recordatorios.get(1).medicamentoNombre()).isEqualTo("Noche");
	}

	@Test
	@DisplayName("Los datos estan disponibles para la app movil y reflejan tomas realizadas almacenadas")
	void datosDisponiblesParaAppMovilReflejanTomasRealizadas() {
		Medicamento med = crearMedicamento("Omeprazol", LocalDate.of(2026, 10, 1), null);
		Horario horario = crearHorario(med, "07:00 AM", "Diario", Horario.TODOS_LOS_DIAS, true, "En ayunas");

		when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
				.thenReturn(List.of(horario));
		when(medicamentoRepository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(USUARIO_PROPIETARIO))
				.thenReturn(List.of(med));

		TomaMedicamento tomaRealizada = new TomaMedicamento(
				med,
				horario,
				USUARIO_PROPIETARIO,
				LocalDate.now(),
				"07:05 AM",
				true,
				"Completado");

		when(tomaRepository.findByUsuarioIdAndFechaToma(eq(USUARIO_PROPIETARIO), any(LocalDate.class)))
				.thenReturn(List.of(tomaRealizada));

		List<MobileMedicationReminderDto> reminders = service.consultarRecordatoriosMovil(USUARIO_PROPIETARIO);
		assertThat(reminders).hasSize(1);
		assertThat(reminders.getFirst().medicationName()).isEqualTo("Omeprazol");
		assertThat(reminders.getFirst().scheduledTime()).isEqualTo("07:00 AM");
		assertThat(reminders.getFirst().isCompleted()).isTrue();
		assertThat(reminders.getFirst().completedAt()).isEqualTo("07:05 AM");

		List<MobileMedicationDoseDto> dosis = service.consultarDosisFechaMovil(LocalDate.now(), USUARIO_PROPIETARIO);
		assertThat(dosis).hasSize(1);
		assertThat(dosis.getFirst().name()).isEqualTo("Omeprazol");
		assertThat(dosis.getFirst().isCompleted()).isTrue();
		assertThat(dosis.getFirst().completedAt()).isEqualTo("07:05 AM");
	}

}

package com.api.medirecord.toma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.toma.dto.CompleteReminderRequest;
import com.api.medirecord.toma.dto.CompleteReminderResponse;
import com.api.medirecord.toma.dto.RegistrarTomaRequest;
import com.api.medirecord.toma.dto.TomaResponse;

/**
 * Tests unitarios de la logica de registro de tomas realizadas (B-09).
 */
@ExtendWith(MockitoExtension.class)
class TomaServiceTest {

	private static final String USUARIO_PROPIETARIO = "usuario-123";
	private static final String OTRO_USUARIO = "usuario-999";

	@Mock
	private TomaMedicamentoRepository tomaRepository;

	@Mock
	private MedicamentoRepository medicamentoRepository;

	@Mock
	private HorarioRepository horarioRepository;

	@InjectMocks
	private TomaService service;

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

	private static Medicamento crearMedicamento(String usuarioId, String nombre) {
		Medicamento m = new Medicamento(
				usuarioId,
				nombre,
				"500 mg",
				"Cada 8 horas",
				"08:00 AM",
				"Con alimentos",
				LocalDate.of(2026, 10, 1),
				null);
		ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
		return m;
	}

	private static Horario crearHorario(Medicamento med, String hora) {
		Horario h = new Horario(med, hora, "Diario", Horario.TODOS_LOS_DIAS, true, null);
		ReflectionTestUtils.setField(h, "id", UUID.randomUUID());
		return h;
	}

	@Test
	@DisplayName("Registra una toma realizada y almacena el estado correctamente")
	void registrarTomaRealizada() {
		Medicamento med = crearMedicamento(USUARIO_PROPIETARIO, "Paracetamol");
		Horario horario = crearHorario(med, "08:00 AM");

		when(medicamentoRepository.findById(med.getId())).thenReturn(Optional.of(med));
		when(horarioRepository.findById(horario.getId())).thenReturn(Optional.of(horario));
		when(tomaRepository.findTomaExistente(USUARIO_PROPIETARIO, med.getId(), horario.getId(), LocalDate.now()))
				.thenReturn(Optional.empty());
		when(tomaRepository.save(any(TomaMedicamento.class))).thenAnswer(inv -> inv.getArgument(0));

		var request = new RegistrarTomaRequest(
				med.getId(),
				horario.getId(),
				LocalDate.now(),
				"08:10 AM",
				true,
				"Toma a tiempo");

		TomaResponse response = service.registrarToma(request);

		ArgumentCaptor<TomaMedicamento> captor = ArgumentCaptor.forClass(TomaMedicamento.class);
		verify(tomaRepository).save(captor.capture());

		TomaMedicamento guardada = captor.getValue();
		assertThat(guardada.isRealizada()).isTrue();
		assertThat(guardada.getHoraToma()).isEqualTo("08:10 AM");
		assertThat(guardada.getUsuarioId()).isEqualTo(USUARIO_PROPIETARIO);

		assertThat(response.medicamentoNombre()).isEqualTo("Paracetamol");
		assertThat(response.realizada()).isTrue();
		assertThat(response.horaToma()).isEqualTo("08:10 AM");
	}

	@Test
	@DisplayName("Un usuario no puede registrar tomas de un medicamento perteneciente a otro usuario")
	void bloqueaTomaDeOtroUsuario() {
		Medicamento medAjeno = crearMedicamento(OTRO_USUARIO, "Omeprazol");
		when(medicamentoRepository.findById(medAjeno.getId())).thenReturn(Optional.of(medAjeno));

		var request = new RegistrarTomaRequest(
				medAjeno.getId(),
				null,
				LocalDate.now(),
				"08:00 AM",
				true,
				null);

		assertThatThrownBy(() -> service.registrarToma(request))
				.isInstanceOf(MedicamentoAccesoDenegadoException.class);
	}

	@Test
	@DisplayName("Marca recordatorio como completado desde la aplicacion movil")
	void marcarRecordatorioCompletado() {
		Medicamento med = crearMedicamento(USUARIO_PROPIETARIO, "Amoxicilina");
		Horario horario = crearHorario(med, "09:00 AM");

		when(horarioRepository.findById(horario.getId())).thenReturn(Optional.of(horario));
		when(tomaRepository.findTomaExistente(USUARIO_PROPIETARIO, med.getId(), horario.getId(), LocalDate.now()))
				.thenReturn(Optional.empty());
		when(tomaRepository.save(any(TomaMedicamento.class))).thenAnswer(inv -> inv.getArgument(0));

		var request = new CompleteReminderRequest(horario.getId().toString(), "09:05 AM", USUARIO_PROPIETARIO);
		CompleteReminderResponse response = service.marcarRecordatorioCompletado(horario.getId().toString(), request);

		assertThat(response.success()).isTrue();
		assertThat(response.completedAt()).isEqualTo("09:05 AM");
		assertThat(response.updatedReminder()).isNotNull();
		assertThat(response.updatedReminder().isCompleted()).isTrue();
		assertThat(response.updatedReminder().completedAt()).isEqualTo("09:05 AM");
	}

	@Test
	@DisplayName("Alternar estado de toma (toggle) invierte entre realizada y pendiente")
	void alternarEstadoToma() {
		Medicamento med = crearMedicamento(USUARIO_PROPIETARIO, "Ibuprofeno");
		Horario horario = crearHorario(med, "02:00 PM");

		when(horarioRepository.findById(horario.getId())).thenReturn(Optional.of(horario));
		when(tomaRepository.findTomaExistente(USUARIO_PROPIETARIO, med.getId(), horario.getId(), LocalDate.now()))
				.thenReturn(Optional.empty());
		when(tomaRepository.save(any(TomaMedicamento.class))).thenAnswer(inv -> inv.getArgument(0));

		// Primera llamada: de pendiente a realizada
		MobileMedicationDoseDto dose1 = service.alternarEstadoToma(horario.getId().toString());
		assertThat(dose1.isCompleted()).isTrue();
		assertThat(dose1.completedAt()).isNotNull();

		// Simulamos que ahora existe en BD como realizada
		TomaMedicamento tomaExistente = new TomaMedicamento(med, horario, USUARIO_PROPIETARIO, LocalDate.now(), "02:05 PM", true, null);
		when(tomaRepository.findTomaExistente(USUARIO_PROPIETARIO, med.getId(), horario.getId(), LocalDate.now()))
				.thenReturn(Optional.of(tomaExistente));

		// Segunda llamada: de realizada a pendiente
		MobileMedicationDoseDto dose2 = service.alternarEstadoToma(horario.getId().toString());
		assertThat(dose2.isCompleted()).isFalse();
		assertThat(dose2.completedAt()).isNull();
	}

}

package com.api.medirecord.medicamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.horario.Horario;
import com.api.medirecord.horario.HorarioRepository;
import com.api.medirecord.medicamento.dto.MedicamentoDelDiaResponse;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.medicamento.dto.MobileMedicationRequest;
import com.api.medirecord.medicamento.dto.MobileMedicationResponse;
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoFechasInvalidasException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;
import com.api.medirecord.toma.TomaMedicamento;
import com.api.medirecord.toma.TomaMedicamentoRepository;

/**
 * Tests unitarios de las reglas de negocio de medicamentos (B-05, B-06 y B-10).
 */
@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

	private static final String USUARIO_PROPIETARIO = "usuario-123";
	private static final String OTRO_USUARIO = "usuario-999";

	@Mock
	private MedicamentoRepository repository;

	@Mock
	private HorarioRepository horarioRepository;

	@Mock
	private TomaMedicamentoRepository tomaRepository;

	@InjectMocks
	private MedicamentoService service;

	@BeforeEach
	void autenticarUsuario() {
		autenticarComo(USUARIO_PROPIETARIO);
	}

	@AfterEach
	void limpiarContextoSeguridad() {
		SecurityContextHolder.clearContext();
	}

	private static void autenticarComo(String usuarioId) {
		var auth = new UsernamePasswordAuthenticationToken(
				usuarioId,
				"N/A",
				List.of(new SimpleGrantedAuthority("ROLE_MEDICO")));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private static Medicamento crearMedicamento(String usuarioId, String nombre, LocalDate inicio, LocalDate fin) {
		Medicamento m = new Medicamento(
				usuarioId,
				nombre,
				"500 mg",
				"Cada 8 horas",
				"08:00 AM",
				"Con alimentos",
				inicio,
				fin);
		ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
		return m;
	}

	private static Horario crearHorario(Medicamento med, String hora, List<String> dias) {
		Horario h = new Horario(med, hora, "Diario", dias, true, null);
		ReflectionTestUtils.setField(h, "id", UUID.randomUUID());
		return h;
	}

	@Nested
	@DisplayName("crear")
	class Crear {

		@Test
		@DisplayName("registra el medicamento con fecha de inicio, fecha de fin, hora programada y lo asocia al usuario autenticado")
		void guardaMedicamentoAsociadoAlUsuario() {
			var request = new MedicamentoRequest(
					"  Paracetamol 500mg  ",
					" 1 tableta ",
					" Cada 8 horas ",
					" 08:00 AM ",
					" Con alimentos ",
					LocalDate.of(2026, 10, 25),
					LocalDate.of(2026, 11, 5));

			when(repository.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

			MedicamentoResponse response = service.crear(request);

			ArgumentCaptor<Medicamento> captor = ArgumentCaptor.forClass(Medicamento.class);
			verify(repository).save(captor.capture());

			Medicamento saved = captor.getValue();
			assertThat(saved.getUsuarioId()).isEqualTo(USUARIO_PROPIETARIO);
			assertThat(saved.getNombre()).isEqualTo("Paracetamol 500mg");
			assertThat(saved.getDosis()).isEqualTo("1 tableta");
			assertThat(saved.getFrecuencia()).isEqualTo("Cada 8 horas");
			assertThat(saved.getHoraProgramada()).isEqualTo("08:00 AM");
			assertThat(saved.getInstrucciones()).isEqualTo("Con alimentos");
			assertThat(saved.getFechaInicio()).isEqualTo(LocalDate.of(2026, 10, 25));
			assertThat(saved.getFechaFin()).isEqualTo(LocalDate.of(2026, 11, 5));
			assertThat(saved.isActivo()).isTrue();

			assertThat(response.usuarioId()).isEqualTo(USUARIO_PROPIETARIO);
			assertThat(response.nombre()).isEqualTo("Paracetamol 500mg");
			assertThat(response.horaProgramada()).isEqualTo("08:00 AM");
		}

		@Test
		@DisplayName("permite registrar un medicamento sin fecha de finalizacion (tratamiento continuo)")
		void permiteSinFechaFin() {
			var request = new MedicamentoRequest(
					"Losartan",
					"50 mg",
					"Cada 24 horas",
					null,
					LocalDate.of(2026, 10, 1),
					null);

			when(repository.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

			MedicamentoResponse response = service.crear(request);

			assertThat(response.fechaInicio()).isEqualTo(LocalDate.of(2026, 10, 1));
			assertThat(response.fechaFin()).isNull();
			assertThat(response.vigente()).isTrue();
		}

		@Test
		@DisplayName("rechaza fecha de finalizacion anterior a la fecha de inicio")
		void rechazaFechaFinAnteriorAFechaInicio() {
			var request = new MedicamentoRequest(
					"Ibuprofeno",
					"400 mg",
					"Cada 8 horas",
					null,
					LocalDate.of(2026, 10, 25),
					LocalDate.of(2026, 10, 20));

			assertThatThrownBy(() -> service.crear(request))
					.isInstanceOf(MedicamentoFechasInvalidasException.class);

			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("registra desde la aplicacion movil parseando fechas dd/MM/yyyy y asociando al usuario autenticado")
		void registraDesdeAppMovil() {
			var request = new MobileMedicationRequest(
					"Amoxicilina",
					"500 mg - 1 capsula",
					"25/10/2026",
					"01/11/2026",
					"Cada 8 horas",
					"09:00 AM",
					"Tomar con agua",
					"correo-ignorado@test.com");

			when(repository.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

			MobileMedicationResponse response = service.registrarDesdeApp(request);

			assertThat(response.success()).isTrue();
			assertThat(response.message()).contains("Amoxicilina");
			assertThat(response.medication().userEmail()).isEqualTo(USUARIO_PROPIETARIO);
			assertThat(response.medication().scheduledDate()).isEqualTo("2026-10-25");
		}

		@Test
		@DisplayName("rechaza formato de fecha invalido desde la aplicacion movil")
		void rechazaFechaInvalidaDesdeApp() {
			var request = new MobileMedicationRequest(
					"Amoxicilina",
					"500 mg",
					"fecha-invalida",
					null,
					"Diario",
					"08:00 AM",
					null,
					null);

			assertThatThrownBy(() -> service.registrarDesdeApp(request))
					.isInstanceOf(ApiException.class)
					.hasMessageContaining("startDate");

			verify(repository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("consultar medicamentos del dia (B-10)")
	class ConsultarMedicamentosDelDia {

		@Test
		@DisplayName("devuelve medicamentos asociados a sus horarios e indica estado pendiente o realizada")
		void consultaMedicamentosDelDiaConEstado() {
			// 2026-10-26 es Lunes
			LocalDate lunes = LocalDate.of(2026, 10, 26);
			Medicamento med1 = crearMedicamento(USUARIO_PROPIETARIO, "Paracetamol", LocalDate.of(2026, 10, 1), null);
			Medicamento med2 = crearMedicamento(USUARIO_PROPIETARIO, "Omeprazol", LocalDate.of(2026, 10, 1), null);

			Horario h1 = crearHorario(med1, "08:00 AM", List.of("Lunes", "Miércoles", "Viernes"));
			Horario h2 = crearHorario(med2, "09:00 AM", Horario.TODOS_LOS_DIAS);

			when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
					.thenReturn(List.of(h1, h2));
			when(repository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(USUARIO_PROPIETARIO))
					.thenReturn(List.of(med1, med2));

			// Simulamos que la primera toma ya fue realizada
			TomaMedicamento tomaRealizada = new TomaMedicamento(
					med1,
					h1,
					USUARIO_PROPIETARIO,
					lunes,
					"08:05 AM",
					true,
					null);
			when(tomaRepository.findByUsuarioIdAndFechaToma(USUARIO_PROPIETARIO, lunes))
					.thenReturn(List.of(tomaRealizada));

			List<MedicamentoDelDiaResponse> delDia = service.consultarMedicamentosDelDia(lunes);

			assertThat(delDia).hasSize(2);

			MedicamentoDelDiaResponse r1 = delDia.get(0);
			assertThat(r1.nombre()).isEqualTo("Paracetamol");
			assertThat(r1.horaProgramada()).isEqualTo("08:00 AM");
			assertThat(r1.realizada()).isTrue();
			assertThat(r1.horaRealizada()).isEqualTo("08:05 AM");
			assertThat(r1.estado()).isEqualTo("REALIZADA");

			MedicamentoDelDiaResponse r2 = delDia.get(1);
			assertThat(r2.nombre()).isEqualTo("Omeprazol");
			assertThat(r2.horaProgramada()).isEqualTo("09:00 AM");
			assertThat(r2.realizada()).isFalse();
			assertThat(r2.horaRealizada()).isNull();
			assertThat(r2.estado()).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("devuelve una respuesta valida vacia si no existen medicamentos programados para el dia")
		void devuelveVacioSiNoHayMedicamentos() {
			when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
					.thenReturn(List.of());
			when(repository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(USUARIO_PROPIETARIO))
					.thenReturn(List.of());

			List<MedicamentoDelDiaResponse> delDia = service.consultarMedicamentosDelDia(LocalDate.now());
			assertThat(delDia).isNotNull().isEmpty();

			List<MobileMedicationDoseDto> dosisMovil = service.consultarDosisHoyMovil(LocalDate.now(), USUARIO_PROPIETARIO);
			assertThat(dosisMovil).isNotNull().isEmpty();
		}
	}

	@Nested
	@DisplayName("aislamiento por usuario")
	class AislamientoPorUsuario {

		@Test
		@DisplayName("un usuario no puede obtener un medicamento perteneciente a otro usuario")
		void bloqueaObtenerDeOtroUsuario() {
			UUID id = UUID.randomUUID();
			Medicamento deOtro = new Medicamento(
					OTRO_USUARIO,
					"Omeprazol",
					"20 mg",
					"Diario",
					null,
					LocalDate.of(2026, 10, 10),
					null);
			when(repository.findById(id)).thenReturn(Optional.of(deOtro));

			assertThatThrownBy(() -> service.obtener(id))
					.isInstanceOf(MedicamentoAccesoDenegadoException.class);
		}

		@Test
		@DisplayName("un usuario no puede actualizar un medicamento perteneciente a otro usuario")
		void bloqueaActualizarDeOtroUsuario() {
			UUID id = UUID.randomUUID();
			Medicamento deOtro = new Medicamento(
					OTRO_USUARIO,
					"Omeprazol",
					"20 mg",
					"Diario",
					null,
					LocalDate.of(2026, 10, 10),
					null);
			when(repository.findById(id)).thenReturn(Optional.of(deOtro));

			var request = new MedicamentoRequest(
					"Omeprazol Modificado",
					"40 mg",
					"Diario",
					null,
					LocalDate.of(2026, 10, 10),
					null);

			assertThatThrownBy(() -> service.actualizar(id, request))
					.isInstanceOf(MedicamentoAccesoDenegadoException.class);

			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("un usuario no puede dar de baja un medicamento perteneciente a otro usuario")
		void bloqueaDesactivarDeOtroUsuario() {
			UUID id = UUID.randomUUID();
			Medicamento deOtro = new Medicamento(
					OTRO_USUARIO,
					"Omeprazol",
					"20 mg",
					"Diario",
					null,
					LocalDate.of(2026, 10, 10),
					null);
			when(repository.findById(id)).thenReturn(Optional.of(deOtro));

			assertThatThrownBy(() -> service.desactivar(id))
					.isInstanceOf(MedicamentoAccesoDenegadoException.class);

			assertThat(deOtro.isActivo()).isTrue();
			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("lanza NOT_FOUND cuando el medicamento no existe")
		void lanzaNotFound() {
			UUID id = UUID.randomUUID();
			when(repository.findById(id)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.obtener(id))
					.isInstanceOf(MedicamentoNotFoundException.class);
		}
	}

}

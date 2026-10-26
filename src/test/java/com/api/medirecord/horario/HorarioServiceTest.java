package com.api.medirecord.horario;

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

import com.api.medirecord.horario.dto.HorarioRequest;
import com.api.medirecord.horario.dto.HorarioResponse;
import com.api.medirecord.medicamento.Medicamento;
import com.api.medirecord.medicamento.MedicamentoRepository;
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

/**
 * Tests unitarios del modelo y servicio de horarios (B-07).
 */
@ExtendWith(MockitoExtension.class)
class HorarioServiceTest {

	private static final String USUARIO_PROPIETARIO = "usuario-123";
	private static final String OTRO_USUARIO = "usuario-999";

	@Mock
	private HorarioRepository horarioRepository;

	@Mock
	private MedicamentoRepository medicamentoRepository;

	@InjectMocks
	private HorarioService service;

	@BeforeEach
	void autenticarUsuario() {
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

	private static Medicamento medicamentoDe(String usuarioId) {
		return new Medicamento(
				usuarioId,
				"Paracetamol",
				"500 mg - 1 tableta",
				"Cada 8 horas",
				"08:00 AM",
				"Con alimentos",
				LocalDate.of(2026, 10, 25),
				LocalDate.of(2026, 11, 5));
	}

	@Nested
	@DisplayName("crear horario")
	class CrearHorario {

		@Test
		@DisplayName("almacena hora de administracion, frecuencia y dias asociados a un medicamento existente")
		void guardaHorarioCorrectamente() {
			UUID medicamentoId = UUID.randomUUID();
			Medicamento medicamento = medicamentoDe(USUARIO_PROPIETARIO);
			when(medicamentoRepository.findById(medicamentoId)).thenReturn(Optional.of(medicamento));
			when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

			var request = new HorarioRequest(
					medicamentoId,
					" 08:30 AM ",
					" Días específicos de la semana ",
					List.of("Lunes", "Miércoles", "Viernes"),
					true,
					" Tomar después del desayuno ");

			HorarioResponse response = service.crear(request);

			ArgumentCaptor<Horario> captor = ArgumentCaptor.forClass(Horario.class);
			verify(horarioRepository).save(captor.capture());

			Horario saved = captor.getValue();
			assertThat(saved.getMedicamento()).isSameAs(medicamento);
			assertThat(saved.getHoraAdministracion()).isEqualTo("08:30 AM");
			assertThat(saved.getFrecuencia()).isEqualTo("Días específicos de la semana");
			assertThat(saved.getDiasSeleccionados()).containsExactly("Lunes", "Miércoles", "Viernes");
			assertThat(saved.isRecordatorioActivo()).isTrue();
			assertThat(saved.getNotas()).isEqualTo("Tomar después del desayuno");

			assertThat(response.medicamentoNombre()).isEqualTo("Paracetamol");
			assertThat(response.horaAdministracion()).isEqualTo("08:30 AM");
			assertThat(response.diasSemana()).containsExactly("Lunes", "Miércoles", "Viernes");
		}

		@Test
		@DisplayName("no permite asociar un horario a un medicamento inexistente")
		void rechazaMedicamentoInexistente() {
			UUID idInexistente = UUID.randomUUID();
			when(medicamentoRepository.findById(idInexistente)).thenReturn(Optional.empty());

			var request = new HorarioRequest(
					idInexistente,
					"08:00 AM",
					"Cada 8 horas",
					null,
					true,
					null);

			assertThatThrownBy(() -> service.crear(request))
					.isInstanceOf(MedicamentoNotFoundException.class);

			verify(horarioRepository, never()).save(any());
		}

		@Test
		@DisplayName("no permite asociar un horario a un medicamento de otro usuario")
		void rechazaMedicamentoDeOtroUsuario() {
			UUID idAjeno = UUID.randomUUID();
			when(medicamentoRepository.findById(idAjeno)).thenReturn(Optional.of(medicamentoDe(OTRO_USUARIO)));

			var request = new HorarioRequest(
					idAjeno,
					"08:00 AM",
					"Cada 8 horas",
					null,
					true,
					null);

			assertThatThrownBy(() -> service.crear(request))
					.isInstanceOf(MedicamentoAccesoDenegadoException.class);

			verify(horarioRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("disponibilidad para el servicio de recordatorios")
	class DisponibilidadRecordatorios {

		@Test
		@DisplayName("expone los horarios activos con la informacion completa del medicamento para el servicio de recordatorios")
		void proveeHorariosParaRecordatorios() {
			Medicamento medicamento = medicamentoDe(USUARIO_PROPIETARIO);
			Horario horario = new Horario(
					medicamento,
					"09:00 AM",
					"Todos los días (Diario)",
					List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"),
					true,
					"En ayunas");

			when(horarioRepository.findHorariosParaRecordatoriosPorUsuario(USUARIO_PROPIETARIO))
					.thenReturn(List.of(horario));

			List<HorarioResponse> disponibles = service.obtenerHorariosParaRecordatorios();

			assertThat(disponibles).hasSize(1);
			HorarioResponse item = disponibles.getFirst();
			assertThat(item.medicamentoNombre()).isEqualTo("Paracetamol");
			assertThat(item.medicamentoDosis()).isEqualTo("500 mg - 1 tableta");
			assertThat(item.horaAdministracion()).isEqualTo("09:00 AM");
			assertThat(item.frecuencia()).isEqualTo("Todos los días (Diario)");
			assertThat(item.diasSemana()).hasSize(7);
			assertThat(item.recordatorioActivo()).isTrue();
		}
	}

}

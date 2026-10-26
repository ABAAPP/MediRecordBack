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

import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoFechasInvalidasException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

/**
 * Tests unitarios de las reglas de negocio de medicamentos (B-05).
 */
@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

	private static final String USUARIO_PROPIETARIO = "usuario-123";
	private static final String OTRO_USUARIO = "usuario-999";

	@Mock
	private MedicamentoRepository repository;

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

	@Nested
	@DisplayName("crear")
	class Crear {

		@Test
		@DisplayName("registra el medicamento con fecha de inicio, fecha de fin y lo asocia al usuario autenticado")
		void guardaMedicamentoAsociadoAlUsuario() {
			var request = new MedicamentoRequest(
					"  Paracetamol 500mg  ",
					" 1 tableta ",
					" Cada 8 horas ",
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
			assertThat(saved.getInstrucciones()).isEqualTo("Con alimentos");
			assertThat(saved.getFechaInicio()).isEqualTo(LocalDate.of(2026, 10, 25));
			assertThat(saved.getFechaFin()).isEqualTo(LocalDate.of(2026, 11, 5));
			assertThat(saved.isActivo()).isTrue();

			assertThat(response.usuarioId()).isEqualTo(USUARIO_PROPIETARIO);
			assertThat(response.nombre()).isEqualTo("Paracetamol 500mg");
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

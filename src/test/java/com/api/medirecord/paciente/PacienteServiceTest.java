package com.api.medirecord.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.medirecord.paciente.dto.PacienteRequest;
import com.api.medirecord.paciente.dto.PacienteResponse;
import com.api.medirecord.paciente.exception.PacienteEmailDuplicadoException;
import com.api.medirecord.paciente.exception.PacienteNotFoundException;

/**
 * Test unitario del servicio: sin Spring, sin base de datos (el repositorio se
 * mockea). Es la red de seguridad mas rapida para las reglas de negocio.
 */
@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

	@Mock
	private PacienteRepository repository;

	@InjectMocks
	private PacienteService service;

	private static PacienteRequest request() {
		return new PacienteRequest(
				"  Ana  ",
				" Gomez ",
				" Lopez ",
				LocalDate.of(1990, 5, 14),
				"  Ana.Gomez@Correo.COM ",
				" +52 664 123 4567 ");
	}

	@Nested
	@DisplayName("crear")
	class Crear {

		@Test
		@DisplayName("normaliza correo y espacios, y guarda el paciente")
		void guardaPacienteNormalizado() {
			when(repository.existsByEmailIgnoreCase("ana.gomez@correo.com")).thenReturn(false);
			when(repository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

			PacienteResponse response = service.crear(request());

			ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
			verify(repository).save(captor.capture());

			Paciente saved = captor.getValue();
			assertThat(saved.getEmail()).isEqualTo("ana.gomez@correo.com");
			assertThat(saved.getNombre()).isEqualTo("Ana");
			assertThat(saved.getApellidoPaterno()).isEqualTo("Gomez");
			assertThat(saved.getApellidoMaterno()).isEqualTo("Lopez");
			assertThat(saved.getTelefono()).isEqualTo("+52 664 123 4567");
			assertThat(saved.isActivo()).isTrue();

			assertThat(response.nombreCompleto()).isEqualTo("Ana Gomez Lopez");
			assertThat(response.edad())
					.isEqualTo(Period.between(LocalDate.of(1990, 5, 14), LocalDate.now()).getYears());
		}

		@Test
		@DisplayName("rechaza correos ya registrados")
		void rechazaCorreoDuplicado() {
			when(repository.existsByEmailIgnoreCase("ana.gomez@correo.com")).thenReturn(true);

			assertThatThrownBy(() -> service.crear(request()))
					.isInstanceOf(PacienteEmailDuplicadoException.class)
					.hasMessageContaining("ana.gomez@correo.com");

			verify(repository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("obtener")
	class Obtener {

		@Test
		@DisplayName("lanza NOT_FOUND cuando el id no existe")
		void lanzaNotFound() {
			UUID id = UUID.randomUUID();
			when(repository.findById(id)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.obtener(id)).isInstanceOf(PacienteNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("desactivar")
	class Desactivar {

		@Test
		@DisplayName("hace baja logica sin borrar la fila")
		void desactivaPaciente() {
			UUID id = UUID.randomUUID();
			Paciente paciente = new Paciente("Ana", "Gomez", "Lopez",
					LocalDate.of(1990, 5, 14), "ana.gomez@correo.com", null);
			when(repository.findById(id)).thenReturn(Optional.of(paciente));
			when(repository.save(paciente)).thenReturn(paciente);

			service.desactivar(id);

			assertThat(paciente.isActivo()).isFalse();
			verify(repository, never()).deleteById(any(UUID.class));
		}
	}

}
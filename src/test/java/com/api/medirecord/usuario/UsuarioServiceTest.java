package com.api.medirecord.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.api.medirecord.usuario.dto.UsuarioRequest;
import com.api.medirecord.usuario.dto.UsuarioResponse;
import com.api.medirecord.usuario.exception.UsuarioEmailDuplicadoException;
import com.api.medirecord.usuario.exception.UsuarioNotFoundException;

/**
 * Test unitario del servicio de usuarios: sin Spring ni base de datos (el
 * repositorio se mockea).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository repository;

	@InjectMocks
	private UsuarioService service;

	private static UsuarioRequest request() {
		return new UsuarioRequest(
				"  Ana  ",
				" Gomez ",
				" Lopez ",
				"  Ana.Gomez@Correo.COM ",
				" +52 664 123 4567 ");
	}

	@Nested
	@DisplayName("crear")
	class Crear {

		@Test
		@DisplayName("normaliza datos y guarda el usuario activo")
		void guardaUsuarioNormalizado() {
			when(repository.existsByEmailIgnoreCase("ana.gomez@correo.com")).thenReturn(false);
			when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

			UsuarioResponse response = service.crear(request());

			ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
			verify(repository).save(captor.capture());

			Usuario saved = captor.getValue();
			assertThat(saved.getEmail()).isEqualTo("ana.gomez@correo.com");
			assertThat(saved.getNombre()).isEqualTo("Ana");
			assertThat(saved.getApellidoPaterno()).isEqualTo("Gomez");
			assertThat(saved.getApellidoMaterno()).isEqualTo("Lopez");
			assertThat(saved.getTelefono()).isEqualTo("+52 664 123 4567");
			assertThat(saved.isActivo()).isTrue();

			assertThat(response.nombreCompleto()).isEqualTo("Ana Gomez Lopez");
		}

		@Test
		@DisplayName("rechaza correos ya registrados")
		void rechazaCorreoDuplicado() {
			when(repository.existsByEmailIgnoreCase("ana.gomez@correo.com")).thenReturn(true);

			assertThatThrownBy(() -> service.crear(request()))
					.isInstanceOf(UsuarioEmailDuplicadoException.class)
					.hasMessageContaining("ana.gomez@correo.com");

			verify(repository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("actualizar")
	class Actualizar {

		@Test
		@DisplayName("actualiza los datos del usuario")
		void actualizaDatos() {
			UUID id = UUID.randomUUID();
			Usuario existente = new Usuario("Ana", "Gomez", "Lopez",
					"ana.gomez@correo.com", null);
			when(repository.findById(id)).thenReturn(Optional.of(existente));
			when(repository.existsByEmailIgnoreCaseAndIdNot("ana.gomez@correo.com", id)).thenReturn(false);
			when(repository.save(existente)).thenReturn(existente);

			UsuarioResponse response = service.actualizar(id, request());

			assertThat(response.email()).isEqualTo("ana.gomez@correo.com");
			assertThat(existente.getTelefono()).isEqualTo("+52 664 123 4567");
		}

		@Test
		@DisplayName("rechaza correo de otro usuario")
		void rechazaCorreoDeOtro() {
			UUID id = UUID.randomUUID();
			Usuario existente = new Usuario("Ana", "Gomez", "Lopez",
					"ana.gomez@correo.com", null);
			when(repository.findById(id)).thenReturn(Optional.of(existente));
			when(repository.existsByEmailIgnoreCaseAndIdNot("ana.gomez@correo.com", id)).thenReturn(true);

			assertThatThrownBy(() -> service.actualizar(id, request()))
					.isInstanceOf(UsuarioEmailDuplicadoException.class);
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

			assertThatThrownBy(() -> service.obtener(id)).isInstanceOf(UsuarioNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("desactivar")
	class Desactivar {

		@Test
		@DisplayName("hace baja logica sin borrar la fila")
		void desactivaUsuario() {
			UUID id = UUID.randomUUID();
			Usuario usuario = new Usuario("Ana", "Gomez", "Lopez",
					"ana.gomez@correo.com", null);
			when(repository.findById(id)).thenReturn(Optional.of(usuario));
			when(repository.save(usuario)).thenReturn(usuario);

			service.desactivar(id);

			assertThat(usuario.isActivo()).isFalse();
			verify(repository, never()).deleteById(any(UUID.class));
		}
	}

	@Nested
	@DisplayName("identidad externa")
	class IdentidadExterna {

		@Test
		@DisplayName("buscarPorKeycloakId devuelve la cuenta enlazada o vacio")
		void buscaPorIdentidad() {
			Usuario usuario = new Usuario("Ana", "Gomez", "Lopez", "ana.gomez@correo.com", null);
			usuario.vincularKeycloakId("sub-123");
			when(repository.findByKeycloakId("sub-123")).thenReturn(Optional.of(usuario));
			when(repository.findByKeycloakId("otro")).thenReturn(Optional.empty());

			assertThat(service.buscarPorKeycloakId("sub-123"))
					.hasValueSatisfying(response -> assertThat(response.keycloakId()).isEqualTo("sub-123"));
			assertThat(service.buscarPorKeycloakId("otro")).isEmpty();
		}

		@Test
		@DisplayName("vincularIdentidad guarda el sub del token en la cuenta local")
		void vinculaIdentidad() {
			UUID id = UUID.randomUUID();
			Usuario usuario = new Usuario("Ana", "Gomez", "Lopez", "ana.gomez@correo.com", null);
			when(repository.findById(id)).thenReturn(Optional.of(usuario));
			when(repository.save(usuario)).thenReturn(usuario);

			UsuarioResponse response = service.vincularIdentidad(id, "sub-123");

			assertThat(response.keycloakId()).isEqualTo("sub-123");
			assertThat(usuario.getKeycloakId()).isEqualTo("sub-123");
		}
	}

}

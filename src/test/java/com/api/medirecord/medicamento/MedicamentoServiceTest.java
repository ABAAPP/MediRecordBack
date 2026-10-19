package com.api.medirecord.medicamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.exception.CuentaNoVinculadaException;
import com.api.medirecord.medicamento.exception.FechasTratamientoInvalidasException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;
import com.api.medirecord.usuario.Usuario;
import com.api.medirecord.usuario.UsuarioRepository;

/**
 * Test unitario del servicio: sin Spring, sin base de datos (los repositorios se
 * mockean). La sesion autenticada se coloca en el SecurityContextHolder con el
 * mismo formato que produce el filtro JWT en runtime.
 */
@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

	private static final String KEYCLOAK_ID = "sub-ana";

	@Mock
	private MedicamentoRepository repository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@InjectMocks
	private MedicamentoService service;

	private Usuario ana;

	@BeforeEach
	void autenticarComoAna() {
		ana = new Usuario("Ana", "Gomez", "Lopez", "ana@correo.com", null);
		ana.vincularKeycloakId(KEYCLOAK_ID);

		Jwt jwt = new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(300),
				java.util.Map.of("alg", "none"), java.util.Map.of("sub", KEYCLOAK_ID));
		SecurityContextHolder.getContext()
				.setAuthentication(new JwtAuthenticationToken(jwt, List.of(), KEYCLOAK_ID));
	}

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	private static MedicamentoRequest request(LocalDate inicio, LocalDate fin) {
		return new MedicamentoRequest("  Paracetamol  ", " 500 mg ", null, inicio, fin);
	}

	@Nested
	@DisplayName("crear")
	class Crear {

		@Test
		@DisplayName("asocia el medicamento al usuario autenticado y normaliza el payload")
		void creaMedicamentoDelUsuarioActual() {
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));
			when(repository.save(any(Medicamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

			MedicamentoResponse response = service.crear(request(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)));

			ArgumentCaptor<Medicamento> captor = ArgumentCaptor.forClass(Medicamento.class);
			verify(repository).save(captor.capture());

			Medicamento saved = captor.getValue();
			assertThat(saved.getUsuario()).isSameAs(ana);
			assertThat(saved.getNombre()).isEqualTo("Paracetamol");
			assertThat(saved.getDosis()).isEqualTo("500 mg");
			assertThat(saved.getFrecuencia()).isNull();
			assertThat(saved.getFechaInicio()).isEqualTo(LocalDate.of(2026, 10, 1));
			assertThat(saved.getFechaFin()).isEqualTo(LocalDate.of(2026, 10, 15));
			assertThat(saved.isActivo()).isTrue();

			assertThat(response.enTratamiento()).isFalse();
		}

		@Test
		@DisplayName("un medicamento sin fecha de fin sigue en tratamiento")
		void sinFechaFinEstaEnTratamiento() {
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));
			when(repository.save(any(Medicamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

			MedicamentoResponse response = service.crear(request(LocalDate.of(2026, 10, 1), null));

			assertThat(response.fechaFin()).isNull();
			assertThat(response.enTratamiento()).isTrue();
		}

		@Test
		@DisplayName("rechaza una fecha de fin anterior a la de inicio")
		void rechazaFechasInconsistentes() {
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));

			assertThatThrownBy(() -> service.crear(request(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 1))))
					.isInstanceOf(FechasTratamientoInvalidasException.class);

			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("sin cuenta local vinculada responde FORBIDDEN")
		void cuentaNoVinculada() {
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.crear(request(LocalDate.of(2026, 10, 1), null)))
					.isInstanceOf(CuentaNoVinculadaException.class);

			verify(repository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("obtener")
	class Obtener {

		@Test
		@DisplayName("devuelve el medicamento cuando pertenece al usuario autenticado")
		void obtieneMedicamentoPropio() {
			UUID id = UUID.randomUUID();
			Medicamento medicamento = new Medicamento(ana, "Paracetamol", "500 mg", null,
					LocalDate.of(2026, 10, 1), null);
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));
			when(repository.findByIdAndUsuarioId(id, ana.getId())).thenReturn(Optional.of(medicamento));

			MedicamentoResponse response = service.obtener(id);

			assertThat(response.nombre()).isEqualTo("Paracetamol");
			assertThat(response.fechaInicio()).isEqualTo(LocalDate.of(2026, 10, 1));
			assertThat(response.enTratamiento()).isTrue();
		}

		@Test
		@DisplayName("un id ajeno o inexistente responde 404 sin filtrar")
		void idAjenoRespondeNotFound() {
			UUID id = UUID.randomUUID();
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));
			when(repository.findByIdAndUsuarioId(id, ana.getId())).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.obtener(id)).isInstanceOf(MedicamentoNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("listar")
	class Listar {

		@Test
		@DisplayName("el listado solo incluye medicamentos del usuario autenticado")
		void listaSoloLosPropios() {
			Medicamento propio = new Medicamento(ana, "Paracetamol", "500 mg", null,
					LocalDate.of(2026, 10, 1), null);
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));
			when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
					any(org.springframework.data.domain.Pageable.class)))
					.thenReturn(new PageImpl<>(List.of(propio), PageRequest.of(0, 20), 1));

			PageResponse<MedicamentoResponse> page = service.listar(null, false, PageRequest.of(0, 20));

			assertThat(page.content()).hasSize(1);
			assertThat(page.content().getFirst().nombre()).isEqualTo("Paracetamol");
		}
	}

	@Nested
	@DisplayName("actualizar")
	class Actualizar {

		@Test
		@DisplayName("actualiza solo si el medicamento pertenece al usuario autenticado")
		void actualizaMedicamentoPropio() {
			UUID id = UUID.randomUUID();
			Medicamento medicamento = new Medicamento(ana, "Ibuprofeno", "400 mg", null,
					LocalDate.of(2026, 10, 1), null);
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));
			when(repository.findByIdAndUsuarioId(id, ana.getId())).thenReturn(Optional.of(medicamento));
			when(repository.save(any(Medicamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

			MedicamentoResponse response = service.actualizar(id,
					request(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 20)));

			assertThat(response.nombre()).isEqualTo("Paracetamol");
			assertThat(response.fechaInicio()).isEqualTo(LocalDate.of(2026, 10, 2));
			assertThat(response.fechaFin()).isEqualTo(LocalDate.of(2026, 10, 20));
		}
	}

	@Nested
	@DisplayName("desactivar")
	class Desactivar {

		@Test
		@DisplayName("hace baja logica sin borrar la fila")
		void desactivaMedicamento() {
			UUID id = UUID.randomUUID();
			Medicamento medicamento = new Medicamento(ana, "Paracetamol", "500 mg", null,
					LocalDate.of(2026, 10, 1), null);
			when(usuarioRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(ana));
			when(repository.findByIdAndUsuarioId(id, ana.getId())).thenReturn(Optional.of(medicamento));
			when(repository.save(medicamento)).thenReturn(medicamento);

			service.desactivar(id);

			assertThat(medicamento.isActivo()).isFalse();
			verify(repository, never()).deleteById(any(UUID.class));
		}
	}

}

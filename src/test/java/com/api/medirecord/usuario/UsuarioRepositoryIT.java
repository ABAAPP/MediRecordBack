package com.api.medirecord.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.support.PostgresRepositoryTest;

/**
 * Test de integracion del repositorio de usuarios contra PostgreSQL real: valida
 * el mapeo JPA, la auditoria, las restricciones unicas y que las migraciones de
 * Flyway se aplicaron.
 */
@Transactional
class UsuarioRepositoryIT extends PostgresRepositoryTest {

	@Autowired
	private UsuarioRepository repository;

	private Usuario nuevoUsuario(String email) {
		return repository.save(new Usuario("Ana", "Gomez", "Lopez", email, "+52 664 123 4567"));
	}

	@Test
	@DisplayName("Persiste y recupera un usuario con auditoria")
	void persisteYRecupera() {
		Usuario saved = nuevoUsuario("ana.gomez@correo.com");

		Optional<Usuario> found = repository.findById(saved.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("ana.gomez@correo.com");
		assertThat(found.get().isActivo()).isTrue();
		assertThat(found.get().getCreatedAt()).isNotNull();
		assertThat(found.get().getCreatedBy()).isNotBlank();
	}

	@Test
	@DisplayName("El correo es unico (restriccion uk_usuario_email)")
	void correoUnico() {
		nuevoUsuario("duplicado@correo.com");

		assertThat(repository.existsByEmailIgnoreCase("DUPLICADO@correo.com")).isTrue();

		assertThatThrownBy(() -> nuevoUsuario("duplicado@correo.com"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Resuelve un usuario por su identidad de Keycloak")
	void buscaPorKeycloakId() {
		Usuario usuario = nuevoUsuario("keycloak@correo.com");
		usuario.vincularKeycloakId("a1b2c3d4-0000-0000-0000-000000000001");
		repository.saveAndFlush(usuario);

		Optional<Usuario> found = repository.findByKeycloakId("a1b2c3d4-0000-0000-0000-000000000001");

		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("keycloak@correo.com");
		assertThat(repository.existsByKeycloakId("a1b2c3d4-0000-0000-0000-000000000001")).isTrue();
	}

	@Test
	@DisplayName("La identidad de Keycloak es unica (restriccion uk_usuario_keycloak_id)")
	void keycloakIdUnico() {
		Usuario primero = nuevoUsuario("primero@correo.com");
		primero.vincularKeycloakId("identidad-compartida");
		repository.saveAndFlush(primero);

		Usuario segundo = new Usuario("Luis", "Perez", null, "segundo@correo.com", null);
		segundo.vincularKeycloakId("identidad-compartida");

		assertThatThrownBy(() -> repository.saveAndFlush(segundo))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("existsByEmailIgnoreCaseAndIdNot ignora el propio registro")
	void existeEmailDeOtro() {
		Usuario usuario = nuevoUsuario("update@correo.com");

		assertThat(repository.existsByEmailIgnoreCaseAndIdNot("update@correo.com", usuario.getId())).isFalse();
		assertThat(repository.existsByEmailIgnoreCaseAndIdNot("update@correo.com", UUID.randomUUID())).isTrue();
	}

}

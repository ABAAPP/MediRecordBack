package com.api.medirecord.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de usuarios. Los filtros dinamicos se construyen con
 * {@code Specification} en la capa de servicio.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

	Optional<Usuario> findByEmailIgnoreCase(String email);

	/** Resuelve al usuario a partir del claim {@code sub} del JWT. */
	Optional<Usuario> findByKeycloakId(String keycloakId);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

	boolean existsByKeycloakId(String keycloakId);

}

package com.api.medirecord.paciente;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de pacientes. Los filtros dinamicos se construyen con
 * {@code Specification} en la capa de servicio (evita metodos
 * {@code findBy...And...} inmanejables).
 */
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, UUID>, JpaSpecificationExecutor<Paciente> {

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

}
package com.api.medirecord.medicamento;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de medicamentos.
 *
 * <p>Expone las consultas que hacen cumplir la relacion usuario -&gt; medicamento:
 * un usuario solo debe poder leer y administrar los medicamentos cuyo
 * {@code usuario_id} le corresponde.</p>
 */
@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, UUID> {

	List<Medicamento> findByUsuarioId(UUID usuarioId);

	long countByUsuarioId(UUID usuarioId);

	List<Medicamento> findByUsuarioIdAndActivoTrue(UUID usuarioId);

}

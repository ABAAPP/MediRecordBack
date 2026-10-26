package com.api.medirecord.medicamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de medicamentos. Permite filtrar por propietario y criterios
 * dinamicos mediante {@code Specification}.
 */
@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, UUID>, JpaSpecificationExecutor<Medicamento> {

	List<Medicamento> findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(String usuarioId);

	Optional<Medicamento> findByIdAndUsuarioId(UUID id, String usuarioId);

}

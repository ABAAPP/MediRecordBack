package com.api.medirecord.horario;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de horarios de administracion de medicamentos (B-07).
 *
 * <p>Expone consultas optimizadas con {@code JOIN FETCH} para que el servicio
 * de recordatorios consulte horarios activos junto con la informacion de su
 * medicamento asociado.</p>
 */
@Repository
public interface HorarioRepository extends JpaRepository<Horario, UUID> {

	@Query("""
			SELECT h FROM Horario h
			JOIN FETCH h.medicamento m
			WHERE m.id = :medicamentoId
			  AND h.activo = true
			ORDER BY h.horaAdministracion ASC
			""")
	List<Horario> findActivosByMedicamentoId(@Param("medicamentoId") UUID medicamentoId);

	@Query("""
			SELECT h FROM Horario h
			JOIN FETCH h.medicamento m
			WHERE m.usuarioId = :usuarioId
			  AND m.activo = true
			  AND h.activo = true
			  AND h.recordatorioActivo = true
			ORDER BY h.horaAdministracion ASC, m.nombre ASC
			""")
	List<Horario> findHorariosParaRecordatoriosPorUsuario(@Param("usuarioId") String usuarioId);

}

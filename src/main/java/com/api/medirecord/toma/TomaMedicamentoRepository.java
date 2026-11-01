package com.api.medirecord.toma;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de persistencia de tomas de medicamentos (B-09).
 */
@Repository
public interface TomaMedicamentoRepository extends JpaRepository<TomaMedicamento, UUID> {

	List<TomaMedicamento> findByUsuarioIdAndFechaToma(String usuarioId, LocalDate fechaToma);

	@Query("""
			SELECT t FROM TomaMedicamento t
			WHERE t.usuarioId = :usuarioId
			  AND t.medicamento.id = :medicamentoId
			  AND t.fechaToma = :fechaToma
			""")
	List<TomaMedicamento> findByUsuarioAndMedicamentoAndFecha(
			@Param("usuarioId") String usuarioId,
			@Param("medicamentoId") UUID medicamentoId,
			@Param("fechaToma") LocalDate fechaToma);

	@Query("""
			SELECT t FROM TomaMedicamento t
			WHERE t.usuarioId = :usuarioId
			  AND t.horario.id = :horarioId
			  AND t.fechaToma = :fechaToma
			""")
	Optional<TomaMedicamento> findByUsuarioAndHorarioAndFecha(
			@Param("usuarioId") String usuarioId,
			@Param("horarioId") UUID horarioId,
			@Param("fechaToma") LocalDate fechaToma);

	@Query("""
			SELECT t FROM TomaMedicamento t
			WHERE t.usuarioId = :usuarioId
			  AND (t.horario.id = :horarioId OR (t.horario IS NULL AND t.medicamento.id = :medicamentoId))
			  AND t.fechaToma = :fechaToma
			""")
	Optional<TomaMedicamento> findTomaExistente(
			@Param("usuarioId") String usuarioId,
			@Param("medicamentoId") UUID medicamentoId,
			@Param("horarioId") UUID horarioId,
			@Param("fechaToma") LocalDate fechaToma);

}

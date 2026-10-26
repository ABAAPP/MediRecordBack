package com.api.medirecord.medicamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.support.PostgresRepositoryTest;

/**
 * Test de integracion de MedicamentoRepository contra PostgreSQL real:
 * valida migracion V2, persistencia de fechas, restriccion ck_medicamento_fechas
 * y aislamiento de consultas por usuario.
 */
@Transactional
class MedicamentoRepositoryIT extends PostgresRepositoryTest {

	@Autowired
	private MedicamentoRepository repository;

	@Test
	@DisplayName("Persiste y recupera un medicamento con fechas de inicio, fin y auditoria en PostgreSQL")
	void persisteYRecuperaConFechas() {
		Medicamento saved = repository.saveAndFlush(new Medicamento(
				"usuario-a",
				"Amoxicilina",
				"500 mg",
				"Cada 8 horas",
				"Tomar con agua",
				LocalDate.of(2026, 10, 20),
				LocalDate.of(2026, 10, 27)));

		Optional<Medicamento> found = repository.findById(saved.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getUsuarioId()).isEqualTo("usuario-a");
		assertThat(found.get().getNombre()).isEqualTo("Amoxicilina");
		assertThat(found.get().getFechaInicio()).isEqualTo(LocalDate.of(2026, 10, 20));
		assertThat(found.get().getFechaFin()).isEqualTo(LocalDate.of(2026, 10, 27));
		assertThat(found.get().getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("La restriccion ck_medicamento_fechas en PostgreSQL impide fecha_fin < fecha_inicio")
	void restriccionFechasEnPostgres() {
		Medicamento invalido = new Medicamento(
				"usuario-a",
				"Ibuprofeno",
				"400 mg",
				"Cada 8 horas",
				null,
				LocalDate.of(2026, 10, 25),
				LocalDate.of(2026, 10, 10));

		assertThatThrownBy(() -> repository.saveAndFlush(invalido))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Aisla correctamente los medicamentos por usuario en las consultas")
	void aislaMedicamentosPorUsuario() {
		Medicamento medUsuarioA = repository.saveAndFlush(new Medicamento(
				"usuario-a",
				"Paracetamol",
				"500 mg",
				"Cada 8 horas",
				null,
				LocalDate.of(2026, 10, 25),
				null));

		repository.saveAndFlush(new Medicamento(
				"usuario-b",
				"Metformina",
				"850 mg",
				"Cada 12 horas",
				null,
				LocalDate.of(2026, 10, 25),
				null));

		List<Medicamento> deUsuarioA = repository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc("usuario-a");
		assertThat(deUsuarioA)
				.hasSize(1)
				.allSatisfy(m -> assertThat(m.getNombre()).isEqualTo("Paracetamol"));

		assertThat(repository.findByIdAndUsuarioId(medUsuarioA.getId(), "usuario-a")).isPresent();
		assertThat(repository.findByIdAndUsuarioId(medUsuarioA.getId(), "usuario-b")).isEmpty();
	}

}

package com.api.medirecord.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.support.PostgresRepositoryTest;

/**
 * Test de integracion del repositorio contra PostgreSQL real: valida el mapeo JPA,
 * las restricciones unicas y que las migraciones de Flyway se aplicaron.
 */
@Transactional
class PacienteRepositoryIT extends PostgresRepositoryTest {

	@Autowired
	private PacienteRepository repository;

	private Paciente nuevoPaciente(String email) {
		return repository.save(new Paciente("Ana", "Gomez", "Lopez",
				LocalDate.of(1990, 5, 14), email, "+52 664 123 4567"));
	}

	@Test
	@DisplayName("Persiste y recupera un paciente con auditoria")
	void persisteYRecupera() {
		Paciente saved = nuevoPaciente("ana.gomez@correo.com");

		Optional<Paciente> found = repository.findById(saved.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("ana.gomez@correo.com");
		assertThat(found.get().getCreatedAt()).isNotNull();
		assertThat(found.get().getCreatedBy()).isNotBlank();
	}

	@Test
	@DisplayName("El correo es unico (restriccion uk_paciente_email)")
	void correoUnico() {
		nuevoPaciente("duplicado@correo.com");

		assertThat(repository.existsByEmailIgnoreCase("DUPLICADO@correo.com")).isTrue();

		assertThatThrownBy(() -> nuevoPaciente("duplicado@correo.com"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("El filtro por specification funciona sobre datos reales")
	void filtraPorSpecification() {
		nuevoPaciente("filtro@correo.com");

		Specification<Paciente> spec = (root, query, builder) -> {
			var predicate = builder.like(builder.lower(root.get("apellidoPaterno")), "%gomez%");
			query.where(predicate);
			return predicate;
		};

		assertThat(repository.findAll(spec, PageRequest.of(0, 10, Sort.by("apellidoPaterno"))))
				.hasSize(1)
				.allSatisfy(paciente -> assertThat(paciente.getEmail()).isEqualTo("filtro@correo.com"));
	}

	@Test
	@DisplayName("existsByEmailIgnoreCaseAndIdNot ignora el propio registro")
	void existeEmailDeOtro() {
		Paciente paciente = nuevoPaciente("update@correo.com");

		assertThat(repository.existsByEmailIgnoreCaseAndIdNot("update@correo.com", paciente.getId())).isFalse();
		assertThat(repository.existsByEmailIgnoreCaseAndIdNot("update@correo.com", UUID.randomUUID())).isTrue();
	}

}
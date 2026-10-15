package com.api.medirecord.medicamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.support.PostgresRepositoryTest;
import com.api.medirecord.usuario.Usuario;
import com.api.medirecord.usuario.UsuarioRepository;

/**
 * Test de integracion que comprueba la relacion 1:N usuario -&gt; medicamento
 * contra PostgreSQL real: cada medicamento pertenece a un unico usuario y las
 * consultas por usuario solo devuelven sus propios medicamentos.
 */
@Transactional
class MedicamentoRepositoryIT extends PostgresRepositoryTest {

	@Autowired
	private MedicamentoRepository medicamentoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Usuario crearUsuario(String email) {
		return usuarioRepository.save(new Usuario("Ana", "Gomez", "Lopez", email, null));
	}

	@Test
	@DisplayName("Relaciona cada medicamento con su usuario y filtra por usuario")
	void relacionaYFiltraPorUsuario() {
		Usuario ana = crearUsuario("ana@correo.com");
		Usuario luis = crearUsuario("luis@correo.com");

		medicamentoRepository.save(new Medicamento(ana, "Paracetamol", "500 mg", "Cada 8 horas"));
		medicamentoRepository.save(new Medicamento(ana, "Ibuprofeno", "400 mg", "Cada 12 horas"));
		medicamentoRepository.save(new Medicamento(luis, "Amoxicilina", "250 mg", "Cada 6 horas"));
		medicamentoRepository.flush();

		List<Medicamento> deAna = medicamentoRepository.findByUsuarioId(ana.getId());

		assertThat(deAna).hasSize(2)
				.extracting(Medicamento::getNombre)
				.containsExactlyInAnyOrder("Paracetamol", "Ibuprofeno");
		assertThat(deAna).allSatisfy(medicamento ->
				assertThat(medicamento.getUsuario().getId()).isEqualTo(ana.getId()));
		assertThat(medicamentoRepository.countByUsuarioId(luis.getId())).isEqualTo(1);
	}

	@Test
	@DisplayName("findByUsuarioIdAndActivoTrue excluye los medicamentos dados de baja")
	void filtraSoloActivos() {
		Usuario ana = crearUsuario("activos@correo.com");
		Medicamento activo = medicamentoRepository.save(
				new Medicamento(ana, "Paracetamol", "500 mg", "Cada 8 horas"));
		Medicamento inactivo = new Medicamento(ana, "Ibuprofeno", "400 mg", "Cada 12 horas");
		inactivo.desactivar();
		medicamentoRepository.saveAndFlush(inactivo);

		List<Medicamento> activos = medicamentoRepository.findByUsuarioIdAndActivoTrue(ana.getId());

		assertThat(activos).extracting(Medicamento::getId).containsExactly(activo.getId());
	}

	@Test
	@DisplayName("Un medicamento no puede existir sin usuario (fk_medicamento_usuario)")
	void medicamentoSinUsuarioEsRechazado() {
		Medicamento huerfano = new Medicamento(null, "Paracetamol", "500 mg", "Cada 8 horas");

		assertThatThrownBy(() -> medicamentoRepository.saveAndFlush(huerfano))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}

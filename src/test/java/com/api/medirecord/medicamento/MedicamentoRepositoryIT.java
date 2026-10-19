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

import jakarta.persistence.EntityManager;

import com.api.medirecord.support.PostgresRepositoryTest;
import com.api.medirecord.usuario.Usuario;
import com.api.medirecord.usuario.UsuarioRepository;

/**
 * Test de integracion que comprueba la relacion 1:N usuario -&gt; medicamento
 * contra PostgreSQL real: cada medicamento pertenece a un unico usuario, las
 * consultas por usuario solo devuelven sus propios medicamentos y las fechas
 * del tratamiento se persisten con sus restricciones.
 */
@Transactional
class MedicamentoRepositoryIT extends PostgresRepositoryTest {

	@Autowired
	private MedicamentoRepository medicamentoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EntityManager entityManager;

	private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);
	private static final LocalDate FIN = LocalDate.of(2026, 10, 15);

	private Usuario crearUsuario(String email) {
		return usuarioRepository.save(new Usuario("Ana", "Gomez", "Lopez", email, null));
	}

	private static Medicamento medicamento(Usuario usuario, String nombre, LocalDate inicio, LocalDate fin) {
		return new Medicamento(usuario, nombre, "500 mg", "Cada 8 horas", inicio, fin);
	}

	@Test
	@DisplayName("Relaciona cada medicamento con su usuario y filtra por usuario")
	void relacionaYFiltraPorUsuario() {
		Usuario ana = crearUsuario("ana@correo.com");
		Usuario luis = crearUsuario("luis@correo.com");

		medicamentoRepository.save(medicamento(ana, "Paracetamol", INICIO, FIN));
		medicamentoRepository.save(medicamento(ana, "Ibuprofeno", INICIO, null));
		medicamentoRepository.save(medicamento(luis, "Amoxicilina", INICIO, null));
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
		Medicamento activo = medicamentoRepository.save(medicamento(ana, "Paracetamol", INICIO, FIN));
		Medicamento inactivo = medicamento(ana, "Ibuprofeno", INICIO, null);
		inactivo.desactivar();
		medicamentoRepository.saveAndFlush(inactivo);

		List<Medicamento> activos = medicamentoRepository.findByUsuarioIdAndActivoTrue(ana.getId());

		assertThat(activos).extracting(Medicamento::getId).containsExactly(activo.getId());
	}

	@Test
	@DisplayName("findByIdAndUsuarioId solo devuelve el medicamento si pertenece al usuario")
	void controlaPropiedad() {
		Usuario ana = crearUsuario("propiedad@correo.com");
		Usuario luis = crearUsuario("propiedad-luis@correo.com");
		Medicamento deAna = medicamentoRepository.saveAndFlush(medicamento(ana, "Paracetamol", INICIO, FIN));

		Optional<Medicamento> propio = medicamentoRepository.findByIdAndUsuarioId(deAna.getId(), ana.getId());
		Optional<Medicamento> ajeno = medicamentoRepository.findByIdAndUsuarioId(deAna.getId(), luis.getId());

		assertThat(propio).contains(deAna);
		assertThat(ajeno).isEmpty();
	}

	@Test
	@DisplayName("Persiste las fechas de inicio y fin del tratamiento")
	void persisteFechas() {
		Usuario ana = crearUsuario("fechas@correo.com");
		Medicamento guardado = medicamentoRepository.saveAndFlush(medicamento(ana, "Paracetamol", INICIO, FIN));
		entityManager.clear();

		Medicamento recargado = medicamentoRepository.findById(guardado.getId()).orElseThrow();

		assertThat(recargado.getFechaInicio()).isEqualTo(INICIO);
		assertThat(recargado.getFechaFin()).isEqualTo(FIN);
	}

	@Test
	@DisplayName("Permite medicamento vigente sin fecha de fin")
	void permiteFechaFinNula() {
		Usuario ana = crearUsuario("vigente@correo.com");
		Medicamento guardado = medicamentoRepository.saveAndFlush(medicamento(ana, "Ibuprofeno", INICIO, null));

		assertThat(guardado.getFechaFin()).isNull();
	}

	@Test
	@DisplayName("Rechaza una fecha de fin anterior a la de inicio (ck_medicamento_fechas)")
	void rechazaFechaFinAnteriorAInicio() {
		Usuario ana = crearUsuario("fechas-invalidas@correo.com");
		Medicamento invalido = medicamento(ana, "Paracetamol", FIN, INICIO);

		assertThatThrownBy(() -> medicamentoRepository.saveAndFlush(invalido))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Un medicamento no puede existir sin usuario (fk_medicamento_usuario)")
	void medicamentoSinUsuarioEsRechazado() {
		Medicamento huerfano = new Medicamento(null, "Paracetamol", "500 mg", "Cada 8 horas", INICIO, null);

		assertThatThrownBy(() -> medicamentoRepository.saveAndFlush(huerfano))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Un medicamento sin fecha de inicio es rechazado (fecha_inicio NOT NULL)")
	void medicamentoSinFechaInicioEsRechazado() {
		Usuario ana = crearUsuario("sin-fecha@correo.com");
		Medicamento sinInicio = new Medicamento(ana, "Paracetamol", "500 mg", "Cada 8 horas", null, null);

		assertThatThrownBy(() -> medicamentoRepository.saveAndFlush(sinInicio))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}

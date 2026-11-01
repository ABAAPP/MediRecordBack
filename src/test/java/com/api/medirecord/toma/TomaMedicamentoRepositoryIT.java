package com.api.medirecord.toma;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.horario.Horario;
import com.api.medirecord.horario.HorarioRepository;
import com.api.medirecord.medicamento.Medicamento;
import com.api.medirecord.medicamento.MedicamentoRepository;
import com.api.medirecord.support.PostgresRepositoryTest;

/**
 * Test de integracion de TomaMedicamentoRepository contra PostgreSQL real (B-09).
 */
@Transactional
class TomaMedicamentoRepositoryIT extends PostgresRepositoryTest {

	@Autowired
	private TomaMedicamentoRepository tomaRepository;

	@Autowired
	private MedicamentoRepository medicamentoRepository;

	@Autowired
	private HorarioRepository horarioRepository;

	@Test
	@DisplayName("Persiste una toma realizada en PostgreSQL y la consulta por fecha y usuario")
	void persisteYConsultaToma() {
		Medicamento med = medicamentoRepository.saveAndFlush(new Medicamento(
				"usuario-tomas",
				"Losartan",
				"50 mg",
				"Cada 24 horas",
				"08:00 AM",
				"Tomar con agua",
				LocalDate.of(2026, 10, 1),
				null));

		Horario horario = horarioRepository.saveAndFlush(new Horario(
				med,
				"08:00 AM",
				"Diario",
				Horario.TODOS_LOS_DIAS,
				true,
				null));

		TomaMedicamento toma = tomaRepository.saveAndFlush(new TomaMedicamento(
				med,
				horario,
				"usuario-tomas",
				LocalDate.of(2026, 10, 31),
				"08:05 AM",
				true,
				"Toma a tiempo"));

		List<TomaMedicamento> tomas = tomaRepository.findByUsuarioIdAndFechaToma(
				"usuario-tomas",
				LocalDate.of(2026, 10, 31));

		assertThat(tomas).hasSize(1);
		TomaMedicamento recuperada = tomas.getFirst();
		assertThat(recuperada.getId()).isEqualTo(toma.getId());
		assertThat(recuperada.isRealizada()).isTrue();
		assertThat(recuperada.getHoraToma()).isEqualTo("08:05 AM");
		assertThat(recuperada.getMedicamento().getNombre()).isEqualTo("Losartan");

		Optional<TomaMedicamento> optExistente = tomaRepository.findTomaExistente(
				"usuario-tomas",
				med.getId(),
				horario.getId(),
				LocalDate.of(2026, 10, 31));

		assertThat(optExistente).isPresent();
		assertThat(optExistente.get().isRealizada()).isTrue();
	}

}

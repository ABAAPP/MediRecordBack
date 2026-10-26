package com.api.medirecord.horario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.medicamento.Medicamento;
import com.api.medirecord.medicamento.MedicamentoRepository;
import com.api.medirecord.support.PostgresRepositoryTest;

/**
 * Test de integracion de HorarioRepository contra PostgreSQL real (B-07).
 */
@Transactional
class HorarioRepositoryIT extends PostgresRepositoryTest {

	@Autowired
	private HorarioRepository horarioRepository;

	@Autowired
	private MedicamentoRepository medicamentoRepository;

	@Test
	@DisplayName("Persiste un horario relacionado con un medicamento y lo recupera para el servicio de recordatorios")
	void persisteYRecuperaParaRecordatorios() {
		Medicamento med = medicamentoRepository.saveAndFlush(new Medicamento(
				"usuario-recordatorios",
				"Omeprazol",
				"20 mg",
				"Diario",
				"07:30 AM",
				"En ayunas",
				LocalDate.of(2026, 10, 25),
				null));

		Horario horario = horarioRepository.saveAndFlush(new Horario(
				med,
				"07:30 AM",
				"Todos los días (Diario)",
				List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes"),
				true,
				"En ayunas"));

		List<Horario> paraRecordatorios = horarioRepository
				.findHorariosParaRecordatoriosPorUsuario("usuario-recordatorios");

		assertThat(paraRecordatorios).hasSize(1);
		Horario recuperado = paraRecordatorios.getFirst();
		assertThat(recuperado.getId()).isEqualTo(horario.getId());
		assertThat(recuperado.getMedicamento().getNombre()).isEqualTo("Omeprazol");
		assertThat(recuperado.getHoraAdministracion()).isEqualTo("07:30 AM");
		assertThat(recuperado.getFrecuencia()).isEqualTo("Todos los días (Diario)");
		assertThat(recuperado.getDiasSeleccionados())
				.containsExactly("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");
	}

}

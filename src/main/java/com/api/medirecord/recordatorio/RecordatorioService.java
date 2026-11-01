package com.api.medirecord.recordatorio;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.common.security.SecurityUtils;
import com.api.medirecord.horario.Horario;
import com.api.medirecord.horario.HorarioRepository;
import com.api.medirecord.medicamento.Medicamento;
import com.api.medirecord.medicamento.MedicamentoRepository;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;
import com.api.medirecord.recordatorio.dto.RecordatorioResponse;

/**
 * Servicio de logica de negocio para la consulta y generacion de recordatorios (B-08).
 *
 * <p>Identifica los recordatorios aplicables a la fecha y usuario consultados,
 * relacionando los medicamentos y horarios correspondientes, considerando la
 * vigencia del tratamiento y los dias de la semana seleccionados.</p>
 */
@Service
@Transactional(readOnly = true)
public class RecordatorioService {

	private static final Pattern PATTERN_HORA = Pattern.compile("^(\\d{1,2}):(\\d{2})(?:\\s*(?i)(AM|PM))?$");

	private final HorarioRepository horarioRepository;
	private final MedicamentoRepository medicamentoRepository;

	public RecordatorioService(HorarioRepository horarioRepository, MedicamentoRepository medicamentoRepository) {
		this.horarioRepository = horarioRepository;
		this.medicamentoRepository = medicamentoRepository;
	}

	/**
	 * Consulta los recordatorios correspondientes a una fecha determinada para el
	 * usuario autenticado.
	 */
	public List<RecordatorioResponse> consultarRecordatorios(LocalDate fecha) {
		String usuarioId = obtenerUsuarioAutenticado();
		LocalDate fechaObjetivo = fecha != null ? fecha : LocalDate.now();
		List<Horario> horariosAplicables = obtenerHorariosFiltrados(usuarioId, fechaObjetivo);

		return horariosAplicables.stream()
				.map(horario -> RecordatorioResponse.from(horario, fechaObjetivo))
				.toList();
	}

	/**
	 * Consulta los recordatorios activos del usuario para la aplicacion movil
	 * ({@code GET /api/reminders}).
	 */
	public List<MobileMedicationReminderDto> consultarRecordatoriosMovil(String userEmail) {
		String usuarioId = resolverUsuario(userEmail);
		LocalDate hoy = LocalDate.now();
		List<Horario> horarios = obtenerHorariosFiltrados(usuarioId, hoy);

		// Si no hay horarios especificos para hoy, devolver los horarios activos del tratamiento
		if (horarios.isEmpty()) {
			horarios = horarioRepository.findHorariosParaRecordatoriosPorUsuario(usuarioId);
		}

		return horarios.stream()
				.sorted(comparadorCronologico())
				.map(MobileMedicationReminderDto::from)
				.toList();
	}

	/**
	 * Consulta las dosis o tomas de medicamentos de una fecha dada para la
	 * aplicacion movil ({@code GET /api/medications/today}).
	 */
	public List<MobileMedicationDoseDto> consultarDosisFechaMovil(LocalDate fecha, String userEmail) {
		String usuarioId = resolverUsuario(userEmail);
		LocalDate fechaObjetivo = fecha != null ? fecha : LocalDate.now();
		List<Horario> horarios = obtenerHorariosFiltrados(usuarioId, fechaObjetivo);

		return horarios.stream()
				.map(horario -> {
					Medicamento med = horario.getMedicamento();
					String id = horario.getId() != null ? horario.getId().toString()
							: (med != null && med.getId() != null ? med.getId().toString() : UUID.randomUUID().toString());
					String instrucciones = (horario.getNotas() != null && !horario.getNotas().isBlank())
							? horario.getNotas()
							: (med != null ? med.getInstrucciones() : null);

					return new MobileMedicationDoseDto(
							id,
							med != null ? med.getNombre() : "",
							med != null && med.getDosis() != null ? med.getDosis() : "",
							horario.getHoraAdministracion(),
							false,
							instrucciones,
							horario.getFrecuencia(),
							horario.getDiasSeleccionados(),
							usuarioId,
							null,
							fechaObjetivo.toString());
				})
				.toList();
	}

	private List<Horario> obtenerHorariosFiltrados(String usuarioId, LocalDate fecha) {
		List<Horario> todos = new ArrayList<>(horarioRepository.findHorariosParaRecordatoriosPorUsuario(usuarioId));

		// Identificar medicamentos activos que no tengan horario registrado pero si horaProgramada
		Set<UUID> idsConHorario = todos.stream()
				.map(Horario::getMedicamento)
				.filter(Objects::nonNull)
				.map(Medicamento::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<Medicamento> medicamentos = medicamentoRepository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(usuarioId);
		for (Medicamento med : medicamentos) {
			if (!idsConHorario.contains(med.getId()) && med.getHoraProgramada() != null && !med.getHoraProgramada().isBlank()) {
				Horario horarioVirtual = new Horario(
						med,
						med.getHoraProgramada(),
						med.getFrecuencia() != null ? med.getFrecuencia() : "Todos los días (Diario)",
						Horario.TODOS_LOS_DIAS,
						true,
						med.getInstrucciones());
				todos.add(horarioVirtual);
			}
		}

		String diaSemana = obtenerDiaSemanaEspanol(fecha.getDayOfWeek());

		return todos.stream()
				.filter(h -> esTratamientoVigente(h.getMedicamento(), fecha))
				.filter(h -> correspondeADiaSemana(h, diaSemana))
				.sorted(comparadorCronologico())
				.toList();
	}

	private static boolean esTratamientoVigente(Medicamento medicamento, LocalDate fecha) {
		if (medicamento == null || !medicamento.isActivo() || medicamento.getFechaInicio() == null) {
			return false;
		}
		if (fecha.isBefore(medicamento.getFechaInicio())) {
			return false;
		}
		return medicamento.getFechaFin() == null || !fecha.isAfter(medicamento.getFechaFin());
	}

	private static boolean correspondeADiaSemana(Horario horario, String diaSemana) {
		String diaNormalizado = normalizarTexto(diaSemana);
		List<String> diasConfigurados = horario.getDiasSeleccionados();

		if (diasConfigurados == null || diasConfigurados.isEmpty()) {
			return true;
		}

		String frecuenciaNormalizada = normalizarTexto(horario.getFrecuencia());
		if (frecuenciaNormalizada.contains("diario") || frecuenciaNormalizada.contains("todos los dias")) {
			return true;
		}

		return diasConfigurados.stream()
				.map(RecordatorioService::normalizarTexto)
				.anyMatch(dia -> dia.equals(diaNormalizado));
	}

	private static Comparator<Horario> comparadorCronologico() {
		return Comparator.comparingInt((Horario h) -> parsearMinutos(h.getHoraAdministracion()))
				.thenComparing(h -> h.getMedicamento() != null ? h.getMedicamento().getNombre() : "");
	}

	public static int parsearMinutos(String horaStr) {
		if (horaStr == null || horaStr.isBlank()) {
			return Integer.MAX_VALUE;
		}
		Matcher matcher = PATTERN_HORA.matcher(horaStr.trim());
		if (!matcher.find()) {
			return Integer.MAX_VALUE;
		}
		try {
			int hora = Integer.parseInt(matcher.group(1));
			int minutos = Integer.parseInt(matcher.group(2));
			String amPm = matcher.group(3);

			if (amPm != null) {
				if (amPm.equalsIgnoreCase("PM") && hora < 12) {
					hora += 12;
				} else if (amPm.equalsIgnoreCase("AM") && hora == 12) {
					hora = 0;
				}
			}
			return hora * 60 + minutos;
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	private static String normalizarTexto(String texto) {
		if (texto == null) {
			return "";
		}
		return Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
	}

	private static String obtenerDiaSemanaEspanol(DayOfWeek dow) {
		return switch (dow) {
			case MONDAY -> "Lunes";
			case TUESDAY -> "Martes";
			case WEDNESDAY -> "Miércoles";
			case THURSDAY -> "Jueves";
			case FRIDAY -> "Viernes";
			case SATURDAY -> "Sábado";
			case SUNDAY -> "Domingo";
		};
	}

	private static String resolverUsuario(String userEmailParam) {
		String usuarioAutenticado = SecurityUtils.currentUsername()
				.filter(u -> !u.isBlank())
				.orElse(null);

		if (usuarioAutenticado != null) {
			return usuarioAutenticado;
		}

		if (userEmailParam != null && !userEmailParam.isBlank()) {
			return userEmailParam.trim();
		}

		throw new ApiException(ErrorCode.UNAUTHENTICATED);
	}

	private static String obtenerUsuarioAutenticado() {
		return SecurityUtils.currentUsername()
				.filter(username -> !username.isBlank())
				.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
	}

}

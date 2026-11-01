package com.api.medirecord.medicamento;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.common.security.SecurityUtils;
import com.api.medirecord.horario.Horario;
import com.api.medirecord.horario.HorarioRepository;
import com.api.medirecord.medicamento.dto.MedicamentoDelDiaResponse;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.medicamento.dto.MobileMedicationRequest;
import com.api.medirecord.medicamento.dto.MobileMedicationResponse;
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoFechasInvalidasException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;
import com.api.medirecord.toma.TomaMedicamento;
import com.api.medirecord.toma.TomaMedicamentoRepository;

import jakarta.persistence.criteria.Predicate;

/**
 * Reglas de negocio de medicamentos asociados a la cuenta del usuario (B-05, B-06 y B-10).
 *
 * <p>Garantiza el aislamiento por propietario: cada operacion obtiene el
 * usuario autenticado del contexto de seguridad y restringe el acceso a sus
 * propios medicamentos.</p>
 */
@Service
@Transactional(readOnly = true)
public class MedicamentoService {

	private static final DateTimeFormatter FORMATO_DIA_MES_ANIO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final Pattern PATTERN_HORA = Pattern.compile("^(\\d{1,2}):(\\d{2})(?:\\s*(?i)(AM|PM))?$");

	private final MedicamentoRepository repository;
	private final HorarioRepository horarioRepository;
	private final TomaMedicamentoRepository tomaRepository;

	public MedicamentoService(
			MedicamentoRepository repository,
			HorarioRepository horarioRepository,
			TomaMedicamentoRepository tomaRepository) {
		this.repository = repository;
		this.horarioRepository = horarioRepository;
		this.tomaRepository = tomaRepository;
	}

	@Transactional
	public MedicamentoResponse crear(MedicamentoRequest request) {
		String usuarioId = obtenerUsuarioAutenticado();
		validarRangoFechas(request.fechaInicio(), request.fechaFin());

		Medicamento medicamento = new Medicamento(
				usuarioId,
				request.nombre().trim(),
				aplicarTrimOpcional(request.dosis()),
				aplicarTrimOpcional(request.frecuencia()),
				aplicarTrimOpcional(request.horaProgramada()),
				aplicarTrimOpcional(request.instrucciones()),
				request.fechaInicio(),
				request.fechaFin());

		return MedicamentoResponse.from(repository.save(medicamento));
	}

	/**
	 * Registra un medicamento desde el contrato de la aplicacion movil
	 * ({@code POST /api/medications}), asociandolo al usuario autenticado.
	 */
	@Transactional
	public MobileMedicationResponse registrarDesdeApp(MobileMedicationRequest request) {
		String usuarioId = obtenerUsuarioAutenticado();
		LocalDate fechaInicio = parsearFecha(request.startDate(), "startDate");
		LocalDate fechaFin = (request.endDate() == null || request.endDate().isBlank())
				? null
				: parsearFecha(request.endDate(), "endDate");

		validarRangoFechas(fechaInicio, fechaFin);

		Medicamento medicamento = new Medicamento(
				usuarioId,
				request.name().trim(),
				request.dosage().trim(),
				request.frequency().trim(),
				request.scheduledTime().trim(),
				aplicarTrimOpcional(request.instructions()),
				fechaInicio,
				fechaFin);

		return MobileMedicationResponse.from(repository.save(medicamento));
	}

	@Transactional
	public MedicamentoResponse actualizar(UUID id, MedicamentoRequest request) {
		String usuarioId = obtenerUsuarioAutenticado();
		Medicamento medicamento = obtenerEntidadDeUsuario(id, usuarioId);
		validarRangoFechas(request.fechaInicio(), request.fechaFin());

		medicamento.setNombre(request.nombre().trim());
		medicamento.setDosis(aplicarTrimOpcional(request.dosis()));
		medicamento.setFrecuencia(aplicarTrimOpcional(request.frecuencia()));
		medicamento.setHoraProgramada(aplicarTrimOpcional(request.horaProgramada()));
		medicamento.setInstrucciones(aplicarTrimOpcional(request.instrucciones()));
		medicamento.setFechaInicio(request.fechaInicio());
		medicamento.setFechaFin(request.fechaFin());

		return MedicamentoResponse.from(repository.save(medicamento));
	}

	public MedicamentoResponse obtener(UUID id) {
		String usuarioId = obtenerUsuarioAutenticado();
		return MedicamentoResponse.from(obtenerEntidadDeUsuario(id, usuarioId));
	}

	public PageResponse<MedicamentoResponse> buscar(String termino, boolean incluirInactivos, Pageable pageable) {
		String usuarioId = obtenerUsuarioAutenticado();
		Page<Medicamento> resultados = repository.findAll(filtroPorUsuario(usuarioId, termino, incluirInactivos), pageable);
		return PageResponse.from(resultados, MedicamentoResponse::from);
	}

	/**
	 * Consulta los medicamentos del dia para el usuario autenticado (B-10).
	 *
	 * <p>Muestra los medicamentos asociados a sus horarios para la fecha indicada (por
	 * defecto hoy), reflejando si cada toma esta pendiente o realizada. Si no hay
	 * tomas programadas, devuelve una lista vacia valida.</p>
	 */
	public List<MedicamentoDelDiaResponse> consultarMedicamentosDelDia(LocalDate fecha) {
		String usuarioId = obtenerUsuarioAutenticado();
		LocalDate fechaObjetivo = fecha != null ? fecha : LocalDate.now();

		List<Horario> horariosAplicables = obtenerHorariosDelDia(usuarioId, fechaObjetivo);
		if (horariosAplicables.isEmpty()) {
			return List.of();
		}

		List<TomaMedicamento> tomasFecha = tomaRepository.findByUsuarioIdAndFechaToma(usuarioId, fechaObjetivo);

		return horariosAplicables.stream()
				.map(horario -> {
					Medicamento med = horario.getMedicamento();
					TomaMedicamento tomaMatch = buscarTomaCorrespondiente(tomasFecha, horario);
					boolean realizada = tomaMatch != null && tomaMatch.isRealizada();
					String horaRealizada = realizada ? tomaMatch.getHoraToma() : null;

					String instrucciones = (horario.getNotas() != null && !horario.getNotas().isBlank())
							? horario.getNotas()
							: (med != null ? med.getInstrucciones() : null);

					return MedicamentoDelDiaResponse.de(
							med != null ? med.getId() : null,
							med != null ? med.getNombre() : "",
							med != null ? med.getDosis() : "",
							horario.getId(),
							horario.getHoraAdministracion(),
							horario.getFrecuencia(),
							horario.getDiasSeleccionados(),
							instrucciones,
							fechaObjetivo,
							realizada,
							horaRealizada,
							usuarioId);
				})
				.toList();
	}

	/**
	 * Consulta los medicamentos del dia en formato consumible por la app movil (B-10: GET /api/medications/today).
	 */
	public List<MobileMedicationDoseDto> consultarDosisHoyMovil(LocalDate fecha, String userEmail) {
		String usuarioId = resolverUsuario(userEmail);
		LocalDate fechaObjetivo = fecha != null ? fecha : LocalDate.now();

		List<Horario> horariosAplicables = obtenerHorariosDelDia(usuarioId, fechaObjetivo);
		if (horariosAplicables.isEmpty()) {
			return List.of();
		}

		List<TomaMedicamento> tomasFecha = tomaRepository.findByUsuarioIdAndFechaToma(usuarioId, fechaObjetivo);

		return horariosAplicables.stream()
				.map(horario -> {
					Medicamento med = horario.getMedicamento();
					String id = horario.getId() != null ? horario.getId().toString()
							: (med != null && med.getId() != null ? med.getId().toString() : UUID.randomUUID().toString());
					String instrucciones = (horario.getNotas() != null && !horario.getNotas().isBlank())
							? horario.getNotas()
							: (med != null ? med.getInstrucciones() : null);

					TomaMedicamento tomaMatch = buscarTomaCorrespondiente(tomasFecha, horario);
					boolean realizada = tomaMatch != null && tomaMatch.isRealizada();
					String completedAt = realizada ? tomaMatch.getHoraToma() : null;

					return new MobileMedicationDoseDto(
							id,
							med != null ? med.getNombre() : "",
							med != null && med.getDosis() != null ? med.getDosis() : "",
							horario.getHoraAdministracion(),
							realizada,
							instrucciones,
							horario.getFrecuencia(),
							horario.getDiasSeleccionados(),
							usuarioId,
							completedAt,
							fechaObjetivo.toString());
				})
				.toList();
	}

	private List<Horario> obtenerHorariosDelDia(String usuarioId, LocalDate fecha) {
		List<Horario> todos = new ArrayList<>(horarioRepository.findHorariosParaRecordatoriosPorUsuario(usuarioId));

		Set<UUID> idsConHorario = todos.stream()
				.map(Horario::getMedicamento)
				.filter(Objects::nonNull)
				.map(Medicamento::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<Medicamento> medicamentos = repository.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(usuarioId);
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

	private static TomaMedicamento buscarTomaCorrespondiente(List<TomaMedicamento> tomas, Horario horario) {
		if (tomas == null || tomas.isEmpty() || horario == null) {
			return null;
		}
		for (TomaMedicamento t : tomas) {
			if (horario.getId() != null && t.getHorario() != null && horario.getId().equals(t.getHorario().getId())) {
				return t;
			}
			if (horario.getMedicamento() != null && t.getMedicamento() != null
					&& horario.getMedicamento().getId().equals(t.getMedicamento().getId())
					&& (t.getHorario() == null || horario.getId() == null)) {
				return t;
			}
		}
		return null;
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
				.map(MedicamentoService::normalizarTexto)
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

	/** Baja logica: los datos clinicos no se borran fisicamente. */
	@Transactional
	public void desactivar(UUID id) {
		String usuarioId = obtenerUsuarioAutenticado();
		Medicamento medicamento = obtenerEntidadDeUsuario(id, usuarioId);
		medicamento.desactivar();
		repository.save(medicamento);
	}

	private Medicamento obtenerEntidadDeUsuario(UUID id, String usuarioId) {
		Medicamento medicamento = repository.findById(id)
				.orElseThrow(() -> new MedicamentoNotFoundException(id));

		if (!medicamento.perteneceAUsuario(usuarioId)) {
			throw new MedicamentoAccesoDenegadoException(id);
		}
		return medicamento;
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

	private static void validarRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
		if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
			throw new MedicamentoFechasInvalidasException(fechaInicio, fechaFin);
		}
	}

	private static LocalDate parsearFecha(String valor, String campo) {
		String limpio = valor.trim();
		try {
			if (limpio.contains("/")) {
				return LocalDate.parse(limpio, FORMATO_DIA_MES_ANIO);
			}
			return LocalDate.parse(limpio);
		} catch (DateTimeParseException ex) {
			throw new ApiException(
					ErrorCode.VALIDATION_ERROR,
					"Formato de fecha invalido en '" + campo + "' (usa YYYY-MM-DD o DD/MM/AAAA)")
					.withDetail("field", campo);
		}
	}

	private static Specification<Medicamento> filtroPorUsuario(String usuarioId, String termino, boolean incluirInactivos) {
		return (root, query, builder) -> {
			Predicate predicate = builder.equal(root.get("usuarioId"), usuarioId);

			if (!incluirInactivos) {
				predicate = builder.and(predicate, builder.isTrue(root.get("activo")));
			}

			if (termino == null || termino.isBlank()) {
				return predicate;
			}

			String like = "%" + termino.toLowerCase(Locale.ROOT) + "%";
			Predicate coincide = builder.or(
					builder.like(builder.lower(root.get("nombre")), like),
					builder.like(builder.lower(root.get("dosis")), like),
					builder.like(builder.lower(root.get("frecuencia")), like),
					builder.like(builder.lower(root.get("instrucciones")), like));
			return builder.and(predicate, coincide);
		};
	}

	private static String aplicarTrimOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

}

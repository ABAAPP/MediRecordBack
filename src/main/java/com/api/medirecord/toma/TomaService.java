package com.api.medirecord.toma;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;
import com.api.medirecord.recordatorio.dto.MobileMedicationReminderDto;
import com.api.medirecord.toma.dto.CompleteReminderRequest;
import com.api.medirecord.toma.dto.CompleteReminderResponse;
import com.api.medirecord.toma.dto.RegistrarTomaRequest;
import com.api.medirecord.toma.dto.TomaResponse;

/**
 * Servicio de logica de negocio para el registro y actualizacion de tomas
 * realizadas de medicamentos (B-09).
 */
@Service
@Transactional
public class TomaService {

	private static final DateTimeFormatter FORMATO_HORA_12H = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

	private final TomaMedicamentoRepository tomaRepository;
	private final MedicamentoRepository medicamentoRepository;
	private final HorarioRepository horarioRepository;

	public TomaService(
			TomaMedicamentoRepository tomaRepository,
			MedicamentoRepository medicamentoRepository,
			HorarioRepository horarioRepository) {
		this.tomaRepository = tomaRepository;
		this.medicamentoRepository = medicamentoRepository;
		this.horarioRepository = horarioRepository;
	}

	/**
	 * Registra o actualiza el estado de una toma realizada para el usuario autenticado.
	 */
	public TomaResponse registrarToma(RegistrarTomaRequest request) {
		String usuarioId = obtenerUsuarioAutenticado();
		Medicamento medicamento = validarYObtenerMedicamento(request.medicamentoId(), usuarioId);

		Horario horario = null;
		if (request.horarioId() != null) {
			horario = horarioRepository.findById(request.horarioId())
					.orElse(null);
			if (horario != null && horario.getMedicamento() != null
					&& !horario.getMedicamento().getId().equals(medicamento.getId())) {
				throw new ApiException(
						ErrorCode.DATA_INTEGRITY_ERROR,
						"El horario indicado no corresponde al medicamento especificado");
			}
		}

		LocalDate fechaToma = request.fechaToma() != null ? request.fechaToma() : LocalDate.now();
		String horaToma = (request.horaToma() != null && !request.horaToma().isBlank())
				? request.horaToma().trim()
				: obtenerHoraActualFormateada();
		boolean realizada = request.realizada() == null || request.realizada();

		UUID horarioId = horario != null ? horario.getId() : null;
		TomaMedicamento toma = tomaRepository
				.findTomaExistente(usuarioId, medicamento.getId(), horarioId, fechaToma)
				.orElseGet(() -> new TomaMedicamento(
						medicamento,
						null,
						usuarioId,
						fechaToma,
						horaToma,
						realizada,
						request.notas()));

		toma.setMedicamento(medicamento);
		toma.setHorario(horario);
		toma.setRealizada(realizada);
		toma.setHoraToma(horaToma);
		if (request.notas() != null) {
			toma.setNotas(request.notas());
		}

		return TomaResponse.from(tomaRepository.save(toma));
	}

	/**
	 * Marca un recordatorio como completado desde la aplicacion movil
	 * ({@code POST /api/reminders/{id}/complete}).
	 */
	public CompleteReminderResponse marcarRecordatorioCompletado(String reminderId, CompleteReminderRequest request) {
		String usuarioId = resolverUsuario(request != null ? request.userEmail() : null);
		EntidadesRecordatorio entidades = identificarRecordatorio(reminderId, usuarioId);

		Medicamento medicamento = entidades.medicamento();
		Horario horario = entidades.horario();

		LocalDate hoy = LocalDate.now();
		String completedAt = (request != null && request.completedAt() != null && !request.completedAt().isBlank())
				? request.completedAt().trim()
				: obtenerHoraActualFormateada();

		UUID horarioId = horario != null ? horario.getId() : null;
		TomaMedicamento toma = tomaRepository
				.findTomaExistente(usuarioId, medicamento.getId(), horarioId, hoy)
				.orElseGet(() -> new TomaMedicamento(
						medicamento,
						horario,
						usuarioId,
						hoy,
						completedAt,
						true,
						null));

		toma.setMedicamento(medicamento);
		toma.setHorario(horario);
		toma.setRealizada(true);
		toma.setHoraToma(completedAt);
		tomaRepository.save(toma);

		MobileMedicationReminderDto updatedDto = construirReminderDto(medicamento, horario, reminderId, true, completedAt);

		return new CompleteReminderResponse(
				true,
				"Toma de '" + medicamento.getNombre() + "' marcada como realizada",
				reminderId,
				completedAt,
				updatedDto);
	}

	/**
	 * Alterna el estado de cumplimiento (realizada <-> pendiente) para una dosis o medicamento
	 * en la fecha actual ({@code POST /api/medications/{id}/toggle}).
	 */
	public MobileMedicationDoseDto alternarEstadoToma(String doseId) {
		String usuarioId = obtenerUsuarioAutenticado();
		EntidadesRecordatorio entidades = identificarRecordatorio(doseId, usuarioId);

		Medicamento medicamento = entidades.medicamento();
		Horario horario = entidades.horario();
		LocalDate hoy = LocalDate.now();

		UUID horarioId = horario != null ? horario.getId() : null;
		Optional<TomaMedicamento> optToma = tomaRepository
				.findTomaExistente(usuarioId, medicamento.getId(), horarioId, hoy);

		boolean nuevoEstado = true;
		String horaToma = obtenerHoraActualFormateada();

		TomaMedicamento toma;
		if (optToma.isPresent()) {
			toma = optToma.get();
			nuevoEstado = !toma.isRealizada();
			toma.setRealizada(nuevoEstado);
			if (nuevoEstado) {
				toma.setHoraToma(horaToma);
			}
		} else {
			toma = new TomaMedicamento(
					medicamento,
					horario,
					usuarioId,
					hoy,
					horaToma,
					true,
					null);
		}
		tomaRepository.save(toma);

		String finalCompletedAt = nuevoEstado ? toma.getHoraToma() : null;
		String scheduledTime = horario != null ? horario.getHoraAdministracion()
				: (medicamento.getHoraProgramada() != null ? medicamento.getHoraProgramada() : "08:00 AM");
		String frecuencia = horario != null ? horario.getFrecuencia()
				: (medicamento.getFrecuencia() != null ? medicamento.getFrecuencia() : "Todos los días (Diario)");
		List<String> dias = horario != null ? horario.getDiasSeleccionados() : Horario.TODOS_LOS_DIAS;
		String instrucciones = (horario != null && horario.getNotas() != null && !horario.getNotas().isBlank())
				? horario.getNotas()
				: medicamento.getInstrucciones();

		return new MobileMedicationDoseDto(
				doseId,
				medicamento.getNombre(),
				medicamento.getDosis() != null ? medicamento.getDosis() : "",
				scheduledTime,
				nuevoEstado,
				instrucciones,
				frecuencia,
				dias,
				usuarioId,
				finalCompletedAt,
				hoy.toString());
	}

	@Transactional(readOnly = true)
	public List<TomaResponse> consultarTomasFecha(LocalDate fecha) {
		String usuarioId = obtenerUsuarioAutenticado();
		LocalDate fechaObjetivo = fecha != null ? fecha : LocalDate.now();
		return tomaRepository.findByUsuarioIdAndFechaToma(usuarioId, fechaObjetivo).stream()
				.map(TomaResponse::from)
				.toList();
	}

	private Medicamento validarYObtenerMedicamento(UUID medicamentoId, String usuarioId) {
		Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
				.orElseThrow(() -> new MedicamentoNotFoundException(medicamentoId));

		if (!medicamento.perteneceAUsuario(usuarioId)) {
			throw new MedicamentoAccesoDenegadoException(medicamentoId);
		}
		return medicamento;
	}

	private EntidadesRecordatorio identificarRecordatorio(String idStr, String usuarioId) {
		if (idStr == null || idStr.isBlank()) {
			throw new ApiException(ErrorCode.MISSING_PARAMETER, "El id del recordatorio es obligatorio");
		}

		UUID uuid = null;
		try {
			uuid = UUID.fromString(idStr.trim());
		} catch (IllegalArgumentException ignored) {
			// No es UUID; se tratara como ID numerico o alias
		}

		if (uuid != null) {
			// Intentar primero como Horario
			Optional<Horario> optHorario = horarioRepository.findById(uuid);
			if (optHorario.isPresent()) {
				Horario h = optHorario.get();
				Medicamento m = h.getMedicamento();
				if (m != null && !m.perteneceAUsuario(usuarioId)) {
					throw new MedicamentoAccesoDenegadoException(uuid);
				}
				return new EntidadesRecordatorio(m, h);
			}

			// Intentar luego como Medicamento
			Optional<Medicamento> optMed = medicamentoRepository.findById(uuid);
			if (optMed.isPresent()) {
				Medicamento m = optMed.get();
				if (!m.perteneceAUsuario(usuarioId)) {
					throw new MedicamentoAccesoDenegadoException(uuid);
				}
				List<Horario> horarios = horarioRepository.findActivosByMedicamentoId(m.getId());
				Horario primerHorario = horarios.isEmpty() ? null : horarios.getFirst();
				return new EntidadesRecordatorio(m, primerHorario);
			}
		}

		// Si es un id numerico de prueba/simulacion movil, buscar entre los medicamentos del usuario
		List<Medicamento> medicamentos = medicamentoRepository
				.findByUsuarioIdAndActivoTrueOrderByFechaInicioDesc(usuarioId);
		if (!medicamentos.isEmpty()) {
			Medicamento m = medicamentos.getFirst();
			List<Horario> horarios = horarioRepository.findActivosByMedicamentoId(m.getId());
			Horario primerHorario = horarios.isEmpty() ? null : horarios.getFirst();
			return new EntidadesRecordatorio(m, primerHorario);
		}

		throw new MedicamentoNotFoundException(uuid != null ? uuid : UUID.randomUUID());
	}

	private static MobileMedicationReminderDto construirReminderDto(
			Medicamento med,
			Horario horario,
			String reminderId,
			boolean isCompleted,
			String completedAt) {
		String id = reminderId != null ? reminderId
				: (horario != null && horario.getId() != null ? horario.getId().toString()
				: (med.getId() != null ? med.getId().toString() : "1"));
		String medId = med.getId() != null ? med.getId().toString() : id;
		String scheduledTime = horario != null ? horario.getHoraAdministracion()
				: (med.getHoraProgramada() != null ? med.getHoraProgramada() : "08:00 AM");
		String frequency = horario != null ? horario.getFrecuencia()
				: (med.getFrecuencia() != null ? med.getFrecuencia() : "Todos los días (Diario)");
		List<String> dias = horario != null ? horario.getDiasSeleccionados() : Horario.TODOS_LOS_DIAS;
		String instrucciones = (horario != null && horario.getNotas() != null && !horario.getNotas().isBlank())
				? horario.getNotas()
				: med.getInstrucciones();

		return new MobileMedicationReminderDto(
				id,
				medId,
				med.getNombre(),
				med.getDosis() != null ? med.getDosis() : "",
				scheduledTime,
				frequency,
				dias,
				instrucciones,
				isCompleted,
				med.getUsuarioId(),
				completedAt);
	}

	private static String obtenerHoraActualFormateada() {
		return LocalTime.now().format(FORMATO_HORA_12H);
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

	private record EntidadesRecordatorio(Medicamento medicamento, Horario horario) {
	}

}

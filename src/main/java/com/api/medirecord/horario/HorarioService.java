package com.api.medirecord.horario;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.common.security.SecurityUtils;
import com.api.medirecord.horario.dto.HorarioRequest;
import com.api.medirecord.horario.dto.HorarioResponse;
import com.api.medirecord.horario.dto.MobileScheduleRequest;
import com.api.medirecord.horario.dto.MobileScheduleResponse;
import com.api.medirecord.horario.exception.HorarioNotFoundException;
import com.api.medirecord.medicamento.Medicamento;
import com.api.medirecord.medicamento.MedicamentoRepository;
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

/**
 * Servicio de dominio para la gestion de horarios y frecuencias de medicamentos (B-07).
 *
 * <p>Garantiza que ningun horario pueda asociarse a un medicamento inexistente
 * o perteneciente a otro usuario, y expone la programacion activa para el
 * servicio de recordatorios.</p>
 */
@Service
@Transactional(readOnly = true)
public class HorarioService {

	private final HorarioRepository horarioRepository;
	private final MedicamentoRepository medicamentoRepository;

	public HorarioService(HorarioRepository horarioRepository, MedicamentoRepository medicamentoRepository) {
		this.horarioRepository = horarioRepository;
		this.medicamentoRepository = medicamentoRepository;
	}

	@Transactional
	public HorarioResponse crear(HorarioRequest request) {
		String usuarioId = obtenerUsuarioAutenticado();
		Medicamento medicamento = obtenerMedicamentoValido(request.medicamentoId(), usuarioId);

		boolean recordatorioActivo = request.recordatorioActivo() == null || request.recordatorioActivo();
		String notasLimpias = aplicarTrimOpcional(request.notas());

		Horario horario = new Horario(
				medicamento,
				request.horaAdministracion().trim(),
				request.frecuencia().trim(),
				request.diasSemana(),
				recordatorioActivo,
				notasLimpias);

		sincronizarMedicamentoConHorario(medicamento, horario);
		return HorarioResponse.from(horarioRepository.save(horario));
	}

	/**
	 * Configura un horario desde la aplicacion movil ({@code POST /api/schedules}),
	 * verificando que el medicamento exista y pertenezca al usuario autenticado.
	 */
	@Transactional
	public MobileScheduleResponse guardarDesdeApp(MobileScheduleRequest request) {
		UUID medicamentoId = parsearUuidMedicamento(request.medicationId());
		String usuarioId = obtenerUsuarioAutenticado();
		Medicamento medicamento = obtenerMedicamentoValido(medicamentoId, usuarioId);

		boolean recordatorioActivo = request.reminderEnabled() == null || request.reminderEnabled();
		Horario horario = new Horario(
				medicamento,
				request.scheduledTime().trim(),
				request.frequency().trim(),
				request.selectedDays(),
				recordatorioActivo,
				aplicarTrimOpcional(request.notes()));

		sincronizarMedicamentoConHorario(medicamento, horario);
		return MobileScheduleResponse.from(horarioRepository.save(horario));
	}

	@Transactional
	public HorarioResponse actualizar(UUID id, HorarioRequest request) {
		String usuarioId = obtenerUsuarioAutenticado();
		Horario horario = obtenerHorarioDeUsuario(id, usuarioId);
		Medicamento medicamento = obtenerMedicamentoValido(request.medicamentoId(), usuarioId);

		horario.setMedicamento(medicamento);
		horario.setHoraAdministracion(request.horaAdministracion().trim());
		horario.setFrecuencia(request.frecuencia().trim());
		horario.setDiasSeleccionados(request.diasSemana());
		if (request.recordatorioActivo() != null) {
			horario.setRecordatorioActivo(request.recordatorioActivo());
		}
		horario.setNotas(aplicarTrimOpcional(request.notas()));

		sincronizarMedicamentoConHorario(medicamento, horario);
		return HorarioResponse.from(horarioRepository.save(horario));
	}

	public HorarioResponse obtener(UUID id) {
		String usuarioId = obtenerUsuarioAutenticado();
		return HorarioResponse.from(obtenerHorarioDeUsuario(id, usuarioId));
	}

	public List<HorarioResponse> listarPorMedicamento(UUID medicamentoId) {
		String usuarioId = obtenerUsuarioAutenticado();
		obtenerMedicamentoValido(medicamentoId, usuarioId);
		return horarioRepository.findActivosByMedicamentoId(medicamentoId).stream()
				.map(HorarioResponse::from)
				.toList();
	}

	/**
	 * Provee todos los horarios activos con recordatorio habilitado del usuario
	 * autenticado para el servicio de recordatorios (Criterio 7 de B-07).
	 */
	public List<HorarioResponse> obtenerHorariosParaRecordatorios() {
		String usuarioId = obtenerUsuarioAutenticado();
		return obtenerHorariosParaRecordatoriosDeUsuario(usuarioId);
	}

	/**
	 * Consulta de uso interno para el servicio de recordatorios dado un usuario.
	 */
	public List<HorarioResponse> obtenerHorariosParaRecordatoriosDeUsuario(String usuarioId) {
		return horarioRepository.findHorariosParaRecordatoriosPorUsuario(usuarioId).stream()
				.map(HorarioResponse::from)
				.toList();
	}

	@Transactional
	public void desactivar(UUID id) {
		String usuarioId = obtenerUsuarioAutenticado();
		Horario horario = obtenerHorarioDeUsuario(id, usuarioId);
		horario.desactivar();
		horarioRepository.save(horario);
	}

	private Medicamento obtenerMedicamentoValido(UUID medicamentoId, String usuarioId) {
		Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
				.filter(Medicamento::isActivo)
				.orElseThrow(() -> new MedicamentoNotFoundException(medicamentoId));

		if (!medicamento.perteneceAUsuario(usuarioId)) {
			throw new MedicamentoAccesoDenegadoException(medicamentoId);
		}
		return medicamento;
	}

	private Horario obtenerHorarioDeUsuario(UUID id, String usuarioId) {
		Horario horario = horarioRepository.findById(id)
				.orElseThrow(() -> new HorarioNotFoundException(id));

		if (horario.getMedicamento() == null || !horario.getMedicamento().perteneceAUsuario(usuarioId)) {
			throw new MedicamentoAccesoDenegadoException(id);
		}
		return horario;
	}

	private void sincronizarMedicamentoConHorario(Medicamento medicamento, Horario horario) {
		medicamento.setHoraProgramada(horario.getHoraAdministracion());
		medicamento.setFrecuencia(horario.getFrecuencia());
		if (horario.getNotas() != null && !horario.getNotas().isBlank()) {
			medicamento.setInstrucciones(horario.getNotas());
		}
		medicamentoRepository.save(medicamento);
	}

	private static UUID parsearUuidMedicamento(String valor) {
		try {
			return UUID.fromString(valor.trim());
		} catch (IllegalArgumentException ex) {
			throw new ApiException(
					ErrorCode.VALIDATION_ERROR,
					"El identificador del medicamento no tiene un formato UUID valido: " + valor)
					.withDetail("field", "medicationId");
		}
	}

	private static String obtenerUsuarioAutenticado() {
		return SecurityUtils.currentUsername()
				.filter(username -> !username.isBlank())
				.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
	}

	private static String aplicarTrimOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

}

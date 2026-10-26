package com.api.medirecord.medicamento;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.common.security.SecurityUtils;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoFechasInvalidasException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

import jakarta.persistence.criteria.Predicate;

/**
 * Reglas de negocio de medicamentos asociados a la cuenta del usuario.
 *
 * <p>Garantiza el aislamiento por propietario: cada operacion obtiene el
 * usuario autenticado del contexto de seguridad y restringe el acceso a sus
 * propios medicamentos.</p>
 */
@Service
@Transactional(readOnly = true)
public class MedicamentoService {

	private final MedicamentoRepository repository;

	public MedicamentoService(MedicamentoRepository repository) {
		this.repository = repository;
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
				aplicarTrimOpcional(request.instrucciones()),
				request.fechaInicio(),
				request.fechaFin());

		return MedicamentoResponse.from(repository.save(medicamento));
	}

	@Transactional
	public MedicamentoResponse actualizar(UUID id, MedicamentoRequest request) {
		String usuarioId = obtenerUsuarioAutenticado();
		Medicamento medicamento = obtenerEntidadDeUsuario(id, usuarioId);
		validarRangoFechas(request.fechaInicio(), request.fechaFin());

		medicamento.setNombre(request.nombre().trim());
		medicamento.setDosis(aplicarTrimOpcional(request.dosis()));
		medicamento.setFrecuencia(aplicarTrimOpcional(request.frecuencia()));
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

	/** Baja logica del medicamento verificando que pertenezca al usuario autenticado. */
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

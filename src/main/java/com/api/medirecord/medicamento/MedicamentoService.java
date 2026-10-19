package com.api.medirecord.medicamento;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.common.security.SecurityUtils;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.exception.CuentaNoVinculadaException;
import com.api.medirecord.medicamento.exception.FechasTratamientoInvalidasException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;
import com.api.medirecord.usuario.Usuario;
import com.api.medirecord.usuario.UsuarioRepository;

import jakarta.persistence.criteria.Predicate;

/**
 * Reglas de negocio de medicamentos.
 *
 * <p>Cada operacion esta acotada al usuario autenticado (claim {@code sub} del
 * JWT -&gt; cuenta local): un usuario nunca puede leer ni modificar medicamentos
 * de otro. Es la unica capa que conoce las entidades y las expone como DTOs.
 * Toda operacion de escritura se ejecuta dentro de una transaccion.</p>
 */
@Service
@Transactional(readOnly = true)
public class MedicamentoService {

	private final MedicamentoRepository repository;
	private final UsuarioRepository usuarioRepository;

	public MedicamentoService(MedicamentoRepository repository, UsuarioRepository usuarioRepository) {
		this.repository = repository;
		this.usuarioRepository = usuarioRepository;
	}

	public MedicamentoResponse crear(MedicamentoRequest request) {
		Usuario usuario = usuarioActual();
		validarFechas(request.fechaInicio(), request.fechaFin());

		Medicamento medicamento = new Medicamento(
				usuario,
				request.nombre().trim(),
				aplicarTrimOpcional(request.dosis()),
				aplicarTrimOpcional(request.frecuencia()),
				request.fechaInicio(),
				request.fechaFin());

		return MedicamentoResponse.from(repository.save(medicamento));
	}

	public MedicamentoResponse actualizar(UUID id, MedicamentoRequest request) {
		Medicamento medicamento = obtenerEntidadPropia(id);
		validarFechas(request.fechaInicio(), request.fechaFin());

		medicamento.setNombre(request.nombre().trim());
		medicamento.setDosis(aplicarTrimOpcional(request.dosis()));
		medicamento.setFrecuencia(aplicarTrimOpcional(request.frecuencia()));
		medicamento.setFechaInicio(request.fechaInicio());
		medicamento.setFechaFin(request.fechaFin());

		return MedicamentoResponse.from(repository.save(medicamento));
	}

	public MedicamentoResponse obtener(UUID id) {
		return MedicamentoResponse.from(obtenerEntidadPropia(id));
	}

	public PageResponse<MedicamentoResponse> listar(String termino, boolean incluirInactivos, Pageable pageable) {
		Usuario usuario = usuarioActual();
		Page<Medicamento> resultados = repository.findAll(filtro(usuario.getId(), termino, incluirInactivos),
				pageable);
		return PageResponse.from(resultados, MedicamentoResponse::from);
	}

	/** Baja logica: los datos clinicos no se borran fisicamente. */
	@Transactional
	public void desactivar(UUID id) {
		Medicamento medicamento = obtenerEntidadPropia(id);
		medicamento.desactivar();
		repository.save(medicamento);
	}

	/**
	 * Cuenta local del usuario autenticado. Sin vinculo o cuenta desactivada no hay
	 * acceso al modulo (los endpoints exigen JWT via SecurityConfig).
	 */
	private Usuario usuarioActual() {
		String keycloakId = SecurityUtils.currentUsername()
				.orElseThrow(CuentaNoVinculadaException::new);
		return usuarioRepository.findByKeycloakId(keycloakId)
				.filter(Usuario::isActivo)
				.orElseThrow(CuentaNoVinculadaException::new);
	}

	/** Si el medicamento no existe o es de otro usuario responde 404 (sin filtrar). */
	private Medicamento obtenerEntidadPropia(UUID id) {
		Usuario usuario = usuarioActual();
		return repository.findByIdAndUsuarioId(id, usuario.getId())
				.orElseThrow(() -> new MedicamentoNotFoundException(id));
	}

	private static void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
		if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
			throw new FechasTratamientoInvalidasException(fechaInicio, fechaFin);
		}
	}

	private static Specification<Medicamento> filtro(UUID usuarioId, String termino, boolean incluirInactivos) {
		return (root, query, builder) -> {
			Predicate porUsuario = builder.equal(root.get("usuario").get("id"), usuarioId);
			Predicate porActivo = incluirInactivos
					? builder.conjunction()
					: builder.isTrue(root.get("activo"));

			if (termino == null || termino.isBlank()) {
				return builder.and(porUsuario, porActivo);
			}

			String like = "%" + termino.toLowerCase(Locale.ROOT) + "%";
			Predicate coincide = builder.or(
					builder.like(builder.lower(root.get("nombre")), like),
					builder.like(builder.lower(root.get("dosis")), like),
					builder.like(builder.lower(root.get("frecuencia")), like));
			return builder.and(porUsuario, porActivo, coincide);
		};
	}

	private static String aplicarTrimOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

}

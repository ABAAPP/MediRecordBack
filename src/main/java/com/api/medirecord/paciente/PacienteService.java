package com.api.medirecord.paciente;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.paciente.dto.PacienteRequest;
import com.api.medirecord.paciente.dto.PacienteResponse;
import com.api.medirecord.paciente.exception.PacienteEmailDuplicadoException;
import com.api.medirecord.paciente.exception.PacienteNotFoundException;

import jakarta.persistence.criteria.Predicate;

/**
 * Reglas de negocio de pacientes.
 *
 * <p>Es la unica capa que conoce las entidades y las expone como DTOs. Toda
 * operacion de escritura se ejecuta dentro de una transaccion.</p>
 */
@Service
@Transactional(readOnly = true)
public class PacienteService {

	private final PacienteRepository repository;

	public PacienteService(PacienteRepository repository) {
		this.repository = repository;
	}

	public PacienteResponse crear(PacienteRequest request) {
		String email = normalizarEmail(request.email());
		if (repository.existsByEmailIgnoreCase(email)) {
			throw new PacienteEmailDuplicadoException(email);
		}

		Paciente paciente = new Paciente(
				request.nombre().trim(),
				request.apellidoPaterno().trim(),
				aplicarTrimOpcional(request.apellidoMaterno()),
				request.fechaNacimiento(),
				email,
				aplicarTrimOpcional(request.telefono()));

		return PacienteResponse.from(repository.save(paciente));
	}

	public PacienteResponse actualizar(UUID id, PacienteRequest request) {
		Paciente paciente = obtenerEntidad(id);

		String email = normalizarEmail(request.email());
		if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
			throw new PacienteEmailDuplicadoException(email);
		}

		paciente.setNombre(request.nombre().trim());
		paciente.setApellidoPaterno(request.apellidoPaterno().trim());
		paciente.setApellidoMaterno(aplicarTrimOpcional(request.apellidoMaterno()));
		paciente.setFechaNacimiento(request.fechaNacimiento());
		paciente.setEmail(email);
		paciente.setTelefono(aplicarTrimOpcional(request.telefono()));

		return PacienteResponse.from(repository.save(paciente));
	}

	public PacienteResponse obtener(UUID id) {
		return PacienteResponse.from(obtenerEntidad(id));
	}

	public PageResponse<PacienteResponse> buscar(String termino, boolean incluirInactivos, Pageable pageable) {
		Page<Paciente> resultados = repository.findAll(filtro(termino, incluirInactivos), pageable);
		return PageResponse.from(resultados, PacienteResponse::from);
	}

	/** Baja logica: los datos clinicos no se borran fisicamente. */
	@Transactional
	public void desactivar(UUID id) {
		Paciente paciente = obtenerEntidad(id);
		paciente.desactivar();
		repository.save(paciente);
	}

	private Paciente obtenerEntidad(UUID id) {
		return repository.findById(id).orElseThrow(() -> new PacienteNotFoundException(id));
	}

	private static Specification<Paciente> filtro(String termino, boolean incluirInactivos) {
		return (root, query, builder) -> {
			Predicate predicate = incluirInactivos
					? builder.conjunction()
					: builder.isTrue(root.get("activo"));

			if (termino == null || termino.isBlank()) {
				return predicate;
			}

			String like = "%" + termino.toLowerCase(Locale.ROOT) + "%";
			Predicate coincide = builder.or(
					builder.like(builder.lower(root.get("nombre")), like),
					builder.like(builder.lower(root.get("apellidoPaterno")), like),
					builder.like(builder.lower(root.get("apellidoMaterno")), like),
					builder.like(builder.lower(root.get("email")), like));
			return builder.and(predicate, coincide);
		};
	}

	private static String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String aplicarTrimOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

}
package com.api.medirecord.usuario;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.usuario.dto.UsuarioRequest;
import com.api.medirecord.usuario.dto.UsuarioResponse;
import com.api.medirecord.usuario.exception.UsuarioEmailDuplicadoException;
import com.api.medirecord.usuario.exception.UsuarioNotFoundException;

import jakarta.persistence.criteria.Predicate;

/**
 * Reglas de negocio de usuarios.
 *
 * <p>Es la unica capa que conoce las entidades y las expone como DTOs. Toda
 * operacion de escritura se ejecuta dentro de una transaccion. El correo se
 * normaliza (trim + minusculas) para que la unicidad no dependa del formato.</p>
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

	private final UsuarioRepository repository;

	public UsuarioService(UsuarioRepository repository) {
		this.repository = repository;
	}

	public UsuarioResponse crear(UsuarioRequest request) {
		String email = normalizarEmail(request.email());
		if (repository.existsByEmailIgnoreCase(email)) {
			throw new UsuarioEmailDuplicadoException(email);
		}

		Usuario usuario = new Usuario(
				request.nombre().trim(),
				request.apellidoPaterno().trim(),
				aplicarTrimOpcional(request.apellidoMaterno()),
				email,
				aplicarTrimOpcional(request.telefono()));

		return UsuarioResponse.from(repository.save(usuario));
	}

	public UsuarioResponse actualizar(UUID id, UsuarioRequest request) {
		Usuario usuario = obtenerEntidad(id);

		String email = normalizarEmail(request.email());
		if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
			throw new UsuarioEmailDuplicadoException(email);
		}

		usuario.setNombre(request.nombre().trim());
		usuario.setApellidoPaterno(request.apellidoPaterno().trim());
		usuario.setApellidoMaterno(aplicarTrimOpcional(request.apellidoMaterno()));
		usuario.setEmail(email);
		usuario.setTelefono(aplicarTrimOpcional(request.telefono()));

		return UsuarioResponse.from(repository.save(usuario));
	}

	public UsuarioResponse obtener(UUID id) {
		return UsuarioResponse.from(obtenerEntidad(id));
	}

	/** Cuenta local asociada a una identidad externa (claim {@code sub}), si existe. */
	public Optional<UsuarioResponse> buscarPorKeycloakId(String keycloakId) {
		return repository.findByKeycloakId(keycloakId).map(UsuarioResponse::from);
	}

	/**
	 * Enlaza la cuenta local con la identidad creada en el proveedor de identidad.
	 * Se invoca al termino del registro, dentro de la misma transaccion.
	 */
	@Transactional
	public UsuarioResponse vincularIdentidad(UUID id, String keycloakId) {
		Usuario usuario = obtenerEntidad(id);
		usuario.vincularKeycloakId(keycloakId);
		return UsuarioResponse.from(repository.save(usuario));
	}

	public PageResponse<UsuarioResponse> buscar(String termino, boolean incluirInactivos, Pageable pageable) {
		Page<Usuario> resultados = repository.findAll(filtro(termino, incluirInactivos), pageable);
		return PageResponse.from(resultados, UsuarioResponse::from);
	}

	/** Baja logica: el historial se conserva y la cuenta deja de poder usarse. */
	@Transactional
	public void desactivar(UUID id) {
		Usuario usuario = obtenerEntidad(id);
		usuario.desactivar();
		repository.save(usuario);
	}

	private Usuario obtenerEntidad(UUID id) {
		return repository.findById(id).orElseThrow(() -> new UsuarioNotFoundException(id));
	}

	private static Specification<Usuario> filtro(String termino, boolean incluirInactivos) {
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

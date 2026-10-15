package com.api.medirecord.usuario.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.api.medirecord.usuario.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/** Representacion de salida de un usuario (nunca se expone la entidad). */
@Schema(name = "UsuarioResponse", description = "Datos de un usuario registrado")
public record UsuarioResponse(

		@Schema(example = "3f7c8b2e-1d4a-4c9e-9f1b-7a2c3d4e5f60") UUID id,
		@Schema(description = "Identidad externa en Keycloak (claim sub)", example = "a1b2c3d4-...") String keycloakId,
		@Schema(example = "Ana") String nombre,
		@Schema(example = "Gomez") String apellidoPaterno,
		@Schema(example = "Lopez") String apellidoMaterno,
		@Schema(example = "Ana Gomez Lopez") String nombreCompleto,
		@Schema(example = "ana.gomez@correo.com") String email,
		@Schema(example = "+52 664 123 4567") String telefono,
		@Schema(example = "true") boolean activo,
		Instant createdAt,
		Instant updatedAt) {

	public static UsuarioResponse from(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getKeycloakId(),
				usuario.getNombre(),
				usuario.getApellidoPaterno(),
				usuario.getApellidoMaterno(),
				nombreCompleto(usuario),
				usuario.getEmail(),
				usuario.getTelefono(),
				usuario.isActivo(),
				usuario.getCreatedAt(),
				usuario.getUpdatedAt());
	}

	private static String nombreCompleto(Usuario usuario) {
		return Stream.of(usuario.getNombre(), usuario.getApellidoPaterno(), usuario.getApellidoMaterno())
				.filter(part -> part != null && !part.isBlank())
				.collect(Collectors.joining(" "));
	}

}

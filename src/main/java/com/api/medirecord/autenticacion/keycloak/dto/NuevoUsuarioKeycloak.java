package com.api.medirecord.autenticacion.keycloak.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Comando de alta de usuario en el Admin API de Keycloak.
 *
 * <p>La contrasena viaja unicamente aqui, dentro de {@code credentials}: Keycloak la
 * almacena hasheada y MediRecord nunca la persiste.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NuevoUsuarioKeycloak(

		String username,

		String email,

		boolean enabled,

		@JsonProperty("emailVerified") boolean emailVerified,

		@JsonProperty("firstName") String firstName,

		@JsonProperty("lastName") String lastName,

		List<Credencial> credentials) {

	/** Credencial de tipo contrasena; {@code temporary = false} para que el usuario pueda usarla ya. */
	public record Credencial(String type, String value, boolean temporary) {

		public static Credencial contrasena(String password) {
			return new Credencial("password", password, false);
		}
	}

}

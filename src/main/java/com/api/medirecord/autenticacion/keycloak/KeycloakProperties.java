package com.api.medirecord.autenticacion.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integracion con Keycloak (propiedad {@code medirecord.keycloak}).
 *
 * <p>{@code base-url} es la URL del servidor Keycloak vista desde la API: en local
 * {@code http://localhost:8081}; dentro de la red de Docker {@code http://keycloak:8080}.
 * El bloque {@code admin-*} son las credenciales con las que la API da de alta usuarios
 * (provisionamiento); en produccion deben venir de un gestor de secretos y sustituirse
 * por un cliente de servicio con rol {@code manage-users} en lugar del administrador
 * del realm master.</p>
 */
@ConfigurationProperties(prefix = "medirecord.keycloak")
public record KeycloakProperties(

		String baseUrl,
		String realm,
		String clientId,
		String clientSecret,
		String adminRealm,
		String adminClientId,
		String adminUsername,
		String adminPassword) {

	/** URL del endpoint de tokens del realm de la aplicacion. */
	public String tokenUri() {
		return "/realms/" + realm + "/protocol/openid-connect/token";
	}

	/** URL del endpoint de tokens del realm donde vive el cliente de administracion. */
	public String adminTokenUri() {
		return "/realms/" + adminRealm + "/protocol/openid-connect/token";
	}

	/** URL del recurso de usuarios del realm de la aplicacion. */
	public String usuariosUri() {
		return "/admin/realms/" + realm + "/users";
	}

}

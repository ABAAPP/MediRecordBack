package com.api.medirecord.autenticacion.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta cruda del endpoint de tokens de Keycloak (password / refresh grant). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakToken(

		@JsonProperty("access_token") String accessToken,

		@JsonProperty("token_type") String tokenType,

		@JsonProperty("expires_in") long expiresIn,

		@JsonProperty("refresh_expires_in") long refreshExpiresIn,

		@JsonProperty("refresh_token") String refreshToken) {
}

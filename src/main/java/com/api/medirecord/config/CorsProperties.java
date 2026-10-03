package com.api.medirecord.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuracion de CORS de la API (propiedad {@code medirecord.cors}).
 *
 * <p>En produccion se acota con {@code CORS_ALLOWED_ORIGINS}; nunca usar comodines
 * junto con credenciales.</p>
 */
@ConfigurationProperties(prefix = "medirecord.cors")
public record CorsProperties(

		@DefaultValue("http://localhost:3000") List<String> allowedOrigins,
		@DefaultValue("GET,POST,PUT,PATCH,DELETE,OPTIONS") List<String> allowedMethods,
		@DefaultValue("Authorization,Content-Type,Accept,Origin,X-Requested-With") List<String> allowedHeaders,
		@DefaultValue("Location,Content-Disposition") List<String> exposedHeaders,
		@DefaultValue("true") boolean allowCredentials,
		@DefaultValue("1h") Duration maxAge) {
}
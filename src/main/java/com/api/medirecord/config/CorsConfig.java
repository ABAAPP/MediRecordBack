package com.api.medirecord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** CORS aplicado a {@code /api/**}, consumido tanto por Spring MVC como por Security. */
@Configuration(proxyBeanMethods = false)
public class CorsConfig {

	private final CorsProperties properties;

	public CorsConfig(CorsProperties properties) {
		this.properties = properties;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(properties.allowedMethods());
		configuration.setAllowedHeaders(properties.allowedHeaders());
		configuration.setExposedHeaders(properties.exposedHeaders());
		configuration.setAllowCredentials(properties.allowCredentials());
		configuration.setMaxAge(properties.maxAge());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

}
package com.api.medirecord.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.api.medirecord.common.security.JsonAccessDeniedHandler;
import com.api.medirecord.common.security.JsonAuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cadena de seguridad de la API: resource server OAuth2 contra Keycloak.
 *
 * <p>Mapeo de autoridades:
 * <ul>
 *   <li>{@code realm_access.roles} -> {@code ROLE_*} (autoridades de Spring Security)</li>
 *   <li>{@code resource_access.<cliente>.roles} -> {@code SCOPE_*}</li>
 *   <li>{@code scope} -> {@code SCOPE_*}</li>
 * </ul>
 *
 * <p>Reglas por defecto: sesion sin estado, autenticacion obligatoria en toda la API y
 * {@code /api/v1/admin/**} restringida a rol ADMIN. Afinar por metodo con
 * {@code @PreAuthorize}.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final String ROLE_ADMIN = "ADMIN";
	private static final String ROLE_PREFIX = "ROLE_";
	private static final String SCOPE_PREFIX = "SCOPE_";

	/**
	 * Endpoints publicos. Deliberadamente solo health y Prometheus: /actuator/info y
	 * /actuator/metrics requieren token (pueden filtrar topologia interna).
	 */
	private static final String[] PUBLIC_ENDPOINTS = {
			"/actuator/health",
			"/actuator/health/**",
			"/actuator/prometheus",
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/swagger-ui.html",
			"/swagger-ui/**"
	};

	@Bean
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
			CorsConfigurationSource corsConfigurationSource,
			JsonMapper objectMapper) throws Exception {

		var authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);
		var accessDeniedHandler = new JsonAccessDeniedHandler(objectMapper);

		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole(ROLE_ADMIN)
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.headers(headers -> headers
						.frameOptions(frame -> frame.deny())
						.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.requestCache(Customizer.withDefaults())
				.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		var converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
		return converter;
	}

	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		var authorities = new ArrayList<GrantedAuthority>();

		String scope = jwt.getClaimAsString("scope");
		if (scope != null && !scope.isBlank()) {
			Arrays.stream(scope.split("\\s+"))
					.filter(token -> !token.isBlank())
					.map(token -> new SimpleGrantedAuthority(SCOPE_PREFIX + token))
					.forEach(authorities::add);
		}

		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
			roles.stream()
					.map(String::valueOf)
					.map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
					.forEach(authorities::add);
		}

		Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
		if (resourceAccess != null) {
			resourceAccess.forEach((client, access) -> {
				if (access instanceof Map<?, ?> clientAccess && clientAccess.get("roles") instanceof Collection<?> roles) {
					roles.stream()
							.map(String::valueOf)
							.map(role -> new SimpleGrantedAuthority(
									role.startsWith(SCOPE_PREFIX) ? role : SCOPE_PREFIX + role))
							.forEach(authorities::add);
				}
			});
		}

		return authorities;
	}

}
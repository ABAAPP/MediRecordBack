package com.api.medirecord.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.api.medirecord.auth.dto.LoginRequest;
import com.api.medirecord.auth.dto.LoginResponse;
import com.api.medirecord.auth.dto.RegisterRequest;
import com.api.medirecord.auth.dto.RegisterResponse;
import com.api.medirecord.paciente.PacienteRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Servicio de logica de negocio para autenticacion y registro de usuarios (B-12).
 *
 * <p>Soporta autenticacion directa contra Keycloak (OAuth2 Resource Owner Password Credentials),
 * registro de usuarios en memoria/persistencia y credenciales de desarrollo.</p>
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final PacienteRepository pacienteRepository;
	private final JsonMapper objectMapper;
	private final String keycloakIssuerUri;
	private final HttpClient httpClient;

	private final Map<String, UserRecord> registeredUsers = new ConcurrentHashMap<>();

	public record UserRecord(String name, String email, String password) {
	}

	public AuthService(
			PacienteRepository pacienteRepository,
			JsonMapper objectMapper,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8081/realms/medirecord}") String keycloakIssuerUri) {
		this.pacienteRepository = pacienteRepository;
		this.objectMapper = objectMapper;
		this.keycloakIssuerUri = keycloakIssuerUri;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.build();

		// Usuarios predeterminados para desarrollo / pruebas locales
		registeredUsers.put("admin@medirecord.local", new UserRecord("Admin", "admin@medirecord.local", "admin123"));
		registeredUsers.put("admin", new UserRecord("Admin", "admin", "admin123"));
		registeredUsers.put("medico@medirecord.local", new UserRecord("Medico", "medico@medirecord.local", "medico123"));
		registeredUsers.put("medico", new UserRecord("Medico", "medico", "medico123"));
	}

	/**
	 * Registra un nuevo usuario para acceso a la aplicacion movil o web.
	 */
	public RegisterResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();

		if (registeredUsers.containsKey(email) || pacienteRepository.existsByEmailIgnoreCase(email)) {
			return RegisterResponse.failure("El correo electronico ya esta registrado");
		}

		registeredUsers.put(email, new UserRecord(request.name().trim(), email, request.password()));
		log.info("Usuario registrado exitosamente: {}", email);
		return RegisterResponse.ok();
	}

	/**
	 * Autentica un usuario verificando credenciales en Keycloak o repositorio local.
	 */
	public LoginResponse login(LoginRequest request) {
		String identifier = request.email().trim();
		String password = request.password();

		// 1. Verificar si existe en el registro local
		String normalizedKey = identifier.toLowerCase();
		UserRecord localUser = registeredUsers.get(normalizedKey);
		if (localUser != null) {
			if (localUser.password().equals(password)) {
				// Generar token JWT simulado o firmado para sesion
				String token = "medirecord_jwt_" + UUID.randomUUID();
				return LoginResponse.ok(token, identifier);
			} else {
				return LoginResponse.failure("Credenciales invalidas. Contrasena incorrecta.");
			}
		}

		// 2. Intentar autenticar contra Keycloak si esta disponible
		String keycloakToken = callKeycloakTokenEndpoint(identifier, password);
		if (keycloakToken != null) {
			return LoginResponse.ok(keycloakToken, identifier);
		}

		return LoginResponse.failure("Credenciales invalidas. Verifica tu correo y contrasena.");
	}

	private String callKeycloakTokenEndpoint(String username, String password) {
		try {
			String tokenUrl = keycloakIssuerUri + "/protocol/openid-connect/token";

			Map<String, String> formData = Map.of(
					"grant_type", "password",
					"client_id", "medirecord-api",
					"client_secret", "medirecord-secret",
					"username", username,
					"password", password);

			String formBody = formData.entrySet().stream()
					.map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
							+ URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
					.collect(Collectors.joining("&"));

			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create(tokenUrl))
					.timeout(Duration.ofSeconds(3))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(formBody))
					.build();

			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				JsonNode node = objectMapper.readTree(response.body());
				if (node.has("access_token")) {
					return node.get("access_token").asString();
				}
			}
		} catch (Exception ex) {
			log.debug("No se pudo conectar a Keycloak para autenticar {}: {}", username, ex.getMessage());
		}
		return null;
	}

	public boolean isUserRegistered(String email) {
		return registeredUsers.containsKey(email.trim().toLowerCase());
	}

}

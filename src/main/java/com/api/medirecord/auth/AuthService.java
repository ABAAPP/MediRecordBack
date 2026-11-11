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

		// Intentar registrar usuario en Keycloak si esta disponible
		createUserInKeycloak(request.name().trim(), email, request.password());

		log.info("Usuario registrado exitosamente: {}", email);
		return RegisterResponse.ok();
	}

	/**
	 * Autentica un usuario verificando credenciales en Keycloak o repositorio local.
	 */
	public LoginResponse login(LoginRequest request) {
		String identifier = request.email().trim();
		String password = request.password();

		// 1. Intentar autenticar contra Keycloak en primer lugar para obtener un JWT real
		String keycloakToken = callKeycloakTokenEndpoint(identifier, password);
		if (keycloakToken != null) {
			return LoginResponse.ok(keycloakToken, identifier);
		}

		// 2. Si Keycloak no esta disponible o es entorno de prueba/desarrollo, validar registro local
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

		return LoginResponse.failure("Credenciales invalidas. Verifica tu correo y contrasena.");
	}

	private void createUserInKeycloak(String name, String email, String password) {
		try {
			// Obtener token de administrador de Keycloak
			String masterTokenUrl = keycloakIssuerUri.replaceAll("/realms/.*$", "/realms/master/protocol/openid-connect/token");
			Map<String, String> formData = Map.of(
					"grant_type", "password",
					"client_id", "admin-cli",
					"username", "admin",
					"password", "admin123");

			String formBody = formData.entrySet().stream()
					.map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
							+ URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
					.collect(Collectors.joining("&"));

			HttpRequest adminAuthRequest = HttpRequest.newBuilder()
					.uri(URI.create(masterTokenUrl))
					.timeout(Duration.ofSeconds(3))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(formBody))
					.build();

			HttpResponse<String> adminAuthResp = httpClient.send(adminAuthRequest, HttpResponse.BodyHandlers.ofString());
			if (adminAuthResp.statusCode() != 200) {
				return;
			}

			JsonNode adminNode = objectMapper.readTree(adminAuthResp.body());
			String adminAccessToken = adminNode.path("access_token").asString();
			if (adminAccessToken == null || adminAccessToken.isBlank()) {
				return;
			}

			// Crear o actualizar usuario en el realm medirecord
			String usersUrl = keycloakIssuerUri.replaceAll("/realms/.*$", "/admin/realms/medirecord/users");
			String[] parts = name.trim().split("\\s+", 2);
			String firstName = parts[0];
			String lastName = parts.length > 1 ? parts[1] : parts[0];

			String userJson = """
					{
					  "username": "%s",
					  "email": "%s",
					  "enabled": true,
					  "emailVerified": true,
					  "requiredActions": [],
					  "firstName": "%s",
					  "lastName": "%s",
					  "credentials": [
					    {
					      "type": "password",
					      "value": "%s",
					      "temporary": false
					    }
					  ]
					}
					""".formatted(email, email, firstName, lastName, password);

			HttpRequest createUserRequest = HttpRequest.newBuilder()
					.uri(URI.create(usersUrl))
					.timeout(Duration.ofSeconds(3))
					.header("Authorization", "Bearer " + adminAccessToken)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(userJson))
					.build();

			HttpResponse<String> createResp = httpClient.send(createUserRequest, HttpResponse.BodyHandlers.ofString());
			log.info("Resultado de registro en Keycloak para {}: HTTP {}", email, createResp.statusCode());

			// Si el usuario ya existia en Keycloak (409), actualizar su estado y contrasena
			if (createResp.statusCode() == 409) {
				try {
					String findUserUrl = usersUrl + "?username=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&exact=true";
					HttpRequest findReq = HttpRequest.newBuilder()
							.uri(URI.create(findUserUrl))
							.timeout(Duration.ofSeconds(3))
							.header("Authorization", "Bearer " + adminAccessToken)
							.GET()
							.build();
					HttpResponse<String> findResp = httpClient.send(findReq, HttpResponse.BodyHandlers.ofString());
					if (findResp.statusCode() == 200) {
						JsonNode usersList = objectMapper.readTree(findResp.body());
						if (usersList.isArray() && !usersList.isEmpty()) {
							String userId = usersList.get(0).path("id").asText();
							if (userId != null && !userId.isBlank()) {
								// 1. Asegurar emailVerified y limpiar requiredActions
								String updateJson = """
										{
										  "firstName": "%s",
										  "lastName": "%s",
										  "emailVerified": true,
										  "requiredActions": [],
										  "enabled": true
										}
										""".formatted(firstName, lastName);
								HttpRequest updateReq = HttpRequest.newBuilder()
										.uri(URI.create(usersUrl + "/" + userId))
										.timeout(Duration.ofSeconds(3))
										.header("Authorization", "Bearer " + adminAccessToken)
										.header("Content-Type", "application/json")
										.PUT(HttpRequest.BodyPublishers.ofString(updateJson))
										.build();
								httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());

								// 2. Actualizar contrasena
								String resetPassJson = """
										{
										  "type": "password",
										  "value": "%s",
										  "temporary": false
										}
										""".formatted(password);
								HttpRequest resetPassReq = HttpRequest.newBuilder()
										.uri(URI.create(usersUrl + "/" + userId + "/reset-password"))
										.timeout(Duration.ofSeconds(3))
										.header("Authorization", "Bearer " + adminAccessToken)
										.header("Content-Type", "application/json")
										.PUT(HttpRequest.BodyPublishers.ofString(resetPassJson))
										.build();
								httpClient.send(resetPassReq, HttpResponse.BodyHandlers.ofString());
								log.info("Usuario existente en Keycloak actualizado exitosamente: {}", email);
							}
						}
					}
				} catch (Exception ex) {
					log.warn("No se pudo actualizar usuario existente en Keycloak: {}", ex.getMessage());
				}
			}
		} catch (Exception ex) {
			log.debug("No se pudo registrar usuario en Keycloak (modo autonomo/local): {}", ex.getMessage());
		}
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

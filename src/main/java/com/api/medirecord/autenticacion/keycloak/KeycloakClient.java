package com.api.medirecord.autenticacion.keycloak;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.api.medirecord.autenticacion.exception.CredencialesInvalidasException;
import com.api.medirecord.autenticacion.exception.KeycloakNoDisponibleException;
import com.api.medirecord.autenticacion.exception.RegistroRechazadoException;
import com.api.medirecord.autenticacion.keycloak.dto.KeycloakToken;
import com.api.medirecord.autenticacion.keycloak.dto.NuevoUsuarioKeycloak;
import com.api.medirecord.usuario.exception.UsuarioEmailDuplicadoException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP de Keycloak: la unica puerta de entrada de MediRecord al proveedor
 * de identidad.
 *
 * <p>Tres operaciones:</p>
 * <ul>
 *   <li>{@link #autenticar} / {@link #refrescarToken}: grant de tokens de la sesion,
 *       con las credenciales del usuario (password grant habilitado solo para
 *       integraciones, ver README).</li>
 *   <li>{@link #crearUsuario}: alta en el Admin API con un token de administracion
 *       obtenido por client credentials y cacheado mientras este vigente.</li>
 * </ul>
 *
 * <p>Los errores se traducen al catalogo {@code ErrorCode} de la API: credenciales
 * rechazadas a {@code UNAUTHENTICATED}, duplicados a {@code DUPLICATED_RESOURCE},
 * rechazos del proveedor a {@code VALIDATION_ERROR} y cualquier otra falla a
 * {@code SERVICE_UNAVAILABLE}. Ningun log incluye la contrasena del usuario.</p>
 */
@Component
public class KeycloakClient {

	private static final Logger log = LoggerFactory.getLogger(KeycloakClient.class);

	private static final Duration MARGEN_TOKEN_ADMIN = Duration.ofSeconds(15);

	private final RestClient restClient;
	private final KeycloakProperties properties;
	private final JsonMapper objectMapper;

	private String tokenAdmin;
	private Instant tokenAdminExpiraEn;

	public KeycloakClient(RestClient.Builder restClientBuilder, KeycloakProperties properties,
			JsonMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	/** Intercambia correo y contrasena por los tokens de sesion. */
	public KeycloakToken autenticar(String username, String password) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "password");
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		form.add("username", username);
		form.add("password", password);
		return grantDeUsuario(form);
	}

	/** Renueva la sesion a partir de un refresh token vigente. */
	public KeycloakToken refrescarToken(String refreshToken) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "refresh_token");
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		form.add("refresh_token", refreshToken);
		return grantDeUsuario(form);
	}

	/**
	 * Da de alta un usuario en Keycloak y devuelve su identidad ({@code sub} del token).
	 *
	 * @param email usuario y correo de la cuenta
	 * @throws UsuarioEmailDuplicadoException si la cuenta ya existe en Keycloak
	 * @throws RegistroRechazadoException si Keycloak rechaza los datos (p. ej. politica
	 *         de contrasenas)
	 * @throws KeycloakNoDisponibleException ante caidas o errores no atribuibles al usuario
	 */
	public String crearUsuario(String email, String nombre, String apellidoPaterno,
			String apellidoMaterno, String password) {

		var comando = new NuevoUsuarioKeycloak(
				email,
				email,
				true,
				false,
				nombre,
				apellidoClave(apellidoPaterno, apellidoMaterno),
				List.of(NuevoUsuarioKeycloak.Credencial.contrasena(password)));

		try {
			ResponseEntity<Void> respuesta = restClient.post()
					.uri(properties.usuariosUri())
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeAdministrador())
					.contentType(MediaType.APPLICATION_JSON)
					.body(comando)
					.retrieve()
					.toBodilessEntity();
			return identidadDesdeLocation(respuesta.getHeaders().getLocation());
		} catch (RestClientResponseException e) {
			if (e.getStatusCode() == HttpStatus.CONFLICT) {
				throw new UsuarioEmailDuplicadoException(email);
			}
			if (e.getStatusCode().is4xxClientError()) {
				throw new RegistroRechazadoException(descripcionDeError(e));
			}
			log.error("Keycloak fallo al crear el usuario ({}): {}", e.getStatusCode().value(),
					e.getResponseBodyAsString());
			throw new KeycloakNoDisponibleException(e);
		} catch (RestClientException e) {
			log.error("Keycloak no respondio al crear el usuario", e);
			throw new KeycloakNoDisponibleException(e);
		}
	}

	private KeycloakToken grantDeUsuario(MultiValueMap<String, String> form) {
		try {
			return solicitarToken(properties.tokenUri(), form);
		} catch (RestClientResponseException e) {
			String error = campoDeError(e, "error");
			if (esCredencialRechazada(e, error)) {
				log.debug("Keycloak rechazo el grant de token ({}): {}", e.getStatusCode().value(), error);
				throw new CredencialesInvalidasException();
			}
			log.error("Keycloak fallo al procesar el grant de token ({}): {}", e.getStatusCode().value(),
					e.getResponseBodyAsString());
			throw new KeycloakNoDisponibleException(e);
		} catch (RestClientException e) {
			log.error("Keycloak no respondio al procesar el grant de token", e);
			throw new KeycloakNoDisponibleException(e);
		}
	}

	private KeycloakToken solicitarToken(String uri, MultiValueMap<String, String> form) {
		return restClient.post()
				.uri(uri)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(KeycloakToken.class);
	}

	/**
	 * Token de administracion del realm {@code admin-realm}, cacheado hasta casi su
	 * expiracion. Solo se usa para dar de alta usuarios.
	 */
	private String tokenDeAdministrador() {
		synchronized (this) {
			if (tokenAdmin != null && tokenAdminExpiraEn != null
					&& tokenAdminExpiraEn.isAfter(Instant.now().plus(MARGEN_TOKEN_ADMIN))) {
				return tokenAdmin;
			}

			var form = new LinkedMultiValueMap<String, String>();
			form.add("grant_type", "password");
			form.add("client_id", properties.adminClientId());
			form.add("username", properties.adminUsername());
			form.add("password", properties.adminPassword());

			try {
				KeycloakToken token = solicitarToken(properties.adminTokenUri(), form);
				tokenAdmin = token.accessToken();
				tokenAdminExpiraEn = Instant.now()
						.plusSeconds(Math.max(token.expiresIn() - MARGEN_TOKEN_ADMIN.toSeconds(), 5));
				return tokenAdmin;
			} catch (RestClientResponseException e) {
				log.error("Keycloak rechazo las credenciales de administracion ({}): {}",
						e.getStatusCode().value(), e.getResponseBodyAsString());
				throw new KeycloakNoDisponibleException(e);
			} catch (RestClientException e) {
				log.error("No se pudo obtener el token de administracion de Keycloak", e);
				throw new KeycloakNoDisponibleException(e);
			}
		}
	}

	private static boolean esCredencialRechazada(RestClientResponseException e, String error) {
		if (error != null) {
			return "invalid_grant".equals(error);
		}
		return e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401;
	}

	private String descripcionDeError(RestClientResponseException e) {
		String descripcion = campoDeError(e, "error_description");
		if (descripcion != null && !descripcion.isBlank()) {
			return descripcion;
		}
		String error = campoDeError(e, "error");
		return error != null && !error.isBlank() ? error : "datos no validos";
	}

	private String campoDeError(RestClientResponseException e, String nombreCampo) {
		String cuerpo = e.getResponseBodyAsString();
		if (cuerpo.isBlank()) {
			return null;
		}
		try {
			if (objectMapper.readValue(cuerpo, Map.class) instanceof Map<?, ?> mapa
					&& mapa.get(nombreCampo) != null) {
				return String.valueOf(mapa.get(nombreCampo));
			}
		} catch (RuntimeException ex) {
			log.debug("No se pudo interpretar el cuerpo del error de Keycloak", ex);
		}
		return null;
	}

	private static String identidadDesdeLocation(URI location) {
		String ruta = location == null ? null : location.getPath();
		if (ruta == null || ruta.isBlank()) {
			throw new IllegalStateException("Keycloak no devolvio la ubicacion del usuario creado");
		}
		String[] segmentos = ruta.split("/");
		String identidad = segmentos[segmentos.length - 1];
		if (identidad.isBlank()) {
			throw new IllegalStateException("La ubicacion devuelta por Keycloak no contiene el usuario");
		}
		return identidad;
	}

	private static String apellidoClave(String apellidoPaterno, String apellidoMaterno) {
		return apellidoMaterno == null || apellidoMaterno.isBlank()
				? apellidoPaterno
				: apellidoPaterno + " " + apellidoMaterno;
	}

}

package com.api.medirecord.autenticacion.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.api.medirecord.autenticacion.exception.CredencialesInvalidasException;
import com.api.medirecord.autenticacion.exception.KeycloakNoDisponibleException;
import com.api.medirecord.autenticacion.exception.RegistroRechazadoException;
import com.api.medirecord.usuario.exception.UsuarioEmailDuplicadoException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Test del cliente de Keycloak con el servidor HTTP simulado de Spring: valida que los
 * status del proveedor se traduzcan al catalogo de errores de la API y que la contrasena
 * viaje solo en el cuerpo del alta, nunca en cabeceras ni en la URL.
 */
class KeycloakClientTest {

	private static final String BASE = "http://keycloak.test:8081";
	private static final String TOKEN_URI = BASE + "/realms/medirecord/protocol/openid-connect/token";
	private static final String ADMIN_TOKEN_URI = BASE + "/realms/master/protocol/openid-connect/token";
	private static final String USERS_URI = BASE + "/admin/realms/medirecord/users";

	private MockRestServiceServer server;
	private KeycloakClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		var properties = new KeycloakProperties(BASE, "medirecord", "medirecord-api", "medirecord-secret",
				"master", "admin-cli", "admin", "admin123");
		client = new KeycloakClient(builder, properties, JsonMapper.builder().build());
	}

	@Test
	@DisplayName("El password grant devuelve los tokens emitidos por Keycloak")
	void autenticar() {
		server.expect(requestTo(TOKEN_URI))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=password")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("username=ana%40correo.com")))
				.andRespond(withSuccess(tokenJson("access-1", "refresh-1"), MediaType.APPLICATION_JSON));

		var token = client.autenticar("ana@correo.com", "Secreto123");

		assertThat(token.accessToken()).isEqualTo("access-1");
		assertThat(token.refreshToken()).isEqualTo("refresh-1");
		assertThat(token.expiresIn()).isEqualTo(1800);
		server.verify();
	}

	@Test
	@DisplayName("invalid_grant se traduce a UNAUTHENTICATED")
	void autenticarConCredencialesInvalidas() {
		server.expect(requestTo(TOKEN_URI))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}"));

		assertThatThrownBy(() -> client.autenticar("ana@correo.com", "mala"))
				.isInstanceOf(CredencialesInvalidasException.class)
				.hasMessage("Correo o contrasena invalidos");
	}

	@Test
	@DisplayName("El refresh grant renueva los tokens")
	void refrescarToken() {
		server.expect(requestTo(TOKEN_URI))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
				.andRespond(withSuccess(tokenJson("access-2", "refresh-2"), MediaType.APPLICATION_JSON));

		var token = client.refrescarToken("refresh-1");

		assertThat(token.accessToken()).isEqualTo("access-2");
		assertThat(token.refreshToken()).isEqualTo("refresh-2");
		server.verify();
	}

	@Test
	@DisplayName("El alta en Keycloak usa el token de administracion y devuelve la identidad creada")
	void crearUsuario() {
		server.expect(requestTo(ADMIN_TOKEN_URI))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=password")))
				.andRespond(withSuccess(tokenJson("admin-token", null), MediaType.APPLICATION_JSON));
		server.expect(requestTo(USERS_URI))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ana@correo.com")))
				.andRespond(withCreatedEntity(URI.create(USERS_URI + "/sub-keycloak-123")));

		var identidad = client.crearUsuario("ana@correo.com", "Ana", "Gomez", "Lopez", "Secreto123");

		assertThat(identidad).isEqualTo("sub-keycloak-123");
		server.verify();
	}

	@Test
	@DisplayName("Usuario ya existente en Keycloak se traduce a DUPLICATED_RESOURCE")
	void crearUsuarioDuplicado() {
		server.expect(requestTo(ADMIN_TOKEN_URI))
				.andRespond(withSuccess(tokenJson("admin-token", null), MediaType.APPLICATION_JSON));
		server.expect(requestTo(USERS_URI))
				.andRespond(withStatus(HttpStatus.CONFLICT)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\":\"user_exists\"}"));

		assertThatThrownBy(() -> client.crearUsuario("ana@correo.com", "Ana", "Gomez", null, "Secreto123"))
				.isInstanceOf(UsuarioEmailDuplicadoException.class);
	}

	@Test
	@DisplayName("Politica de contrasenas rechazada se traduce a VALIDATION_ERROR")
	void crearUsuarioRechazado() {
		server.expect(requestTo(ADMIN_TOKEN_URI))
				.andRespond(withSuccess(tokenJson("admin-token", null), MediaType.APPLICATION_JSON));
		server.expect(requestTo(USERS_URI))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\":\"invalid_password\",\"error_description\":\"Password policy\"}"));

		assertThatThrownBy(() -> client.crearUsuario("ana@correo.com", "Ana", "Gomez", null, "corta"))
				.isInstanceOf(RegistroRechazadoException.class)
				.hasMessageContaining("Password policy");
	}

	@Test
	@DisplayName("Caida de Keycloak se traduce a SERVICE_UNAVAILABLE")
	void keycloakCaido() {
		server.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

		assertThatThrownBy(() -> client.autenticar("ana@correo.com", "Secreto123"))
				.isInstanceOf(KeycloakNoDisponibleException.class);
	}

	@Test
	@DisplayName("Credenciales de administracion invalidas no se confunden con credenciales de usuario")
	void tokenDeAdministracionInvalido() {
		server.expect(requestTo(ADMIN_TOKEN_URI))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\":\"invalid_grant\"}"));

		assertThatThrownBy(() -> client.crearUsuario("ana@correo.com", "Ana", "Gomez", null, "Secreto123"))
				.isInstanceOf(KeycloakNoDisponibleException.class);
	}

	private static String tokenJson(String accessToken, String refreshToken) {
		String refresh = refreshToken == null ? "" : "\"refresh_token\":\"" + refreshToken + "\",";
		return "{\"access_token\":\"" + accessToken + "\"," + refresh
				+ "\"token_type\":\"Bearer\",\"expires_in\":1800,\"refresh_expires_in\":1800,"
				+ "\"scope\":\"openid\"}";
	}

}

package com.api.medirecord.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.medirecord.autenticacion.dto.LoginRequest;
import com.api.medirecord.autenticacion.dto.RefreshRequest;
import com.api.medirecord.autenticacion.dto.RegistroRequest;
import com.api.medirecord.autenticacion.exception.CuentaDesactivadaException;
import com.api.medirecord.autenticacion.exception.CuentaNoRegistradaException;
import com.api.medirecord.autenticacion.keycloak.KeycloakClient;
import com.api.medirecord.autenticacion.keycloak.dto.KeycloakToken;
import com.api.medirecord.usuario.UsuarioService;
import com.api.medirecord.usuario.dto.UsuarioRequest;
import com.api.medirecord.usuario.dto.UsuarioResponse;

/**
 * Test unitario del servicio de autenticacion: sin Spring ni base de datos (los
 * colaboradores se mockean). Los access tokens son JWT firmados de ejemplo cuyo claim
 * {@code sub} enlaza con la cuenta local.
 */
@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

	@Mock
	private UsuarioService usuarioService;

	@Mock
	private KeycloakClient keycloakClient;

	@InjectMocks
	private AutenticacionService service;

	private static final UUID ID = UUID.fromString("3f7c8b2e-1d4a-4c9e-9f1b-7a2c3d4e5f60");

	private static final String ACCESS_SUB_ABC = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wva2V5Y2xvYWsudGVzdFwvcmVhbG1zXC9tZWRpcmVjb3JkIiwic3ViIjoic3ViLWFiYyIsImV4cCI6MTc5MjMwNDQ3MywiaWF0IjoxNzkyMzAwODczfQ.pdR_GF2uudzJz0r0S__o4toTKoyf5xzCdmeO4Cw5mG1FkMCs0UDlJWAC1b2CLPRqOXQiXYH9sgjRnl0OtWxcgLeq0kxoM8FwvKU30wtv8JZeTtq9RvUI2NW-_rLzVTGRGSGix-F3pb48XqxCjAYspItXKBydBa7Tgn1LS2B6XXOOe8Jfp-opwBXVXa4P-74ZEvHRuLecRv3FD3cZQvgbwL_lyW2szHvvW_b7swKrBBd3vkQeaxoYfFe6Tc5OnXg1vIHBBdEJGfww1oLtW8FGI7fbigxfnvpHpI4QwYDClUbLQFvDWrD_49cSld_ta3eX_b_zjJOSwisiRxATWOpKVA";
	private static final String ACCESS_SUB_NUEVO = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wva2V5Y2xvYWsudGVzdFwvcmVhbG1zXC9tZWRpcmVjb3JkIiwic3ViIjoic3ViLW51ZXZvIiwiZXhwIjoxNzkyMzA0NDczLCJpYXQiOjE3OTIzMDA4NzN9.HIxZnBTBKcKhh3LzXUR2ERQ-pM3-thqeZmq6AJ1NDqrdh3bFP-qZyEVXyhplsbhhIAtSBq0cgbktASrhIbGx03Qf6GwGvLCz3RqLWm6dvk6_6rKBDNKpUwzv5CsTw38MNKJwEErj82G7O-StiGrD78w4YDWXK3K0bXuidsnowbDn3cj-BsEUJxW67WQeTWa4JP-K4b_BsMm5dKlIsrDv_72EXnTAG08lahldQP5AF529dJS9_LeL-tD_y5DkxP5rff1lYB2j2USbSE4fRPbkVLxxPtxC3dCjf5tb1hiq6eFnafuNB7QlWzTXH_JPdKw0kn1gohg3m3jsWsWGUFAf9Q";
	private static final String ACCESS_SUB_INACTIVO = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wva2V5Y2xvYWsudGVzdFwvcmVhbG1zXC9tZWRpcmVjb3JkIiwic3ViIjoic3ViLWluYWN0aXZvIiwiZXhwIjoxNzkyMzA0NDczLCJpYXQiOjE3OTIzMDA4NzN9.av4s7vDa_Cfs0PQk3UqbMHLPkmloU8VEXmDsXONVRcuCHdXfrXrTLHj_BUGvJbdC6vYX5h5uJeYD4Vm1hZqmpysoFepO_Xt1HiBGFfuaLHnb1fy818SvK3r5u0rIRL6kgoHJdIT3SCfYBIH3C-urnvrU0TG1vXGTsaSQYEGXGnd_K6n87p2GFbXhkZcMIQPuXLtsnD4AnUtha2usQlknlVESgbSvQIjDYil1pBGlEHQTQ2DEBYvJ22jcnIrspa-AtmNIEc2WTdBTtEpSpBCV2tyR1aQzHnkqZ8qzwDXPMnJI2VS35HGv1RXDgyc8OFbideNIgJEyZPlLdeyu1SocGA";
	private static final String ACCESS_SUB_RENOVAR = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wva2V5Y2xvYWsudGVzdFwvcmVhbG1zXC9tZWRpcmVjb3JkIiwic3ViIjoic3ViLXJlbm92YXIiLCJleHAiOjE3OTIzMDQ0NzQsImlhdCI6MTc5MjMwMDg3NH0.b5RFeAOhPsG_2OlotZVIfQqMEKeO0p0DjzV1lOZrtYsVNd_t8HsLLA1kWU3VnKB4zgl2I8279IkQk82pHZOn8UW9x43ESIgeSCk0sxvudHZdcov2L4q2wSDWgQ7Z8K8tCL0zstjWUM8ZqnhPhJuKdg0dJ6AXAsVhrE4ntcr2nM6iO5-hEKF9YFRIZ8-L2iacOTGdMnAjo66ohG9gU5vcRIFr4TMqZbzCbR9H92pdJhO0jivQjeW0JNXL9O6jNQOHhJoLUiDDS0eySH6IVzcY47vNjj6BH2BlekOSbqt9PyjaGTPekJvSBivU0IgkkqToKjARSsqaMGd5jNFu70IzRw";

	private static UsuarioResponse usuario(String keycloakId, boolean activo) {
		return new UsuarioResponse(ID, keycloakId, "Ana", "Gomez", "Lopez", "Ana Gomez Lopez",
				"ana.gomez@correo.com", "+52 664 123 4567", activo, null, null);
	}

	private static KeycloakToken token(String accessToken) {
		return new KeycloakToken(accessToken, "Bearer", 1800, 1800, "refresh-" + accessToken.hashCode());
	}

	@Nested
	@DisplayName("registrar")
	class Registrar {

		@Test
		@DisplayName("crea la proyeccion local, da de alta en Keycloak y vincula la identidad")
		void creaCuentaYVinculaIdentidad() {
			var request = new RegistroRequest("Ana", "Gomez", "Lopez", "ana.gomez@correo.com",
					"+52 664 123 4567", "Secreto123");
			when(usuarioService.crear(any(UsuarioRequest.class))).thenReturn(usuario(null, true));
			when(keycloakClient.crearUsuario(eq("ana.gomez@correo.com"), eq("Ana"), eq("Gomez"),
					eq("Lopez"), eq("Secreto123"))).thenReturn("sub-keycloak");
			when(usuarioService.vincularIdentidad(eq(ID), eq("sub-keycloak")))
					.thenReturn(usuario("sub-keycloak", true));

			var response = service.registrar(request);

			assertThat(response.keycloakId()).isEqualTo("sub-keycloak");
			assertThat(response.activo()).isTrue();
		}

		@Test
		@DisplayName("si Keycloak rechaza el alta, la excepcion se propaga y la cuenta local se revierte")
		void propagarFalloDeKeycloak() {
			var request = new RegistroRequest("Ana", "Gomez", "Lopez", "ana.gomez@correo.com",
					"+52 664 123 4567", "Secreto123");
			when(usuarioService.crear(any(UsuarioRequest.class))).thenReturn(usuario(null, true));
			when(keycloakClient.crearUsuario(any(), any(), any(), any(), any()))
					.thenThrow(new com.api.medirecord.autenticacion.exception.KeycloakNoDisponibleException(
							new RuntimeException("caida")));

			assertThatThrownBy(() -> service.registrar(request))
					.isInstanceOf(com.api.medirecord.autenticacion.exception.KeycloakNoDisponibleException.class);
		}
	}

	@Nested
	@DisplayName("iniciarSesion")
	class IniciarSesion {

		@Test
		@DisplayName("credenciales validas y cuenta activa devuelven tokens y usuario")
		void sesionIniciada() {
			when(keycloakClient.autenticar("ana.gomez@correo.com", "Secreto123"))
					.thenReturn(token(ACCESS_SUB_ABC));
			when(usuarioService.buscarPorKeycloakId("sub-abc"))
					.thenReturn(Optional.of(usuario("sub-abc", true)));

			var response = service.iniciarSesion(
					new LoginRequest("ana.gomez@correo.com", "Secreto123"));

			assertThat(response.tokens().accessToken()).isEqualTo(ACCESS_SUB_ABC);
			assertThat(response.tokens().tokenType()).isEqualTo("Bearer");
			assertThat(response.usuario().keycloakId()).isEqualTo("sub-abc");
		}

		@Test
		@DisplayName("credenciales rechazadas por Keycloak se propagan sin consultar la cuenta")
		void credencialesRechazadas() {
			when(keycloakClient.autenticar("ana.gomez@correo.com", "mal"))
					.thenThrow(new com.api.medirecord.autenticacion.exception.CredencialesInvalidasException());

			assertThatThrownBy(() -> service.iniciarSesion(
					new LoginRequest("ana.gomez@correo.com", "mal")))
					.isInstanceOf(com.api.medirecord.autenticacion.exception.CredencialesInvalidasException.class);
		}

		@Test
		@DisplayName("identidad sin cuenta local se rechaza")
		void cuentaNoVinculada() {
			when(keycloakClient.autenticar("ana.gomez@correo.com", "Secreto123"))
					.thenReturn(token(ACCESS_SUB_NUEVO));
			when(usuarioService.buscarPorKeycloakId("sub-nuevo")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.iniciarSesion(
					new LoginRequest("ana.gomez@correo.com", "Secreto123")))
					.isInstanceOf(CuentaNoRegistradaException.class);
		}

		@Test
		@DisplayName("cuenta desactivada se rechaza pese a credenciales validas")
		void cuentaDesactivada() {
			when(keycloakClient.autenticar("ana.gomez@correo.com", "Secreto123"))
					.thenReturn(token(ACCESS_SUB_INACTIVO));
			when(usuarioService.buscarPorKeycloakId("sub-inactivo"))
					.thenReturn(Optional.of(usuario("sub-inactivo", false)));

			assertThatThrownBy(() -> service.iniciarSesion(
					new LoginRequest("ana.gomez@correo.com", "Secreto123")))
					.isInstanceOf(CuentaDesactivadaException.class);
		}
	}

	@Nested
	@DisplayName("renovarSesion")
	class RenovarSesion {

		@Test
		@DisplayName("refresh token valido devuelve tokens nuevos")
		void renuevaTokens() {
			when(keycloakClient.refrescarToken("refresh-viejo")).thenReturn(token(ACCESS_SUB_RENOVAR));
			when(usuarioService.buscarPorKeycloakId("sub-renovar"))
					.thenReturn(Optional.of(usuario("sub-renovar", true)));

			var response = service.renovarSesion(new RefreshRequest("refresh-viejo"));

			assertThat(response.accessToken()).isEqualTo(ACCESS_SUB_RENOVAR);
			assertThat(response.refreshToken()).isNotBlank();
		}

		@Test
		@DisplayName("refresh token rechazado por Keycloak se propaga como 401")
		void refreshRechazado() {
			when(keycloakClient.refrescarToken("refresh-malo"))
					.thenThrow(new com.api.medirecord.autenticacion.exception.CredencialesInvalidasException());

			assertThatThrownBy(() -> service.renovarSesion(new RefreshRequest("refresh-malo")))
					.isInstanceOf(com.api.medirecord.autenticacion.exception.CredencialesInvalidasException.class);
		}
	}

}

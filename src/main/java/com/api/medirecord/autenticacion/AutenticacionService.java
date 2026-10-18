package com.api.medirecord.autenticacion;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.medirecord.autenticacion.dto.LoginRequest;
import com.api.medirecord.autenticacion.dto.LoginResponse;
import com.api.medirecord.autenticacion.dto.RefreshRequest;
import com.api.medirecord.autenticacion.dto.RegistroRequest;
import com.api.medirecord.autenticacion.dto.TokenResponse;
import com.api.medirecord.autenticacion.exception.CuentaDesactivadaException;
import com.api.medirecord.autenticacion.exception.CuentaNoRegistradaException;
import com.api.medirecord.autenticacion.keycloak.KeycloakClient;
import com.api.medirecord.autenticacion.keycloak.dto.KeycloakToken;
import com.api.medirecord.usuario.UsuarioService;
import com.api.medirecord.usuario.dto.UsuarioRequest;
import com.api.medirecord.usuario.dto.UsuarioResponse;

import com.nimbusds.jwt.SignedJWT;

/**
 * Registro, inicio de sesion y renovacion de sesion.
 *
 * <p>MediRecord delega las credenciales en Keycloak: la contrasena viaja al proveedor
 * (que la almacena hasheada) y <b>nunca</b> se persiste en la base de la aplicacion.
 * La API orquesta el alta de la cuenta local, enlaza la identidad externa
 * ({@code sub} del token) y devuelve los tokens que emite Keycloak.</p>
 *
 * <p>Orden del registro: primero la proyeccion local y despues Keycloak, todo dentro
 * de una misma transaccion; si el proveedor rechaza el alta, la cuenta local se
 * revierte y no queda un usuario a medias. La ventana restante (Keycloak confirmado y
 * fallo posterior del commit) es residual y se asume sin compensacion.</p>
 *
 * <p>En el inicio de sesion las credenciales se verifican <b>antes</b> de consultar la
 * cuenta local, de modo que solo quien conoce la contrasena puede distinguir una cuenta
 * desactivada o no vinculada: no hay enumeracion de cuentas.</p>
 */
@Service
@Transactional(readOnly = true)
public class AutenticacionService {

	private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);

	private final UsuarioService usuarioService;
	private final KeycloakClient keycloakClient;

	public AutenticacionService(UsuarioService usuarioService, KeycloakClient keycloakClient) {
		this.usuarioService = usuarioService;
		this.keycloakClient = keycloakClient;
	}

	/**
	 * Crea la cuenta en Keycloak y su proyeccion local, enlazadas por la identidad
	 * externa.
	 *
	 * @throws com.api.medirecord.usuario.exception.UsuarioEmailDuplicadoException si el
	 *         correo ya esta registrado
	 */
	@Transactional
	public UsuarioResponse registrar(RegistroRequest request) {
		UsuarioResponse usuario = usuarioService.crear(new UsuarioRequest(
				request.nombre(),
				request.apellidoPaterno(),
				request.apellidoMaterno(),
				request.email(),
				request.telefono()));

		String identidad = keycloakClient.crearUsuario(
				usuario.email(),
				usuario.nombre(),
				usuario.apellidoPaterno(),
				usuario.apellidoMaterno(),
				request.password());

		return usuarioService.vincularIdentidad(usuario.id(), identidad);
	}

	/** Verifica las credenciales con Keycloak y devuelve los tokens de la sesion. */
	public LoginResponse iniciarSesion(LoginRequest request) {
		String email = request.email() == null ? null : request.email().trim().toLowerCase();
		KeycloakToken token = keycloakClient.autenticar(email, request.password());
		return new LoginResponse(TokenResponse.from(token), cuentaAsociada(token.accessToken()));
	}

	/** Emite nuevos tokens si el refresh token es valido y la cuenta sigue habilitada. */
	public TokenResponse renovarSesion(RefreshRequest request) {
		KeycloakToken token = keycloakClient.refrescarToken(request.refreshToken());
		cuentaAsociada(token.accessToken());
		return TokenResponse.from(token);
	}

	/** Resuelve y valida la cuenta local a partir del {@code sub} del access token. */
	private UsuarioResponse cuentaAsociada(String accessToken) {
		UsuarioResponse usuario = usuarioService.buscarPorKeycloakId(subDelToken(accessToken))
				.orElseThrow(CuentaNoRegistradaException::new);

		if (!usuario.activo()) {
			log.info("Inicio de sesion rechazado: la cuenta {} esta desactivada", usuario.id());
			throw new CuentaDesactivadaException();
		}
		return usuario;
	}

	private static String subDelToken(String accessToken) {
		try {
			String sub = SignedJWT.parse(accessToken).getJWTClaimsSet().getSubject();
			if (sub == null || sub.isBlank()) {
				throw new IllegalStateException("El token de acceso no contiene el claim sub");
			}
			return sub;
		} catch (ParseException e) {
			throw new IllegalStateException("El token de acceso emitido por Keycloak no es un JWT valido", e);
		}
	}

}

package com.api.medirecord.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.api.medirecord.autenticacion.AutenticacionService;
import com.api.medirecord.autenticacion.dto.RegistroRequest;
import com.api.medirecord.usuario.UsuarioRepository;

/**
 * Datos de ejemplo para desarrollo local.
 *
 * <p>Solo se activa con el perfil {@code dev}. El usuario de desarrollo se crea con el
 * flujo real de registro (Keycloak + proyeccion local), de modo que sirve tambien para
 * probar {@code POST /api/v1/auth/login}. Si Keycloak no esta accesible, la siembra se
 * omite con un aviso y se reintenta en el proximo arranque. En produccion y en tests
 * no se siembran datos de prueba.</p>
 */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DataInitializer {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private final AutenticacionService autenticacionService;
	private final UsuarioRepository usuarioRepository;
	private final String passwordDeDesarrollo;

	public DataInitializer(
			AutenticacionService autenticacionService,
			UsuarioRepository usuarioRepository,
			@Value("${medirecord.dev.password:Medirecord123}") String passwordDeDesarrollo) {
		this.autenticacionService = autenticacionService;
		this.usuarioRepository = usuarioRepository;
		this.passwordDeDesarrollo = passwordDeDesarrollo;
	}

	@Bean
	public CommandLineRunner inicializarUsuariosDeDesarrollo() {
		return args -> {
			if (usuarioRepository.count() > 0) {
				log.info("La tabla usuario ya contiene registros, omitiendo inicializacion de desarrollo.");
				return;
			}

			try {
				autenticacionService.registrar(new RegistroRequest(
						"Usuario", "Recordatorio", null,
						"usuario@medirecord.com", "4181234567",
						passwordDeDesarrollo));
				log.info("Usuario de desarrollo creado: usuario@medirecord.com.");
			} catch (RuntimeException e) {
				log.warn("No se pudo crear el usuario de desarrollo (¿esta Keycloak accesible?): {}",
						e.getMessage());
			}
		};
	}

}

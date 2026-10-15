package com.api.medirecord.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.api.medirecord.usuario.Usuario;
import com.api.medirecord.usuario.UsuarioRepository;

/**
 * Datos de ejemplo para desarrollo local.
 *
 * <p>Solo se activa con el perfil {@code dev}; en produccion y en tests los
 * usuarios se crean por el flujo real (registro o primer inicio de sesion en
 * Keycloak). Nunca se siembran datos de prueba en migraciones versionadas.</p>
 */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DataInitializer {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	@Bean
	public CommandLineRunner inicializarUsuariosDeDesarrollo(UsuarioRepository usuarioRepository) {
		return args -> {
			if (usuarioRepository.count() > 0) {
				log.info("La tabla usuario ya contiene registros, omitiendo inicializacion de desarrollo.");
				return;
			}

			usuarioRepository.save(new Usuario(
					"Usuario", "Recordatorio", null,
					"usuario@medirecord.com", "4181234567"));

			log.info("Usuario de desarrollo creado: usuario@medirecord.com.");
		};
	}

}

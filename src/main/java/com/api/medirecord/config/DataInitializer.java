package com.api.medirecord.config;

import com.api.medirecord.model.Usuario;
import com.api.medirecord.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(UsuarioRepository usuarioRepository) {
        return args -> {
            if (usuarioRepository.count() == 0) {
                log.info("Inicializando datos de prueba en la tabla usuarios...");

                Usuario admin = new Usuario(
                        null,
                        null,
                        "Administrador MediRecord",
                        "admin@medirecord.com",
                        "4180000000",
                        "ADMIN",
                        true,
                        LocalDateTime.now()
                );

                Usuario usuario = new Usuario(
                        null,
                        null,
                        "Usuario Recordatorio",
                        "usuario@medirecord.com",
                        "4181234567",
                        "USER",
                        true,
                        LocalDateTime.now()
                );

                usuarioRepository.save(admin);
                usuarioRepository.save(usuario);

                log.info("Usuarios iniciales creados exitosamente: ADMIN y USER.");
            } else {
                log.info("La tabla usuarios ya contiene registros, omitiendo inicializacion.");
            }
        };
    }
}

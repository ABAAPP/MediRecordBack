package com.api.medirecord.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Base para tests de integracion del contexto completo de Spring con PostgreSQL real.
 *
 * <p>Requiere Docker: sin Docker los tests se omiten automaticamente
 * ({@code disabledWithoutDocker = true}). La conexion la inyecta
 * {@link ServiceConnection}, por eso el perfil "test" no define datasource.</p>
 *
 * <p>Ejecucion: {@code ./mvnw verify -Pintegration}</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = PostgresTestContainerFactory.nuevoContenedor();

}
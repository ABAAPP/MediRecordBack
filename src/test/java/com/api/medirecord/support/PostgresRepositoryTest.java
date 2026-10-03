package com.api.medirecord.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import com.api.medirecord.config.JpaAuditingConfig;

/**
 * Base para tests de repositorio: solo la capa JPA contra PostgreSQL real.
 *
 * <p>Se importa {@link JpaAuditingConfig} porque las entidades heredan de
 * {@code AuditableEntity} y sus columnas de auditoria son obligatorias.</p>
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresRepositoryTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = PostgresTestContainerFactory.nuevoContenedor();

}
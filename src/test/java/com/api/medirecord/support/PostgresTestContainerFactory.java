package com.api.medirecord.support;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Fabrica del contenedor PostgreSQL usado por los tests de integracion.
 *
 * <p>Se usa la misma version de imagen que en produccion para evitar diferencias de
 * comportamiento entre entornos.</p>
 */
public final class PostgresTestContainerFactory {

	public static final String IMAGE = "postgres:17-alpine";
	public static final String DATABASE = "medirecord_test";
	public static final String USERNAME = "medirecord";
	public static final String PASSWORD = "medirecord";

	private PostgresTestContainerFactory() {
	}

	public static PostgreSQLContainer nuevoContenedor() {
		return new PostgreSQLContainer(DockerImageName.parse(IMAGE))
				.withDatabaseName(DATABASE)
				.withUsername(USERNAME)
				.withPassword(PASSWORD);
	}

}
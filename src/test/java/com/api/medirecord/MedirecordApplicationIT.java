package com.api.medirecord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.api.medirecord.support.PostgresIntegrationTest;

/**
 * Verifica que el contexto de Spring arranca completo: base de datos, Flyway,
 * repositorios, seguridad y OpenAPI.
 */
class MedirecordApplicationIT extends PostgresIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("El contexto de la aplicacion carga correctamente")
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
		assertThat(applicationContext.getEnvironment().getActiveProfiles()).contains("test");
	}

}
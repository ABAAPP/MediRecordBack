package com.api.medirecord.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.api.medirecord.common.security.SecurityUtils;

/**
 * Habilita la auditoria JPA. El usuario auditor proviene del JWT de Keycloak y cae a
 * {@code system} en tareas programadas o procesos internos.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

	@Bean
	public AuditorAware<String> auditorAware() {
		return () -> Optional.of(SecurityUtils.currentUsernameOrSystem());
	}

}
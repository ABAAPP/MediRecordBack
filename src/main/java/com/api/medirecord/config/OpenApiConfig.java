package com.api.medirecord.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/** Metadatos de la API y esquema de autenticacion (JWT de Keycloak) en Swagger UI. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	public static final String SECURITY_SCHEME = "bearerAuth";

	@Bean
	public OpenAPI medirecordOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Medirecord API")
						.version("v1")
						.description("""
								API del expediente clinico digital Medirecord.

								### Autenticacion
								1. Obtener un token en Keycloak (password grant, solo para integraciones):
								`POST /realms/medirecord/protocol/openid-connect/token`
								2. Enviarlo como `Authorization: Bearer <token>`.
								3. En Swagger UI, usar el boton **Authorize** con el token(access token).
								""")
						.license(new License().name("Uso interno")))
				.servers(List.of(new Server().url("/").description("Servidor actual")))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("Access token JWT emitido por Keycloak (realm medirecord)")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
	}

}
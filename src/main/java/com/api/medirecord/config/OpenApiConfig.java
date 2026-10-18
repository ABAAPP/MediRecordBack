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
								1. `POST /api/v1/auth/registro` crea la cuenta (datos de perfil y contrasena).
								2. `POST /api/v1/auth/login` con `email` y `password` devuelve los tokens de la sesion.
								3. Enviar `Authorization: Bearer <accessToken>` en cualquier peticion protegida.
								4. `POST /api/v1/auth/refresh` renueva la sesion con el `refreshToken`.

								Las contraseñas se almacenan hasheadas en Keycloak; la API nunca las guarda.
								""")
						.license(new License().name("Uso interno")))
				.servers(List.of(new Server().url("/").description("Servidor actual")))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("Access token JWT emitido por Keycloak y devuelto por `/api/v1/auth/login`")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
	}

}
# Medirecord API

API del expediente clínico digital **Medirecord**. Spring Boot 4 + Java 21, PostgreSQL con Flyway,
JWT administrado por **Keycloak** y OpenAPI/Swagger.

---

## 1. Stack

| Capa | Tecnología |
|------|-----------|
| Runtime | Java 21 (virtual threads habilitadas) |
| Framework | Spring Boot 4.1 (WebMVC, Data JPA, Validation, Actuator) |
| Persistencia | PostgreSQL 17 + Hibernate 6/7 + HikariCP |
| Esquema | Flyway (migraciones versionadas, `ddl-auto=validate`) |
| Seguridad | Spring Security como OAuth2 Resource Server + Keycloak 26 (realm `medirecord`) |
| Documentación | springdoc-openapi 3.1 (Swagger UI) |
| Observabilidad | Actuator + Micrometer/Prometheus |
| Tests | JUnit 6, Mockito, Spring Test (`@WebMvcTest`, `@DataJpaTest`), Testcontainers 2 |
| Build | Maven Wrapper (3.9+), JaCoCo, Maven Enforcer |

---

## 2. Arranque rápido

Requisitos: **JDK 21+** y **Docker**.

```bash
cp .env.example .env

# 1) Infraestructura (PostgreSQL en 5432 y Keycloak en 8081)
make up

# 2) API desde el IDE (perfil dev, hot-reload)
make run

# 3) Registro y login (la API delega en Keycloak y devuelve el JWT)
curl -s -X POST http://localhost:8080/api/v1/auth/registro \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Ana","apellidoPaterno":"Gomez","apellidoMaterno":"Lopez",
       "email":"ana@correo.com","telefono":"+52 664 123 4567","password":"Secreto123"}'
make login        # access token del usuario de desarrollo

# 4) Llamada autenticada
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/pacientes
```

Documentación interactiva: http://localhost:8080/swagger-ui.html

### Usuarios de prueba (Keycloak)

| Usuario | Password | Rol |
|---------|----------|-----|
| `admin` | `admin123` | ADMIN, MEDICO, RECEPCION |
| `medico` | `medico123` | MEDICO |
| `recepcion` | `recepcion123` | RECEPCION |
| `usuario@medirecord.com` | `Medirecord123` | (siembra dev, se crea vía `POST /api/v1/auth/registro`) |

Cliente confidencial: `medirecord-api` / `medirecord-secret` (password grant habilitado **solo para desarrollo**;
en producción usar Authorization Code + PKCE con `medirecord-web`). Las contraseñas de los usuarios
se almacenan **hasheadas en Keycloak**; la API nunca las guarda.

---

## 3. Estructura del proyecto

Organización **por módulos (features)**: todo lo relacionado con un caso de uso vive en su propia
carpeta, sin paquetes globales gigantes.

```
src/main/java/com/api/medirecord/
├── MedirecordApplication.java        # @SpringBootApplication + @ConfigurationPropertiesScan
├── common/                           # Nucleo compartido (no depende de ningun modulo)
│   ├── error/                        # ErrorCode, ApiException, GlobalExceptionHandler (ProblemDetail)
│   ├── persistence/                  # AuditableEntity (created/updated + usuario)
│   ├── response/                     # ApiResponse<T>, PageResponse<T>
│   └── security/                     # SecurityUtils, entry point y handler de 401/403 en JSON
├── config/                           # SecurityConfig, CorsConfig, JpaAuditingConfig, OpenApiConfig
└── <modulo>/                         # Un paquete por dominio (paciente, cita, expediente...)
    ├── <Entidad>.java                # Solo el modelo de datos
    ├── <Entidad>Repository.java      # Spring Data (Specification para filtros dinámicos)
    ├── <Entidad>Service.java         # Reglas de negocio + frontera transaccional
    ├── <Entidad>Controller.java      # HTTP: valida, delega y devuelve status
    ├── dto/                          # Records de entrada/salida (contrato de la API)
    └── exception/                    # Excepciones del módulo, heredan de ApiException

src/main/resources/
├── application.properties            # Configuracion comun
├── application-dev.properties        # Perfil por defecto: verboso, SQL logueado
├── application-prod.properties       # Produccion: fail-fast, sin docs, root en WARN
└── db/migration/                     # Migraciones Flyway (V1 paciente, V2 usuario, V3+V4 medicamento)

src/test/java/com/api/medirecord/
├── support/                          # Bases reutilizables de tests (Testcontainers)
└── <modulo>/
    ├── <X>ServiceTest.java           # Unitario (Mockito, sin Spring)
    ├── <X>ControllerTest.java        # Slice @WebMvcTest (MockMvc + seguridad real)
    └── <X>RepositoryIT.java          # Integracion contra PostgreSQL real (*IT)
```

`paciente/` es un módulo de referencia: cópialo como plantilla para los demás dominios.

El módulo `usuario/` guarda al usuario final (la persona que necesita recordatorios de
medicamentos) y su relación con Keycloak: la columna `keycloak_id` enlaza la cuenta con el claim
`sub` del token, de modo que **nunca se almacenan contraseñas** (la autenticación es
responsabilidad de Keycloak). Cada usuario tiene un `id` UUID único. El módulo `medicamento/`
define la relación 1:N usuario → medicamento (clave foránea `usuario_id`) con las fechas del
tratamiento (`fecha_inicio` obligatoria, `fecha_fin` opcional: `null` = tratamiento vigente),
base de los recordatorios. Sus endpoints son **personales**: el usuario se resuelve del JWT,
de modo que nadie puede leer ni administrar medicamentos de otra cuenta.

El módulo `autenticacion/` expone los endpoints públicos de sesión
(`POST /api/v1/auth/{registro,login,refresh}`) y orquesta al proveedor de identidad:

```
autenticacion/
├── AutenticacionController.java   # /api/v1/auth/** (publico)
├── AutenticacionService.java      # registro, login y refresh (transaccional)
├── dto/                           # RegistroRequest, LoginRequest, RefreshRequest, TokenResponse, LoginResponse
├── keycloak/
│   ├── KeycloakClient.java        # unica puerta de entrada a Keycloak (RestClient)
│   ├── KeycloakProperties.java    # medirecord.keycloak.*
│   └── dto/                       # KeycloakToken, NuevoUsuarioKeycloak
└── exception/                     # 401/403/409/503 del flujo de sesion
```

Flujo del registro: la API crea primero la proyección local y **después** el usuario en
Keycloak dentro de una misma transacción; si el proveedor rechaza el alta, la cuenta local
se revierte y no queda un registro a medias. El login verifica las credenciales contra
Keycloak y solo después resuelve la cuenta local (evita enumerar correos). El password grant
se usa **solo para desarrollo/integraciones**: en producción sustituir por Authorization Code
+ PKCE con el cliente `medirecord-web`.

---

## 4. Convenciones

- **Contrato de éxito:** todo responde `{ success, message, data, timestamp }` (`ApiResponse`).
- **Contrato de error:** `ProblemDetail` (RFC 9457) con `code`, `timestamp`, `traceId` y `errors[]`.
  El `code` es un valor de `ErrorCode` (catálogo estable, no renombrar sin aviso).
- **Las entidades nunca se exponen**: el servicio devuelve DTOs; los controllers no tocan JPA.
- **Validación** en los DTOs (`jakarta.validation`), nunca en la entidad.
- **Borrado lógico** (`activo = false`): los datos clínicos no se eliminan físicamente.
- **Auditoría automática** en todas las entidades (`AuditableEntity`) con el usuario del JWT.
- **Filtros dinámicos** con `Specification`, no con métodos `findBy...And...` encadenados.
- **Trazabilidad de requests**: `traceId` en `MDC` (se propaga desde el gateway en producción).

---

## 5. Seguridad

```
Petición ─► SecurityFilterChain (stateless) ─► JWT validado contra Keycloak
                    │                                    │
                    ├─ /actuator/health, /actuator/prometheus  público
                    ├─ /v3/api-docs/**, /swagger-ui/**   público (desactivable: SWAGGER_ENABLED=false)
                    ├─ /api/v1/auth/**                   público (registro, login y refresh)
                    ├─ /actuator/info, /actuator/metrics  requiere token
                    ├─ /api/v1/admin/**                  requiere ROLE_ADMIN
                    └─ resto                             autenticado
```

### Endpoints de medicamentos (usuario autenticado)

Recurso personal: no reciben id de usuario porque se resuelve del token JWT. Un id ajeno o
inexistente responde `404` (no revela la existencia de medicamentos de otros).

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/medicamentos` | Registra un medicamento propio; `201` + `Location` |
| `GET` | `/api/v1/medicamentos` | Lista los propios con paginación, `search` e `incluirInactivos` |
| `GET` | `/api/v1/medicamentos/{id}` | Obtiene un medicamento propio |
| `PUT` | `/api/v1/medicamentos/{id}` | Actualiza un medicamento propio |
| `PATCH` | `/api/v1/medicamentos/{id}/desactivar` | Baja lógica |
| `DELETE` | `/api/v1/medicamentos/{id}` | Alias de baja lógica |

Reglas: `nombre` y `fechaInicio` son obligatorios; `fechaFin` es opcional (sin ella el
tratamiento sigue **vigente**) y no puede ser anterior a `fechaInicio`
(`400 VALIDATION_ERROR` con el detalle de las fechas).
### Endpoints de sesión

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/auth/registro` | Crea la cuenta en Keycloak y su proyección local; devuelve `201` + `Location` |
| `POST` | `/api/v1/auth/login` | Verifica credenciales; devuelve `accessToken` y `refreshToken` |
| `POST` | `/api/v1/auth/refresh` | Renueva los tokens si el `refreshToken` es válido y la cuenta está activa |

Los errores siguen el catálogo `ErrorCode`: credenciales inválidas → `401 UNAUTHENTICATED`,
cuenta desactivada → `403 FORBIDDEN`, correo duplicado → `409 DUPLICATED_RESOURCE`,
Keycloak caído → `503 SERVICE_UNAVAILABLE`. Ningún mensaje distingue entre "correo
inexistente" y "contraseña incorrecta" (evita enumeración de cuentas), y las contraseñas no
se registran en ningún log.

Mapeo de roles → autoridades de Spring:

| Claim del JWT | Autoridad Spring |
|---------------|------------------|
| `realm_access.roles` | `ROLE_ADMIN`, `ROLE_MEDICO`, ... |
| `resource_access.<cliente>.roles` | `SCOPE_<rol>` |
| `scope` | `SCOPE_<scope>` |

Afinar permisos por operación con `@PreAuthorize("hasRole('ADMIN')")` en el servicio.

---

## 6. Comandos

| Comando | Qué hace |
|---------|----------|
| `make help` | Lista los comandos disponibles |
| `make build` | Compila el jar sin tests |
| `make test` | Tests unitarios y de slice (rápidos, sin Docker) |
| `make test-it` | Tests de integración con PostgreSQL real (requiere Docker) |
| `make verify` | Build completo + integración + reporte JaCoCo en `target/site/jacoco` |
| `make run` | Levanta la API con perfil `dev` |
| `make debug` | Levanta la API en suspendida (JDWP 5005) |
| `make up` / `make up-app` | Infraestructura / stack completa con imagen |
| `make down` | Detiene todo **y borra los volúmenes** |
| `make token` | Obtiene un access token de `admin` (directo contra Keycloak) |
| `make login` | Inicia sesión vía `POST /api/v1/auth/login` y muestra el access token |
| `make psql` | Consola SQL contra el contenedor |

Equivalentes sin `make`:

```bash
./mvnw clean package -DskipTests
./mvnw test
./mvnw verify -Pintegration        # sí, requiere Docker
```

---

## 7. Base de datos y migraciones

- El esquema es **exclusivamente de Flyway**: `spring.jpa.hibernate.ddl-auto=validate`.
- Nueva migración: `src/main/resources/db/migration/V{n}__{descripcion_en_snake_case}.sql`.
- `spring.flyway.clean-disabled=true` en todos los entornos: nadie borra la base por accidente.
- Conexión en *pool* HikariCP con `pool-init-sql` en UTC (fechas consistentes entre entornos).

---

## 8. Configuración

Todo es sobreescribible por variable de entorno o `--property`:

| Variable | Default (dev) | Descripción |
|----------|---------------|-------------|
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil activo |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `medirecord` | Conexión |
| `DB_USER` / `DB_PASSWORD` | `medirecord` | Credenciales (**usar secretos en producción**) |
| `DB_POOL_MAX_SIZE` | `10` | Tamaño máximo del pool |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8081/realms/medirecord` | Emisor del token |
| `KEYCLOAK_JWK_SET_URI` | derivado del issuer | Claves públicas (evita discovery al arrancar) |
| `KEYCLOAK_BASE_URL` | `http://localhost:8081` | URL de Keycloak desde la API (registro/login). En Docker: `http://keycloak:8080` |
| `KEYCLOAK_REALM` | `medirecord` | Realm donde se crean las cuentas |
| `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET` | `medirecord-api` / `medirecord-secret` | Cliente que emite los tokens de sesión |
| `KEYCLOAK_ADMIN_REALM` / `KEYCLOAK_ADMIN_CLIENT_ID` | `master` / `admin-cli` | Cliente usado por la API para dar de alta usuarios |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | `admin` / `admin123` | Credenciales de administración (**gestor de secretos en producción**) |
| `DEV_USER_PASSWORD` | `Medirecord123` | Contraseña del usuario siembra del perfil `dev` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | Orígenes permitidos |
| `SWAGGER_ENABLED` | `true` (dev) / `false` (prod) | Documentación |
| `LOG_LEVEL_APP` | `INFO` | Nivel de log de la app |

---

## 9. Observabilidad

| Endpoint | Autenticación | Uso |
|----------|----------------|-----|
| `/actuator/health` | público | Liveness/readiness (lo usa el healthcheck de Docker y el orquestador) |
| `/actuator/prometheus` | público | Scrape para Prometheus |
| `/actuator/info` | token | Versión y build (`build-info`) |
| `/actuator/metrics` | token | Métricas Micrometer |

> `management.info.env.enabled` está **desactivado** a propósito: expondría variables de
> entorno (incluidas contraseñas) por `/actuator/info`. Los secretos en producción deben venir
> de un gestor de secretos, no de variables en texto plano.

---

## 10. CI

`.github/workflows/ci.yml` ejecuta `./mvnw verify -Pintegration` (unitarios + integración con
Testcontainers + cobertura), publica el reporte JaCoCo y valida el `docker build` en `main`.

---

## 11. Qué falta decidir antes de producción

- [ ] Gestor de secretos (Vault / AWS Secrets Manager) en lugar de variables con contraseña.
- [ ] `CORS_ALLOWED_ORIGINS` y `KEYCLOAK_ISSUER_URI` con HTTPS y `sslRequired=external` real.
- [ ] Sustituir `admin-cli` + credenciales de administrador por un **cliente de servicio** con rol
      `manage-users` para el alta de usuarios (mínimo privilegio).
- [ ] Migrar el login de `/api/v1/auth/login` de password grant a Authorization Code + PKCE
      (cliente `medirecord-web`) y desactivar `directAccessGrantsEnabled` de `medirecord-api`.
- [ ] Verificación del correo electrónico (`emailVerified` hoy es `false` al registrarse).
- [ ] Cifrado a nivel de columna o TDE para datos clínicos, y bitácoras de auditoría inmutables.
- [ ] Umbral mínimo de cobertura en JaCoCo (`check`) una vez estabilizado el modelo de dominio.
- [ ] Migrar los usuarios de prueba del realm fuera del repositorio.

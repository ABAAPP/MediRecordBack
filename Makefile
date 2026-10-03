# ======================================================================================
# Comandos rapidos del proyecto. Ejecuta `make help` para ver la lista.
# ======================================================================================
.DEFAULT_GOAL := help
SHELL := /bin/bash
MVN := ./mvnw
COMPOSE := docker compose

.PHONY: help build test test-unit test-it verify run debug clean \
        up down up-app logs psql token token-medico swagger db-reset fmt-check

help: ## Muestra esta ayuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-14s\033[0m %s\n", $$1, $$2}'

# ---------- Build / tests ----------
build: ## Compila el jar (sin tests)
	$(MVN) clean package -DskipTests

test: ## Tests unitarios y de slice (rapidos, sin Docker)
	$(MVN) test

test-it: ## Tests de integracion con PostgreSQL real (requiere Docker)
	$(MVN) verify -Pintegration

verify: ## Build completo + tests de integracion + reporte JaCoCo
	$(MVN) verify -Pintegration

# ---------- Ejecucion ----------
run: ## Arranca la API con el perfil dev
	$(MVN) spring-boot:run

debug: ## Arranca la API suspendida para depurar desde el IDE
	$(MVN) spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"

clean: ## Limpia target/
	$(MVN) clean

# ---------- Infraestructura ----------
up: ## Levanta PostgreSQL + Keycloak
	$(COMPOSE) up -d postgres keycloak

up-app: ## Levanta PostgreSQL + Keycloak + API (imagen compilada)
	$(COMPOSE) --profile app up -d --build

down: ## Detiene todo y borra los volumenes (CUIDADO: borra la base de datos)
	$(COMPOSE) --profile app down -v

logs: ## Sigue los logs de todos los servicios
	$(COMPOSE) logs -f

db-reset: ## Recrea la base de datos desde cero
	$(COMPOSE) down -v
	$(COMPOSE) up -d postgres keycloak

psql: ## Abre una consola de psql contra el contenedor de desarrollo
	$(COMPOSE) exec postgres psql -U medirecord -d medirecord

# ---------- Autenticacion de prueba ----------
token: ## Obtiene un access token del usuario admin (admin/admin123)
	@curl -s -X POST "http://localhost:$${KEYCLOAK_PORT:-8081}/realms/medirecord/protocol/openid-connect/token" \
		-d "grant_type=password" \
		-d "client_id=medirecord-api" \
		-d "client_secret=medirecord-secret" \
		-d "username=admin" \
		-d "password=admin123" | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token','ERROR: revisa que Keycloak este arriba'))"

swagger: ## Abre la documentacion en el navegador
	@open "http://localhost:$${SERVER_PORT:-8080}/swagger-ui.html"
-- ============================================================================
-- V2: tabla de usuarios de la aplicacion
--
-- El usuario final es quien necesita los recordatorios de medicamentos. La
-- autenticacion la realiza Keycloak; aqui solo se guarda la proyeccion local.
-- La columna keycloak_id enlaza la cuenta con el claim "sub" del token
-- (identidad externa) y nunca almacena contrasenas.
--
-- El esquema es responsabilidad de Flyway; Hibernate solo lo valida.
-- ============================================================================
CREATE TABLE usuario (
    id                 UUID                    NOT NULL DEFAULT gen_random_uuid(),
    keycloak_id        VARCHAR(100),
    nombre             VARCHAR(100)            NOT NULL,
    apellido_paterno   VARCHAR(100)            NOT NULL,
    apellido_materno   VARCHAR(100),
    email              VARCHAR(150)            NOT NULL,
    telefono           VARCHAR(20),
    activo             BOOLEAN                 NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMP(6) WITH TIME ZONE,
    updated_by         VARCHAR(100),
    CONSTRAINT pk_usuario             PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email       UNIQUE (email),
    CONSTRAINT uk_usuario_keycloak_id UNIQUE (keycloak_id)
);

CREATE INDEX idx_usuario_apellidos ON usuario (apellido_paterno, apellido_materno);
CREATE INDEX idx_usuario_activo    ON usuario (activo);

-- ============================================================================
-- V1: tabla de pacientes (expediente clinico)
-- El esquema es responsabilidad de Flyway; Hibernate solo lo valida.
-- ============================================================================
CREATE TABLE paciente (
    id                 UUID                    NOT NULL DEFAULT gen_random_uuid(),
    nombre             VARCHAR(80)             NOT NULL,
    apellido_paterno   VARCHAR(80)             NOT NULL,
    apellido_materno   VARCHAR(80),
    fecha_nacimiento   DATE                    NOT NULL,
    email              VARCHAR(120)            NOT NULL,
    telefono           VARCHAR(20),
    activo             BOOLEAN                 NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMP(6) WITH TIME ZONE,
    updated_by         VARCHAR(100),
    CONSTRAINT pk_paciente PRIMARY KEY (id),
    CONSTRAINT uk_paciente_email UNIQUE (email),
    CONSTRAINT ck_paciente_fecha_nacimiento CHECK (fecha_nacimiento <= CURRENT_DATE)
);

CREATE INDEX idx_paciente_apellidos ON paciente (apellido_paterno, apellido_materno);
CREATE INDEX idx_paciente_activo    ON paciente (activo);

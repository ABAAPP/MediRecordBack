-- ============================================================================
-- V4: tabla de horarios y frecuencias de administracion de medicamentos (B-07)
-- Cada horario pertenece obligatoriamente a un medicamento existente (FK).
-- ============================================================================
CREATE TABLE IF NOT EXISTS horario (
    id                   UUID                        NOT NULL DEFAULT gen_random_uuid(),
    medicamento_id       UUID                        NOT NULL,
    hora_administracion  VARCHAR(20)                 NOT NULL,
    frecuencia           VARCHAR(100)                NOT NULL,
    dias_semana          VARCHAR(250)                NOT NULL,
    recordatorio_activo  BOOLEAN                     NOT NULL DEFAULT TRUE,
    notas                VARCHAR(500),
    activo               BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMP(6) WITH TIME ZONE,
    updated_by           VARCHAR(100),
    CONSTRAINT pk_horario PRIMARY KEY (id),
    CONSTRAINT fk_horario_medicamento FOREIGN KEY (medicamento_id)
        REFERENCES medicamento (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_horario_medicamento_activo ON horario (medicamento_id, activo);
CREATE INDEX IF NOT EXISTS idx_horario_recordatorio_activo ON horario (recordatorio_activo, activo);

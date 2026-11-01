-- ============================================================================
-- V5: tabla de registro de tomas realizadas de medicamentos (B-09)
-- Registra el estado de cumplimiento de cada toma/recordatorio por fecha y usuario.
-- ============================================================================
CREATE TABLE IF NOT EXISTS toma_medicamento (
    id                 UUID                        NOT NULL DEFAULT gen_random_uuid(),
    medicamento_id     UUID                        NOT NULL,
    horario_id         UUID,
    usuario_id         VARCHAR(100)                NOT NULL,
    fecha_toma         DATE                        NOT NULL DEFAULT CURRENT_DATE,
    hora_toma          VARCHAR(20)                 NOT NULL,
    realizada          BOOLEAN                     NOT NULL DEFAULT TRUE,
    notas              VARCHAR(500),
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMP(6) WITH TIME ZONE,
    updated_by         VARCHAR(100),
    CONSTRAINT pk_toma_medicamento PRIMARY KEY (id),
    CONSTRAINT fk_toma_medicamento FOREIGN KEY (medicamento_id)
        REFERENCES medicamento (id) ON DELETE CASCADE,
    CONSTRAINT fk_toma_horario FOREIGN KEY (horario_id)
        REFERENCES horario (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_toma_usuario_fecha ON toma_medicamento (usuario_id, fecha_toma);
CREATE INDEX IF NOT EXISTS idx_toma_medicamento_fecha ON toma_medicamento (medicamento_id, fecha_toma);
CREATE INDEX IF NOT EXISTS idx_toma_horario_fecha ON toma_medicamento (horario_id, fecha_toma);

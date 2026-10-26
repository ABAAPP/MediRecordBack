-- ============================================================================
-- V2: tabla de medicamentos asociados a la cuenta del usuario
-- Cumple con B-05: modelo de medicamentos, fechas de inicio/fin y aislamiento por usuario.
-- ============================================================================
CREATE TABLE IF NOT EXISTS medicamento (
    id                 UUID                        NOT NULL DEFAULT gen_random_uuid(),
    usuario_id         VARCHAR(100)                NOT NULL,
    nombre             VARCHAR(120)                NOT NULL,
    dosis              VARCHAR(100),
    frecuencia         VARCHAR(100),
    instrucciones      VARCHAR(500),
    fecha_inicio       DATE                        NOT NULL,
    fecha_fin          DATE,
    activo             BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMP(6) WITH TIME ZONE,
    updated_by         VARCHAR(100),
    CONSTRAINT pk_medicamento PRIMARY KEY (id),
    CONSTRAINT ck_medicamento_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE INDEX IF NOT EXISTS idx_medicamento_usuario_activo ON medicamento (usuario_id, activo);
CREATE INDEX IF NOT EXISTS idx_medicamento_usuario_fechas ON medicamento (usuario_id, fecha_inicio);

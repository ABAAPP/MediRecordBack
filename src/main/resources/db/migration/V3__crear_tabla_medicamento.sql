-- ============================================================================
-- V3: tabla de medicamentos, ligada al usuario que los registra.
--
-- Relacion 1:N usuario -> medicamento. El borrado del usuario se hace logico en
-- la aplicacion (activo = false); ON DELETE CASCADE es una salvaguarda a nivel
-- de base de datos para no dejar medicamentos huerfanos.
-- ============================================================================
CREATE TABLE medicamento (
    id            UUID                      NOT NULL DEFAULT gen_random_uuid(),
    usuario_id    UUID                      NOT NULL,
    nombre        VARCHAR(120)              NOT NULL,
    dosis         VARCHAR(80),
    frecuencia    VARCHAR(80),
    activo        BOOLEAN                   NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMP(6) WITH TIME ZONE,
    updated_by    VARCHAR(100),
    CONSTRAINT pk_medicamento          PRIMARY KEY (id),
    CONSTRAINT fk_medicamento_usuario  FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);

CREATE INDEX idx_medicamento_usuario ON medicamento (usuario_id);
CREATE INDEX idx_medicamento_activo  ON medicamento (activo);

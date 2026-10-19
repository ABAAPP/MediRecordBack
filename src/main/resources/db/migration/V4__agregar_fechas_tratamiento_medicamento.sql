-- ============================================================================
-- V4: fechas del tratamiento de un medicamento.
--
-- fecha_inicio es obligatoria: no existe un tratamiento sin fecha de inicio.
-- fecha_fin es opcional: el null significa que el tratamiento sigue vigente.
-- ck_medicamento_fechas garantiza a nivel de base de datos que el tratamiento
-- no termine antes de empezar (la aplicacion tambien lo valida).
-- ============================================================================
ALTER TABLE medicamento ADD COLUMN fecha_inicio DATE;

-- Backfill defensivo por si la tabla ya tiene filas de la V3 (entorno dev).
UPDATE medicamento
   SET fecha_inicio = COALESCE(created_at::date, CURRENT_DATE)
 WHERE fecha_inicio IS NULL;

ALTER TABLE medicamento ALTER COLUMN fecha_inicio SET NOT NULL;

ALTER TABLE medicamento ADD COLUMN fecha_fin DATE;

ALTER TABLE medicamento
    ADD CONSTRAINT ck_medicamento_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio);

CREATE INDEX idx_medicamento_fecha_inicio ON medicamento (fecha_inicio);

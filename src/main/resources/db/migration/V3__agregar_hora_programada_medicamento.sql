-- ============================================================================
-- V3: agrega columna hora_programada a la tabla medicamento (B-06)
-- Permite almacenar el horario inicial de toma al registrar el medicamento desde la app.
-- ============================================================================
ALTER TABLE medicamento
    ADD COLUMN IF NOT EXISTS hora_programada VARCHAR(20);

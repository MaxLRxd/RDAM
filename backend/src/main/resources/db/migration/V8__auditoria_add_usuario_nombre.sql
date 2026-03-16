-- =============================================================
-- V8: Agrega usuario_nombre a auditoria_operaciones.
--
-- Motivo: evitar un JOIN con usuario_interno al listar el registro
-- de auditoría. El nombre se desnormaliza al momento de escribir
-- la entrada (en AuditoriaService.registrar()).
-- =============================================================

ALTER TABLE auditoria_operaciones
    ADD COLUMN usuario_nombre VARCHAR(100) NULL
        COMMENT 'Username o etiqueta del actor: email del operador, "sistema", "ciudadano", etc.'
    AFTER id_usuario;

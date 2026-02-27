-- =============================================================
-- V5: Usuarios del panel interno (Operadores y Administradores).
-- Depende de: circunscripcion (V1)
-- Va último porque no hay tablas que dependan de ella.
-- =============================================================

CREATE TABLE usuario_interno (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    username            VARCHAR(100) NOT NULL,

    -- Hash BCrypt cost 12. Nunca texto plano.
    password_hash       VARCHAR(255) NOT NULL,

    -- OPERADOR: gestiona solicitudes de su circunscripción.
    -- ADMIN: acceso total, sin circunscripción asignada.
    rol                 VARCHAR(20) NOT NULL,

    -- NULL para ADMIN (acceso total).
    -- NOT NULL para OPERADOR (solo ve su circunscripción).
    id_circunscripcion  TINYINT     NULL,

    activo              BOOLEAN     NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuario_interno
        PRIMARY KEY (id),

    CONSTRAINT uq_username
        UNIQUE (username),

    CONSTRAINT fk_usuario_circunscripcion
        FOREIGN KEY (id_circunscripcion)
        REFERENCES circunscripcion(id_circunscripcion),

    CONSTRAINT chk_rol CHECK (
        rol IN ('OPERADOR', 'ADMIN')
    ),

    -- Replica la regla de negocio a nivel de DB:
    -- ADMIN no tiene circunscripción, OPERADOR sí.
    CONSTRAINT chk_circ_usuario CHECK (
        (rol = 'ADMIN'    AND id_circunscripcion IS NULL) OR
        (rol = 'OPERADOR' AND id_circunscripcion IS NOT NULL)
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
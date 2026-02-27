-- =============================================================
-- V4: Tablas de auditoría y trazabilidad.
-- Dependen de: solicitud (V3)
-- Van antes de usuario_interno porque no lo referencian,
-- y así usuario_interno (V5) puede ir al final sin problema.
-- =============================================================


-- ----------------------------------------------------
-- Historial de cambios de estado de cada solicitud.
-- Append-only: solo INSERT, nunca UPDATE ni DELETE
-- (salvo CASCADE cuando se borra la solicitud padre).
-- ----------------------------------------------------
CREATE TABLE solicitud_historial_estados (
    id_historial        BIGINT      NOT NULL AUTO_INCREMENT,
    id_solicitud        BIGINT      NOT NULL,

    -- Null en el primer registro (no había estado anterior)
    estado_anterior     VARCHAR(30) NULL,
    estado_nuevo        VARCHAR(30) NOT NULL,

    -- Null si el cambio fue automático (webhook, scheduler).
    -- Tiene valor si lo hizo un operador o admin.
    id_usuario_interno  BIGINT      NULL,

    fecha_cambio        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_historial
        PRIMARY KEY (id_historial),

    -- Si se borra la solicitud, se borran sus registros de historial.
    -- ON DELETE CASCADE a nivel DB es más eficiente que CascadeType.ALL
    -- de JPA porque no necesita cargar los registros en memoria.
    CONSTRAINT fk_hist_solicitud
        FOREIGN KEY (id_solicitud)
        REFERENCES solicitud(id_solicitud)
        ON DELETE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;

-- Índice para obtener el historial completo de una solicitud rápido
CREATE INDEX idx_historial_solicitud
    ON solicitud_historial_estados(id_solicitud);


-- ----------------------------------------------------
-- Registro de operaciones sensibles del sistema.
-- Append-only. No tiene FK obligatoria a solicitud
-- porque registra eventos que pueden no tener solicitud
-- asociada (logins, creación de usuarios, etc.)
-- ----------------------------------------------------
CREATE TABLE auditoria_operaciones (
    id_auditoria    BIGINT      NOT NULL AUTO_INCREMENT,
    fecha_hora      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Null si la operación la realizó un ciudadano o el sistema
    id_usuario      BIGINT      NULL,

    -- Tipo de operación. Ver AuditoriaOperacion.Operaciones
    operacion       VARCHAR(50) NOT NULL,

    -- IPv4 (max 15 chars) o IPv6 (max 45 chars)
    ip_origen       VARCHAR(45) NULL,

    -- Información adicional. NUNCA datos sensibles (DNI, email, tokens)
    detalle         TEXT        NULL,

    -- ID de la entidad relacionada (solicitud, usuario, etc.)
    entidad_id      BIGINT      NULL,

    CONSTRAINT pk_auditoria
        PRIMARY KEY (id_auditoria)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;

-- Índice para búsquedas de auditoría por operación y fecha
CREATE INDEX idx_auditoria_operacion_fecha
    ON auditoria_operaciones(operacion, fecha_hora);
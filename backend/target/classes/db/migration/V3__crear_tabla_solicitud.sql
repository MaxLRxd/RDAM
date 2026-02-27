-- =============================================================
-- V3: Tabla central del sistema. Representa el ciclo de vida
-- completo de un trámite de certificado RDAM.
-- Depende de: circunscripcion (V1)
-- =============================================================

CREATE TABLE solicitud (
    id_solicitud        BIGINT          NOT NULL AUTO_INCREMENT,
    nro_tramite         VARCHAR(20)     NOT NULL,
    dni_cuil            VARCHAR(11)     NOT NULL,
    email               VARCHAR(255)    NOT NULL,

    -- FK hacia la tabla maestra. Determina qué operador
    -- puede trabajar con esta solicitud.
    id_circunscripcion  TINYINT         NOT NULL,

    -- Código OTP de 6 dígitos. Se limpia tras validación exitosa.
    codigo_validacion   VARCHAR(6)      NULL,

    -- Token de sesión del ciudadano. El valor con TTL vive en Redis,
    -- acá guardamos referencia para poder invalidarlo.
    token_acceso        VARCHAR(64)     NULL,

    -- Estado del trámite. Valores controlados por CHECK constraint.
    estado              VARCHAR(30)     NOT NULL DEFAULT 'PENDIENTE',

    -- Campos de pago: null hasta que se confirma el pago.
    monto_arancel       DECIMAL(10,2)   NULL,
    id_orden_pago       VARCHAR(100)    NULL,
    sol_fec_pago        TIMESTAMP       NULL,

    -- Campos de certificado: null hasta que el operador sube el PDF.
    url_certificado     VARCHAR(500)    NULL,
    token_descarga      VARCHAR(64)     NULL,
    sol_fec_emision     TIMESTAMP       NULL,

    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Control de concurrencia optimista.
    -- Hibernate usa este campo en el WHERE de cada UPDATE.
    version             BIGINT          NOT NULL DEFAULT 0,

    -- Claves y unicidades
    CONSTRAINT pk_solicitud
        PRIMARY KEY (id_solicitud),

    CONSTRAINT uq_nro_tramite
        UNIQUE (nro_tramite),

    CONSTRAINT uq_id_orden_pago
        UNIQUE (id_orden_pago),

    CONSTRAINT uq_token_descarga
        UNIQUE (token_descarga),

    CONSTRAINT uq_token_acceso
        UNIQUE (token_acceso),

    -- Integridad referencial con circunscripcion
    CONSTRAINT fk_solicitud_circunscripcion
        FOREIGN KEY (id_circunscripcion)
        REFERENCES circunscripcion(id_circunscripcion),

    -- Valores permitidos para el estado.
    -- Deben coincidir exactamente con los valores del enum
    -- EstadoSolicitud.java (en mayúsculas, por @Enumerated(STRING))
    CONSTRAINT chk_estado CHECK (
        estado IN (
            'PENDIENTE',
            'PAGADO',
            'PUBLICADO',
            'PUBLICADO_VENCIDO',
            'VENCIDO'
        )
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Índices para las búsquedas más frecuentes del sistema
-- El operador busca por estado y circunscripción constantemente.
CREATE INDEX idx_solicitud_estado
    ON solicitud(estado);

CREATE INDEX idx_solicitud_circunscripcion
    ON solicitud(id_circunscripcion);

-- El scheduler de vencimientos busca por estado + fecha_creacion
CREATE INDEX idx_solicitud_estado_fecha
    ON solicitud(estado, fecha_creacion);
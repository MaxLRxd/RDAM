-- =============================================================
-- V2: Tabla de parámetros de configuración del sistema.
-- Centraliza valores de negocio que antes eran magic numbers
-- en el código (monto del arancel, días de validez, etc.)
-- El servicio lee estos valores al iniciar en lugar de
-- hardcodearlos. Pueden modificarse sin recompilar.
-- =============================================================

CREATE TABLE parametros_sistema (
    clave       VARCHAR(50)     NOT NULL,
    valor       VARCHAR(255)    NOT NULL,
    descripcion VARCHAR(255)    NULL,

    CONSTRAINT pk_parametros PRIMARY KEY (clave)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
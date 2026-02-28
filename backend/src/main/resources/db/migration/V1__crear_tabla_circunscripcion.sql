-- =============================================================
-- V1: Tabla maestra de circunscripciones judiciales
-- Sin dependencias externas. Va primero porque otras tablas
-- la referencian mediante FK.
-- =============================================================

CREATE TABLE circunscripcion (
    id_circunscripcion INT NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(50)     NOT NULL,
    sede                VARCHAR(100)    NOT NULL,

    CONSTRAINT pk_circunscripcion PRIMARY KEY (id_circunscripcion)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
-- =============================================================
-- V6: Datos de referencia iniciales.
-- Este script es idempotente: si se ejecuta dos veces,
-- INSERT IGNORE evita errores por duplicados.
-- =============================================================


-- ----------------------------------------------------
-- Las 5 circunscripciones judiciales de Santa Fe.
-- Estos datos son fijos del negocio, no se modifican
-- desde la API. Solo un DBA puede cambiarlos.
-- ----------------------------------------------------
INSERT IGNORE INTO circunscripcion
    (id_circunscripcion, nombre, sede)
VALUES
    (1, 'Santa Fe',       'Sede Santa Fe'),
    (2, 'Rosario',        'Sede Rosario'),
    (3, 'Venado Tuerto',  'Sede Venado Tuerto'),
    (4, 'Reconquista',    'Sede Reconquista'),
    (5, 'Rafaela',        'Sede Rafaela');


-- ----------------------------------------------------
-- Parámetros del sistema.
-- Los servicios leen estos valores en lugar de usar
-- magic numbers en el código.
-- Para cambiar el monto del arancel: UPDATE acá,
-- no en application.properties ni en código Java.
-- ----------------------------------------------------
INSERT IGNORE INTO parametros_sistema
    (clave, valor, descripcion)
VALUES
    ('MONTO_ARANCEL',
     '1500.00',
     'Costo de emisión del certificado en pesos'),

    ('VALIDEZ_DIAS_CERT',
     '65',
     'Días de validez del token de descarga (PRD)'),

    ('VENCIMIENTO_PENDIENTE_DIAS',
     '60',
     'Días hasta vencimiento de solicitud sin pago (PRD)');


-- ----------------------------------------------------
-- Usuario ADMIN inicial del sistema.
-- IMPORTANTE: cambiar la contraseña en el primer login.
-- Hash BCrypt de 'Admin1234!' con cost=12.
-- Generado con: BCryptPasswordEncoder(12).encode("Admin1234!")
-- En producción: generar un hash nuevo y no usar este.
-- ----------------------------------------------------
INSERT IGNORE INTO usuario_interno
    (username, password_hash, rol, id_circunscripcion, activo)
VALUES
    (
        'admin@rdam.santafe.gob.ar',
        '$2a$12$VSIaYnbpohM0wvIkpXmsSuIvctaWOjhFhP9aXHrNagCy0EZJLRGYm',
        'ADMIN',
        NULL,
        TRUE
    );

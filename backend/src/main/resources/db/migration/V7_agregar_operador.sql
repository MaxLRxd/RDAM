-- --------------------------------------------------------
-- V7: Agrega el usuario operador1 para pruebas.
-- No se incluyó en V6 porque esa migración ya fue ejecutada.
-- Hash BCrypt cost=12 de 'Admin1234!'
-- --------------------------------------------------------
INSERT IGNORE INTO usuario_interno
    (username, password_hash, rol, id_circunscripcion, activo)
VALUES
    (
        'operador1@rdam.santafe.gob.ar',
        '$2a$12$VSIaYnbpohM0wvIkpXmsSuIvctaWOjhFhP9aXHrNagCy0EZJLRGYm',
        'OPERADOR',
        1,
        TRUE
    );
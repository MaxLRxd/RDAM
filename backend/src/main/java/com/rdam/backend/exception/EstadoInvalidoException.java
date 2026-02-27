package com.rdam.backend.exception;


/**
 * Se lanza cuando se intenta una transición de estado no permitida.
 * El GlobalExceptionHandler la convierte en HTTP 400.
 */
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
package com.rdam.backend.exception;


/**
 * Se lanza cuando un OPERADOR intenta acceder o modificar
 * una solicitud de una circunscripción distinta a la suya.
 * El GlobalExceptionHandler la convierte en HTTP 403.
 */
public class CircunscripcionMismatchException extends RuntimeException {
    public CircunscripcionMismatchException() {
        super("No tenés permisos para acceder a solicitudes " +
              "de otra circunscripción");
    }
}
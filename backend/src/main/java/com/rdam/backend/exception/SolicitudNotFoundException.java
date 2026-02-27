package com.rdam.backend.exception;

/**
 * Se lanza cuando no se encuentra una solicitud por
 * nroTramite, id, tokenDescarga, etc.
 * El GlobalExceptionHandler la convierte en HTTP 404.
 */
public class SolicitudNotFoundException extends RuntimeException {
    public SolicitudNotFoundException(String identificador) {
        super("Solicitud no encontrada: " + identificador);
    }
}
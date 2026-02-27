package com.rdam.backend.exception;
/**
 * Se lanza cuando un JWT o token ciudadano es inválido,
 * está expirado o no se encuentra en Redis.
 * El GlobalExceptionHandler la convierte en HTTP 401.
 */
public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
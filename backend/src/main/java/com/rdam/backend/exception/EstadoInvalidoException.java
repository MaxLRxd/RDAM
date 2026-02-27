package com.rdam.backend.exception;


public class EstadoInvalidoException extends Exception {          // or RuntimeException
    public EstadoInvalidoException() {
        super();
    }

    public EstadoInvalidoException(String message) {
        super(message);
    }

    public EstadoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }

    public EstadoInvalidoException(Throwable cause) {
        super(cause);
    }
}
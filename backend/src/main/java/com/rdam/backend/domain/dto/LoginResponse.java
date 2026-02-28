package com.rdam.backend.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Respuesta al login exitoso de un usuario interno.
 * HTTP 200
 */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long   expiresIn;   // en segundos
    private String rol;
    private Integer circunscripcion; // null para ADMIN
}
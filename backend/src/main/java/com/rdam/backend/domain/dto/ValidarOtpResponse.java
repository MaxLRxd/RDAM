package com.rdam.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Respuesta al validar el OTP exitosamente.
 * HTTP 200
 * Incluye el token de acceso que el ciudadano guarda
 * en LocalStorage para futuros requests.
 */
@Getter
@AllArgsConstructor
public class ValidarOtpResponse {
    private String tokenAcceso;
    private String nroTramite;
    private String estado;
}
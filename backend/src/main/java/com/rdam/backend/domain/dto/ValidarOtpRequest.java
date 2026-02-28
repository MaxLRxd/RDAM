package com.rdam.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para validar el OTP recibido por email.
 * POST /api/v1/solicitudes/{id}/validar
 */
@Getter
@Setter
@NoArgsConstructor
public class ValidarOtpRequest {

    /**
     * Código OTP de exactamente 6 dígitos.
     * Regex garantiza que solo aceptamos dígitos.
     */
    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(
        regexp = "^[0-9]{6}$",
        message = "El código debe ser de 6 dígitos numéricos"
    )
    private String codigo;
}
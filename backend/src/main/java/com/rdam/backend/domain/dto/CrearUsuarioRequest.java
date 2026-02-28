package com.rdam.backend.domain.dto;

import com.rdam.backend.enums.RolUsuario;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para crear un usuario interno.
 * POST /api/v1/usuarios-internos
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearUsuarioRequest {

    @NotBlank(message = "El username es obligatorio")
    @Email(message = "El username debe ser un email válido")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    /**
     * Obligatorio si rol = OPERADOR.
     * Debe ser null si rol = ADMIN.
     */
    private Integer idCircunscripcion;
}
package com.rdam.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload para activar o desactivar un usuario interno.
 * PATCH /api/v1/usuarios-internos/{id}/estado
 */
@Getter
@Setter
@NoArgsConstructor
public class CambiarEstadoUsuarioRequest {

    @NotNull(message = "El campo activo es obligatorio")
    private Boolean activo;
}
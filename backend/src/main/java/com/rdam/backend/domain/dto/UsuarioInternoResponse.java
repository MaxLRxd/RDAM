package com.rdam.backend.domain.dto;

import com.rdam.backend.domain.entity.UsuarioInterno;

import lombok.Getter;

/**
 * Respuesta con datos de un usuario interno.
 * Nunca expone el password_hash.
 */
@Getter
public class UsuarioInternoResponse {

    private final Long    id;
    private final String  username;
    private final String  rol;
    private final Integer circunscripcion;
    private final boolean activo;

    public UsuarioInternoResponse(UsuarioInterno usuario) {
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.rol = usuario.getRol().name();
        this.circunscripcion = usuario.getCircunscripcion() != null
            ? usuario.getCircunscripcion().getId()
            : null;
        this.activo = usuario.isActivo();
    }
}
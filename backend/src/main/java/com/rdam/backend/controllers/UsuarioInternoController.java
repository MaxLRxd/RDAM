package com.rdam.backend.controllers;

import com.rdam.backend.domain.dto.CambiarEstadoUsuarioRequest;
import com.rdam.backend.domain.dto.CrearUsuarioRequest;
import com.rdam.backend.domain.dto.UsuarioInternoResponse;
import com.rdam.backend.domain.entity.UsuarioInterno;
import com.rdam.backend.service.UsuarioInternoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Gestión de usuarios internos. Solo accesible para ADMIN.
 * El SecurityConfig ya restringe estas URLs a ROLE_ADMIN.
 *
 * POST  /usuarios-internos          → crear usuario
 * PATCH /usuarios-internos/{id}/estado → activar/desactivar
 */
@RestController
@RequestMapping("/usuarios-internos")
@RequiredArgsConstructor
public class UsuarioInternoController {

    private final UsuarioInternoService usuarioService;

    /**
     * Crea un nuevo usuario interno.
     *
     * HTTP 201: usuario creado.
     * HTTP 400: datos inválidos (circunscripción faltante para OPERADOR, etc.).
     * HTTP 409: el username ya existe.
     */
    @PostMapping
    public ResponseEntity<UsuarioInternoResponse> crear(
            @Valid @RequestBody CrearUsuarioRequest request) {

        UsuarioInterno usuario = usuarioService.crearUsuario(
            request.getUsername(),
            request.getPassword(),
            request.getRol(),
            request.getIdCircunscripcion()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new UsuarioInternoResponse(usuario));
    }

    /**
     * Activa o desactiva un usuario interno.
     *
     * @AuthenticationPrincipal inyecta el ADMIN que realiza la acción.
     * Lo necesitamos para prevenir la auto-desactivación.
     *
     * HTTP 200: estado actualizado.
     * HTTP 400: intento de desactivar el propio usuario.
     * HTTP 404: usuario no encontrado.
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioInternoResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request,
            @AuthenticationPrincipal UsuarioInterno adminActual) {

        UsuarioInterno usuario = usuarioService.cambiarEstado(
            id,
            request.getActivo(),
            adminActual.getId()
        );

        return ResponseEntity.ok(new UsuarioInternoResponse(usuario));
    }
}
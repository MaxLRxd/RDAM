package com.rdam.backend.controllers;

import com.rdam.backend.domain.dto.LoginRequest;
import com.rdam.backend.domain.dto.LoginResponse;
import com.rdam.backend.domain.entity.UsuarioInterno;
import com.rdam.backend.security.JwtProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Autenticación de usuarios internos.
 *
 * POST /auth/login
 *
 * No requiere autenticación previa (es público).
 * El SecurityConfig lo configura como permitAll().
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider           jwtProvider;

    /**
     * Autentica un usuario interno y devuelve un JWT de 8 horas.
     *
     * Flujo:
     *   1. AuthenticationManager delega en UserDetailsServiceImpl
     *      para cargar el usuario y en BCrypt para comparar passwords.
     *   2. Si las credenciales son válidas, Spring devuelve
     *      un objeto Authentication con el usuario cargado.
     *   3. Generamos el JWT con los claims de rol y circunscripción.
     *
     * Spring Security lanza AuthenticationException automáticamente
     * si las credenciales son incorrectas o el usuario está inactivo.
     * El GlobalExceptionHandler la convierte en HTTP 401/403.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        // Delega la validación de credenciales a Spring Security
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        // Si llegamos acá, las credenciales son válidas
        UsuarioInterno usuario = (UsuarioInterno) auth.getPrincipal();
        String jwt = jwtProvider.generarToken(usuario);

        return ResponseEntity.ok(new LoginResponse(
            jwt,
            "Bearer",
            28800L,  // 8 horas en segundos
            usuario.getRol().name(),
            usuario.getCircunscripcion() != null
                ? usuario.getCircunscripcion().getId()
                : null
        ));
    }
}
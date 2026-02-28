package com.rdam.backend.security;
import com.rdam.backend.domain.entity.UsuarioInterno;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida JWT para usuarios internos (Operador / Admin).
 *
 * El JWT contiene estos claims:
 *   - sub          → username del usuario
 *   - rol          → OPERADOR | ADMIN
 *   - circunscripcion → ID de la circunscripción (null para ADMIN)
 *   - iat          → issued at (timestamp de emisión)
 *   - exp          → expiration (timestamp de expiración)
 *
 * El secret se inyecta desde la variable de entorno JWT_SECRET.
 * La expiración viene de application.properties (8hs en ms).
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Spring inyecta los valores desde application.properties.
     * @Value("${...}") lee la propiedad y la pasa al constructor.
     *
     * Construimos la SecretKey aquí (una sola vez) en lugar de
     * hacerlo en cada llamada a generateToken() por performance.
     */
    public JwtProvider(
            @Value("${rdam.jwt.secret}") String secret,
            @Value("${rdam.jwt.expiration-ms}") long expirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8)
        );
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un JWT firmado para un usuario interno autenticado.
     *
     * @param usuario El usuario interno recién autenticado.
     * @return JWT como String listo para enviar al cliente.
     */
    public String generarToken(UsuarioInterno usuario) {
        Date ahora     = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                // Identificador del usuario (estándar JWT)
                .subject(usuario.getUsername())
                // Claims personalizados de negocio
                .claim("rol", usuario.getRol().name())
                .claim("circunscripcion",
                        usuario.getCircunscripcion() != null
                            ? usuario.getCircunscripcion().getId()
                            : null)
                .issuedAt(ahora)
                .expiration(expiracion)
                // Firmamos con HMAC-SHA256 y nuestra SecretKey
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrae el username del JWT.
     * El JwtFilter lo usa para cargar el usuario desde la DB.
     *
     * @param token JWT como String (sin el prefijo "Bearer ")
     * @return username del subject
     */
    public String extraerUsername(String token) {
        return parsearClaims(token).getSubject();
    }

    /**
     * Extrae el claim 'circunscripcion' del JWT.
     * Puede ser null para usuarios con rol ADMIN.
     */
    public Integer extraerCircunscripcion(String token) {
        Object valor = parsearClaims(token).get("circunscripcion");
        return valor != null ? ((Number) valor).intValue() : null;
    }

    /**
     * Valida que el JWT sea auténtico y no esté expirado.
     *
     * @return true si el token es válido.
     * @throws TokenInvalidoException si el token es inválido o expiró.
     */
    public boolean validarToken(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new com.rdam.backend.exception.TokenInvalidoException(
                "El JWT expiró. Por favor iniciá sesión nuevamente.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new com.rdam.backend.exception.TokenInvalidoException(
                "JWT inválido.");
        }
    }

    // -------------------------
    // Helper privado
    // -------------------------

    /**
     * Parsea el JWT y devuelve el payload (Claims).
     * Si la firma no coincide o el token expiró, JJWT lanza excepción.
     */
    private Claims parsearClaims(String token) {
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }
}
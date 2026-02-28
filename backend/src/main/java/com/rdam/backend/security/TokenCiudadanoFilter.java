package com.rdam.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de autenticación para ciudadanos (token Redis).
 *
 * Los ciudadanos no tienen JWT. Tienen un token opaco de 64
 * caracteres almacenado en Redis con TTL. Al validar el OTP,
 * el sistema genera este token y lo entrega al ciudadano.
 *
 * Flujo:
 *   1. Extrae el token del header "Authorization: Bearer {token}"
 *   2. Busca la clave "ciudadano:token:{token}" en Redis
 *   3. Si existe, extrae el nroTramite del valor almacenado
 *   4. Setea una autenticación con rol ROLE_CIUDADANO
 *
 * Este filtro corre DESPUÉS de JwtFilter. Si JwtFilter ya
 * seteó una autenticación, este filtro no hace nada.
 *
 * Convención de clave Redis:
 *   KEY:   "ciudadano:token:{tokenAcceso}"
 *   VALUE: "{nroTramite}"
 *   TTL:   configurable (ej: 24 horas)
 */
@Component
@RequiredArgsConstructor
public class TokenCiudadanoFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    // Prefijo de la clave en Redis para evitar colisiones
    // con otras claves (OTP, configuraciones, etc.)
    private static final String PREFIJO_TOKEN = "ciudadano:token:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Si JwtFilter ya autenticó al usuario (usuario interno),
        // este filtro no interviene.
        if (SecurityContextHolder.getContext()
                                 .getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extraerToken(request);

        if (token != null) {
            // Buscamos el token en Redis.
            // Si no existe o expiró, Redis devuelve null.
            String nroTramite = redisTemplate.opsForValue()
                                             .get(PREFIJO_TOKEN + token);

            if (StringUtils.hasText(nroTramite)) {
                // Token válido: creamos una autenticación mínima
                // con el nroTramite como "principal" y rol CIUDADANO.
                // Los controladores pueden obtener el nroTramite así:
                //   String nro = (String) authentication.getPrincipal();
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        nroTramite,  // principal = nroTramite
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CIUDADANO"))
                    );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
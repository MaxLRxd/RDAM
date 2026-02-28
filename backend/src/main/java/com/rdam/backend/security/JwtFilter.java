package com.rdam.backend.security;

import com.rdam.backend.exception.TokenInvalidoException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación para usuarios internos (JWT).
 *
 * Se ejecuta UNA VEZ por request (OncePerRequestFilter).
 *
 * Flujo:
 *   1. Extrae el token del header "Authorization: Bearer {jwt}"
 *   2. Valida el JWT con JwtProvider
 *   3. Carga el usuario desde DB con UserDetailsServiceImpl
 *   4. Verifica que el usuario sigue activo (isEnabled())
 *   5. Setea la autenticación en el SecurityContext
 *
 * Si algo falla, NO lanza excepción: simplemente no setea
 * la autenticación y deja que SecurityConfig decida si la
 * URL requiere auth (en cuyo caso devuelve 401/403).
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extraerToken(request);

        // Si no hay token JWT en el header, pasamos al siguiente filtro.
        // TokenCiudadanoFilter se encargará si corresponde.
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Valida firma y expiración. Lanza TokenInvalidoException si falla.
            jwtProvider.validarToken(token);

            String username = jwtProvider.extraerUsername(token);

            // Solo procesamos si no hay autenticación previa en el contexto
            if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

                // Carga el usuario desde DB para verificar que sigue activo.
                // Si activo=false, isEnabled() devuelve false
                // y Spring Security rechaza con 403.
                UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,                        // credentials: null post-auth
                            userDetails.getAuthorities() // ROLE_ADMIN / ROLE_OPERADOR
                        );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                            .buildDetails(request)
                    );

                    // Registra la autenticación para este request.
                    // A partir de acá, @PreAuthorize y hasRole() funcionan.
                    SecurityContextHolder.getContext()
                                        .setAuthentication(authToken);
                }
            }

        } catch (TokenInvalidoException e) {
            // Token inválido: limpiamos el contexto y seguimos.
            // SecurityConfig devolverá 401 si la URL lo requiere.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token del header Authorization.
     * Formato esperado: "Bearer eyJhbGci..."
     *
     * @return el token sin el prefijo "Bearer ", o null si no existe.
     */
    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7); // Remueve "Bearer "
        }
        return null;
    }
}
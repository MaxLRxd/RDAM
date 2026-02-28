package com.rdam.backend.security;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security.
 *
 * @EnableMethodSecurity: habilita @PreAuthorize en los
 * controladores para control de acceso por rol/circunscripción.
 *
 * Decisiones de diseño:
 *   - Sin sesiones HTTP (STATELESS): cada request se autentica
 *     por sí solo con su token. No hay HttpSession.
 *   - Sin CSRF: no aplica en APIs REST stateless.
 *   - Sin formLogin ni httpBasic: tenemos nuestros propios filtros.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final TokenCiudadanoFilter tokenCiudadanoFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Sin CSRF: no aplica en APIs REST con tokens.
            // CSRF protege contra ataques en formularios web con cookies.
            .csrf(AbstractHttpConfigurer::disable)

            // Sin sesiones HTTP. Cada request se autentica de forma
            // independiente. El estado vive en Redis (ciudadanos)
            // o en el JWT (usuarios internos).
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ------------------------------------------------
            // Reglas de autorización por URL
            // Orden importa: Spring evalúa de arriba hacia abajo
            // y aplica la primera regla que coincide.
            // ------------------------------------------------
            .authorizeHttpRequests(auth -> auth

                // ENDPOINTS PÚBLICOS — sin autenticación
                .requestMatchers(HttpMethod.POST,
                    "/solicitudes",
                    "/solicitudes/*/validar",
                    "/auth/login"
                ).permitAll()

                .requestMatchers(HttpMethod.GET,
                    "/health",
                    "/certificados/*"   // token en URL, validación en el servicio
                ).permitAll()

                // Página mock de pago (solo en modo simulación)
                .requestMatchers("/sim/**").permitAll()

                // ENDPOINTS CIUDADANO — requieren token Redis
                .requestMatchers(HttpMethod.GET,
                    "/solicitudes/*"
                ).hasRole("CIUDADANO")

                .requestMatchers(HttpMethod.POST,
                    "/solicitudes/*/pago/crear"
                ).hasRole("CIUDADANO")

                // ENDPOINTS WEBHOOK — validación HMAC en el servicio
                .requestMatchers(HttpMethod.POST,
                    "/webhooks/pluspagos"
                ).permitAll()

                // ENDPOINTS PANEL INTERNO — requieren JWT
                .requestMatchers(HttpMethod.GET,
                    "/solicitudes"
                ).hasAnyRole("OPERADOR", "ADMIN")

                .requestMatchers(HttpMethod.POST,
                    "/solicitudes/*/certificado",
                    "/solicitudes/*/certificado/regenerar-token"
                ).hasAnyRole("OPERADOR", "ADMIN")

                // ENDPOINTS ADMIN — solo ADMIN
                .requestMatchers(HttpMethod.POST,
                    "/usuarios-internos"
                ).hasRole("ADMIN")

                .requestMatchers(HttpMethod.PATCH,
                    "/usuarios-internos/*/estado"
                ).hasRole("ADMIN")

                // Cualquier otra URL no configurada explícitamente:
                // requiere autenticación como medida de seguridad por defecto.
                .anyRequest().authenticated()
            )

            // ------------------------------------------------
            // Registro de filtros personalizados
            // Se insertan ANTES del filtro estándar de Spring.
            // Orden: JwtFilter → TokenCiudadanoFilter → resto
            // ------------------------------------------------
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(tokenCiudadanoFilter, JwtFilter.class);

        return http.build();
    }

    /**
     * BCrypt con cost=12 para hashear contraseñas.
     * Cost 12 significa 2^12 = 4096 iteraciones.
     * Más cost = más seguro pero más lento (intencional).
     * En login esto agrega ~300ms, que es aceptable.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationManager: orquesta el proceso de login.
     * Lo necesita AuthService para autenticar credenciales
     * de usuario/contraseña en POST /auth/login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
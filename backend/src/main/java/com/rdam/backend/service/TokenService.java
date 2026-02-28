package com.rdam.backend.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * Gestiona todos los tokens efímeros del sistema en Redis.
 *
 * Responsabilidad única: abstraer las operaciones de Redis
 * (GET, SET con TTL, DELETE) y centralizar las convenciones
 * de naming de claves y duración de tokens.
 *
 * Ningún otro servicio interactúa con Redis directamente.
 * Todos pasan por acá.
 *
 * Tipos de tokens gestionados:
 *   1. OTP (código numérico de 6 dígitos) — validación de email
 *   2. Token de acceso ciudadano (UUID de 64 chars) — sesión
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    // -------------------------------------------------------
    // Constantes de naming y TTL
    // Centralizadas acá para que un cambio afecte todo el sistema
    // -------------------------------------------------------

    private static final String PREFIJO_OTP            = "otp:";
    private static final String PREFIJO_TOKEN_CIUDADANO = "ciudadano:token:";
    private static final String PREFIJO_INTENTOS_OTP   = "otp:intentos:";

    // TTL del código OTP: 15 minutos (SPEC: HU02)
    private static final Duration TTL_OTP = Duration.ofMinutes(15);

    // TTL del token de sesión del ciudadano: 24 horas
    private static final Duration TTL_TOKEN_CIUDADANO = Duration.ofHours(24);

    // Máximo de intentos de validación OTP (SPEC: HU02 — máx 3 intentos)
    public static final int MAX_INTENTOS_OTP = 3;

    // -------------------------------------------------------
    // OTP — Código de validación de email
    // -------------------------------------------------------

    /**
     * Genera un código OTP numérico de 6 dígitos y lo almacena
     * en Redis con TTL de 15 minutos.
     *
     * Usamos SecureRandom (no Math.random) porque es
     * criptográficamente seguro. Math.random es predecible.
     *
     * @param idSolicitud ID de la solicitud asociada al OTP.
     * @return El código OTP generado (para enviarlo por email).
     */
    public String generarYGuardarOtp(Long idSolicitud) {
        SecureRandom random = new SecureRandom();
        // Genera número entre 100000 y 999999 (siempre 6 dígitos)
        String otp = String.valueOf(100000 + random.nextInt(900000));

        redisTemplate.opsForValue().set(
            PREFIJO_OTP + idSolicitud,
            otp,
            TTL_OTP
        );

        // Inicializamos el contador de intentos en 0
        redisTemplate.opsForValue().set(
            PREFIJO_INTENTOS_OTP + idSolicitud,
            "0",
            TTL_OTP
        );

        return otp;
    }

    /**
     * Valida el OTP ingresado por el ciudadano.
     *
     * Incrementa el contador de intentos en cada llamada.
     * Si supera MAX_INTENTOS_OTP, invalida el OTP automáticamente.
     *
     * @return resultado de la validación con detalle del error si falla
     */
    public ResultadoValidacionOtp validarOtp(Long idSolicitud, String otpIngresado) {
        String claveOtp      = PREFIJO_OTP + idSolicitud;
        String claveIntentos = PREFIJO_INTENTOS_OTP + idSolicitud;

        // ¿El OTP existe en Redis? Si no existe, expiró.
        String otpGuardado = redisTemplate.opsForValue().get(claveOtp);
        if (otpGuardado == null) {
            return ResultadoValidacionOtp.EXPIRADO;
        }

        // Incrementamos el contador de intentos
        Long intentos = redisTemplate.opsForValue().increment(claveIntentos);

        // ¿Superó el límite de intentos?
        if (intentos != null && intentos > MAX_INTENTOS_OTP) {
            // Invalidamos el OTP inmediatamente
            eliminarOtp(idSolicitud);
            return ResultadoValidacionOtp.INTENTOS_AGOTADOS;
        }

        // ¿El código coincide?
        if (!otpGuardado.equals(otpIngresado)) {
            return ResultadoValidacionOtp.INCORRECTO;
        }

        // Válido: eliminamos el OTP para que no pueda reutilizarse
        eliminarOtp(idSolicitud);
        return ResultadoValidacionOtp.VALIDO;
    }

    /**
     * Elimina el OTP y su contador de intentos de Redis.
     * Se llama tras validación exitosa o al agotar intentos.
     */
    public void eliminarOtp(Long idSolicitud) {
        redisTemplate.delete(PREFIJO_OTP + idSolicitud);
        redisTemplate.delete(PREFIJO_INTENTOS_OTP + idSolicitud);
    }

    // -------------------------------------------------------
    // Token de acceso ciudadano
    // -------------------------------------------------------

    /**
     * Genera un token de acceso para el ciudadano y lo guarda
     * en Redis vinculado al nroTramite.
     *
     * El token es un UUID sin guiones (32 chars) + otro UUID (32 chars)
     * = 64 caracteres hexadecimales. Criptográficamente aleatorio.
     *
     * @param nroTramite Número de trámite que este token representa.
     * @return El token generado para enviar al ciudadano.
     */
    public String generarTokenCiudadano(String nroTramite) {
        String token = generarToken64();

        redisTemplate.opsForValue().set(
            PREFIJO_TOKEN_CIUDADANO + token,
            nroTramite,
            TTL_TOKEN_CIUDADANO
        );

        return token;
    }

    /**
     * Verifica si un token ciudadano existe en Redis.
     *
     * @return el nroTramite asociado, o null si el token no existe.
     */
    public String obtenerNroTramitePorToken(String token) {
        return redisTemplate.opsForValue()
                            .get(PREFIJO_TOKEN_CIUDADANO + token);
    }

    /**
     * Elimina el token ciudadano de Redis.
     * Útil si en el futuro se implementa "cerrar sesión".
     */
    public void eliminarTokenCiudadano(String token) {
        redisTemplate.delete(PREFIJO_TOKEN_CIUDADANO + token);
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    /**
     * Genera un token aleatorio de 64 caracteres hexadecimales.
     * Combina dos UUID v4 (que usan SecureRandom internamente).
     */
    private String generarToken64() {
        return UUID.randomUUID().toString().replace("-", "")
             + UUID.randomUUID().toString().replace("-", "");
    }

    // -------------------------------------------------------
    // Enum de resultado de validación OTP
    // -------------------------------------------------------

    /**
     * Resultado posible de validarOtp().
     * El servicio que llama decide qué HTTP status devolver
     * según este resultado.
     *
     * Lo definimos como enum anidado porque solo tiene
     * sentido en el contexto de TokenService.
     */
    public enum ResultadoValidacionOtp {
        /** El código coincide y era válido */
        VALIDO,
        /** El código no coincide pero aún quedan intentos */
        INCORRECTO,
        /** El código expiró (TTL de Redis venció) */
        EXPIRADO,
        /** Se superó el límite de intentos. OTP invalidado. */
        INTENTOS_AGOTADOS
    }
}
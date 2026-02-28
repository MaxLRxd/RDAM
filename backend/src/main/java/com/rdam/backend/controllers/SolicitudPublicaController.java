package com.rdam.backend.controllers;
import com.rdam.backend.domain.dto.CrearSolicitudRequest;
import com.rdam.backend.domain.dto.ValidarOtpRequest;
import com.rdam.backend.domain.dto.CrearSolicitudResponse;
import com.rdam.backend.domain.dto.SolicitudEstadoResponse;
import com.rdam.backend.domain.dto.ValidarOtpResponse;
import com.rdam.backend.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints del portal ciudadano.
 *
 * POST /solicitudes              → crear solicitud
 * POST /solicitudes/{id}/validar → validar OTP
 * GET  /solicitudes/{nroTramite} → consultar estado (requiere token ciudadano)
 */
@RestController
@RequestMapping("/solicitudes")
@RequiredArgsConstructor
public class SolicitudPublicaController {

    private final SolicitudService solicitudService;

    /**
     * Crea una nueva solicitud de certificado RDAM.
     * Público: no requiere autenticación.
     *
     * Antes de llamar a este endpoint, el frontend debe
     * haber mostrado el popup de confirmación de arancel.
     *
     * HTTP 201: solicitud creada, OTP enviado por email.
     */
    @PostMapping
    public ResponseEntity<CrearSolicitudResponse> crear(
            @Valid @RequestBody CrearSolicitudRequest request) {

        CrearSolicitudResponse response = solicitudService.crearSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Valida el código OTP recibido por email.
     * Público: no requiere autenticación previa.
     *
     * HTTP 200: OTP válido, devuelve tokenAcceso para el ciudadano.
     * HTTP 401: OTP incorrecto, expirado o intentos agotados.
     *           (lanzado por SolicitudService via TokenInvalidoException)
     */
    @PostMapping("/{id}/validar")
    public ResponseEntity<ValidarOtpResponse> validarOtp(
            @PathVariable Long id,
            @Valid @RequestBody ValidarOtpRequest request) {

        ValidarOtpResponse response =
            solicitudService.validarOtp(id, request.getCodigo());
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta el estado de una solicitud.
     * Requiere token de sesión ciudadano (Bearer en header).
     * El SecurityConfig define que esta URL requiere ROLE_CIUDADANO.
     *
     * El TokenCiudadanoFilter ya validó el token antes de llegar acá.
     *
     * HTTP 200: estado actual del trámite.
     * HTTP 401: token inválido o expirado.
     * HTTP 404: nroTramite no encontrado.
     */
    @GetMapping("/{nroTramite}")
    public ResponseEntity<SolicitudEstadoResponse> consultarEstado(
            @PathVariable String nroTramite) {

        SolicitudEstadoResponse response =
            solicitudService.consultarEstado(nroTramite);
        return ResponseEntity.ok(response);
    }
}
package com.rdam.backend.controllers;
import com.rdam.backend.service.PagoService;
import com.rdam.backend.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints de integración con PlusPagos.
 *
 * POST /solicitudes/{id}/pago/crear → crear orden de pago
 *
 * Requiere token ciudadano (ROLE_CIUDADANO).
 */
@RestController
@RequestMapping("/solicitudes")
@RequiredArgsConstructor
public class PagoController {

    private final SolicitudService solicitudService;

    /**
     * Genera una orden de pago para la solicitud.
     *
     * En modo simulación devuelve una urlPago mock.
     * En modo real devolvería la URL de PlusPagos.
     *
     * El ciudadano es redirigido a urlPago para completar el pago.
     *
     * HTTP 200: orden creada, devuelve urlPago e idOrdenPago.
     * HTTP 400: la solicitud no está en estado PENDIENTE.
     * HTTP 401: token de sesión inválido.
     */
    @PostMapping("/{id}/pago/crear")
    public ResponseEntity<Map<String, Object>> crearOrdenPago(
            @PathVariable Long id) {

        PagoService.ResultadoOrdenPago resultado =
            solicitudService.crearOrdenPago(id);

        // Devolvemos un Map para mantener flexibilidad en el contrato
        // y alinearnos con la estructura descrita en los endpoints.
        return ResponseEntity.ok(Map.of(
            "idOrdenPago",      resultado.idOrdenPago(),
            "urlPago",          resultado.urlPago(),
            "modoSimulacion",   resultado.modoSimulacion()
        ));
    }
}
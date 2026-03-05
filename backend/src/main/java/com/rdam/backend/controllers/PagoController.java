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
 *
 * La respuesta incluye formularioDatos: un mapa de campos (algunos
 * encriptados con AES-256-CBC) que el frontend usa para construir
 * un form HTML con POST automático hacia la pasarela.
 *
 * Ejemplo de uso en el frontend:
 *   const form = document.createElement('form');
 *   form.method = 'POST';
 *   form.action = resultado.urlPago;
 *   for (const [key, value] of Object.entries(resultado.formularioDatos)) {
 *     const input = document.createElement('input');
 *     input.name = key;
 *     input.value = value;
 *     form.appendChild(input);
 *   }
 *   document.body.appendChild(form);
 *   form.submit();
 */
@RestController
@RequestMapping("/solicitudes")
@RequiredArgsConstructor
public class PagoController {

    private final SolicitudService solicitudService;

    /**
     * Genera una orden de pago para la solicitud.
     *
     * HTTP 200: orden creada. Devuelve urlPago, idOrdenPago y formularioDatos.
     * HTTP 400: la solicitud no está en estado PENDIENTE.
     * HTTP 401: token de sesión inválido o expirado.
     */
    @PostMapping("/{id}/pago/crear")
    public ResponseEntity<Map<String, Object>> crearOrdenPago(
            @PathVariable Long id) {

        PagoService.ResultadoOrdenPago resultado =
            solicitudService.crearOrdenPago(id);

        return ResponseEntity.ok(Map.of(
            "idOrdenPago",      resultado.idOrdenPago(),
            "urlPago",          resultado.urlPago(),
            "modoSimulacion",   resultado.modoSimulacion(),
            "formularioDatos",  resultado.formularioDatos()
        ));
    }
}
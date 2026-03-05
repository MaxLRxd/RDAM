package com.rdam.backend.controllers;

import com.rdam.backend.domain.dto.CallbackPlusPagosRequest;
import com.rdam.backend.domain.dto.WebhookPlusPagosRequest;
import com.rdam.backend.service.PagoService;
import com.rdam.backend.service.SolicitudService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Recibe notificaciones asíncronas de PlusPagos.
 *
 * POST /webhooks/pluspagos          → webhook global (busca por idOrdenPago)
 * POST /webhooks/pluspagos/callback → callback por transacción (busca por nroTramite)
 *
 * Ambos endpoints son públicos en SecurityConfig pero protegidos
 * por validación HMAC-SHA256. Si la firma no es válida → HTTP 401.
 *
 * Siempre devuelven HTTP 200 al final para confirmarle a PlusPagos
 * que la notificación fue recibida, independientemente del resultado
 * del pago (aprobado o rechazado).
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final SolicitudService solicitudService;
    private final PagoService      pagoService;

    /**
     * Webhook global de PlusPagos.
     * Formato PascalCase. Busca la solicitud por idOrdenPago.
     *
     * @param firma      Header X-PlusPagos-Signature con la firma HMAC del payload.
     * @param payload    Body deserializado por Jackson.
     * @param httpRequest Request original (para leer el body raw y validar HMAC).
     */
    @PostMapping("/pluspagos")
    public ResponseEntity<Void> recibirWebhook(
            @RequestHeader(value = "X-PlusPagos-Signature",
                        required = false) String firma,
            @RequestBody WebhookPlusPagosRequest payload,
            HttpServletRequest httpRequest) {

        // ContentCachingRequestWrapper guarda el body después de que
        // Jackson lo leyó para deserializar el @RequestBody.
        // Lo obtenemos con getContentAsByteArray(), no con getInputStream().
        ContentCachingRequestWrapper wrapper =
            (ContentCachingRequestWrapper) httpRequest;

        String rawBody = new String(
            wrapper.getContentAsByteArray(),
            StandardCharsets.UTF_8
        );

        if (!pagoService.validarFirmaHmac(rawBody, firma)) {
            log.warn("Webhook rechazado: firma HMAC inválida.");
            return ResponseEntity.status(401).build();
        }

        int codigoEstado = Integer.parseInt(payload.getEstadoId());
        BigDecimal monto = new BigDecimal(payload.getMonto());

        solicitudService.procesarWebhookPago(
            payload.getTransaccionComercioId(),
            codigoEstado,
            monto
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Callback por transacción de PlusPagos.
     * Formato camelCase. Busca la solicitud por nroTramite (comercioId).
     *
     * Este endpoint es llamado directamente por el mock usando la URL
     * encriptada en CallbackSuccess / CallbackCancel del formulario de pago.
     *
     * El campo "estado" viene como "approved" o "rejected" (string),
     * a diferencia del webhook global que usa códigos numéricos.
     *
     * @param firma      Header X-PlusPagos-Signature con la firma HMAC del payload.
     * @param payload    Body deserializado por Jackson.
     * @param httpRequest Request original (para leer el body raw y validar HMAC).
     */
    @PostMapping("/pluspagos/callback")
    public ResponseEntity<Void> recibirCallback(
            @RequestHeader(value = "X-PlusPagos-Signature",
                        required = false) String firma,
            @RequestBody CallbackPlusPagosRequest payload,
            HttpServletRequest httpRequest) {

        ContentCachingRequestWrapper wrapper =
            (ContentCachingRequestWrapper) httpRequest;

        String rawBody = new String(
            wrapper.getContentAsByteArray(),
            StandardCharsets.UTF_8
        );

        if (!pagoService.validarFirmaHmac(rawBody, firma)) {
            log.warn("Callback rechazado: firma HMAC inválida.");
            return ResponseEntity.status(401).build();
        }

        // Mapeamos el string "approved"/"rejected" al código numérico
        // que espera interpretarCodigoEstado():
        //   "approved" → 0 → APROBADO → PAGADO
        //   cualquier otro valor → 4 → RECHAZADO → VENCIDO
        int codigoEstado = "approved".equalsIgnoreCase(payload.getEstado()) ? 0 : 4;
        BigDecimal monto = new BigDecimal(payload.getMonto());

        solicitudService.procesarCallbackPago(
            payload.getComercioId(),  // nroTramite
            codigoEstado,
            monto
        );

        return ResponseEntity.ok().build();
    }
}
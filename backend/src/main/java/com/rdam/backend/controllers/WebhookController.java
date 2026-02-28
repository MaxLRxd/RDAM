package com.rdam.backend.controllers;
import com.rdam.backend.domain.dto.WebhookPlusPagosRequest;
import com.rdam.backend.service.PagoService;
import com.rdam.backend.service.SolicitudService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Recibe notificaciones asíncronas de PlusPagos.
 *
 * POST /webhooks/pluspagos
 *
 * Público en SecurityConfig, pero protegido por
 * validación HMAC-SHA256 en este controlador.
 * Si la firma no es válida → HTTP 401.
 *
 * El controlador valida la firma ANTES de llamar
 * al servicio. El servicio recibe datos ya verificados.
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final SolicitudService solicitudService;
    private final PagoService      pagoService;

    /**
     * Procesa la notificación de resultado de pago.
     *
     * La firma HMAC-SHA256 viene en el header X-PlusPagos-Signature.
     * Se calcula sobre el body raw del request.
     *
     * Siempre devuelve HTTP 200 si llegamos al final,
     * aunque el pago haya sido rechazado. PlusPagos espera
     * un 200 para confirmar que recibimos la notificación.
     *
     * @param firma   Header con la firma HMAC del payload.
     * @param payload Body del request (ya deserializado por Jackson).
     * @param rawBody Body como String para recalcular la firma.
     */
    @PostMapping("/pluspagos")
    public ResponseEntity<Void> recibirWebhook(
            @RequestHeader(value = "X-PlusPagos-Signature",
                        required = false) String firma,
            @RequestBody WebhookPlusPagosRequest payload,
            HttpServletRequest httpRequest) throws IOException {

        // Leer el body como String para validar la firma
        String rawBody = new String(
            httpRequest.getInputStream().readAllBytes(),
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
}
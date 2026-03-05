package com.rdam.backend.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload del callback por transacción de PlusPagos.
 * POST /api/v1/webhooks/pluspagos/callback
 *
 * A diferencia del webhook global (WebhookPlusPagosRequest),
 * este formato usa camelCase y viene del callback directo
 * configurado por transacción (CallbackSuccess / CallbackCancel).
 *
 * Campos clave:
 *   - comercioId → nroTramite de la solicitud (usado para buscarla)
 *   - estado     → "approved" | "rejected"
 */
@Getter
@Setter
@NoArgsConstructor
public class CallbackPlusPagosRequest {

    private Long transaccionId;

    /** nroTramite de la solicitud (ej: RDAM-20260305-0001) */
    private String comercioId;

    private String monto;

    /** "approved" o "rejected" */
    private String estado;

    private String fecha;
}
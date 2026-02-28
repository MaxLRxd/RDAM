package com.rdam.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload del webhook de PlusPagos.
 * POST /api/v1/webhooks/pluspagos
 *
 * Los nombres de campo usan la convención de PlusPagos
 * (PascalCase). @JsonProperty mapea el JSON entrante
 * a los campos Java.
 */
@Getter
@Setter
@NoArgsConstructor
public class WebhookPlusPagosRequest {

    @JsonProperty("TransaccionComercioId")
    private String transaccionComercioId;

    @JsonProperty("EstadoId")
    private String estadoId;

    @JsonProperty("Monto")
    private String monto;

    @JsonProperty("TransaccionPlataformaId")
    private String transaccionPlataformaId;
}
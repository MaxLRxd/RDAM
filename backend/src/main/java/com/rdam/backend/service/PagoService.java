package com.rdam.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Gestiona la integración con la pasarela de pagos PlusPagos.
 *
 * Responsabilidades:
 *   - Generar órdenes de pago (real o simulada)
 *   - Validar la firma HMAC-SHA256 del webhook entrante
 *   - Mapear códigos de estado PlusPagos a estados del sistema
 *
 * El modo de operación se controla con la variable PAYMENT_MODE.
 * En modo 'sim' no se realizan llamadas HTTP externas.
 */
@Service
@Slf4j
public class PagoService {

    @Value("${rdam.payment.mode}")
    private String paymentMode;

    @Value("${rdam.payment.hmac-secret}")
    private String hmacSecret;

    @Value("${rdam.payment.merchant-guid}")
    private String merchantGuid;

    @Value("${rdam.payment.api-url}")
    private String apiUrl;

    // Algoritmo HMAC usado por PlusPagos
    private static final String HMAC_ALGORITMO = "HmacSHA256";

    /**
     * Genera una orden de pago para una solicitud.
     *
     * En modo simulación devuelve datos mock.
     * En modo real generaría la orden firmada en PlusPagos.
     *
     * @param idSolicitud  ID interno de la solicitud.
     * @param nroTramite   Número de trámite (se usa como ID de comercio).
     * @param montoCentavos Monto en centavos (ej: 150000 = $1500.00).
     * @return Resultado con la URL de pago e ID de la orden.
     */
    public ResultadoOrdenPago crearOrdenPago(Long idSolicitud,
                                              String nroTramite,
                                              long montoCentavos) {
        if ("sim".equalsIgnoreCase(paymentMode)) {
            return crearOrdenSimulada(idSolicitud, nroTramite);
        }

        // Modo real: aquí iría la integración con PlusPagos
        // Por ahora lanzamos excepción hasta que se implemente
        throw new UnsupportedOperationException(
            "Modo REAL de PlusPagos no implementado en esta versión."
        );
    }

    /**
     * Valida la firma HMAC-SHA256 del webhook de PlusPagos.
     *
     * PlusPagos firma el payload con el HMAC secret compartido
     * y lo envía en el header X-PlusPagos-Signature.
     * Nosotros recalculamos la firma y comparamos.
     *
     * Si las firmas no coinciden → el webhook no viene de PlusPagos
     * → rechazamos con 401.
     *
     * En modo simulación usamos el secret 'dev-secret'.
     *
     * @param payload   Cuerpo del request como String.
     * @param firma     Valor del header X-PlusPagos-Signature.
     * @return true si la firma es válida.
     */
    public boolean validarFirmaHmac(String payload, String firma) {
        if (firma == null || firma.isBlank()) {
            log.warn("Webhook recibido sin header de firma HMAC.");
            return false;
        }

        try {
            String firmaCalculada = calcularHmac(payload, hmacSecret);

            // Comparación en tiempo constante para evitar timing attacks.
            // No usamos .equals() porque su tiempo varía según la posición
            // del primer carácter diferente, lo que permite ataques de timing.
            boolean valida = MessageDigest.isEqual(
                firmaCalculada.getBytes(StandardCharsets.UTF_8),
                firma.getBytes(StandardCharsets.UTF_8)
            );

            if (!valida) {
                log.warn("Firma HMAC inválida en webhook. firma_recibida={}",
                        firma);
            }

            return valida;

        } catch (Exception e) {
            log.error("Error validando firma HMAC: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Mapea el código de estado de PlusPagos al resultado
     * que el sistema RDAM necesita para cambiar el estado.
     *
     * Códigos según SPEC.md sección 7:
     *   0  → APROBADA  → solicitud pasa a PAGADO
     *   4  → RECHAZADA → solicitud pasa a VENCIDO
     *   7  → EXPIRADA  → solicitud pasa a VENCIDO
     *   8  → CANCELADA → solicitud pasa a VENCIDO
     *   9  → DEVUELTA  → solicitud pasa a VENCIDO
     *   11 → VENCIDA   → solicitud pasa a VENCIDO
     *
     * @param codigoEstado Código numérico recibido en el webhook.
     * @return ResultadoPago.APROBADO o ResultadoPago.RECHAZADO
     */
    public ResultadoPago interpretarCodigoEstado(int codigoEstado) {
        return switch (codigoEstado) {
            case 0 -> ResultadoPago.APROBADO;
            case 4, 7, 8, 9, 11 -> ResultadoPago.RECHAZADO;
            default -> {
                log.warn("Código de estado PlusPagos desconocido: {}",
                        codigoEstado);
                yield ResultadoPago.RECHAZADO;
            }
        };
    }

    // -------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------

    /**
     * Genera una orden de pago simulada.
     * No llama a ninguna API externa.
     */
    private ResultadoOrdenPago crearOrdenSimulada(Long idSolicitud,
                                                   String nroTramite) {
        String idOrden = "SIM-" + UUID.randomUUID()
                                      .toString()
                                      .replace("-", "")
                                      .substring(0, 16)
                                      .toUpperCase();

        String urlPago = apiUrl + "/sim/pago/" + idOrden;

        log.info("Orden de pago SIMULADA creada. idSolicitud={} idOrden={} url={}",
                idSolicitud, idOrden, urlPago);

        return new ResultadoOrdenPago(idOrden, urlPago, true);
    }

    /**
     * Calcula el HMAC-SHA256 de un payload con un secret dado.
     *
     * @return HMAC en formato hexadecimal lowercase.
     */
    private String calcularHmac(String payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac mac = Mac.getInstance(HMAC_ALGORITMO);
        SecretKeySpec keySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            HMAC_ALGORITMO
        );
        mac.init(keySpec);
        byte[] hashBytes = mac.doFinal(
            payload.getBytes(StandardCharsets.UTF_8)
        );
        return HexFormat.of().formatHex(hashBytes);
    }

    // -------------------------------------------------------
    // Tipos de resultado (records — Java 16+)
    // -------------------------------------------------------

    /**
     * Resultado de crear una orden de pago.
     * Record: clase inmutable con getter automático por Java.
     */
    public record ResultadoOrdenPago(
            String idOrdenPago,
            String urlPago,
            boolean modoSimulacion
    ) {}

    /**
     * Resultado de interpretar un webhook de PlusPagos.
     */
    public enum ResultadoPago {
        APROBADO,
        RECHAZADO
    }
}
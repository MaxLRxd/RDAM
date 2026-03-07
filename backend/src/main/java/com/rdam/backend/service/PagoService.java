package com.rdam.backend.service;

import com.rdam.backend.config.PlusPagosCrypto;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona la integración con la pasarela de pagos PlusPagos.
 *
 * Responsabilidades:
 *   - Generar órdenes de pago con campos AES-256-CBC encriptados
 *   - Validar la firma HMAC-SHA256 del webhook entrante
 *   - Mapear códigos de estado PlusPagos a estados del sistema
 *
 * El modo de operación se controla con PAYMENT_MODE:
 *   - 'sim': apunta al mock local en Docker (localhost:3000)
 *   - 'real': apuntaría a PlusPagos producción (no implementado)
 *
 * En ambos modos, el flujo de encriptación y el form POST son reales.
 * La diferencia es solo la URL destino.
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

    @Value("${rdam.payment.secret-key}")
    private String secretKey;

    @Value("${rdam.payment.api-url}")
    private String apiUrl;

    @Value("${rdam.payment.public-url}")
    private String publicUrl;

    @Value("${server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    // Algoritmo HMAC usado por PlusPagos
    private static final String HMAC_ALGORITMO = "HmacSHA256";

    /**
     * Genera una orden de pago para una solicitud.
     *
     * Produce los campos encriptados necesarios para que el frontend
     * construya el form POST hacia la pasarela (mock o real).
     *
     * El campo CallbackSuccess encripta la URL del backend para que
     * la pasarela notifique automáticamente el resultado del pago.
     *
     * @param idSolicitud   ID interno de la solicitud.
     * @param nroTramite    Número de trámite (TransaccionComercioId del form).
     * @param montoCentavos Monto en centavos (ej: 150000 = $1500.00).
     * @return Resultado con urlPago, idOrdenPago y formularioDatos encriptados.
     */
    public ResultadoOrdenPago crearOrdenPago(Long idSolicitud,
                                              String nroTramite,
                                              long montoCentavos) {
        if ("sim".equalsIgnoreCase(paymentMode)) {
            return crearOrdenConFormulario(idSolicitud, nroTramite, montoCentavos);
        }

        // Modo real: aquí iría la integración con PlusPagos producción.
        throw new UnsupportedOperationException(
            "Modo REAL de PlusPagos no implementado en esta versión."
        );
    }

    /**
     * Valida la firma HMAC-SHA256 del webhook/callback de PlusPagos.
     *
     * En modo SIM se bypasea la validación porque el mock no firma
     * los callbacks (ver sendCallback en server.js del mock).
     * En modo real se valida estrictamente.
     *
     * @param payload Cuerpo del request como String.
     * @param firma   Valor del header X-PlusPagos-Signature.
     * @return true si la firma es válida (o si estamos en modo SIM).
     */
    public boolean validarFirmaHmac(String payload, String firma) {
        // En modo SIM el mock no agrega firma HMAC en los callbacks.
        // Ver función sendCallback() en pluspagos-mock/server.js.
        if ("sim".equalsIgnoreCase(paymentMode)) {
            log.debug("Modo SIM: validación HMAC omitida.");
            return true;
        }

        if (firma == null || firma.isBlank()) {
            log.warn("Webhook recibido sin header de firma HMAC.");
            return false;
        }

        try {
            String firmaCalculada = calcularHmac(payload, hmacSecret);

            // Comparación en tiempo constante para evitar timing attacks.
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
     *   3  → REALIZADA → solicitud pasa a PAGADO (código del mock)
     *   4  → RECHAZADA → solicitud pasa a VENCIDO
     *   7  → EXPIRADA  → solicitud pasa a VENCIDO
     *   8  → CANCELADA → solicitud pasa a VENCIDO
     *   9  → DEVUELTA  → solicitud pasa a VENCIDO
     *   11 → VENCIDA   → solicitud pasa a VENCIDO
     */
    public ResultadoPago interpretarCodigoEstado(int codigoEstado) {
        return switch (codigoEstado) {
            case 0, 3 -> ResultadoPago.APROBADO;
            case 4, 7, 8, 9, 11 -> ResultadoPago.RECHAZADO;
            default -> {
                log.warn("Código de estado PlusPagos desconocido: {}. " +
                        "Se trata como RECHAZADO por seguridad.", codigoEstado);
                yield ResultadoPago.RECHAZADO;
            }
        };
    }

    // -------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------

    /**
     * Genera la orden de pago con todos los campos del formulario encriptados.
     *
     * Este método produce el formularioDatos que el frontend usa para
     * construir un form HTML con POST automático hacia la pasarela.
     *
     * Campos encriptados con AES-256-CBC (PlusPagosCrypto):
     *   - Monto:           monto en centavos como string
     *   - CallbackSuccess: URL que el mock llamará cuando el pago sea aprobado
     *   - CallbackCancel:  URL que el mock llamará cuando el pago sea rechazado
     *   - UrlSuccess:      URL a la que el mock redirigirá al usuario si aprueba
     *   - UrlError:        URL a la que el mock redirigirá al usuario si rechaza
     *
     * Campos en texto plano:
     *   - Comercio:              GUID del comercio (merchantGuid)
     *   - TransaccionComercioId: nroTramite (lo usa el mock como comercioId en el callback)
     *   - Informacion:           descripción del pago
     */
    private ResultadoOrdenPago crearOrdenConFormulario(Long idSolicitud,
                                                        String nroTramite,
                                                        long montoCentavos) {
        String idOrden = "SIM-" + UUID.randomUUID()
                                      .toString()
                                      .replace("-", "")
                                      .substring(0, 16)
                                      .toUpperCase();

        // URL pública accesible desde el navegador del ciudadano.
        // apiUrl es la URL interna de Docker (pluspagos-mock:3000) usada
        // para comunicación entre contenedores.
        // publicUrl es localhost:3000, la que el browser puede resolver.
        String urlPago = publicUrl + "/sim/pago/" + idOrden;

        // URL del backend que el mock llamará al terminar el pago.
        // El mock la desencripta del form y hace POST con el resultado.
        String callbackUrl = serverBaseUrl + "/api/v1/webhooks/pluspagos/callback";

        // URLs de redirección del usuario en el browser
        String urlExito = serverBaseUrl + "/pago/exito?nro=" + nroTramite;
        String urlError  = serverBaseUrl + "/pago/error?nro=" + nroTramite;

        Map<String, String> formulario = new LinkedHashMap<>();

        // Campos en texto plano
        formulario.put("Comercio",              merchantGuid);
        formulario.put("TransaccionComercioId", nroTramite);
        formulario.put("Informacion",           "Certificado RDAM - " + nroTramite);

        // Campos encriptados con AES-256-CBC
        try {
            formulario.put("Monto",           PlusPagosCrypto.encrypt(
                                                  String.valueOf(montoCentavos), secretKey));
            formulario.put("CallbackSuccess", PlusPagosCrypto.encrypt(callbackUrl, secretKey));
            formulario.put("CallbackCancel",  PlusPagosCrypto.encrypt(callbackUrl, secretKey));
            formulario.put("UrlSuccess",      PlusPagosCrypto.encrypt(urlExito, secretKey));
            formulario.put("UrlError",        PlusPagosCrypto.encrypt(urlError, secretKey));
        } catch (Exception e) {
            log.error("Error encriptando campos del formulario de pago: {}", e.getMessage());
            throw new RuntimeException("No se pudo generar el formulario de pago.", e);
        }

        log.info("Formulario de pago generado. idSolicitud={} nroTramite={} idOrden={} callbackUrl={}",
                idSolicitud, nroTramite, idOrden, callbackUrl);

        return new ResultadoOrdenPago(idOrden, urlPago, true, formulario);
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
     *
     * formularioDatos contiene los campos listos para el form POST:
     *   - campos planos (Comercio, TransaccionComercioId, Informacion)
     *   - campos AES encriptados (Monto, Callback*, Url*)
     *
     * El frontend itera este map para construir los inputs del form.
     */
    public record ResultadoOrdenPago(
            String idOrdenPago,
            String urlPago,
            boolean modoSimulacion,
            Map<String, String> formularioDatos
    ) {}

    /**
     * Resultado de interpretar un webhook de PlusPagos.
     */
    public enum ResultadoPago {
        APROBADO,
        RECHAZADO
    }
}
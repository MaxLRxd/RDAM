package com.rdam.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de emails del sistema RDAM.
 *
 * Responsabilidad única: construir y enviar mensajes de email.
 * La lógica de negocio (cuándo enviar qué) vive en los
 * servicios que llaman a este.
 *
 * Todos los métodos son @Async: el envío de email no bloquea
 * el hilo principal. Si el SMTP está caído, se loguea el error
 * pero el flujo del trámite continúa sin interrupciones.
 * (Ver IMPLEMENTATION.md — "SMTP caído: no bloquea el flujo")
 *
 * @Slf4j: inyecta automáticamente un logger via Lombok.
 * Usamos log.error() en lugar de e.printStackTrace() porque
 * los logs estructurados son más fáciles de monitorear.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${rdam.mail.from}")
    private String fromAddress;

    // -------------------------------------------------------
    // Emails del portal ciudadano
    // -------------------------------------------------------

    /**
     * Envía el código OTP al ciudadano para validar su email.
     * Se llama desde SolicitudService al crear una solicitud.
     *
     * @param destinatario Email del ciudadano.
     * @param nroTramite   Número de trámite para que el ciudadano
     *                     pueda identificar a qué solicitud corresponde.
     * @param codigoOtp    Código de 6 dígitos a enviar.
     */
    @Async
    public void enviarOtp(String destinatario,
                          String nroTramite,
                          String codigoOtp) {
        String asunto = "RDAM — Código de verificación para trámite " + nroTramite;
        String cuerpo = """
            Recibimos tu solicitud de certificado RDAM.
            
            Tu número de trámite es: %s
            Tu código de verificación es: %s
            
            Este código expira en 15 minutos.
            Si no solicitaste este certificado, ignorá este mensaje.
            
            Poder Judicial de Santa Fe — Sistema RDAM
            """.formatted(nroTramite, codigoOtp);

        enviar(destinatario, asunto, cuerpo);
    }

    /**
     * Notifica al ciudadano que su pago fue confirmado y que
     * el operador está preparando el certificado.
     *
     * @param destinatario Email del ciudadano.
     * @param nroTramite   Número de trámite.
     */
    @Async
    public void notificarPagoConfirmado(String destinatario,
                                        String nroTramite) {
        String asunto = "RDAM — Pago confirmado para trámite " + nroTramite;
        String cuerpo = """
            Tu pago fue confirmado exitosamente.
            
            Número de trámite: %s
            Estado: Pago recibido
            
            El personal de la circunscripción correspondiente está
            procesando tu solicitud. Te notificaremos cuando el
            certificado esté disponible para descargar.
            
            Poder Judicial de Santa Fe — Sistema RDAM
            """.formatted(nroTramite);

        enviar(destinatario, asunto, cuerpo);
    }

    /**
     * Notifica al ciudadano que su certificado está disponible
     * para descargar. Incluye el enlace con el token de descarga.
     *
     * @param destinatario   Email del ciudadano.
     * @param nroTramite     Número de trámite.
     * @param urlDescarga    URL completa con el token de descarga.
     * @param vigenciaDias   Días de vigencia del enlace.
     */
    @Async
    public void notificarCertificadoDisponible(String destinatario,
                                               String nroTramite,
                                               String urlDescarga,
                                               int vigenciaDias) {
        String asunto = "RDAM — Tu certificado está disponible — " + nroTramite;
        String cuerpo = """
            Tu certificado RDAM está disponible para descargar.
            
            Número de trámite: %s
            
            Descargá tu certificado desde el siguiente enlace:
            %s
            
            Este enlace es válido por %d días.
            Guardá el archivo en un lugar seguro.
            
            Poder Judicial de Santa Fe — Sistema RDAM
            """.formatted(nroTramite, urlDescarga, vigenciaDias);

        enviar(destinatario, asunto, cuerpo);
    }

    /**
     * Notifica al ciudadano que su solicitud venció por falta
     * de pago o por rechazo de PlusPagos.
     *
     * @param destinatario Email del ciudadano.
     * @param nroTramite   Número de trámite.
     */
    @Async
    public void notificarSolicitudVencida(String destinatario,
                                          String nroTramite) {
        String asunto = "RDAM — Trámite vencido — " + nroTramite;
        String cuerpo = """
            Tu solicitud de certificado RDAM venció o el pago no pudo procesarse.
            
            Número de trámite: %s
            
            Si querés obtener el certificado, necesitás iniciar
            una nueva solicitud en https://rdam.santafe.gob.ar
            
            Para consultas: soporte@rdam.santafe.gob.ar
            
            Poder Judicial de Santa Fe — Sistema RDAM
            """.formatted(nroTramite);

        enviar(destinatario, asunto, cuerpo);
    }

    // -------------------------------------------------------
    // Helper privado
    // -------------------------------------------------------

    /**
     * Método base de envío. Todos los métodos públicos lo usan.
     *
     * Si el envío falla (SMTP caído, dirección inválida, etc.),
     * loguea el error pero NO lanza excepción. Esto es intencional:
     * un email fallido no debe interrumpir el flujo del trámite.
     *
     * En producción, este log debe disparar una alerta de monitoreo.
     */
    private void enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(fromAddress);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            mailSender.send(mensaje);

            log.info("Email enviado. destinatario={} asunto='{}'",
                    destinatario, asunto);

        } catch (MailException e) {
            // Logueamos el error con nivel ERROR para que el sistema
            // de monitoreo lo detecte, pero no propagamos la excepción.
            log.error("Error enviando email. destinatario={} asunto='{}' error={}",
                    destinatario, asunto, e.getMessage());
        }
    }
}
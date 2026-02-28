package com.rdam.backend.service;

import com.rdam.backend.domain.entity.Solicitud;
import com.rdam.backend.enums.EstadoSolicitud;
import com.rdam.backend.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job programado que corre diariamente para detectar y procesar
 * solicitudes vencidas.
 *
 * Dos tipos de vencimiento:
 *
 *   1. Solicitudes PENDIENTE sin pago:
 *      Si pasaron más de X días desde fecha_creacion → VENCIDO
 *
 *   2. Certificados PUBLICADO vencidos:
 *      Si pasaron más de Y días desde sol_fec_emision → PUBLICADO_VENCIDO
 *      + se elimina el archivo de MinIO
 *
 * Los días de vencimiento vienen de application.properties
 * (distintos para DEV y PRD mediante variables de entorno).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VencimientoScheduler {

    private final SolicitudRepository  solicitudRepository;
    private final CertificadoService   certificadoService;
    private final EmailService         emailService;
    private final SolicitudStateMachine stateMachine;

    @Value("${rdam.negocio.vencimiento-pendiente-dias}")
    private int vencimientoPendienteDias;

    @Value("${rdam.negocio.validez-certificado-dias}")
    private int validezCertificadoDias;

    /**
     * Procesa solicitudes PENDIENTE que superaron el plazo de pago.
     *
     * Cron: "0 0 2 * * *" = todos los días a las 2:00 AM.
     * Configurable desde application.properties.
     */
    @Scheduled(cron = "${rdam.scheduler.vencimiento-cron}")
    @Transactional
    public void vencerSolicitudesSinPago() {
        LocalDateTime limite = LocalDateTime.now()
            .minusDays(vencimientoPendienteDias);

        List<Solicitud> vencidas = solicitudRepository
            .findByEstadoAndFechaCreacionBefore(
                EstadoSolicitud.PENDIENTE, limite
            );

        if (vencidas.isEmpty()) {
            log.info("Scheduler vencimientos: sin solicitudes PENDIENTE vencidas.");
            return;
        }

        log.info("Scheduler vencimientos: procesando {} solicitud(es) PENDIENTE vencidas.",
                vencidas.size());

        for (Solicitud solicitud : vencidas) {
            try {
                stateMachine.validarTransicion(
                    solicitud.getEstado(), EstadoSolicitud.VENCIDO
                );
                solicitud.setEstado(EstadoSolicitud.VENCIDO);
                solicitudRepository.save(solicitud);

                emailService.notificarSolicitudVencida(
                    solicitud.getEmail(),
                    solicitud.getNroTramite()
                );

                log.info("Solicitud vencida por plazo de pago. nroTramite={}",
                        solicitud.getNroTramite());

            } catch (Exception e) {
                // Si una falla, continuamos con las demás.
                // El error se logea para revisión manual.
                log.error("Error venciendo solicitud. nroTramite={} error={}",
                        solicitud.getNroTramite(), e.getMessage());
            }
        }
    }

    /**
     * Procesa certificados PUBLICADO que superaron el plazo de vigencia.
     * Elimina el archivo de MinIO y transiciona a PUBLICADO_VENCIDO.
     */
    @Scheduled(cron = "${rdam.scheduler.vencimiento-cron}")
    @Transactional
    public void vencerCertificados() {
        LocalDateTime limite = LocalDateTime.now()
            .minusDays(validezCertificadoDias);

        List<Solicitud> vencidas = solicitudRepository
            .findByEstadoAndSolFecEmisionBefore(
                EstadoSolicitud.PUBLICADO, limite
            );

        if (vencidas.isEmpty()) {
            log.info("Scheduler certificados: sin certificados vencidos.");
            return;
        }

        log.info("Scheduler certificados: procesando {} certificado(s) vencido(s).",
                vencidas.size());

        for (Solicitud solicitud : vencidas) {
            try {
                stateMachine.validarTransicion(
                    solicitud.getEstado(), EstadoSolicitud.PUBLICADO_VENCIDO
                );

                // Eliminar archivo de MinIO antes de cambiar el estado
                if (solicitud.getUrlCertificado() != null) {
                    certificadoService.eliminarCertificado(
                        solicitud.getUrlCertificado()
                    );
                }

                solicitud.setEstado(EstadoSolicitud.PUBLICADO_VENCIDO);
                // El token queda en DB pero ya no sirve para descargar
                // porque el archivo fue eliminado de MinIO
                solicitudRepository.save(solicitud);

                log.info("Certificado vencido y eliminado. nroTramite={}",
                        solicitud.getNroTramite());

            } catch (Exception e) {
                log.error("Error venciendo certificado. nroTramite={} error={}",
                        solicitud.getNroTramite(), e.getMessage());
            }
        }
    }
}
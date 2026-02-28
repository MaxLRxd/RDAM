package com.rdam.backend.controllers;

import com.rdam.backend.exception.SolicitudNotFoundException;
import com.rdam.backend.exception.TokenInvalidoException;
import com.rdam.backend.domain.entity.Solicitud;
import com.rdam.backend.enums.EstadoSolicitud;
import com.rdam.backend.repository.SolicitudRepository;
import com.rdam.backend.service.CertificadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * Descarga de certificados PDF.
 *
 * GET /certificados/{tokenDescarga}
 *
 * Público: el token en la URL actúa como factor de autenticación.
 * No requiere header de Authorization.
 * El link puede compartirse (ej: el ciudadano lo reenvía a su empleador).
 */
@RestController
@RequestMapping("/certificados")
@RequiredArgsConstructor
@Slf4j
public class CertificadoController {

    private final SolicitudRepository solicitudRepository;
    private final CertificadoService  certificadoService;

    /**
     * Descarga el PDF del certificado usando el token de descarga.
     *
     * El token es de 64 caracteres y actúa como secreto en la URL.
     * Válido por 65 días (PRD) / 80 días (DEV).
     *
     * HTTP 200: stream binario del PDF.
     * HTTP 404: token no encontrado en DB.
     * HTTP 410: solicitud en estado PUBLICADO_VENCIDO
     *           (archivo eliminado de MinIO).
     */
    @GetMapping("/{tokenDescarga}")
    public ResponseEntity<InputStreamResource> descargar(
            @PathVariable String tokenDescarga) throws IOException {

        // Buscar la solicitud por el token de descarga
        Solicitud solicitud = solicitudRepository
            .findByTokenDescarga(tokenDescarga)
            .orElseThrow(() -> new SolicitudNotFoundException(
                "token=" + tokenDescarga
            ));

        // Verificar que el certificado sigue vigente
        if (solicitud.getEstado() == EstadoSolicitud.PUBLICADO_VENCIDO) {
            throw new TokenInvalidoException(
                "El enlace de descarga venció. " +
                "Contactá a la circunscripción para regenerar el link."
            );
        }

        if (solicitud.getEstado() != EstadoSolicitud.PUBLICADO) {
            throw new SolicitudNotFoundException(
                "El certificado no está disponible. " +
                "Estado actual: " + solicitud.getEstado()
            );
        }

        // Obtener el stream del PDF desde MinIO
        InputStream pdfStream = certificadoService.obtenerCertificado(
            solicitud.getUrlCertificado()
        );

        // Nombre del archivo que verá el ciudadano al descargar
        String nombreArchivo = "certificado-rdam-"
            + solicitud.getNroTramite() + ".pdf";

        log.info("Descarga de certificado. nroTramite={} circunscripcion={}",
                solicitud.getNroTramite(),
                solicitud.getCircunscripcion().getNombre());

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + nombreArchivo + "\"")
            .body(new InputStreamResource(pdfStream));
    }
}
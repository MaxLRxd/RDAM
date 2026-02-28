package com.rdam.backend.domain.dto;

import com.rdam.backend.domain.entity.Solicitud;
import com.rdam.backend.enums.EstadoSolicitud;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Respuesta al consultar el estado de una solicitud.
 * GET /api/v1/solicitudes/{nroTramite}
 *
 * Solo expone los campos visibles al ciudadano.
 * NUNCA expone url_certificado (ruta interna de MinIO).
 * Solo expone linkDescarga cuando el estado es PUBLICADO.
 */
@Getter
public class SolicitudEstadoResponse {

    private final String        nroTramite;
    private final EstadoSolicitud estado;
    private final String        circunscripcion;
    private final LocalDateTime fechaCreacion;

    /**
     * Solo presente cuando estado = PUBLICADO.
     * Es la URL pública de descarga con el token.
     * Null en cualquier otro estado.
     */
    private final String linkDescarga;

    /**
     * Constructor que toma la entidad y construye la respuesta.
     * La transformación entidad → DTO vive acá, no en el servicio.
     *
     * @param solicitud    La entidad de la DB.
     * @param baseUrl      URL base del sistema (ej: https://rdam.santafe.gob.ar)
     *                     para construir el link de descarga.
     */
    public SolicitudEstadoResponse(Solicitud solicitud, String baseUrl) {
        this.nroTramite      = solicitud.getNroTramite();
        this.estado          = solicitud.getEstado();
        this.circunscripcion = solicitud.getCircunscripcion().getNombre();
        this.fechaCreacion   = solicitud.getFechaCreacion();

        // Solo construimos el link si el certificado está disponible
        this.linkDescarga = solicitud.getEstado() == EstadoSolicitud.PUBLICADO
                && solicitud.getTokenDescarga() != null
            ? baseUrl + "/api/v1/certificados/" + solicitud.getTokenDescarga()
            : null;
    }
}
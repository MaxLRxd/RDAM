package com.rdam.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Respuesta al crear una solicitud exitosamente.
 * HTTP 201
 */
@Getter
@AllArgsConstructor
public class CrearSolicitudResponse {
    private Long   idSolicitud;
    private String nroTramite;
    // El mensaje le recuerda al ciudadano que revise su email
    private String mensaje;
}
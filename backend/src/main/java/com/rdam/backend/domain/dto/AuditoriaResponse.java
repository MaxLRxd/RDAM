package com.rdam.backend.domain.dto;

import com.rdam.backend.domain.entity.AuditoriaOperacion;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Respuesta al listar el registro de auditoría.
 * GET /api/v1/auditoria
 *
 * Los nombres de campos coinciden con los que usa PanelAuditoria.jsx:
 *   entry.accion, entry.detalle, entry.usuario, entry.timestamp
 *
 * NUNCA expone ipOrigen ni entidadId al frontend para evitar
 * filtrar información de infraestructura.
 */
@Getter
public class AuditoriaResponse {

    private final Long          id;
    private final String        accion;      // operacion del registro
    private final String        detalle;
    private final String        usuario;     // usuarioNombre del registro
    private final LocalDateTime timestamp;  // fechaHora del registro

    public AuditoriaResponse(AuditoriaOperacion op) {
        this.id        = op.getId();
        this.accion    = op.getOperacion();
        this.detalle   = op.getDetalle();
        this.usuario   = op.getUsuarioNombre() != null ? op.getUsuarioNombre() : "sistema";
        this.timestamp = op.getFechaHora();
    }
}

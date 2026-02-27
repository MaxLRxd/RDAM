package com.rdam.backend.repository;

import com.rdam.backend.domain.entity.SolicitudHistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a la tabla solicitud_historial_estados.
 * Solo operaciones de INSERT y SELECT. Nunca UPDATE.
 */
@Repository
public interface SolicitudHistorialEstadoRepository
        extends JpaRepository<SolicitudHistorialEstado, Long> {

    /**
     * Obtiene el historial completo de una solicitud ordenado
     * cronológicamente. Usado en el panel interno para que el
     * operador vea el ciclo de vida completo del trámite.
     */
    List<SolicitudHistorialEstado> findBySolicitudIdOrderByFechaCambioAsc(
            Long solicitudId
    );
}
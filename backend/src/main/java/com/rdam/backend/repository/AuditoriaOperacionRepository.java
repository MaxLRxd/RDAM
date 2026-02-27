package com.rdam.backend.repository;

import com.rdam.backend.domain.entity.AuditoriaOperacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Acceso a la tabla auditoria_operaciones.
 * Solo operaciones de INSERT y SELECT. Nunca UPDATE.
 */
@Repository
public interface AuditoriaOperacionRepository
        extends JpaRepository<AuditoriaOperacion, Long> {

    /**
     * Lista operaciones de auditoría con filtro opcional
     * por tipo de operación y rango de fechas.
     * Usado por el ADMIN para revisar el log de actividad.
     *
     * Page<> en lugar de List<> porque el log puede tener
     * miles de registros. Siempre paginamos.
     */
    Page<AuditoriaOperacion> findByOperacionAndFechaHoraBetween(
            String operacion,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable
    );
}
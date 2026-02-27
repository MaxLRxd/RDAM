package com.rdam.backend.repository;

import com.rdam.backend.domain.entity.Solicitud;
import com.rdam.backend.enums.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla solicitud.
 * Es el repository más utilizado del sistema.
 */
@Repository
public interface SolicitudRepository
        extends JpaRepository<Solicitud, Long> {

    // =========================================================
    // BÚSQUEDAS SIMPLES — usadas en el portal ciudadano
    // =========================================================

    /**
     * Busca por número de trámite público (RDAM-YYYYMMDD-NNNN).
     * Usado en GET /api/v1/solicitudes/{nroTramite} por el ciudadano.
     */
    Optional<Solicitud> findByNroTramite(String nroTramite);

    /**
     * Busca por token de descarga.
     * Usado en GET /api/v1/certificados/{tokenDescarga}.
     * El token actúa como factor de autenticación en la URL.
     */
    Optional<Solicitud> findByTokenDescarga(String tokenDescarga);

    /**
     * Busca por token de acceso del ciudadano.
     * Usado en el filtro de seguridad para validar
     * que el token Bearer corresponde a una solicitud real.
     */
    Optional<Solicitud> findByTokenAcceso(String tokenAcceso);

    /**
     * Busca por ID de orden de pago de PlusPagos.
     * Usado en el webhook para encontrar la solicitud
     * correspondiente al pago recibido.
     *
     * La idempotencia del webhook depende de este método:
     * si ya existe una solicitud con ese idOrdenPago Y
     * ya está pagada, ignoramos el webhook duplicado.
     */
    Optional<Solicitud> findByIdOrdenPago(String idOrdenPago);


    // =========================================================
    // BÚSQUEDAS PARA EL PANEL INTERNO — con paginación y filtros
    // =========================================================

    /**
     * Lista solicitudes de UNA circunscripción específica.
     * Usada cuando el usuario autenticado es OPERADOR.
     * El filtro de circunscripción es automático: viene del JWT.
     *
     * Soporta filtro opcional por estado y paginación.
     *
     * JPQL en lugar de nombre de método porque la condición
     * "estado es opcional" no se puede expresar con convención
     * de nombres de forma legible.
     *
     * (:estado IS NULL OR s.estado = :estado) permite pasar
     * null como estado para obtener todos los estados.
     */
    @Query("""
        SELECT s FROM Solicitud s
        WHERE s.circunscripcion.id = :idCircunscripcion
          AND (:estado IS NULL OR s.estado = :estado)
          AND (:dniCuil IS NULL OR s.dniCuil = :dniCuil)
        ORDER BY s.fechaCreacion DESC
        """)
    Page<Solicitud> buscarPorCircunscripcion(
            @Param("idCircunscripcion") Integer idCircunscripcion,
            @Param("estado")           EstadoSolicitud estado,
            @Param("dniCuil")          String dniCuil,
            Pageable pageable
    );

    /**
     * Lista solicitudes de TODAS las circunscripciones.
     * Usada cuando el usuario autenticado es ADMIN.
     * Agrega el filtro opcional por circunscripción.
     */
    @Query("""
        SELECT s FROM Solicitud s
        WHERE (:idCircunscripcion IS NULL OR s.circunscripcion.id = :idCircunscripcion)
          AND (:estado IS NULL OR s.estado = :estado)
          AND (:dniCuil IS NULL OR s.dniCuil = :dniCuil)
        ORDER BY s.fechaCreacion DESC
        """)
    Page<Solicitud> buscarConFiltros(
            @Param("idCircunscripcion") Integer idCircunscripcion,
            @Param("estado")            EstadoSolicitud estado,
            @Param("dniCuil")           String dniCuil,
            Pageable pageable
    );


    // =========================================================
    // BÚSQUEDAS PARA EL SCHEDULER DE VENCIMIENTOS
    // =========================================================

    /**
     * Encuentra solicitudes PENDIENTE cuya fecha de creación
     * es anterior al límite de vencimiento.
     *
     * El VencimientoScheduler llama a este método diariamente
     * para mover solicitudes sin pago al estado VENCIDO.
     *
     * Ejemplo con límite de 60 días:
     *   limiteVencimiento = LocalDateTime.now().minusDays(60)
     *   → trae las solicitudes creadas hace más de 60 días
     */
    List<Solicitud> findByEstadoAndFechaCreacionBefore(
            EstadoSolicitud estado,
            LocalDateTime limiteVencimiento
    );

    /**
     * Encuentra solicitudes PUBLICADO cuya fecha de emisión
     * del certificado es anterior al límite.
     *
     * El VencimientoScheduler las mueve a PUBLICADO_VENCIDO
     * y elimina el archivo de MinIO.
     *
     * Ejemplo con límite de 65 días:
     *   limiteEmision = LocalDateTime.now().minusDays(65)
     */
    List<Solicitud> findByEstadoAndSolFecEmisionBefore(
            EstadoSolicitud estado,
            LocalDateTime limiteEmision
    );
}
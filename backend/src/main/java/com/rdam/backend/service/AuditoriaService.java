package com.rdam.backend.service;

import com.rdam.backend.domain.dto.AuditoriaResponse;
import com.rdam.backend.domain.entity.AuditoriaOperacion;
import com.rdam.backend.repository.AuditoriaOperacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona la escritura y consulta del registro de auditoría.
 *
 * Diseño de escritura:
 *   - registrar() es @Async para no bloquear el hilo de negocio.
 *   - Usa Propagation.REQUIRES_NEW: si la TX principal hace rollback,
 *     el log de auditoría se persiste igual (no se pierde la traza).
 *
 * Uso desde otros servicios:
 *   auditoriaService.registrar(
 *       AuditoriaOperacion.Operaciones.CERTIFICADO_PUBLICADO,
 *       "Trámite RDAM-xxx · Circunscripción II",
 *       usuario.getId(),
 *       usuario.getUsername(),
 *       solicitud.getId()
 *   );
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final AuditoriaOperacionRepository auditoriaRepo;

    // ─── Escritura ─────────────────────────────────────────────────────────

    /**
     * Registra una operación en el log de auditoría.
     *
     * @param operacion      Tipo de operación (usar AuditoriaOperacion.Operaciones.*).
     * @param detalle        Descripción sin datos sensibles.
     * @param idUsuario      ID del usuario interno (null si es sistema/ciudadano).
     * @param usuarioNombre  Nombre del actor para mostrar en el log.
     * @param entidadId      ID de la entidad relacionada (puede ser null).
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String operacion,
                          String detalle,
                          Long idUsuario,
                          String usuarioNombre,
                          Long entidadId) {
        try {
            AuditoriaOperacion entrada = new AuditoriaOperacion();
            entrada.setOperacion(operacion);
            entrada.setDetalle(detalle);
            entrada.setIdUsuario(idUsuario);
            entrada.setUsuarioNombre(usuarioNombre);
            entrada.setEntidadId(entidadId);
            auditoriaRepo.save(entrada);
        } catch (Exception e) {
            // El log de auditoría no debe interrumpir el flujo de negocio.
            log.error("Error al registrar entrada de auditoría: op={} error={}",
                    operacion, e.getMessage());
        }
    }

    // ─── Lectura ───────────────────────────────────────────────────────────

    /**
     * Lista todas las entradas de auditoría paginadas (sin filtro de operación).
     *
     * @param pageable Configuración de página/orden (normalmente fechaHora,desc).
     * @return Página de AuditoriaResponse.
     */
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> listar(Pageable pageable) {
        return auditoriaRepo.findAll(pageable)
                            .map(AuditoriaResponse::new);
    }

    /**
     * Lista entradas filtradas por tipo de operación.
     *
     * @param operacion Filtro de operación (ej: "CERTIFICADO_PUBLICADO").
     * @param pageable  Configuración de página/orden.
     * @return Página de AuditoriaResponse.
     */
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> listarPorOperacion(String operacion, Pageable pageable) {
        // Reutilizamos el método existente del repositorio.
        // Pasamos null para "desde/hasta" requeriría otro método — por ahora
        // usamos un rango amplio para simular "sin filtro de fecha".
        return auditoriaRepo
                .findByOperacion(operacion, pageable)
                .map(AuditoriaResponse::new);
    }
}

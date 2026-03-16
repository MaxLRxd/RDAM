package com.rdam.backend.controllers;

import com.rdam.backend.domain.dto.AuditoriaResponse;
import com.rdam.backend.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el registro de auditoría al portal interno.
 *
 * Solo accesible para ROLE_ADMIN (configurado en SecurityConfig).
 *
 * GET /auditoria                 → todas las entradas, paginadas
 * GET /auditoria?accion=TIPO     → filtradas por tipo de operación
 *
 * El backend ordena por fechaHora DESC por defecto;
 * el cliente puede sobrescribir el orden vía ?sort=.
 */
@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /**
     * Lista entradas del log de auditoría, con filtro opcional por tipo.
     *
     * @param accion   Filtro opcional: ej. "CERTIFICADO_PUBLICADO".
     *                 Si se omite, devuelve todas las operaciones.
     * @param pageable Paginación inyectada por Spring (page, size, sort).
     *                 Default: 20 por página, orden fechaHora DESC.
     * @return Página de AuditoriaResponse.
     */
    @GetMapping
    public ResponseEntity<Page<AuditoriaResponse>> listar(
            @RequestParam(required = false) String accion,
            @PageableDefault(size = 20, sort = "fechaHora",
                             direction = Sort.Direction.DESC) Pageable pageable) {

        if (accion != null && !accion.isBlank()) {
            return ResponseEntity.ok(
                    auditoriaService.listarPorOperacion(accion, pageable));
        }

        return ResponseEntity.ok(auditoriaService.listar(pageable));
    }
}

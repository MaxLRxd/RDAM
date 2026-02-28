package com.rdam.backend.controllers;

import com.rdam.backend.domain.entity.Solicitud;
import com.rdam.backend.domain.entity.UsuarioInterno;
import com.rdam.backend.enums.EstadoSolicitud;
import com.rdam.backend.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Endpoints del panel interno para Operadores y Administradores.
 *
 * GET  /solicitudes                                → listar
 * POST /solicitudes/{id}/certificado               → subir PDF
 * POST /solicitudes/{id}/certificado/regenerar-token → regenerar token
 *
 * Todos requieren JWT válido (OPERADOR o ADMIN).
 * El filtro automático por circunscripción lo aplica SolicitudService.
 *
 * @AuthenticationPrincipal: inyecta el UsuarioInterno directamente
 * desde el SecurityContext sin necesidad de buscarlo en DB.
 * Spring lo resuelve porque JwtFilter seteó la autenticación
 * con el objeto UserDetails cargado por UserDetailsServiceImpl.
 */
@RestController
@RequestMapping("/solicitudes")
@RequiredArgsConstructor
public class SolicitudInternaController {

    private final SolicitudService solicitudService;

    /**
     * Lista solicitudes con paginación y filtros opcionales.
     *
     * OPERADOR: solo ve las de su circunscripción (filtro automático).
     * ADMIN: puede filtrar por cualquier circunscripción.
     *
     * Query params opcionales:
     *   ?circunscripcion=2   (solo ADMIN)
     *   ?estado=PAGADO
     *   ?dniCuil=20123456789
     *   ?page=0&size=20&sort=fechaCreacion,desc
     *
     * @PageableDefault: si el cliente no manda parámetros de
     * paginación, usamos 20 elementos por página por defecto.
     */
    @GetMapping
    public ResponseEntity<Page<Solicitud>> listar(
            @AuthenticationPrincipal UsuarioInterno usuario,
            @RequestParam(required = false) Integer circunscripcion,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) String dniCuil,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {

        Page<Solicitud> resultado = solicitudService.listarSolicitudes(
            usuario, circunscripcion, estado, dniCuil, pageable
        );

        return ResponseEntity.ok(resultado);
    }

    /**
     * Sube el certificado PDF para una solicitud en estado PAGADO.
     *
     * Recibe el archivo con multipart/form-data.
     * El archivo debe ser PDF y no superar 10 MB.
     *
     * HTTP 200: certificado publicado, devuelve tokenDescarga.
     * HTTP 400: solicitud no está en estado PAGADO, o archivo inválido.
     * HTTP 403: el operador no tiene acceso a esa circunscripción.
     * HTTP 409: conflicto de concurrencia (@Version).
     * HTTP 413: archivo mayor a 10 MB.
     *
     * @param id      ID de la solicitud (path variable).
     * @param archivo Archivo PDF (multipart field "archivo").
     * @param usuario Usuario interno autenticado (del JWT).
     */
    @PostMapping("/{id}/certificado")
    public ResponseEntity<Map<String, Object>> subirCertificado(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UsuarioInterno usuario) throws IOException {

        String tokenDescarga = solicitudService.subirCertificado(
            id, archivo, usuario
        );

        return ResponseEntity.ok(Map.of(
            "tokenDescarga",  tokenDescarga,
            "urlDescarga",    "/api/v1/certificados/" + tokenDescarga,
            "vigenciaDias",   65
        ));
    }

    /**
     * Regenera el token de descarga sin reemplazar el archivo en MinIO.
     *
     * HTTP 200: nuevo token generado.
     * HTTP 400: solicitud no está en PUBLICADO ni PUBLICADO_VENCIDO.
     * HTTP 403: el operador no tiene acceso a esa circunscripción.
     */
    @PostMapping("/{id}/certificado/regenerar-token")
    public ResponseEntity<Map<String, Object>> regenerarToken(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioInterno usuario) {

        String nuevoToken = solicitudService.regenerarTokenDescarga(
            id, usuario
        );

        return ResponseEntity.ok(Map.of(
            "nuevoTokenDescarga", nuevoToken,
            "vigenciaDias",       65
        ));
    }
}
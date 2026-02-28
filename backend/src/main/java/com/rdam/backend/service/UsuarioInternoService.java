package com.rdam.backend.service;

import com.rdam.backend.exception.CircunscripcionMismatchException;
import com.rdam.backend.domain.entity.Circunscripcion;
import com.rdam.backend.domain.entity.UsuarioInterno;
import com.rdam.backend.enums.RolUsuario;
import com.rdam.backend.repository.CircunscripcionRepository;
import com.rdam.backend.repository.UsuarioInternoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona el ciclo de vida de los usuarios internos.
 *
 * Responsabilidades:
 *   - Crear usuarios con rol y circunscripción válidos
 *   - Hashear contraseñas con BCrypt
 *   - Activar y desactivar usuarios
 *
 * Solo accesible para el rol ADMIN (se valida en el controlador
 * con @PreAuthorize, no acá).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioInternoService {

    private final UsuarioInternoRepository usuarioRepository;
    private final CircunscripcionRepository circunscripcionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea un nuevo usuario interno.
     *
     * Validaciones:
     *   - El username no debe existir ya (→ 409)
     *   - OPERADOR debe tener circunscripción (→ 400)
     *   - ADMIN no debe tener circunscripción (→ 400)
     *   - La circunscripción debe existir en DB (→ 400)
     *
     * @param username         Email institucional del usuario.
     * @param passwordPlano    Contraseña en texto plano (se hashea acá).
     * @param rol              OPERADOR o ADMIN.
     * @param idCircunscripcion ID de la circunscripción (null para ADMIN).
     * @return El usuario creado y persistido.
     */
    @Transactional
    public UsuarioInterno crearUsuario(String username,
                                       String passwordPlano,
                                       RolUsuario rol,
                                       Integer idCircunscripcion) {

        // ¿Ya existe un usuario con ese username?
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                "Ya existe un usuario con el username: " + username
            );
        }

        // Validar regla de circunscripción según rol
        validarCircunscripcionParaRol(rol, idCircunscripcion);

        // Construir la entidad
        UsuarioInterno usuario = new UsuarioInterno();
        usuario.setUsername(username);

        // Hasheamos la contraseña. NUNCA se guarda el texto plano.
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setRol(rol);

        // Resolver la circunscripción desde DB si corresponde
        if (idCircunscripcion != null) {
            Circunscripcion circunscripcion = circunscripcionRepository
                .findById(idCircunscripcion)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Circunscripción no encontrada: " + idCircunscripcion
                ));
            usuario.setCircunscripcion(circunscripcion);
        }

        UsuarioInterno guardado = usuarioRepository.save(usuario);

        log.info("Usuario interno creado. id={} username={} rol={} circunscripcion={}",
                guardado.getId(), guardado.getUsername(),
                guardado.getRol(), idCircunscripcion);

        return guardado;
    }

    /**
     * Activa o desactiva un usuario interno.
     *
     * Regla de negocio: un Admin no puede desactivarse a sí mismo.
     * Si lo intenta, lanzamos excepción → HTTP 400.
     *
     * El JWT activo del usuario desactivado será rechazado en el
     * próximo request porque JwtFilter llama a loadUserByUsername()
     * que devuelve el usuario con activo=false → isEnabled()=false.
     *
     * @param idUsuario      ID del usuario a modificar.
     * @param nuevoEstado    true=activar, false=desactivar.
     * @param idAdminActual  ID del admin que ejecuta la acción.
     *                       Se usa para prevenir la auto-desactivación.
     */
    @Transactional
    public UsuarioInterno cambiarEstado(Long idUsuario,
                                        boolean nuevoEstado,
                                        Long idAdminActual) {

        UsuarioInterno usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException(
                "Usuario no encontrado: " + idUsuario
            ));

        // Un Admin no puede desactivarse a sí mismo
        if (!nuevoEstado && idUsuario.equals(idAdminActual)) {
            throw new IllegalArgumentException(
                "No podés desactivar tu propio usuario."
            );
        }

        usuario.setActivo(nuevoEstado);
        UsuarioInterno guardado = usuarioRepository.save(usuario);

        log.info("Estado de usuario modificado. id={} username={} activo={}",
                guardado.getId(), guardado.getUsername(), nuevoEstado);

        return guardado;
    }

    // -------------------------------------------------------
    // Helper privado
    // -------------------------------------------------------

    /**
     * Valida la regla de circunscripción según el rol.
     * Replica en Java el CHECK constraint del DDL:
     *   (rol = 'ADMIN' AND id_circunscripcion IS NULL) OR
     *   (rol = 'OPERADOR' AND id_circunscripcion IS NOT NULL)
     */
    private void validarCircunscripcionParaRol(RolUsuario rol,
                                               Integer idCircunscripcion) {
        if (rol == RolUsuario.OPERADOR && idCircunscripcion == null) {
            throw new IllegalArgumentException(
                "Un OPERADOR debe tener una circunscripción asignada."
            );
        }
        if (rol == RolUsuario.ADMIN && idCircunscripcion != null) {
            throw new IllegalArgumentException(
                "Un ADMIN no debe tener circunscripción asignada."
            );
        }
    }
}
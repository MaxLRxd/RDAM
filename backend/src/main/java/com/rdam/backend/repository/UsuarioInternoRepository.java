package com.rdam.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdam.backend.domain.entity.UsuarioInterno;

import java.util.Optional;

/**
 * Acceso a la tabla usuario_interno.
 *
 * JpaRepository<UsuarioInterno, Long>:
 *   - UsuarioInterno → entidad que maneja
 *   - Long           → tipo del @Id
 */
@Repository
public interface UsuarioInternoRepository
        extends JpaRepository<UsuarioInterno, Long> {

    /**
     * Busca un usuario por su username (email institucional).
     *
     * Usado por:
     *   - UserDetailsServiceImpl: Spring Security llama a este
     *     método en cada request autenticado para cargar el usuario.
     *   - AuthService: para validar credenciales en el login.
     *
     * Spring genera:
     *   SELECT * FROM usuario_interno WHERE username = ?
     *
     * Devuelve Optional porque el usuario puede no existir
     * (credenciales incorrectas). El servicio decide qué hacer
     * si está vacío (lanzar excepción, etc.)
     */
    Optional<UsuarioInterno> findByUsername(String username);

    /**
     * Verifica si ya existe un usuario con ese username.
     *
     * Usado por UsuarioInternoService al crear un usuario nuevo
     * para devolver HTTP 409 antes de intentar el INSERT
     * y recibir un error de constraint de DB.
     *
     * Spring genera:
     *   SELECT COUNT(*) > 0 FROM usuario_interno WHERE username = ?
     */
    boolean existsByUsername(String username);
}
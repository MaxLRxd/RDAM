package com.rdam.backend.security;
import com.rdam.backend.repository.UsuarioInternoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de UserDetailsService para usuarios internos.
 *
 * Spring Security llama a loadUserByUsername() en dos momentos:
 *   1. Durante el login: para verificar credenciales.
 *   2. En cada request autenticado: el JwtFilter llama aquí
 *      para verificar que el usuario sigue activo en DB.
 *
 * El segundo punto es crucial: si un Admin desactiva a un
 * Operador, su JWT sigue siendo válido criptográficamente,
 * pero este método carga el usuario con activo=false y
 * Spring Security rechaza la request con 403 gracias a
 * isEnabled() = false en UsuarioInterno.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioInternoRepository usuarioRepository;

    /**
     * Carga el usuario desde la DB por su username.
     *
     * @Transactional: necesario porque UsuarioInterno tiene
     * la circunscripción con FetchType.EAGER. Sin transacción
     * activa, Hibernate no puede hacer el JOIN.
     *
     * @throws UsernameNotFoundException si el usuario no existe.
     * Spring Security la convierte en 401 automáticamente.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "Usuario no encontrado: " + username
                ));
    }
}
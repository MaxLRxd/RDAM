package com.rdam.backend.domain.entity;

import com.rdam.backend.enums.RolUsuario;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Representa a un empleado del Poder Judicial con acceso al panel interno.
 *
 * Tabla: usuario_interno
 *
 * Implementa UserDetails para integrarse con Spring Security.
 * Spring Security llama a los métodos de esta interfaz para
 * decidir si el usuario puede autenticarse y qué puede hacer.
 *
 * Regla de negocio clave:
 *   - OPERADOR: tiene id_circunscripcion asignada (NOT NULL)
 *   - ADMIN:    id_circunscripcion es NULL (acceso total)
 */
@Entity
@Table(name = "usuario_interno")
public class UsuarioInterno implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email institucional. Es el nombre de usuario para el login.
     * UNIQUE en DB garantiza que no haya duplicados.
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Hash BCrypt de la contraseña. NUNCA se almacena el texto plano.
     * Se genera con BCryptPasswordEncoder(strength=12) en UsuarioInternoService.
     * No tiene getter público — Spring Security lo accede via getPassword().
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * @Enumerated(STRING): guarda "OPERADOR" o "ADMIN" en la columna,
     * no el número 0 o 1. Más legible y resistente a reordenamientos.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolUsuario rol;

    /**
     * Circunscripción asignada al operador.
     *
     * @ManyToOne: muchos usuarios pueden pertenecer a la misma circunscripción.
     * @JoinColumn: especifica qué columna de esta tabla es la FK.
     * nullable = true porque los ADMIN no tienen circunscripción.
     *
     * FetchType.EAGER: cargamos la circunscripción siempre junto al usuario
     * porque la necesitamos en cada request autenticado para el claim del JWT.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_circunscripcion", nullable = true)
    private Circunscripcion circunscripcion;

    /**
     * Si es false, el usuario no puede operar aunque tenga un JWT válido.
     * El JwtFilter verifica este campo en cada request.
     * Ver: SPEC.md — HU08 y endpoint PATCH /usuarios-internos/{id}/estado
     */
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // -------------------------
    // Ciclo de vida JPA
    // -------------------------

    /**
     * @PrePersist se ejecuta justo antes de que JPA haga el INSERT.
     * Garantiza que fecha_creacion siempre tenga valor,
     * sin depender de DEFAULT en MySQL.
     */
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // -------------------------
    // Implementación UserDetails
    // Estos métodos los llama Spring Security internamente.
    // -------------------------

    /**
     * Devuelve los roles del usuario como "authorities" de Spring Security.
     * Usamos el prefijo "ROLE_" porque Spring Security lo requiere
     * para que funcione hasRole('ADMIN') en @PreAuthorize.
     *
     * Ejemplo: RolUsuario.ADMIN → "ROLE_ADMIN"
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.rol.name()));
    }

    /**
     * Spring Security necesita el password vía este método.
     * Devolvemos el hash, nunca el texto plano.
     */
    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    /**
     * El identificador único del usuario para Spring Security.
     * Usamos el email (username) porque es único y reconocible.
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * ¿La cuenta expiró? En nuestro sistema las cuentas no expiran
     * por tiempo, solo se desactivan manualmente. Siempre true.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * ¿La cuenta está bloqueada? No implementamos bloqueo por
     * intentos fallidos en el MVP. Siempre true.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * ¿Las credenciales expiraron? No implementamos rotación
     * forzada de contraseñas en el MVP. Siempre true.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * ¿El usuario está activo?
     * Este es el método CLAVE. Si devuelve false, Spring Security
     * rechaza la autenticación con 403 automáticamente.
     * Lo controla el campo 'activo' en DB.
     */
    @Override
    public boolean isEnabled() {
        return this.activo;
    }

    // -------------------------
    // Getters de negocio
    // -------------------------

    public Long getId() { return id; }

    public RolUsuario getRol() { return rol; }

    public Circunscripcion getCircunscripcion() { return circunscripcion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public boolean isActivo() { return activo; }

    // -------------------------
    // Setters (solo los necesarios para UsuarioInternoService)
    // -------------------------

    public void setUsername(String username) { this.username = username; }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRol(RolUsuario rol) { this.rol = rol; }

    public void setCircunscripcion(Circunscripcion circunscripcion) {
        this.circunscripcion = circunscripcion;
    }

    /**
     * Activa o desactiva el usuario.
     * Llamado exclusivamente desde UsuarioInternoService,
     * que valida que un Admin no pueda desactivarse a sí mismo.
     */
    public void setActivo(boolean activo) { this.activo = activo; }

    // -------------------------
    // Validación de negocio
    // -------------------------

    /**
     * Verifica la regla: OPERADOR requiere circunscripción,
     * ADMIN no debe tenerla.
     *
     * Se llama desde UsuarioInternoService antes de persistir.
     * Replica el CHECK constraint del DDL en la capa Java.
     */
    public boolean tieneCircunscripcionValida() {
        if (this.rol == RolUsuario.OPERADOR) {
            return this.circunscripcion != null;
        }
        if (this.rol == RolUsuario.ADMIN) {
            return this.circunscripcion == null;
        }
        return false;
    }

    /**
     * Helper: ¿puede este usuario ver solicitudes de una
     * circunscripción dada?
     * El ADMIN puede ver todas. El OPERADOR solo la suya.
     * Lo usará SolicitudService para filtrar automáticamente.
     */
    public boolean puedeAccederCircunscripcion(Integer idCircunscripcion) {
        if (this.rol == RolUsuario.ADMIN) return true;
        return this.circunscripcion != null &&
               this.circunscripcion.getId().equals(idCircunscripcion);
    }

    @Override
    public String toString() {
        return "UsuarioInterno{id=" + id +
               ", username='" + username + "'" +
               ", rol=" + rol +
               ", circunscripcion=" + circunscripcion +
               ", activo=" + activo + "}";
    }
}
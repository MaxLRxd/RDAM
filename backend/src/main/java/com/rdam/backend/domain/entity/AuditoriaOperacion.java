package com.rdam.backend.domain.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro inmutable de operaciones sensibles del sistema.
 *
 * Tabla: auditoria_operaciones
 *
 * Responsabilidad única: persistir un evento de auditoría.
 * Nunca se actualiza, solo se inserta.
 *
 * Ejemplos de operaciones auditadas:
 *   - DESCARGA_CERTIFICADO  → ciudadano descargó un PDF
 *   - LOGIN_EXITOSO         → usuario interno autenticado
 *   - LOGIN_FALLIDO         → intento de login fallido
 *   - REGENERAR_TOKEN       → operador regeneró token de descarga
 *   - CREAR_USUARIO         → admin creó un usuario interno
 *   - DESACTIVAR_USUARIO    → admin desactivó un usuario
 *
 * A diferencia de SolicitudHistorialEstado, no tiene FK
 * obligatoria a solicitud porque registra eventos del sistema
 * que pueden no estar relacionados a ninguna solicitud.
 */
@Entity
@Table(name = "auditoria_operaciones")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    /**
     * Timestamp del evento. Lo setea @PrePersist.
     * updatable = false: registro inmutable.
     */
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    /**
     * ID del usuario interno que realizó la operación.
     * Null si la operación la realizó un ciudadano o el sistema.
     */
    @Column(name = "id_usuario")
    private Long idUsuario;

    /**
     * Tipo de operación realizada. Valores fijos definidos
     * en AuditoriaOperacion.Operacion para evitar strings libres.
     */
    @Column(name = "operacion", nullable = false, length = 50)
    private String operacion;

    /**
     * IP de origen de la request.
     * IPv4 (15 chars) o IPv6 (45 chars).
     */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    /**
     * Información adicional en texto libre.
     * Ejemplo: "nroTramite=RDAM-20260201-0042, circunscripcion=2"
     * NUNCA incluir datos sensibles: DNI, email, tokens.
     */
    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    /**
     * ID de la entidad relacionada (id_solicitud, id_usuario, etc.)
     * Null si la operación no está asociada a una entidad específica.
     */
    @Column(name = "entidad_id")
    private Long entidadId;

    @PrePersist
    protected void onCreate() {
        this.fechaHora = LocalDateTime.now();
    }

    // -------------------------
    // Constantes de operaciones
    // -------------------------

    /**
     * Clase interna con las operaciones posibles del sistema.
     * Usamos constantes String en lugar de un enum separado
     * porque el campo en DB es VARCHAR y queremos evitar
     * una migración Flyway si agregamos nuevas operaciones.
     *
     * El servicio las usa así:
     *   auditoria.setOperacion(AuditoriaOperacion.Operaciones.DESCARGA_CERTIFICADO);
     */
    public static final class Operaciones {
        public static final String DESCARGA_CERTIFICADO = "DESCARGA_CERTIFICADO";
        public static final String LOGIN_EXITOSO        = "LOGIN_EXITOSO";
        public static final String LOGIN_FALLIDO        = "LOGIN_FALLIDO";
        public static final String REGENERAR_TOKEN      = "REGENERAR_TOKEN";
        public static final String CREAR_USUARIO        = "CREAR_USUARIO";
        public static final String DESACTIVAR_USUARIO   = "DESACTIVAR_USUARIO";
        public static final String SUBIR_CERTIFICADO    = "SUBIR_CERTIFICADO";

        // Constructor privado: esta clase no se instancia
        private Operaciones() {}
    }
}
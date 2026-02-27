package com.rdam.backend.domain.entity;

import com.rdam.backend.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una solicitud de certificado RDAM.
 * Responsabilidad única: mapear la tabla 'solicitud' a un objeto Java.
 * Toda la lógica de negocio (transiciones, validaciones) vive en
 * SolicitudService y SolicitudStateMachine.
 */
@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;

    @Column(name = "nro_tramite", nullable = false, unique = true, length = 20)
    private String nroTramite;

    @Column(name = "dni_cuil", nullable = false, length = 11)
    private String dniCuil;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_circunscripcion", nullable = false)
    private Circunscripcion circunscripcion;

    @Column(name = "codigo_validacion", length = 6)
    private String codigoValidacion;

    @Column(name = "token_acceso", unique = true, length = 64)
    private String tokenAcceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "monto_arancel", precision = 10, scale = 2)
    private BigDecimal montoArancel;

    @Column(name = "id_orden_pago", unique = true, length = 100)
    private String idOrdenPago;

    @Column(name = "sol_fec_pago")
    private LocalDateTime solFecPago;

    @Column(name = "url_certificado", length = 500)
    private String urlCertificado;

    @Column(name = "token_descarga", unique = true, length = 64)
    private String tokenDescarga;

    @Column(name = "sol_fec_emision")
    private LocalDateTime solFecEmision;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Versión para concurrencia optimista.
     * Hibernate agrega "WHERE version = ?" en cada UPDATE.
     * Si otro thread modificó el registro primero, lanza
     * OptimisticLockException → HTTP 409.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoSolicitud.PENDIENTE;
        }
    }
}
package com.rdam.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Registro inmutable de cada cambio de estado de una Solicitud.
 *
 * Tabla: solicitud_historial_estados
 *
 * Responsabilidad única: persistir el evento de cambio de estado.
 * Nunca se actualiza, solo se inserta.
 *
 * Lo crea SolicitudService cada vez que llama a
 * stateMachine.validarTransicion() y luego persiste el cambio.
 *
 * Permite auditoría completa del ciclo de vida y cálculo
 * de tiempos de respuesta por circunscripción.
 */
@Entity
@Table(name = "solicitud_historial_estados")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudHistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    /**
     * Referencia a la solicitud auditada.
     *
     * @ManyToOne: muchos registros de historial → una solicitud.
     *
     * @OnDelete(CASCADE): si la solicitud se elimina, sus registros
     * de historial se eliminan en cascada a nivel de base de datos,
     * no a nivel de JPA. Es más eficiente porque no necesita cargar
     * los registros en memoria para borrarlos.
     *
     * insertable/updatable = false porque la FK la maneja
     * la columna id_solicitud directamente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Solicitud solicitud;

    /**
     * Estado anterior al cambio. Puede ser null si es el
     * primer registro (creación de la solicitud).
     */
    @Column(name = "estado_anterior", length = 30)
    private String estadoAnterior;

    /**
     * Estado nuevo después del cambio. Nunca null.
     */
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private String estadoNuevo;

    /**
     * ID del usuario interno que realizó el cambio.
     * Null si el cambio fue automático (webhook, scheduler).
     */
    @Column(name = "id_usuario_interno")
    private Long idUsuarioInterno;

    /**
     * Timestamp del cambio. Lo setea @PrePersist.
     * updatable = false: una vez insertado, nunca cambia.
     */
    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    @PrePersist
    protected void onCreate() {
        this.fechaCambio = LocalDateTime.now();
    }
}
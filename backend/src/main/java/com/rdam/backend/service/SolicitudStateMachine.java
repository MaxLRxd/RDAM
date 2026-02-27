package com.rdam.backend.service;

import org.springframework.stereotype.Component;
import com.rdam.backend.exception.EstadoInvalidoException;
import com.rdam.backend.enums.EstadoSolicitud;

/**
 * Valida las transiciones de estado de una Solicitud.
 *
 * Responsabilidad única: dado un estado actual y uno destino,
 * decir si la transición es válida según las reglas de negocio.
 *
 * Al estar separada de la entidad y del servicio, podemos
 * testearla con un test unitario puro, sin base de datos ni
 * contexto Spring.
 *
 * Reglas (ver SPEC.md sección 6):
 *   PENDIENTE        → PAGADO, VENCIDO
 *   PAGADO           → PUBLICADO, VENCIDO
 *   PUBLICADO        → PUBLICADO_VENCIDO
 *   VENCIDO          → (ninguna, terminal)
 *   PUBLICADO_VENCIDO→ (ninguna, terminal)
 */
@Component
public class SolicitudStateMachine {

    /**
     * Valida que la transición sea permitida.
     * Lanza excepción si no lo es, para que el servicio
     * no tenga que manejar el boolean y decidir qué hacer.
     *
     * @param actual   Estado actual de la solicitud
     * @param destino  Estado al que se quiere transicionar
     * @throws EstadoInvalidoException si la transición no está permitida
     */
    public void validarTransicion(EstadoSolicitud actual, EstadoSolicitud destino) {
        boolean esValida = switch (actual) {
            case PENDIENTE -> destino == EstadoSolicitud.PAGADO
                           || destino == EstadoSolicitud.VENCIDO;

            case PAGADO    -> destino == EstadoSolicitud.PUBLICADO
                           || destino == EstadoSolicitud.VENCIDO;

            case PUBLICADO -> destino == EstadoSolicitud.PUBLICADO_VENCIDO;

            // Estados terminales: ninguna transición es válida
            case VENCIDO, PUBLICADO_VENCIDO -> false;
        };

        if (!esValida) {
            //CAMBIAR A EstadoInvalidoException de "exception" package
            throw new RuntimeException(
                "Transición no permitida: " + actual + " → " + destino
            );
        }
    }
}

package com.rdam.backend.enums;
/**
 * Representa el ciclo de vida completo de una Solicitud.
 *
 * Transiciones válidas (ver SPEC.md sección 6):
 *
 *  [inicio] ──► PENDIENTE ──► PAGADO ──► PUBLICADO ──► PUBLICADO_VENCIDO
 *                   │                                      (terminal)
 *                   └──────────────────────────────────► VENCIDO
 *                                                         (terminal)
 *
 * Los estados terminales (VENCIDO, PUBLICADO_VENCIDO) no tienen
 * transición de salida. El VencimientoScheduler los asigna
 * automáticamente por tiempo.
 */
public enum EstadoSolicitud {

    /**
     * Estado inicial: el ciudadano completó el formulario
     * y validó su email con el código OTP.
     * Espera el pago del arancel.
     */
    PENDIENTE,

    /**
     * PlusPagos confirmó el pago vía webhook (código 0 = APROBADA).
     * El operador debe cargar el certificado PDF.
     */
    PAGADO,

    /**
     * El operador cargó el certificado en MinIO.
     * El ciudadano puede descargarlo con su token_descarga.
     */
    PUBLICADO,

    /**
     * Estado terminal: el certificado venció (65 días PRD / 80 días DEV).
     * El archivo fue eliminado de MinIO.
     * El operador puede regenerar el token si es necesario.
     */
    PUBLICADO_VENCIDO,

    /**
     * Estado terminal. Causas posibles:
     * - El ciudadano no pagó en 60 días (PRD) / 15 días (DEV).
     * - PlusPagos rechazó el pago (códigos 4, 7, 8, 9, 11).
     */
    VENCIDO;

    /**
     * Valida si una transición de estado es permitida por las
     * reglas de negocio. Centralizar esta lógica aquí evita
     * que la validación quede dispersa en múltiples servicios.
     *
     * @param destino El estado al que se quiere transicionar.
     * @return true si la transición es válida.
     */
    public boolean puedeTransicionarA(EstadoSolicitud destino) {
        return switch (this) {
            case PENDIENTE -> destino == PAGADO || destino == VENCIDO;
            case PAGADO    -> destino == PUBLICADO || destino == VENCIDO;
            case PUBLICADO -> destino == PUBLICADO_VENCIDO;
            // Los estados terminales no tienen salida
            case PUBLICADO_VENCIDO, VENCIDO -> false;
        };
    }
}
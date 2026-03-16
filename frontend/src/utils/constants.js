/**
 * constants.js — Constantes del dominio de negocio.
 *
 * Centraliza los datos maestros que raramente cambian y que
 * de otra forma estarían hardcodeados en múltiples componentes.
 */

/**
 * Circunscripciones judiciales de la Provincia de Santa Fe.
 * Coincide con los datos maestros del backend (tabla `circunscripcion`, V6__datos_iniciales.sql).
 * id = id_circunscripcion en la DB / campo idCircunscripcion en CrearSolicitudRequest.
 */
export const CIRCUNSCRIPCIONES = [
  { id: 1, label: 'Circunscripción I · Santa Fe',      sede: 'Sede Santa Fe'      },
  { id: 2, label: 'Circunscripción II · Rosario',      sede: 'Sede Rosario'       },
  { id: 3, label: 'Circunscripción III · Venado Tuerto', sede: 'Sede Venado Tuerto' },
  { id: 4, label: 'Circunscripción IV · Reconquista',  sede: 'Sede Reconquista'   },
  { id: 5, label: 'Circunscripción V · Rafaela',       sede: 'Sede Rafaela'       },
]

/** Devuelve el label corto de una circunscripción por su id numérico. */
export function getCircunscripcionLabel(id) {
  const c = CIRCUNSCRIPCIONES.find(c => c.id === id)
  return c ? c.label : `Circunscripción ${id}`
}

/**
 * Estados del ciclo de vida de una solicitud.
 * Coincide con el enum EstadoSolicitud.java del backend.
 */
export const ESTADOS = {
  PENDIENTE:         'PENDIENTE',
  PAGADO:            'PAGADO',
  PUBLICADO:         'PUBLICADO',
  PUBLICADO_VENCIDO: 'PUBLICADO_VENCIDO',
  VENCIDO:           'VENCIDO',
}

/**
 * Steps del portal ciudadano.
 * Representa el "paso activo" dentro del flujo de CiudadanoPage.
 */
export const STEPS = {
  FORM:           'form',
  VALIDAR_EMAIL:  'validar-email',
  PENDIENTE:      'pendiente',
  PAGO:           'pago',
  ESPERANDO_CERT: 'esperando-cert',
  DESCARGA:       'descarga',
  VENCIDO:        'vencido',
  MIS_SOLICITUDES: 'mis-solicitudes',
}

/**
 * Mapeo de EstadoSolicitud → STEP del portal ciudadano.
 * Se usa cuando el ciudadano regresa al portal con una sesión preexistente.
 */
export function stepFromEstado(estado) {
  const map = {
    [ESTADOS.PENDIENTE]:         STEPS.PENDIENTE,
    [ESTADOS.PAGADO]:            STEPS.ESPERANDO_CERT,
    [ESTADOS.PUBLICADO]:         STEPS.DESCARGA,
    [ESTADOS.PUBLICADO_VENCIDO]: STEPS.VENCIDO,
    [ESTADOS.VENCIDO]:           STEPS.VENCIDO,
  }
  return map[estado] || STEPS.FORM
}

/**
 * Mapeo de STEP → índice en el stepper visual (0-based).
 * El stepper muestra: Datos (0) → Email (1) → Pago (2) → Certificado (3)
 */
export function stepperIndexFromStep(step) {
  const map = {
    [STEPS.FORM]:           0,
    [STEPS.VALIDAR_EMAIL]:  1,
    [STEPS.PENDIENTE]:      2,
    [STEPS.PAGO]:           2,
    [STEPS.ESPERANDO_CERT]: 3,
    [STEPS.DESCARGA]:       3,
    [STEPS.VENCIDO]:        3,
  }
  return map[step] ?? 0
}

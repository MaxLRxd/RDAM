/**
 * solicitudesApi.js — Servicios del portal ciudadano.
 *
 * Encapsula todas las llamadas HTTP relacionadas con el flujo ciudadano.
 * Cada función corresponde a un caso de uso de la especificación RDAM.
 *
 * Contratos de API verificados contra los DTOs del backend:
 *   - CrearSolicitudRequest / CrearSolicitudResponse
 *   - ValidarOtpRequest / ValidarOtpResponse
 *   - SolicitudEstadoResponse
 *   - PagoController.crearOrdenPago()
 */

import { fetchClient } from '../utils/fetchClient.js'

/**
 * HU01 — Crear una nueva solicitud de certificado RDAM.
 * POST /api/v1/solicitudes
 *
 * @param {{ dniCuil: string, email: string, idCircunscripcion: number }} data
 * @returns {{ idSolicitud: number, nroTramite: string, mensaje: string }}
 */
export async function crearSolicitud(data) {
  return fetchClient.post('/solicitudes', data)
}

/**
 * HU02 — Validar el código OTP recibido por email.
 * POST /api/v1/solicitudes/{id}/validar
 *
 * @param {number} idSolicitud
 * @param {string} codigo — 6 dígitos numéricos
 * @returns {{ tokenAcceso: string, nroTramite: string, estado: string }}
 */
export async function validarOtp(idSolicitud, codigo) {
  return fetchClient.post(`/solicitudes/${idSolicitud}/validar`, { codigo })
}

/**
 * HU03 — Consultar el estado actual de una solicitud.
 * GET /api/v1/solicitudes/{nroTramite}
 * Requiere token ciudadano en header Authorization.
 *
 * @param {string} nroTramite — "RDAM-20260313-0001"
 * @param {string} tokenAcceso — token opaco de 64 chars (Redis)
 * @returns {SolicitudEstadoResponse}
 */
export async function consultarEstado(nroTramite, tokenAcceso) {
  return fetchClient.get(`/solicitudes/${nroTramite}`, { ciudadanoToken: tokenAcceso })
}

/**
 * HU04 — Crear una orden de pago en PlusPagos.
 * POST /api/v1/solicitudes/{id}/pago/crear
 * Requiere token ciudadano en header Authorization.
 *
 * La respuesta incluye 'formularioDatos': un mapa de campos (algunos encriptados AES)
 * que el frontend debe colocar en inputs hidden y auto-submit hacia 'urlPago'.
 *
 * @param {number} idSolicitud
 * @param {string} tokenAcceso
 * @returns {{ idOrdenPago: string, urlPago: string, modoSimulacion: boolean, formularioDatos: Object }}
 */
export async function crearOrdenPago(idSolicitud, tokenAcceso) {
  return fetchClient.post(`/solicitudes/${idSolicitud}/pago/crear`, null, {
    ciudadanoToken: tokenAcceso,
  })
}

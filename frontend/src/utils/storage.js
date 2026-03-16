/**
 * storage.js — Helpers para localStorage.
 *
 * Centraliza todas las operaciones de persistencia del cliente.
 * Las claves están prefijadas con "rdam_" para evitar colisiones.
 *
 * Estructura de datos almacenados:
 *
 * rdam_ciudadano_session: {
 *   tokenAcceso: string,      // Token opaco de 64 chars (Redis)
 *   nroTramite:  string,      // "RDAM-20260313-0001"
 *   idSolicitud: number,      // Long del backend
 *   email:       string,      // Para mostrar en UI "enviamos código a..."
 *   dniCuil:     string,      // Para mostrar en resumen de pago
 * }
 *
 * rdam_historial: Array<{
 *   nroTramite:     string,
 *   dniCuil:        string,
 *   tokenAcceso:    string,   // Necesario para consultar estado
 *   idSolicitud:    number,
 *   estadoCache:    string,   // Estado al momento de guardar (puede estar desactualizado)
 *   circunscripcion: string,  // Nombre de la circunscripción
 *   email:          string,
 *   fechaCreacion:  string,
 * }>
 *
 * rdam_interno_auth: {
 *   accessToken:     string,
 *   rol:             "OPERADOR" | "ADMIN",
 *   circunscripcion: number | null,
 *   expiresAt:       number,  // timestamp ms
 * }
 */

const KEYS = {
  CIUDADANO_SESSION: 'rdam_ciudadano_session',
  HISTORIAL:         'rdam_historial',
  INTERNO_AUTH:      'rdam_interno_auth',
}

// ─── Sesión del ciudadano (solicitud activa) ───────────────────────────────

export function getCiudadanoSession() {
  try {
    return JSON.parse(localStorage.getItem(KEYS.CIUDADANO_SESSION)) || null
  } catch {
    return null
  }
}

export function setCiudadanoSession(session) {
  localStorage.setItem(KEYS.CIUDADANO_SESSION, JSON.stringify(session))
}

export function clearCiudadanoSession() {
  localStorage.removeItem(KEYS.CIUDADANO_SESSION)
}

// ─── Historial de solicitudes del ciudadano ───────────────────────────────

export function getHistorial() {
  try {
    return JSON.parse(localStorage.getItem(KEYS.HISTORIAL)) || []
  } catch {
    return []
  }
}

/**
 * Agrega o actualiza una solicitud en el historial.
 * Si ya existe un registro con el mismo nroTramite, lo actualiza.
 */
export function upsertHistorial(item) {
  const historial = getHistorial()
  const idx = historial.findIndex(h => h.nroTramite === item.nroTramite)
  if (idx >= 0) {
    historial[idx] = { ...historial[idx], ...item }
  } else {
    historial.unshift(item) // más reciente primero
  }
  localStorage.setItem(KEYS.HISTORIAL, JSON.stringify(historial))
}

/** Actualiza solo el campo 'estadoCache' de un ítem del historial. */
export function updateHistorialEstado(nroTramite, estadoCache) {
  const historial = getHistorial()
  const idx = historial.findIndex(h => h.nroTramite === nroTramite)
  if (idx >= 0) {
    historial[idx].estadoCache = estadoCache
    localStorage.setItem(KEYS.HISTORIAL, JSON.stringify(historial))
  }
}

// ─── Autenticación de usuario interno ─────────────────────────────────────

export function getInternoAuth() {
  try {
    const auth = JSON.parse(localStorage.getItem(KEYS.INTERNO_AUTH))
    if (!auth) return null
    // Verificar que el token no haya expirado localmente
    if (Date.now() > auth.expiresAt) {
      clearInternoAuth()
      return null
    }
    return auth
  } catch {
    return null
  }
}

export function setInternoAuth(auth) {
  // expiresAt ya fue calculado por AuthContext antes de llamar aquí.
  // No recalcular: auth.expiresIn no existe en el objeto session que se recibe,
  // y recalcularlo produce NaN → null en JSON → la sesión se pierde en cada F5.
  localStorage.setItem(KEYS.INTERNO_AUTH, JSON.stringify(auth))
}

export function clearInternoAuth() {
  localStorage.removeItem(KEYS.INTERNO_AUTH)
}

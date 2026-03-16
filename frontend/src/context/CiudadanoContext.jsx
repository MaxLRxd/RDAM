/* eslint-disable react-refresh/only-export-components */
/**
 * CiudadanoContext.jsx — Estado global de la sesión del ciudadano.
 *
 * Gestiona la sesión activa del ciudadano (token, solicitud en curso) y
 * la sincronización con localStorage, para que persista entre recargas.
 *
 * Decisión de diseño: usamos Context + useReducer en lugar de Zustand/Redux
 * porque el estado del ciudadano es simple (una sesión activa + historial)
 * y no justifica dependencias externas.
 *
 * Lo que persiste:
 *   - tokenAcceso:  token opaco Redis de 64 chars para llamadas autenticadas
 *   - idSolicitud:  ID numérico de la solicitud activa (para pago y polling)
 *   - nroTramite:   número legible del trámite
 *   - email:        email del solicitante (para mostrar "código enviado a...")
 *   - dniCuil:      DNI/CUIL consultado (para mostrar en resumen de pago)
 */

import { createContext, useReducer, useCallback } from 'react'
import {
  getCiudadanoSession,
  setCiudadanoSession,
  clearCiudadanoSession,
  upsertHistorial,
} from '../utils/storage.js'

// ─── Estado inicial (desde localStorage si existe) ────────────────────────

function getInitialState() {
  const session = getCiudadanoSession()
  return {
    tokenAcceso:  session?.tokenAcceso  || null,
    idSolicitud:  session?.idSolicitud  || null,
    nroTramite:   session?.nroTramite   || null,
    email:        session?.email        || null,
    dniCuil:      session?.dniCuil      || null,
  }
}

// ─── Reducer ──────────────────────────────────────────────────────────────

function reducer(state, action) {
  switch (action.type) {

    // Después de crear la solicitud (POST /solicitudes):
    // guardamos id y nroTramite, todavía no tenemos token
    case 'SET_SOLICITUD_CREADA':
      return {
        ...state,
        idSolicitud: action.payload.idSolicitud,
        nroTramite:  action.payload.nroTramite,
        email:       action.payload.email,
        dniCuil:     action.payload.dniCuil,
      }

    // Después de validar el OTP (POST /solicitudes/{id}/validar):
    // recibimos el tokenAcceso y ya podemos llamar endpoints de ciudadano
    case 'SET_TOKEN':
      return {
        ...state,
        tokenAcceso: action.payload.tokenAcceso,
        nroTramite:  action.payload.nroTramite,
      }

    // Limpiar toda la sesión activa (por ejemplo, para "Nueva solicitud")
    case 'CLEAR':
      return {
        tokenAcceso: null,
        idSolicitud: null,
        nroTramite:  null,
        email:       null,
        dniCuil:     null,
      }

    default:
      return state
  }
}

// ─── Context ──────────────────────────────────────────────────────────────

export const CiudadanoContext = createContext(null)

export function CiudadanoProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, null, getInitialState)

  /**
   * Lllamado después de POST /solicitudes.
   * Guarda los datos de la solicitud recién creada.
   */
  const setSolicitudCreada = useCallback(({ idSolicitud, nroTramite, email, dniCuil }) => {
    dispatch({ type: 'SET_SOLICITUD_CREADA', payload: { idSolicitud, nroTramite, email, dniCuil } })
    // Persistir parcialmente (sin token aún)
    setCiudadanoSession({ idSolicitud, nroTramite, email, dniCuil, tokenAcceso: null })
  }, [])

  /**
   * Llamado después de POST /solicitudes/{id}/validar.
   * Recibimos el tokenAcceso y completamos la sesión.
   */
  const setToken = useCallback(({ tokenAcceso, nroTramite, circunscripcion, estado }) => {
    dispatch({ type: 'SET_TOKEN', payload: { tokenAcceso, nroTramite } })

    // Recuperar lo que ya teníamos para completar la sesión
    const prev = getCiudadanoSession() || {}
    const session = { ...prev, tokenAcceso, nroTramite }
    setCiudadanoSession(session)

    // Agregar al historial del ciudadano
    upsertHistorial({
      nroTramite,
      dniCuil:        prev.dniCuil || '',
      tokenAcceso,
      idSolicitud:    prev.idSolicitud,
      estadoCache:    estado || 'PENDIENTE',
      circunscripcion: circunscripcion || '',
      email:          prev.email || '',
      fechaCreacion:  new Date().toISOString(),
    })
  }, [])

  /** Limpia la sesión activa (sin borrar el historial). */
  const clearSesion = useCallback(() => {
    dispatch({ type: 'CLEAR' })
    clearCiudadanoSession()
  }, [])

  return (
    <CiudadanoContext.Provider value={{ ...state, setSolicitudCreada, setToken, clearSesion }}>
      {children}
    </CiudadanoContext.Provider>
  )
}

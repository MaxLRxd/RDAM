/**
 * useCiudadano.js — Hook público para acceder al CiudadanoContext.
 *
 * Separado del archivo de contexto para cumplir la regla de React Fast Refresh:
 * un archivo solo debe exportar componentes React (no funciones mixtas).
 */

import { useContext } from 'react'
import { CiudadanoContext } from '../context/CiudadanoContext.jsx'

export function useCiudadano() {
  const ctx = useContext(CiudadanoContext)
  if (!ctx) throw new Error('useCiudadano debe usarse dentro de CiudadanoProvider')
  return ctx
}

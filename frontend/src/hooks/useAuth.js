/**
 * useAuth.js — Hook para consumir AuthContext.
 *
 * Separado del archivo de contexto para cumplir la regla de
 * react-refresh/only-export-components (Fast Refresh de Vite).
 *
 * Uso:
 *   const { isAuthenticated, isAdmin, token, login, logout } = useAuth()
 */

import { useContext } from 'react'
import { AuthContext } from '../context/AuthContext.jsx'

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  }
  return ctx
}

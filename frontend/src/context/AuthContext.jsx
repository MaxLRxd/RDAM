/**
 * AuthContext.jsx — Estado global de autenticación del Portal Interno.
 *
 * Gestiona el JWT del usuario interno: login, logout, y recuperación
 * desde localStorage al recargar la página.
 *
 * Estado persistido en localStorage (clave: rdam_interno_auth):
 *   { accessToken, username, rol, circunscripcion, expiresAt }
 *
 * Diferencia con CiudadanoContext:
 *   - No usa useReducer (el estado de auth es más simple: logueado o no)
 *   - El token es JWT Bearer, no el token ciudadano
 *   - La circunscripcion puede ser null (para ADMIN que ven todo)
 *
 * Fast Refresh: este archivo exporta solo el contexto (no el hook).
 * El hook `useAuth` vive en src/hooks/useAuth.js.
 */

/* eslint-disable react-refresh/only-export-components */

import { createContext, useState, useCallback, useEffect } from 'react'
import {
  getInternoAuth,
  setInternoAuth,
  clearInternoAuth,
} from '../utils/storage.js'
import { loginInterno } from '../api/authApi.js'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // ─── Estado ───────────────────────────────────────────────────────────────
  // null = no autenticado | objeto = sesión activa
  const [auth, setAuth] = useState(() => {
    const saved = getInternoAuth()
    if (!saved) return null
    // Verificar si el token no ha expirado
    if (saved.expiresAt && Date.now() > saved.expiresAt) {
      clearInternoAuth()
      return null
    }
    return saved
  })

  const [loginError, setLoginError] = useState(null)
  const [loginLoading, setLoginLoading] = useState(false)

  // ─── Verificar expiración periódicamente ─────────────────────────────────
  // El token dura 8 horas según la maqueta. Revisamos cada minuto.
  useEffect(() => {
    if (!auth) return
    const timer = setInterval(() => {
      if (auth.expiresAt && Date.now() > auth.expiresAt) {
        clearInternoAuth()
        setAuth(null)
      }
    }, 60_000)
    return () => clearInterval(timer)
  }, [auth])

  // ─── Acciones ─────────────────────────────────────────────────────────────

  /**
   * Ejecuta el login contra POST /auth/login.
   * Si tiene éxito, guarda la sesión en state + localStorage.
   */
  const login = useCallback(async (username, password) => {
    setLoginLoading(true)
    setLoginError(null)
    try {
      const data = await loginInterno(username, password)
      // data: { accessToken, tokenType, expiresIn (seconds), rol, circunscripcion }
      const session = {
        accessToken:     data.accessToken,
        username,
        rol:             data.rol,
        circunscripcion: data.circunscripcion ?? null, // null para ADMIN
        expiresAt:       Date.now() + data.expiresIn * 1000,
      }
      setInternoAuth(session)
      setAuth(session)
    } catch (err) {
      setLoginError(err.message || 'Credenciales inválidas')
    } finally {
      setLoginLoading(false)
    }
  }, [])

  /** Cierra la sesión: limpia state + localStorage */
  const logout = useCallback(() => {
    clearInternoAuth()
    setAuth(null)
  }, [])

  // ─── Derivados convenientes ───────────────────────────────────────────────
  const isAuthenticated = auth !== null
  const isAdmin         = auth?.rol === 'ADMIN'
  const token           = auth?.accessToken ?? null

  const value = {
    auth,
    isAuthenticated,
    isAdmin,
    token,         // shortcut: auth.accessToken
    login,
    logout,
    loginError,
    loginLoading,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

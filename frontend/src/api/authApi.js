/**
 * authApi.js — Servicios de autenticación del portal interno.
 *
 * Contratos verificados contra:
 *   - LoginRequest.java / LoginResponse.java
 *   - AuthController.java (POST /auth/login)
 */

import { fetchClient } from '../utils/fetchClient.js'

/**
 * Login del usuario interno (Operador / Administrador).
 * POST /api/v1/auth/login
 *
 * @param {string} username — email del usuario
 * @param {string} password
 * @returns {{ accessToken: string, tokenType: string, expiresIn: number, rol: string, circunscripcion: number|null }}
 */
export async function loginInterno(username, password) {
  return fetchClient.post('/auth/login', { username, password })
}

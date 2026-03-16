/**
 * internoApi.js — Capa de acceso a la API para el Portal Interno.
 *
 * Todos los endpoints requieren JWT Bearer token (operador o admin).
 * El token se pasa como opción `jwtToken` al fetchClient.
 *
 * Endpoints cubiertos:
 *   GET    /solicitudes                      → listar con filtros + paginación
 *   POST   /solicitudes/{id}/certificado     → subir certificado (multipart)
 *   POST   /solicitudes/{id}/certificado/regenerar-token → regenerar link
 *   POST   /usuarios-internos                → crear usuario (ADMIN)
 *   PATCH  /usuarios-internos/{id}/estado   → activar/desactivar (ADMIN)
 */

import { fetchClient } from '../utils/fetchClient.js'

// ─── Solicitudes ────────────────────────────────────────────────────────────

/**
 * Lista solicitudes con filtros y paginación (Spring Page<T>).
 *
 * @param {object} params
 * @param {number|null}  params.circunscripcion — ID (1-5) o null para todas
 * @param {string|null}  params.estado          — valor del enum o null
 * @param {string|null}  params.dniCuil         — búsqueda parcial o null
 * @param {number}       params.page            — 0-based
 * @param {number}       params.size            — registros por página
 * @param {string}       jwtToken               — Bearer token del interno
 * @returns {Promise<{content: Array, totalElements: number, totalPages: number, number: number}>}
 */
export async function listarSolicitudes(params, jwtToken) {
  const { circunscripcion, estado, dniCuil, page = 0, size = 20 } = params

  const qs = new URLSearchParams()
  if (circunscripcion) qs.set('circunscripcion', circunscripcion)
  if (estado)          qs.set('estado', estado)
  if (dniCuil)         qs.set('dniCuil', dniCuil)
  qs.set('page', page)
  qs.set('size', size)
  qs.set('sort', 'fechaCreacion,desc')

  return fetchClient.get(`/solicitudes?${qs.toString()}`, { jwtToken })
}

/**
 * Sube el archivo PDF del certificado para una solicitud PAGADO.
 * Transiciona el estado → PUBLICADO.
 *
 * @param {number|string} idSolicitud
 * @param {File}          archivo      — objeto File del input[type=file]
 * @param {string}        jwtToken
 */
export async function subirCertificado(idSolicitud, archivo, jwtToken) {
  const formData = new FormData()
  formData.append('archivo', archivo)

  // fetchClient.post(path, body, opts) — body=FormData, opts lleva jwtToken e isMultipart
  return fetchClient.post(
    `/solicitudes/${idSolicitud}/certificado`,
    formData,
    { jwtToken, isMultipart: true },
  )
}

/**
 * Regenera el token de descarga de un certificado ya publicado.
 * Útil cuando el link original expiró (PUBLICADO_VENCIDO).
 *
 * @param {number|string} idSolicitud
 * @param {string}        jwtToken
 */
export async function regenerarToken(idSolicitud, jwtToken) {
  // Sin body (POST vacío), el token va en opts
  return fetchClient.post(
    `/solicitudes/${idSolicitud}/certificado/regenerar-token`,
    null,
    { jwtToken },
  )
}

// ─── Usuarios Internos ───────────────────────────────────────────────────────

/**
 * Crea un nuevo usuario interno (ADMIN only).
 *
 * @param {object} data
 * @param {string}      data.username        — email institucional
 * @param {string}      data.password        — mínimo 8 caracteres
 * @param {string}      data.rol             — 'OPERADOR' | 'ADMIN'
 * @param {number|null} data.idCircunscripcion — requerido para OPERADOR
 * @param {string}      jwtToken
 * @returns {Promise<UsuarioInternoResponse>}
 */
export async function crearUsuario(data, jwtToken) {
  return fetchClient.post('/usuarios-internos', data, { jwtToken })
}

/**
 * Lista todos los usuarios internos paginados (ADMIN only).
 * GET /api/v1/usuarios-internos
 *
 * @param {string} jwtToken
 * @returns {Promise<{content: Array, totalElements: number, totalPages: number}>}
 */
export async function listarUsuarios(jwtToken) {
  return fetchClient.get('/usuarios-internos?size=50&sort=username,asc', { jwtToken })
}

/**
 * Activa o desactiva un usuario interno (ADMIN only).
 * El backend impide que un admin se desactive a sí mismo.
 *
 * @param {number|string} idUsuario
 * @param {boolean}       activo
 * @param {string}        jwtToken
 * @returns {Promise<UsuarioInternoResponse>}
 */
export async function cambiarEstadoUsuario(idUsuario, activo, jwtToken) {
  return fetchClient.patch(
    `/usuarios-internos/${idUsuario}/estado`,
    { activo },
    { jwtToken },
  )
}

// ─── Auditoría ───────────────────────────────────────────────────────────────

/**
 * Lista entradas del registro de auditoría con paginación (ADMIN only).
 * GET /api/v1/auditoria
 *
 * @param {object} params
 * @param {number}      params.page   — 0-based
 * @param {number}      params.size
 * @param {string|null} params.accion — filtro opcional por tipo de operación
 * @param {string}      jwtToken
 * @returns {Promise<{content: Array, totalElements: number, totalPages: number}>}
 */
export async function listarAuditoria({ page = 0, size = 20, accion = null } = {}, jwtToken) {
  const qs = new URLSearchParams()
  qs.set('page', page)
  qs.set('size', size)
  qs.set('sort', 'fechaHora,desc')
  if (accion) qs.set('accion', accion)
  return fetchClient.get(`/auditoria?${qs.toString()}`, { jwtToken })
}

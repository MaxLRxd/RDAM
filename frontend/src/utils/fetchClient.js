/**
 * fetchClient.js — Wrapper sobre la Fetch API nativa.
 *
 * Reemplaza Axios sin dependencias externas. Centraliza:
 *  - La URL base de la API (/api/v1)
 *  - La inyección de tokens de autenticación en headers
 *  - El manejo de errores HTTP con el formato RFC 7807 del backend
 *
 * Diseño de autenticación (ambos esquemas usan prefijo Bearer):
 *  - Ciudadano:  header "Authorization: Bearer {tokenAcceso}"
 *  - Interno:    header "Authorization: Bearer {jwt}"
 *
 * Nota: TokenCiudadanoFilter (backend) extrae con header.startsWith("Bearer ")
 * y luego header.substring(7). El prefijo es obligatorio en ambos casos.
 *
 * Uso:
 *   fetchClient.post('/solicitudes', { dniCuil, email, idCircunscripcion })
 *   fetchClient.get('/solicitudes/RDAM-...', { ciudadanoToken: 'abc...' })
 */

const BASE_URL = '/api/v1'

/**
 * Error enriquecido que expone el status HTTP y el body de error del backend.
 */
export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.status = status
    this.data   = data
  }
}

async function request(method, path, options = {}) {
  const { body, ciudadanoToken, jwtToken, isMultipart } = options

  const headers = {}

  // No seteamos Content-Type para multipart/form-data (el browser lo agrega solo con el boundary)
  if (!isMultipart) {
    headers['Content-Type'] = 'application/json'
  }

  // Inyección del token — ambos esquemas usan "Bearer " como prefijo.
  // TokenCiudadanoFilter y JwtFilter del backend hacen:
  //   header.startsWith("Bearer ") → header.substring(7)
  if (ciudadanoToken) {
    headers['Authorization'] = `Bearer ${ciudadanoToken}`
  } else if (jwtToken) {
    headers['Authorization'] = `Bearer ${jwtToken}`
  }

  const fetchOptions = {
    method,
    headers,
  }

  if (body !== undefined && body !== null) {
    fetchOptions.body = isMultipart ? body : JSON.stringify(body)
  }

  const response = await fetch(`${BASE_URL}${path}`, fetchOptions)

  // 204 No Content — respuesta vacía válida
  if (response.status === 204) return null

  // Si la respuesta es exitosa, parsear JSON
  if (response.ok) {
    return response.json()
  }

  // Intentar parsear el ProblemDetail (RFC 7807) que devuelve Spring
  let errorData = {}
  try {
    errorData = await response.json()
  } catch {
    errorData = { detail: response.statusText || 'Error desconocido' }
  }

  // El backend usa 'detail' como campo principal del error (GlobalExceptionHandler)
  const message = errorData.detail || errorData.title || `Error ${response.status}`
  throw new ApiError(message, response.status, errorData)
}

export const fetchClient = {
  get:    (path, opts = {})        => request('GET',   path, opts),
  post:   (path, body, opts = {})  => request('POST',  path, { ...opts, body }),
  patch:  (path, body, opts = {})  => request('PATCH', path, { ...opts, body }),
  delete: (path, opts = {})        => request('DELETE', path, opts),
}

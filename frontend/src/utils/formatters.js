/**
 * formatters.js — Funciones de presentación de datos.
 *
 * Centraliza el formateo de fechas, monedas y estados para que
 * los componentes no contengan lógica de presentación inline.
 */

/**
 * Formatea una fecha ISO 8601 en formato local argentino.
 * Ej: "2026-03-13T10:23:00" → "13/03/2026 10:23"
 *
 * El backend serializa LocalDateTime sin sufijo de zona ('Z' ni '+00:00').
 * Sin sufijo, JS interpreta el string como hora LOCAL del browser, lo que
 * produce un desfase de +3h en Argentina (UTC-3).
 * La normalización agrega 'Z' para que Date() lo trate como UTC y
 * toLocaleString('es-AR') convierta correctamente a hora argentina.
 */
export function formatFecha(isoString) {
  if (!isoString) return '—'
  try {
    const normalized = /Z$|[+-]\d{2}:\d{2}$/.test(isoString)
      ? isoString
      : isoString + 'Z'
    const d = new Date(normalized)
    return d.toLocaleString('es-AR', {
      day:    '2-digit',
      month:  '2-digit',
      year:   'numeric',
      hour:   '2-digit',
      minute: '2-digit',
    })
  } catch {
    return isoString
  }
}

/**
 * Formatea solo la fecha (sin hora).
 * Ej: "2026-03-13T10:23:00" → "13/03/2026"
 */
export function formatFechaSolo(isoString) {
  if (!isoString) return '—'
  try {
    const normalized = /Z$|[+-]\d{2}:\d{2}$/.test(isoString)
      ? isoString
      : isoString + 'Z'
    const d = new Date(normalized)
    return d.toLocaleDateString('es-AR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    })
  } catch {
    return isoString
  }
}

/**
 * Formatea un monto en pesos argentinos.
 * Ej: 1500 → "$1.500,00"
 */
export function formatMonto(monto) {
  if (monto == null) return '—'
  return new Intl.NumberFormat('es-AR', {
    style: 'currency', currency: 'ARS',
  }).format(monto)
}

/**
 * Mapea los códigos de estado del backend a etiquetas legibles en español.
 * Usados en badges y mensajes de la UI.
 */
export function formatEstado(estado) {
  const map = {
    PENDIENTE:         'Pendiente',
    PAGADO:            'Pagado',
    PUBLICADO:         'Publicado',
    PUBLICADO_VENCIDO: 'Vencido',
    VENCIDO:           'Vencido',
  }
  return map[estado] || estado
}

/**
 * Devuelve la variante de badge correspondiente al estado.
 * Se usa como prop en el componente <Badge>.
 */
export function badgeVariantFromEstado(estado) {
  const map = {
    PENDIENTE:         'pending',
    PAGADO:            'pagado',
    PUBLICADO:         'issued',
    PUBLICADO_VENCIDO: 'rejected',
    VENCIDO:           'rejected',
  }
  return map[estado] || 'pending'
}

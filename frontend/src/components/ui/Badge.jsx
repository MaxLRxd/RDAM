/**
 * Badge.jsx — Etiqueta de estado de una solicitud.
 *
 * Variantes de la maqueta:
 *   pending  → ámbar (PENDIENTE)
 *   pagado   → azul (PAGADO)
 *   issued   → verde (PUBLICADO) — "b-issued" en la maqueta
 *   rejected → rojo (VENCIDO / PUBLICADO_VENCIDO)
 */

import styles from './Badge.module.css'

export function Badge({ children, variant = 'pending' }) {
  return (
    <span className={`${styles.badge} ${styles[variant]}`}>
      {children}
    </span>
  )
}

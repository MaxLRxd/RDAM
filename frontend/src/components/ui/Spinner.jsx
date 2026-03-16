/**
 * Spinner.jsx — Indicador de carga circular.
 * Usado en estados de loading al esperar respuestas de la API.
 */

import styles from './Spinner.module.css'

export function Spinner({ size = 'md', color = 'accent' }) {
  return (
    <div
      className={`${styles.spinner} ${styles[size]} ${styles[color]}`}
      role="status"
      aria-label="Cargando..."
    />
  )
}

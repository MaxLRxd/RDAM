/**
 * Toast.jsx — Notificación transitoria en la esquina inferior derecha.
 *
 * Se usa con el hook useToast(). Cuando visible=true se muestra y desaparece
 * automáticamente después del duration configurado en showToast().
 */

import styles from './Toast.module.css'

export function Toast({ message, visible, type = 'default' }) {
  if (!visible) return null

  return (
    <div className={`${styles.toast} ${styles[type]}`} role="status" aria-live="polite">
      {message}
    </div>
  )
}

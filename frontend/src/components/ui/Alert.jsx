/**
 * Alert.jsx — Mensajes informativos, de advertencia y de éxito.
 *
 * Variantes (tomadas de .a-info, .a-warn, .a-ok en la maqueta):
 *   info  → fondo azul suave (informativo)
 *   warn  → fondo ámbar suave (advertencia)
 *   ok    → fondo verde suave (éxito)
 *   error → fondo rojo suave (error)
 */

import styles from './Alert.module.css'

export function Alert({ children, variant = 'info', icon }) {
  return (
    <div className={`${styles.alert} ${styles[variant]}`}>
      {icon && <span className={styles.icon}>{icon}</span>}
      <span>{children}</span>
    </div>
  )
}

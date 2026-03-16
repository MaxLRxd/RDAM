/**
 * Button.jsx — Componente de botón del sistema de diseño RDAM.
 *
 * Implementa las variantes de la maqueta:
 *   primary → fondo azul oscuro (--accent)
 *   ghost   → borde y fondo transparente
 *   amber   → fondo ámbar (para el botón de pago PlusPagos)
 *   danger  → fondo rojo (acciones destructivas en panel interno)
 */

import styles from './Button.module.css'

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  disabled = false,
  type = 'button',
  onClick,
}) {
  const classes = [
    styles.btn,
    styles[variant],
    styles[size],
    fullWidth ? styles.full : '',
  ].filter(Boolean).join(' ')

  return (
    <button
      type={type}
      className={classes}
      disabled={disabled || loading}
      onClick={onClick}
    >
      {loading ? <span className={styles.spinner} aria-hidden /> : null}
      {children}
    </button>
  )
}

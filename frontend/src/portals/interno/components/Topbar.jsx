/**
 * Topbar.jsx — Barra superior de cada panel del Portal Interno.
 *
 * Muestra el título y subtítulo del panel activo a la izquierda,
 * y un slot de acciones (botones) a la derecha.
 *
 * Props:
 *   title     — string: nombre del panel (ej: "Solicitudes")
 *   subtitle  — string: descripción breve (ej: "Gestión y emisión de certificados")
 *   children  — ReactNode: botones de acción para el lado derecho
 */

import styles from './Topbar.module.css'

export function Topbar({ title, subtitle, children }) {
  return (
    <header className={styles.topbar}>
      <div className={styles.left}>
        <span className={styles.title}>{title}</span>
        {subtitle && <span className={styles.subtitle}>{subtitle}</span>}
      </div>
      {children && (
        <div className={styles.right}>
          {children}
        </div>
      )}
    </header>
  )
}

/**
 * Topbar.ActionBtn — Botón de acción primario para usar dentro del Topbar.
 *
 * Ejemplo:
 *   <Topbar title="Usuarios" subtitle="...">
 *     <Topbar.ActionBtn onClick={handleNew}>+ Nuevo usuario</Topbar.ActionBtn>
 *   </Topbar>
 */
Topbar.ActionBtn = function TopbarActionBtn({ onClick, children, ghost = false }) {
  return (
    <button
      type="button"
      className={ghost ? styles.actionBtnGhost : styles.actionBtn}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

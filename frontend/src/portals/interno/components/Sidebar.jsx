/**
 * Sidebar.jsx — Barra lateral fija del Portal Interno.
 *
 * Diseño: fondo oscuro (#0f0f0f), 220px de ancho, 3 zonas:
 *   1. Marca (logo SF + nombre del sistema)
 *   2. Navegación (Dashboard / Solicitudes / Usuarios / Auditoría)
 *   3. Usuario logueado (avatar con iniciales + rol + logout)
 *
 * Props:
 *   activePanel   — string: 'dashboard' | 'solicitudes' | 'usuarios' | 'auditoria'
 *   onNavigate    — (panel: string) => void
 *   pendingCount  — number: cantidad de solicitudes pendientes para el badge
 */

import { useAuth } from '../../../hooks/useAuth.js'
import styles from './Sidebar.module.css'

const NAV_ITEMS = [
  { id: 'dashboard',   icon: '📊', label: 'Dashboard'    },
  { id: 'solicitudes', icon: '📋', label: 'Solicitudes'  },
  { id: 'usuarios',    icon: '👥', label: 'Usuarios',  adminOnly: true },
  { id: 'auditoria',   icon: '🔍', label: 'Auditoría', adminOnly: true },
]

/** Extrae las iniciales de un email: "juan.perez@..." → "JP" */
function getInitials(username) {
  if (!username) return '?'
  const local = username.split('@')[0]         // "juan.perez"
  const parts = local.split(/[._-]/)           // ["juan", "perez"]
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase()
  }
  return local.slice(0, 2).toUpperCase()
}

export function Sidebar({ activePanel, onNavigate, pendingCount = 0 }) {
  const { auth, isAdmin, logout } = useAuth()

  return (
    <aside className={styles.sidebar}>
      {/* ── Marca ── */}
      <div className={styles.brand}>
        <div className={styles.brandRow}>
          <div className={styles.brandDot}>SF</div>
          <div className={styles.brandText}>
            <span className={styles.brandName}>Sistema RDAM</span>
            <span className={styles.brandRole}>Portal Interno</span>
          </div>
        </div>
      </div>

      {/* ── Navegación ── */}
      <nav className={styles.nav}>
        {NAV_ITEMS.map(item => {
          // Solo los ADMIN ven el panel Usuarios
          if (item.adminOnly && !isAdmin) return null

          const isActive = activePanel === item.id

          return (
            <button
              key={item.id}
              className={`${styles.navItem} ${isActive ? styles.active : ''}`}
              onClick={() => onNavigate(item.id)}
              type="button"
            >
              <span className={styles.navIcon}>{item.icon}</span>
              <span className={styles.navLabel}>{item.label}</span>
              {/* Badge de pendientes solo en Solicitudes */}
              {item.id === 'solicitudes' && pendingCount > 0 && (
                <span className={styles.navBadge}>{pendingCount}</span>
              )}
            </button>
          )
        })}
      </nav>

      {/* ── Área de usuario ── */}
      <div className={styles.userArea}>
        <div className={styles.userCard}>
          <div className={styles.avatar}>
            {getInitials(auth?.username)}
          </div>
          <div className={styles.userInfo}>
            <div className={styles.userName}>{auth?.username ?? '—'}</div>
            <div className={styles.userRole}>{auth?.rol ?? ''}</div>
          </div>
        </div>
        <button className={styles.logoutBtn} type="button" onClick={logout}>
          <span>↩</span>
          <span>Cerrar sesión</span>
        </button>
      </div>
    </aside>
  )
}

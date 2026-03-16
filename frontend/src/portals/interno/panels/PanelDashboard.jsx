/**
 * PanelDashboard.jsx — Panel de inicio del Portal Interno.
 *
 * Correcciones aplicadas:
 *   1A. Métricas reales: total (GET /solicitudes?size=1) y publicados
 *       (GET /solicitudes?estado=PUBLICADO&size=1). "Pendiente de cert."
 *       viene del prop pendingCount que calcula PanelSolicitudes.
 *   1B. Actividad reciente: consume GET /auditoria?size=5 en lugar del
 *       array ACTIVITY hardcodeado. Fallback gracioso si el endpoint
 *       aún no responde.
 *   1C. "Ver auditoría" solo visible para ADMIN (useAuth + isAdmin).
 *
 * Props:
 *   onNavigate   — (panel: string) => void
 *   pendingCount — number: cantidad de solicitudes PAGADO (del sidebar)
 */

import { useState, useEffect } from 'react'
import { useAuth }             from '../../../hooks/useAuth.js'
import { listarSolicitudes, listarAuditoria } from '../../../api/internoApi.js'
import { formatFecha }         from '../../../utils/formatters.js'
import { Topbar }              from '../components/Topbar.jsx'
import { Spinner }             from '../../../components/ui/Spinner.jsx'
import styles from './Panels.module.css'

const ACCION_ICONS = {
  CERTIFICADO_PUBLICADO: '✅',
  PAGO_CONFIRMADO:       '💳',
  SOLICITUD_CREADA:      '📋',
  TOKEN_REGENERADO:      '🔄',
  USUARIO_DESACTIVADO:   '🔴',
  USUARIO_CREADO:        '👤',
  LOGIN_EXITOSO:         '🔑',
}

export function PanelDashboard({ onNavigate, pendingCount = 0 }) {
  const { token, isAdmin } = useAuth()

  // ─── Métricas ─────────────────────────────────────────────────────────────
  const [total,      setTotal]      = useState(null)
  const [publicados, setPublicados] = useState(null)

  // ─── Actividad reciente ───────────────────────────────────────────────────
  const [activity,        setActivity]        = useState([])
  const [activityLoading, setActivityLoading] = useState(true)

  useEffect(() => {
    if (!token) return

    // Métricas: dos llamadas paralelas al endpoint existente con size=1
    Promise.all([
      listarSolicitudes({ page: 0, size: 1 }, token),
      listarSolicitudes({ estado: 'PUBLICADO', page: 0, size: 1 }, token),
    ])
      .then(([all, pub]) => {
        setTotal(all.totalElements ?? 0)
        setPublicados(pub.totalElements ?? 0)
      })
      .catch(() => { /* silencioso — los "—" indican carga fallida */ })

    // Actividad reciente: últimas 5 entradas de auditoría
    listarAuditoria({ page: 0, size: 5 }, token)
      .then(data => setActivity(data.content ?? []))
      .catch(() => setActivity([]))
      .finally(() => setActivityLoading(false))
  }, [token])

  return (
    <div>
      <Topbar
        title="Dashboard"
        subtitle="Resumen del sistema RDAM"
      />

      <div className={styles.panelBody}>
        {/* ── Tarjetas de métricas ── */}
        <div className={styles.metricsGrid}>
          <div className={styles.metricCard}>
            <div className={styles.metricIcon}>📋</div>
            <div className={styles.metricInfo}>
              <span className={styles.metricValue}>
                {total === null ? '—' : total}
              </span>
              <span className={styles.metricLabel}>Total solicitudes</span>
            </div>
          </div>
          <div className={`${styles.metricCard} ${styles.metricAmber}`}>
            <div className={styles.metricIcon}>⏳</div>
            <div className={styles.metricInfo}>
              <span className={styles.metricValue}>{pendingCount}</span>
              <span className={styles.metricLabel}>Pendiente de cert.</span>
            </div>
          </div>
          <div className={`${styles.metricCard} ${styles.metricGreen}`}>
            <div className={styles.metricIcon}>✅</div>
            <div className={styles.metricInfo}>
              <span className={styles.metricValue}>
                {publicados === null ? '—' : publicados}
              </span>
              <span className={styles.metricLabel}>Publicados (total)</span>
            </div>
          </div>
        </div>

        {/* ── Grid 2 columnas ── */}
        <div className={styles.dashGrid}>
          {/* Acciones rápidas */}
          <div className={styles.dashCard}>
            <div className={styles.dashCardHead}>⚡ Acciones rápidas</div>
            <div className={styles.dashCardBody}>
              <button
                type="button"
                className={styles.quickBtn}
                onClick={() => onNavigate('solicitudes')}
              >
                <span>📋</span>
                <span>Ver todas las solicitudes</span>
              </button>
              <button
                type="button"
                className={styles.quickBtn}
                onClick={() => onNavigate('solicitudes')}
              >
                <span>💳</span>
                <span>Solicitudes listas para certificar</span>
              </button>
              {/* 1C: "Ver auditoría" solo para ADMIN */}
              {isAdmin && (
                <button
                  type="button"
                  className={styles.quickBtn}
                  onClick={() => onNavigate('auditoria')}
                >
                  <span>🔍</span>
                  <span>Ver auditoría</span>
                </button>
              )}
            </div>
          </div>

          {/* Actividad reciente */}
          <div className={styles.dashCard}>
            <div className={styles.dashCardHead}>🕐 Actividad reciente</div>
            <div className={styles.dashCardBody}>
              {activityLoading ? (
                <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}>
                  <Spinner size="sm" />
                </div>
              ) : activity.length === 0 ? (
                <p style={{ fontSize: '13px', color: 'var(--ink-3)', textAlign: 'center', padding: '12px' }}>
                  Sin actividad reciente registrada.
                </p>
              ) : (
                activity.map((item, i) => (
                  <div key={item.id ?? i} className={styles.activityItem}>
                    <span className={styles.activityIcon}>
                      {ACCION_ICONS[item.accion] ?? '📌'}
                    </span>
                    <div className={styles.activityContent}>
                      <span className={styles.activityText}>{item.detalle}</span>
                      <span className={styles.activityTime}>
                        {formatFecha(item.timestamp)}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

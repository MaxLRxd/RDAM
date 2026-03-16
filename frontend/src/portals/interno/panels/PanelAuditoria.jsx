/**
 * PanelAuditoria.jsx — Panel de auditoría del Portal Interno.
 *
 * Consume GET /api/v1/auditoria (ADMIN only) con paginación y filtro
 * opcional por tipo de operación.
 *
 * Funcionalidades:
 *   - Chips de filtro por tipo de operación (Todas / cada operación relevante)
 *   - Timeline paginada (20 entradas por página)
 *   - Loading / error / empty states
 *   - Timestamps corregidos a UTC (igual que el resto del panel interno)
 *
 * Integración con backend:
 *   GET /auditoria?page=0&size=20&sort=fechaHora,desc[&accion=TIPO]
 *   Respuesta: Spring Page<AuditoriaResponse> { content, totalElements, totalPages, number }
 */

import { useState, useEffect, useCallback } from 'react'
import { useAuth }          from '../../../hooks/useAuth.js'
import { listarAuditoria }  from '../../../api/internoApi.js'
import { formatFecha }      from '../../../utils/formatters.js'
import { Spinner }          from '../../../components/ui/Spinner.jsx'
import { Topbar }           from '../components/Topbar.jsx'
import styles from './Panels.module.css'

const PAGE_SIZE = 20

// Iconos por tipo de operación (alineados con AuditoriaOperacion.Operaciones)
const ACCION_ICONS = {
  CERTIFICADO_PUBLICADO: '✅',
  PAGO_CONFIRMADO:       '💳',
  SOLICITUD_CREADA:      '📋',
  TOKEN_REGENERADO:      '🔄',
  DESCARGA_CERTIFICADO:  '⬇️',
  USUARIO_CREADO:        '👤',
  USUARIO_ACTIVADO:      '🟢',
  USUARIO_DESACTIVADO:   '🔴',
  LOGIN_EXITOSO:         '🔑',
  LOGIN_FALLIDO:         '⛔',
}

const ACCION_CHIPS = [
  { label: 'Todas',           value: null },
  { label: 'Certificados',    value: 'CERTIFICADO_PUBLICADO' },
  { label: 'Pagos',           value: 'PAGO_CONFIRMADO' },
  { label: 'Solicitudes',     value: 'SOLICITUD_CREADA' },
  { label: 'Tokens',          value: 'TOKEN_REGENERADO' },
  { label: 'Usuarios',        value: 'USUARIO_CREADO' },
  { label: 'Logins',          value: 'LOGIN_EXITOSO' },
]

export function PanelAuditoria() {
  const { token } = useAuth()

  // ─── Filtros ──────────────────────────────────────────────────────────────
  const [accionFiltro, setAccionFiltro] = useState(null)

  // ─── Paginación ───────────────────────────────────────────────────────────
  const [page, setPage] = useState(0)

  // ─── Datos ────────────────────────────────────────────────────────────────
  const [loading,  setLoading]  = useState(false)
  const [pageData, setPageData] = useState(null)
  const [error,    setError]    = useState(null)

  // ─── Carga de datos ───────────────────────────────────────────────────────

  const loadData = useCallback(async (currentPage = 0) => {
    setLoading(true)
    setError(null)
    try {
      const data = await listarAuditoria(
        { page: currentPage, size: PAGE_SIZE, accion: accionFiltro },
        token
      )
      setPageData(data)
    } catch (err) {
      setError(err.message || 'Error al cargar el registro de auditoría')
    } finally {
      setLoading(false)
    }
  }, [token, accionFiltro])

  // Cargar al montar y cuando cambia el filtro de acción
  useEffect(() => {
    setPage(0)
    loadData(0)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accionFiltro])

  function handlePageChange(newPage) {
    setPage(newPage)
    loadData(newPage)
  }

  const rows   = pageData?.content      ?? []
  const total  = pageData?.totalElements ?? 0
  const totalP = pageData?.totalPages    ?? 0

  return (
    <div>
      <Topbar
        title="Auditoría"
        subtitle="Registro de operaciones del sistema"
      />

      <div className={styles.panelBody}>

        {/* ── Chips de filtro ── */}
        <div className={styles.chips}>
          {ACCION_CHIPS.map(chip => (
            <button
              key={chip.label}
              type="button"
              className={`${styles.chip} ${accionFiltro === chip.value ? styles.chipActive : ''}`}
              onClick={() => setAccionFiltro(chip.value)}
            >
              {chip.label}
            </button>
          ))}
        </div>

        {/* ── Contenido ── */}
        {loading ? (
          <div className={styles.emptyState}>
            <Spinner size="md" />
          </div>
        ) : error ? (
          <div className={styles.emptyState}>
            <span>⚠️</span>
            <p>{error}</p>
          </div>
        ) : rows.length === 0 ? (
          <div className={styles.emptyState}>
            <span>📭</span>
            <p>No hay entradas de auditoría para los filtros aplicados.</p>
          </div>
        ) : (
          <>
            {/* ── Timeline ── */}
            <div className={styles.timeline}>
              {rows.map(entry => (
                <div key={entry.id} className={styles.timelineItem}>
                  <div className={styles.timelineDot} />
                  <div className={styles.timelineContent}>
                    <div className={styles.timelineAction}>
                      {ACCION_ICONS[entry.accion] ?? '📌'}{' '}
                      {entry.accion.replace(/_/g, ' ')}
                    </div>
                    <div className={styles.timelineMeta}>
                      <span>📅 {formatFecha(entry.timestamp)}</span>
                      <span>👤 {entry.usuario ?? 'sistema'}</span>
                    </div>
                    {entry.detalle && (
                      <div style={{ marginTop: '4px', fontSize: '12px', color: 'var(--ink-3)' }}>
                        {entry.detalle}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* ── Paginación ── */}
            <div className={styles.pagination}>
              <span>
                Mostrando {rows.length} de {total} entradas
              </span>
              <div className={styles.paginationBtns}>
                <button
                  type="button"
                  className={styles.pageBtn}
                  onClick={() => handlePageChange(page - 1)}
                  disabled={page === 0}
                >
                  ← Anterior
                </button>
                <button
                  type="button"
                  className={`${styles.pageBtn} ${styles.pageBtnActive}`}
                  disabled
                >
                  {page + 1} / {Math.max(totalP, 1)}
                </button>
                <button
                  type="button"
                  className={styles.pageBtn}
                  onClick={() => handlePageChange(page + 1)}
                  disabled={page >= totalP - 1}
                >
                  Siguiente →
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

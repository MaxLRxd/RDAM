/**
 * PanelSolicitudes.jsx — Panel de gestión de solicitudes.
 *
 * Réplica de screen-solicitudes de la maqueta rdam-interno.html.
 *
 * Funcionalidades:
 *   - Búsqueda por DNI/CUIL (input de texto)
 *   - Filtro por circunscripción (select, solo ADMIN ve todas)
 *   - Chips de filtro por estado (Todas / Pendiente pago / Pagado / Publicado / Vencido)
 *   - Tabla paginada con columnas: ID · DNI/CUIL · Email · Circ. · Estado · Fecha · Acciones
 *   - Botón "Cargar cert." para solicitudes en estado PAGADO
 *   - Paginación (prev / página actual / next)
 *
 * Integración con backend:
 *   - GET /solicitudes con query params: circunscripcion, estado, dniCuil, page, size, sort
 *   - El backend devuelve Spring Page<T>: { content, totalElements, totalPages, number }
 *
 * Props:
 *   onPendingCount — (count: number) => void: informa al InternoDashboard cuántas
 *                    solicitudes PAGADO hay (para el badge del sidebar)
 */

import { useState, useEffect, useCallback } from 'react'
import { useAuth }               from '../../../hooks/useAuth.js'
import { listarSolicitudes }     from '../../../api/internoApi.js'
import { CIRCUNSCRIPCIONES, ESTADOS } from '../../../utils/constants.js'
import { formatFecha, formatEstado, badgeVariantFromEstado } from '../../../utils/formatters.js'
import { Badge }   from '../../../components/ui/Badge.jsx'
import { Spinner } from '../../../components/ui/Spinner.jsx'
import { ModalDetalleSolicitud } from '../modals/ModalDetalleSolicitud.jsx'
import { Topbar }                from '../components/Topbar.jsx'
import styles from './Panels.module.css'

const PAGE_SIZE = 15

const ESTADO_CHIPS = [
  { label: 'Todas',          value: null },
  { label: 'Pendiente pago', value: ESTADOS.PENDIENTE },
  { label: 'Pagado',         value: ESTADOS.PAGADO },
  { label: 'Publicado',      value: ESTADOS.PUBLICADO },
  { label: 'Vencido',        value: ESTADOS.VENCIDO },
]

export function PanelSolicitudes({ onPendingCount }) {
  const { token, isAdmin, auth } = useAuth()

  // ─── Filtros ──────────────────────────────────────────────────────────────
  const [dniCuil,         setDniCuil]         = useState('')
  const [circunscripcion, setCircunscripcion] = useState(
    // OPERADOR siempre ve su circunscripción; ADMIN empieza con "todas"
    isAdmin ? '' : String(auth?.circunscripcion ?? '')
  )
  const [estadoFiltro, setEstadoFiltro] = useState(null) // null = todas

  // ─── Paginación ───────────────────────────────────────────────────────────
  const [page, setPage] = useState(0)

  // ─── Datos ────────────────────────────────────────────────────────────────
  const [loading,  setLoading]  = useState(false)
  const [pageData, setPageData] = useState(null) // Spring Page<T>
  const [error,    setError]    = useState(null)

  // ─── Modal ────────────────────────────────────────────────────────────────
  const [selectedSolicitud, setSelectedSolicitud] = useState(null)

  // ─── Carga de datos ───────────────────────────────────────────────────────

  const loadData = useCallback(async (currentPage = 0) => {
    setLoading(true)
    setError(null)
    try {
      const data = await listarSolicitudes(
        {
          circunscripcion: circunscripcion || null,
          estado:          estadoFiltro,
          dniCuil:         dniCuil.trim() || null,
          page:            currentPage,
          size:            PAGE_SIZE,
        },
        token
      )
      setPageData(data)

      // Informar al dashboard cuántas solicitudes están listas para certificar
      if (!estadoFiltro && !dniCuil.trim()) {
        // Hacer una consulta adicional liviana para contar PAGADO
        try {
          const pagadoData = await listarSolicitudes(
            { estado: ESTADOS.PAGADO, page: 0, size: 1 },
            token
          )
          onPendingCount?.(pagadoData.totalElements ?? 0)
        } catch {
          // Ignorar — es solo para el badge
        }
      }
    } catch (err) {
      setError(err.message || 'Error al cargar las solicitudes')
    } finally {
      setLoading(false)
    }
  }, [token, circunscripcion, estadoFiltro, dniCuil, onPendingCount])

  // Cargar al montar y cuando cambian los filtros
  useEffect(() => {
    setPage(0)
    loadData(0)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [circunscripcion, estadoFiltro])

  // Buscar por DNI con debounce simple (300ms)
  useEffect(() => {
    const t = setTimeout(() => {
      setPage(0)
      loadData(0)
    }, 300)
    return () => clearTimeout(t)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dniCuil])

  function handlePageChange(newPage) {
    setPage(newPage)
    loadData(newPage)
  }

  function handleCertificadoSubido() {
    setSelectedSolicitud(null)
    loadData(page)
  }

  const rows   = pageData?.content      ?? []
  const total  = pageData?.totalElements ?? 0
  const totalP = pageData?.totalPages    ?? 0

  return (
    <div>
      <Topbar
        title="Solicitudes"
        subtitle="Gestión y emisión de certificados RDAM"
      />

      <div className={styles.panelBody}>
        {/* ── Barra de filtros ── */}
        <div className={styles.filterBar}>
          <input
            type="text"
            className={styles.searchInput}
            placeholder="Buscar por DNI o CUIL..."
            value={dniCuil}
            onChange={e => setDniCuil(e.target.value)}
          />
          {/* OPERADOR ve solo su circunscripción, ADMIN puede filtrar */}
          {isAdmin && (
            <select
              className={styles.filterSelect}
              value={circunscripcion}
              onChange={e => setCircunscripcion(e.target.value)}
            >
              <option value="">Todas las circunscripciones</option>
              {CIRCUNSCRIPCIONES.map(c => (
                <option key={c.id} value={c.id}>{c.label}</option>
              ))}
            </select>
          )}
        </div>

        {/* ── Chips de estado ── */}
        <div className={styles.chips}>
          {ESTADO_CHIPS.map(chip => (
            <button
              key={chip.label}
              type="button"
              className={`${styles.chip} ${estadoFiltro === chip.value ? styles.chipActive : ''}`}
              onClick={() => setEstadoFiltro(chip.value)}
            >
              {chip.label}
            </button>
          ))}
        </div>

        {/* ── Tabla ── */}
        <div className={styles.tableWrapper}>
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
              <p>No se encontraron solicitudes con los filtros aplicados.</p>
            </div>
          ) : (
            <>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Nro. Trámite</th>
                    <th>DNI/CUIL</th>
                    <th>Email</th>
                    <th>Circunscripción</th>
                    <th>Estado</th>
                    <th>Fecha</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map(sol => (
                    <tr key={sol.id ?? sol.nroTramite}>
                      <td className={styles.monoText}>{sol.nroTramite}</td>
                      <td className={styles.monoText}>{sol.dniCuil}</td>
                      <td style={{ fontSize: '12px' }}>{sol.email}</td>
                      <td style={{ fontSize: '12px' }}>{sol.circunscripcion?.nombre ?? '—'}</td>
                      <td>
                        <Badge variant={badgeVariantFromEstado(sol.estado)}>
                          {formatEstado(sol.estado)}
                        </Badge>
                      </td>
                      <td style={{ fontSize: '12px', color: 'var(--ink-3)' }}>
                        {formatFecha(sol.fechaCreacion)}
                      </td>
                      <td>
                        {sol.estado === ESTADOS.PAGADO ? (
                          <button
                            type="button"
                            className={`${styles.tableBtn} ${styles.tableBtnAmber}`}
                            onClick={() => setSelectedSolicitud(sol)}
                          >
                            📄 Cargar cert.
                          </button>
                        ) : (
                          <button
                            type="button"
                            className={styles.tableBtn}
                            onClick={() => setSelectedSolicitud(sol)}
                          >
                            Ver detalle
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* ── Paginación ── */}
              <div className={styles.pagination}>
                <span>
                  Mostrando {rows.length} de {total} solicitudes
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

      {/* ── Modal detalle / certificado ── */}
      {selectedSolicitud && (
        <ModalDetalleSolicitud
          solicitud={selectedSolicitud}
          onClose={() => setSelectedSolicitud(null)}
          onCertificadoSubido={handleCertificadoSubido}
        />
      )}
    </div>
  )
}

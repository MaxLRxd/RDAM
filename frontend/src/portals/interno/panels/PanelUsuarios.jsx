/**
 * PanelUsuarios.jsx — Panel de gestión de usuarios internos.
 *
 * Solo visible para ADMIN.
 *
 * Corrección aplicada (punto 3):
 *   Se agrega fetch real a GET /usuarios-internos al montar el panel.
 *   La lista ahora persiste entre sesiones. Los usuarios creados en esta
 *   sesión se prependen optimísticamente vía handleUsuarioCreado.
 *   Si la llamada falla, se muestra el error sin romper el panel.
 *
 * Funcionalidades:
 *   - Listar todos los usuarios (GET /usuarios-internos)
 *   - Crear nuevo usuario (ModalNuevoUsuario → POST /usuarios-internos)
 *   - Activar/desactivar (PATCH /usuarios-internos/{id}/estado)
 */

import { useState, useEffect }  from 'react'
import { useAuth }              from '../../../hooks/useAuth.js'
import { listarUsuarios, cambiarEstadoUsuario } from '../../../api/internoApi.js'
import { getCircunscripcionLabel } from '../../../utils/constants.js'
import { ModalNuevoUsuario }    from '../modals/ModalNuevoUsuario.jsx'
import { Badge }                from '../../../components/ui/Badge.jsx'
import { Spinner }              from '../../../components/ui/Spinner.jsx'
import { Topbar }               from '../components/Topbar.jsx'
import styles from './Panels.module.css'

export function PanelUsuarios() {
  const { token } = useAuth()

  const [showModal,  setShowModal]  = useState(false)
  const [usuarios,   setUsuarios]   = useState([])
  const [loading,    setLoading]    = useState(true)
  const [error,      setError]      = useState(null)
  const [togglingId, setTogglingId] = useState(null)

  // ─── Carga inicial desde el backend ───────────────────────────────────────
  useEffect(() => {
    setLoading(true)
    listarUsuarios(token)
      .then(data => setUsuarios(data.content ?? []))
      .catch(err => setError(err.message || 'Error al cargar usuarios'))
      .finally(() => setLoading(false))
  }, [token])

  // ─── Alta optimística: prepend sin necesidad de re-fetch ──────────────────
  function handleUsuarioCreado(usuario) {
    setUsuarios(prev => [usuario, ...prev])
    setShowModal(false)
  }

  async function handleToggleActivo(usuario) {
    setTogglingId(usuario.id)
    try {
      const updated = await cambiarEstadoUsuario(usuario.id, !usuario.activo, token)
      setUsuarios(prev =>
        prev.map(u => u.id === updated.id ? updated : u)
      )
    } catch (err) {
      console.error('Error al cambiar estado:', err.message)
    } finally {
      setTogglingId(null)
    }
  }

  return (
    <div>
      <Topbar
        title="Usuarios"
        subtitle="Gestión de accesos al portal interno"
      >
        <Topbar.ActionBtn onClick={() => setShowModal(true)}>
          + Nuevo usuario
        </Topbar.ActionBtn>
      </Topbar>

      <div className={styles.panelBody}>
        <div className={styles.tableWrapper}>
          {loading ? (
            <div className={styles.emptyState}><Spinner size="md" /></div>
          ) : error ? (
            <div className={styles.emptyState}>
              <span>⚠️</span><p>{error}</p>
            </div>
          ) : usuarios.length === 0 ? (
            <div className={styles.emptyState}>
              <span>👥</span>
              <p>No hay usuarios registrados. Use "+ Nuevo usuario" para agregar.</p>
            </div>
          ) : (
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Rol</th>
                  <th>Circunscripción</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map(u => (
                  <tr key={u.id}>
                    <td style={{ fontSize: '13px' }}>{u.username}</td>
                    <td>
                      <span style={{
                        fontSize: '11px',
                        fontWeight: '600',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        background: u.rol === 'ADMIN' ? 'rgba(26,58,107,0.1)' : 'rgba(21,106,64,0.1)',
                        color: u.rol === 'ADMIN' ? 'var(--accent)' : 'var(--green)',
                      }}>
                        {u.rol}
                      </span>
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--ink-3)' }}>
                      {u.circunscripcion
                        ? getCircunscripcionLabel(u.circunscripcion)
                        : 'Todas'}
                    </td>
                    <td>
                      <Badge variant={u.activo ? 'issued' : 'rejected'}>
                        {u.activo ? 'Activo' : 'Inactivo'}
                      </Badge>
                    </td>
                    <td>
                      <button
                        type="button"
                        className={styles.tableBtn}
                        onClick={() => handleToggleActivo(u)}
                        disabled={togglingId === u.id}
                      >
                        {togglingId === u.id
                          ? '...'
                          : u.activo ? 'Desactivar' : 'Activar'
                        }
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showModal && (
        <ModalNuevoUsuario
          onClose={() => setShowModal(false)}
          onCreated={handleUsuarioCreado}
        />
      )}
    </div>
  )
}

/**
 * ModalNuevoUsuario.jsx — Modal para crear un nuevo usuario interno.
 *
 * Solo visible para ADMIN (el padre ya verifica el rol antes de montar).
 *
 * Campos:
 *   - username    (email institucional, @Email, mínimo requerido)
 *   - password    (mínimo 8 caracteres)
 *   - rol         ('OPERADOR' | 'ADMIN')
 *   - idCircunscripcion  (requerido solo para OPERADOR, 1-5)
 *
 * Contrato con el backend: POST /usuarios-internos
 * Request: { username, password, rol, idCircunscripcion? }
 * Response: UsuarioInternoResponse { id, username, rol, circunscripcion, activo }
 *
 * Props:
 *   onClose     — () => void
 *   onCreated   — (usuario: UsuarioInternoResponse) => void
 */

import { useState } from 'react'
import { useAuth }         from '../../../hooks/useAuth.js'
import { crearUsuario }    from '../../../api/internoApi.js'
import { CIRCUNSCRIPCIONES } from '../../../utils/constants.js'
import { Button }          from '../../../components/ui/Button.jsx'
import { Alert }           from '../../../components/ui/Alert.jsx'
import styles from './Modals.module.css'

export function ModalNuevoUsuario({ onClose, onCreated }) {
  const { token } = useAuth()

  const [form, setForm] = useState({
    username:         '',
    password:         '',
    rol:              'OPERADOR',
    idCircunscripcion: '',
  })
  const [errors,  setErrors]  = useState({})
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState(null)

  const isOperador = form.rol === 'OPERADOR'

  function handleChange(field, value) {
    setForm(prev => ({ ...prev, [field]: value }))
    setErrors(prev => ({ ...prev, [field]: '' }))
    setApiError(null)
  }

  function validate() {
    const errs = {}
    if (!form.username.trim()) {
      errs.username = 'El email es requerido'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.username)) {
      errs.username = 'Ingrese un email válido'
    }
    if (!form.password) {
      errs.password = 'La contraseña es requerida'
    } else if (form.password.length < 8) {
      errs.password = 'La contraseña debe tener al menos 8 caracteres'
    }
    if (isOperador && !form.idCircunscripcion) {
      errs.idCircunscripcion = 'La circunscripción es requerida para OPERADOR'
    }
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }

    setLoading(true)
    setApiError(null)
    try {
      const payload = {
        username: form.username.trim(),
        password: form.password,
        rol:      form.rol,
      }
      if (isOperador && form.idCircunscripcion) {
        payload.idCircunscripcion = Number(form.idCircunscripcion)
      }
      const usuario = await crearUsuario(payload, token)
      onCreated?.(usuario)
    } catch (err) {
      setApiError(err.message || 'Error al crear el usuario. Intente nuevamente.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.overlay} onClick={e => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal} role="dialog" aria-modal="true">

        {/* ── Cabecera ── */}
        <div className={styles.modalHead}>
          <span className={styles.modalTitle}>👥 Nuevo usuario interno</span>
          <button className={styles.closeBtn} onClick={onClose} type="button" aria-label="Cerrar">
            ✕
          </button>
        </div>

        {/* ── Cuerpo ── */}
        <form onSubmit={handleSubmit} noValidate>
          <div className={styles.modalBody}>
            {apiError && (
              <Alert variant="error" icon="✕">{apiError}</Alert>
            )}

            <div className={styles.fGroup}>
              <label className={styles.fLabel} htmlFor="nu-username">
                Email institucional
              </label>
              <input
                id="nu-username"
                type="email"
                className={`${styles.fInput} ${errors.username ? styles.fError : ''}`}
                placeholder="operador@justiciasantafe.gov.ar"
                value={form.username}
                onChange={e => handleChange('username', e.target.value)}
                autoComplete="off"
              />
              {errors.username && (
                <span className={styles.fErrorMsg}>{errors.username}</span>
              )}
            </div>

            <div className={styles.fGroup}>
              <label className={styles.fLabel} htmlFor="nu-password">
                Contraseña inicial
              </label>
              <input
                id="nu-password"
                type="password"
                className={`${styles.fInput} ${errors.password ? styles.fError : ''}`}
                placeholder="Mínimo 8 caracteres"
                value={form.password}
                onChange={e => handleChange('password', e.target.value)}
                autoComplete="new-password"
              />
              {errors.password && (
                <span className={styles.fErrorMsg}>{errors.password}</span>
              )}
            </div>

            <div className={styles.fGroup}>
              <label className={styles.fLabel} htmlFor="nu-rol">
                Rol
              </label>
              <select
                id="nu-rol"
                className={styles.fSelect}
                value={form.rol}
                onChange={e => handleChange('rol', e.target.value)}
              >
                <option value="OPERADOR">Operador</option>
                <option value="ADMIN">Administrador</option>
              </select>
            </div>

            {/* Circunscripción (solo para OPERADOR) */}
            {isOperador && (
              <div className={styles.fGroup}>
                <label className={styles.fLabel} htmlFor="nu-circ">
                  Circunscripción asignada
                </label>
                <select
                  id="nu-circ"
                  className={`${styles.fSelect} ${errors.idCircunscripcion ? styles.fError : ''}`}
                  value={form.idCircunscripcion}
                  onChange={e => handleChange('idCircunscripcion', e.target.value)}
                >
                  <option value="">Seleccione una circunscripción</option>
                  {CIRCUNSCRIPCIONES.map(c => (
                    <option key={c.id} value={c.id}>{c.label}</option>
                  ))}
                </select>
                {errors.idCircunscripcion && (
                  <span className={styles.fErrorMsg}>{errors.idCircunscripcion}</span>
                )}
              </div>
            )}

            <p className={styles.hint}>
              El usuario recibirá el acceso con las credenciales ingresadas.
              Los ADMIN no tienen circunscripción asignada y pueden ver todas las solicitudes.
            </p>
          </div>

          {/* ── Pie ── */}
          <div className={styles.modalFoot}>
            <Button variant="ghost" type="button" onClick={onClose} disabled={loading}>
              Cancelar
            </Button>
            <Button type="submit" loading={loading} disabled={loading}>
              Crear usuario
            </Button>
          </div>
        </form>

      </div>
    </div>
  )
}

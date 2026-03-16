/**
 * StepFormulario.jsx — Paso 1: Formulario de nueva solicitud (HU01).
 *
 * Réplica de screen-form de la maqueta rdam-ciudadano.html.
 *
 * Responsabilidades:
 *   1. Valida el formulario localmente antes de llamar la API.
 *   2. Muestra el PopupArancel para confirmación del ciudadano.
 *   3. Llama a POST /api/v1/solicitudes.
 *   4. Al éxito, notifica al padre para avanzar al step 'validar-email'.
 *
 * Validación del campo dniCuil:
 *   - El backend acepta solo dígitos (7-11 chars), regex ^[0-9]{7,11}$
 *   - La maqueta muestra "Sin puntos para DNI. Con guiones para CUIL."
 *   - Sanitizamos guiones y puntos antes de enviar al backend.
 */

import { useState } from 'react'
import { crearSolicitud } from '../../../api/solicitudesApi.js'
import { CIRCUNSCRIPCIONES } from '../../../utils/constants.js'
import { Button } from '../../../components/ui/Button.jsx'
import { Alert } from '../../../components/ui/Alert.jsx'
import { PopupArancel } from '../PopupArancel.jsx'
import styles from './Steps.module.css'

export function StepFormulario({ onSuccess }) {
  const [form, setForm] = useState({ dniCuil: '', email: '', idCircunscripcion: '' })
  const [errors, setErrors] = useState({})
  const [showPopup, setShowPopup] = useState(false)
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState(null)

  // ─── Validación local ───────────────────────────────────────────────────

  function validate() {
    const errs = {}
    const raw = form.dniCuil.replace(/[-. ]/g, '')
    if (!raw) {
      errs.dniCuil = 'El DNI/CUIL es obligatorio'
    } else if (!/^[0-9]{7,11}$/.test(raw)) {
      errs.dniCuil = 'Ingrese un DNI (7-8 dígitos) o CUIL (11 dígitos)'
    }
    if (!form.email) {
      errs.email = 'El email es obligatorio'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      errs.email = 'Ingrese un email válido'
    }
    if (!form.idCircunscripcion) {
      errs.idCircunscripcion = 'Debe seleccionar una circunscripción'
    }
    return errs
  }

  function handleSubmitClick(e) {
    e.preventDefault()
    setApiError(null)
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }
    setErrors({})
    setShowPopup(true)
  }

  // ─── Envío a la API después de confirmar el popup ───────────────────────

  async function handleConfirm() {
    setShowPopup(false)
    setLoading(true)
    setApiError(null)
    try {
      const dniCuilClean = form.dniCuil.replace(/[-. ]/g, '')
      const data = await crearSolicitud({
        dniCuil:          dniCuilClean,
        email:            form.email.trim(),
        idCircunscripcion: Number(form.idCircunscripcion),
      })
      // Pasar datos al padre para que actualice el contexto y avance el step
      onSuccess({
        idSolicitud: data.idSolicitud,
        nroTramite:  data.nroTramite,
        email:       form.email.trim(),
        dniCuil:     dniCuilClean,
      })
    } catch (err) {
      setApiError(err.message || 'Error al crear la solicitud. Intente nuevamente.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {showPopup && (
        <PopupArancel
          onConfirm={handleConfirm}
          onCancel={() => setShowPopup(false)}
        />
      )}

      <div className={`${styles.card} fade-up`}>
        <div className={styles.cardTop}>
          <span>📋</span>
          <h2>Nueva Solicitud de Certificado</h2>
        </div>
        <div className={styles.cardBody}>
          <Alert variant="info" icon="ℹ️">
            El arancel se cobra por la emisión del certificado, no por la deuda.
            Cualquier persona puede solicitar el certificado de un tercero.
          </Alert>

          {apiError && (
            <Alert variant="error" icon="✕">
              {apiError}
            </Alert>
          )}

          <form onSubmit={handleSubmitClick} noValidate>
            {/* DNI / CUIL */}
            <div className={styles.fGroup}>
              <label className={styles.fLabel}>DNI / CUIL del consultado</label>
              <input
                className={`${styles.fInput} ${errors.dniCuil ? styles.fInputError : ''}`}
                type="text"
                placeholder="Ej: 30123456 o 20-30123456-7"
                maxLength={13}
                value={form.dniCuil}
                onChange={e => setForm(f => ({ ...f, dniCuil: e.target.value }))}
              />
              {errors.dniCuil
                ? <p className={styles.fError}>{errors.dniCuil}</p>
                : <p className={styles.fHint}>Sin puntos para DNI. Con guiones para CUIL.</p>
              }
            </div>

            {/* Email */}
            <div className={styles.fGroup}>
              <label className={styles.fLabel}>Su correo electrónico</label>
              <input
                className={`${styles.fInput} ${errors.email ? styles.fInputError : ''}`}
                type="email"
                placeholder="ejemplo@correo.com"
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
              />
              {errors.email
                ? <p className={styles.fError}>{errors.email}</p>
                : <p className={styles.fHint}>Recibirá el código de verificación aquí.</p>
              }
            </div>

            {/* Circunscripción */}
            <div className={styles.fGroup}>
              <label className={styles.fLabel}>Circunscripción Judicial</label>
              <select
                className={`${styles.fSelect} ${errors.idCircunscripcion ? styles.fInputError : ''}`}
                value={form.idCircunscripcion}
                onChange={e => setForm(f => ({ ...f, idCircunscripcion: e.target.value }))}
              >
                <option value="">— Seleccione su circunscripción —</option>
                {CIRCUNSCRIPCIONES.map(c => (
                  <option key={c.id} value={c.id}>{c.label}</option>
                ))}
              </select>
              {errors.idCircunscripcion
                ? <p className={styles.fError}>{errors.idCircunscripcion}</p>
                : (
                  <p className={styles.fHint}>
                    ¿No sabe a qué circunscripción pertenece?{' '}
                    <a
                      href="https://www.justiciasantafe.gov.ar/index.php/poder-judicial/guia-judicial/"
                      target="_blank"
                      rel="noopener noreferrer"
                      className={styles.fLink}
                    >
                      Consultá la Guía Judicial →
                    </a>
                  </p>
                )
              }
            </div>

            <div className={styles.divider} />

            <Button type="submit" fullWidth loading={loading}>
              Enviar solicitud →
            </Button>
          </form>
        </div>
      </div>
    </>
  )
}

/**
 * StepValidarEmail.jsx — Paso 2: Validación del OTP (HU02).
 *
 * Réplica de screen-validar-email de la maqueta rdam-ciudadano.html.
 *
 * Características del input OTP:
 *   - 6 cajas individuales de 1 dígito (como en la maqueta)
 *   - Auto-focus al siguiente campo al ingresar un dígito
 *   - Backspace limpia el campo actual y regresa al anterior
 *   - Soporte de pegado (Ctrl+V) que distribuye los 6 dígitos
 *
 * Lógica de negocio:
 *   - Máximo 3 intentos (el backend devuelve 401 al agotarlos)
 *   - Código válido por 15 minutos (manejado en Redis por el backend)
 *   - Al validar exitosamente, onSuccess recibe { tokenAcceso, nroTramite, estado }
 */

import { useState, useRef, useCallback } from 'react'
import { validarOtp } from '../../../api/solicitudesApi.js'
import { Button } from '../../../components/ui/Button.jsx'
import { Alert } from '../../../components/ui/Alert.jsx'
import styles from './Steps.module.css'

const OTP_LENGTH = 6

export function StepValidarEmail({ idSolicitud, email, onSuccess, onBack }) {
  const [digits, setDigits] = useState(Array(OTP_LENGTH).fill(''))
  const [error, setError]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [intentosRestantes, setIntentosRestantes] = useState(3)
  const inputsRef = useRef([])

  // ─── Manejo del input OTP ────────────────────────────────────────────────

  const handleChange = useCallback((idx, value) => {
    const v = value.replace(/\D/g, '').slice(-1) // solo el último dígito
    const next = [...digits]
    next[idx] = v
    setDigits(next)
    setError(null)

    if (v && idx < OTP_LENGTH - 1) {
      inputsRef.current[idx + 1]?.focus()
    }
  }, [digits])

  const handleKeyDown = useCallback((idx, e) => {
    if (e.key === 'Backspace') {
      if (digits[idx]) {
        const next = [...digits]
        next[idx] = ''
        setDigits(next)
      } else if (idx > 0) {
        inputsRef.current[idx - 1]?.focus()
      }
    }
  }, [digits])

  // Soporte de pegado: distribuye los dígitos entre las cajas
  const handlePaste = useCallback((e) => {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH)
    const next = [...digits]
    pasted.split('').forEach((ch, i) => { next[i] = ch })
    setDigits(next)
    // Mover foco al último campo llenado
    const lastIdx = Math.min(pasted.length - 1, OTP_LENGTH - 1)
    setTimeout(() => inputsRef.current[lastIdx]?.focus(), 0)
  }, [digits])

  // ─── Envío a la API ──────────────────────────────────────────────────────

  async function handleVerificar() {
    const code = digits.join('')
    if (code.length < OTP_LENGTH) {
      setError('Ingrese los 6 dígitos del código.')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const data = await validarOtp(idSolicitud, code)
      // data = { tokenAcceso, nroTramite, estado }
      onSuccess(data)
    } catch (err) {
      const remaining = intentosRestantes - 1
      setIntentosRestantes(remaining)
      setDigits(Array(OTP_LENGTH).fill(''))
      inputsRef.current[0]?.focus()

      if (err.status === 401) {
        if (remaining <= 0) {
          setError('Has agotado los intentos disponibles. La solicitud fue cancelada.')
        } else {
          setError(`Código incorrecto. Te quedan ${remaining} intento${remaining !== 1 ? 's' : ''}.`)
        }
      } else {
        setError(err.message || 'Error al verificar el código.')
      }
    } finally {
      setLoading(false)
    }
  }

  const isBlocked = intentosRestantes <= 0

  return (
    <div className={`${styles.card} fade-up`}>
      <div className={styles.cardTop}>
        <span>📧</span>
        <h2>Validación de Email</h2>
      </div>
      <div className={styles.cardBody}>
        <Alert variant="ok" icon="✅">
          Enviamos un código de 6 dígitos a <strong>{email}</strong>. Válido por 15 minutos.
        </Alert>

        {error && (
          <Alert variant="error" icon="⚠️">
            {error}
          </Alert>
        )}

        <p style={{ fontSize: '13px', color: 'var(--ink-3)', textAlign: 'center', marginBottom: '4px' }}>
          Ingrese el código recibido
        </p>

        {/* Inputs OTP */}
        <div className={styles.otpRow} onPaste={handlePaste}>
          {digits.map((d, i) => (
            <input
              key={i}
              ref={el => { inputsRef.current[i] = el }}
              className={`${styles.otpDigit} ${error ? styles.otpDigitError : ''}`}
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={1}
              value={d}
              disabled={isBlocked}
              onChange={e => handleChange(i, e.target.value)}
              onKeyDown={e => handleKeyDown(i, e)}
            />
          ))}
        </div>

        <p style={{ fontSize: '12px', textAlign: 'center', color: 'var(--ink-3)', marginBottom: '18px' }}>
          {isBlocked
            ? 'Sin intentos disponibles'
            : `Intentos restantes: ${intentosRestantes}`
          }
        </p>

        {!isBlocked && (
          <Button fullWidth loading={loading} onClick={handleVerificar}>
            Verificar código
          </Button>
        )}
        <Button
          variant="ghost"
          fullWidth
          onClick={onBack}
          disabled={loading}
          style={{ marginTop: '8px' }}
        >
          ← Volver
        </Button>
      </div>
    </div>
  )
}

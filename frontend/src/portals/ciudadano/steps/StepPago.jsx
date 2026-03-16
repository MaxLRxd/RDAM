/**
 * StepPago.jsx — Paso 4: Pago del arancel con PlusPagos (HU04).
 *
 * Réplica de screen-pagar de la maqueta rdam-ciudadano.html.
 *
 * Flujo de integración con PlusPagos (documentado en CONTEXT-FOR-AI.md §7):
 *   1. Llama a POST /api/v1/solicitudes/{id}/pago/crear (con token ciudadano)
 *   2. El backend devuelve { urlPago, formularioDatos }
 *   3. Este componente crea un <form> invisible con inputs hidden para cada campo
 *      de formularioDatos (algunos vienen encriptados con AES-256-CBC, no los tocamos)
 *   4. Llama form.submit() → el navegador navega hacia el mock de PlusPagos
 *   5. PlusPagos procesa el pago y redirige de vuelta al frontend
 *   6. El CiudadanoContext reacciona via polling del estado y avanza el step
 *
 * Decisión de diseño: el formulario se submite con el botón "Pagar". Previo a eso
 * mostramos el detalle de pago para que el ciudadano confirme el monto.
 */

import { useState } from 'react'
import { crearOrdenPago } from '../../../api/solicitudesApi.js'
import { Button } from '../../../components/ui/Button.jsx'
import { Alert } from '../../../components/ui/Alert.jsx'
import styles from './Steps.module.css'

export function StepPago({ idSolicitud, tokenAcceso, nroTramite, dniCuil, circunscripcion, onBack }) {
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)

  async function handlePagar() {
    setLoading(true)
    setError(null)
    try {
      const data = await crearOrdenPago(idSolicitud, tokenAcceso)
      // data = { urlPago, idOrdenPago, modoSimulacion, formularioDatos }
      submitPlusPagosForm(data.urlPago, data.formularioDatos)
    } catch (err) {
      setError(err.message || 'Error al iniciar el pago. Intente nuevamente.')
      setLoading(false)
    }
  }

  /**
   * Construye un <form> invisible con los datos encriptados de PlusPagos
   * y lo envía (form POST) hacia la pasarela. El backend ya se encargó
   * de encriptar los campos sensibles (Monto, UrlSuccess, UrlError, etc.).
   * El frontend solo los pasa como-están — no los desencripta.
   */
  function submitPlusPagosForm(urlPago, formularioDatos) {
    const form = document.createElement('form')
    form.method = 'POST'
    form.action = urlPago
    form.style.display = 'none'

    Object.entries(formularioDatos).forEach(([name, value]) => {
      const input = document.createElement('input')
      input.type  = 'hidden'
      input.name  = name
      input.value = value
      form.appendChild(input)
    })

    document.body.appendChild(form)
    form.submit()
    // El navegador navega al mock de PlusPagos.
    // No limpiar form — el browser lo descarta al navegar.
  }

  return (
    <div className={`${styles.card} fade-up`}>
      <div className={styles.cardTop}>
        <span>💳</span>
        <h2>Pago del Arancel de Emisión</h2>
      </div>
      <div className={styles.cardBody}>
        <Alert variant="ok" icon="✅">
          Su solicitud está registrada. Debe abonar el arancel para que el personal procese y emita el certificado.
        </Alert>

        {error && (
          <Alert variant="error" icon="✕">
            {error}
          </Alert>
        )}

        {/* Detalle del pago */}
        <div className={styles.sCard}>
          <div className={styles.sHead}>📋 Detalle del pago</div>
          <div className={styles.sBody}>
            <div className={styles.sRow}>
              <span className={styles.sKey}>Concepto</span>
              <span className={styles.sVal}>Emisión certificado RDAM</span>
            </div>
            <div className={styles.sRow}>
              <span className={styles.sKey}>DNI/CUIL consultado</span>
              <span className={styles.sVal}>{dniCuil || '—'}</span>
            </div>
            <div className={styles.sRow}>
              <span className={styles.sKey}>Circunscripción</span>
              <span className={styles.sVal}>{circunscripcion || '—'}</span>
            </div>
            <div className={styles.sRow}>
              <span className={styles.sKey}>Nro. Trámite</span>
              <span className={styles.sVal}>{nroTramite}</span>
            </div>
          </div>
        </div>

        <Alert variant="warn" icon="⚠️">
          El arancel se cobra por la <strong>emisión del certificado</strong>. No constituye pago de deuda alimentaria.
        </Alert>

        <Button
          variant="amber"
          fullWidth
          loading={loading}
          onClick={handlePagar}
        >
          Pagar con PlusPagos →
        </Button>
        <p style={{ textAlign: 'center', fontSize: '12px', color: 'var(--ink-3)', marginTop: '8px' }}>
          Redirige a la pasarela de pago segura PlusPagos
        </p>

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

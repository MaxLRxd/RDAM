/**
 * StepDescarga.jsx — Paso 5: Descarga del certificado (HU05).
 *
 * Réplica de screen-descarga de la maqueta rdam-ciudadano.html.
 *
 * Muestra cuando el estado de la solicitud es PUBLICADO.
 * El link de descarga viene en solicitudEstado.linkDescarga, que es una URL
 * construida por el backend con el token de descarga:
 *   http://localhost:8080/api/v1/certificados/{tokenDescarga}
 *
 * La descarga es directa (href) — no requiere token de sesión ciudadano
 * porque el tokenDescarga ya es la autenticación del link.
 */

import { formatFechaSolo } from '../../../utils/formatters.js'
import { Button } from '../../../components/ui/Button.jsx'
import { Alert } from '../../../components/ui/Alert.jsx'
import styles from './Steps.module.css'

export function StepDescarga({ nroTramite, linkDescarga, fechaEmision }) {
  function handleDescargar() {
    if (linkDescarga) {
      window.open(linkDescarga, '_blank', 'noopener,noreferrer')
    }
  }

  return (
    <div className={`${styles.card} fade-up`}>
      <div className={styles.cardTop}>
        <span>✅</span>
        <h2>Certificado Disponible</h2>
      </div>
      <div className={styles.cardBody}>
        <Alert variant="ok" icon="✅">
          Pago procesado. Su certificado ha sido publicado y está listo para descargar.
        </Alert>

        {/* Preview del certificado */}
        <div className={styles.pdfBox}>
          <div style={{ fontSize: '40px', marginBottom: '10px' }}>📄</div>
          <h3 style={{ fontSize: '17px', fontWeight: '600', marginBottom: '6px', letterSpacing: '-0.01em' }}>
            Certificado RDAM Oficial
          </h3>
          <p style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '12px', color: 'var(--accent-light)' }}>
            {nroTramite}
          </p>
          {fechaEmision && (
            <p style={{ fontSize: '12px', color: 'var(--ink-3)', marginTop: '6px' }}>
              Publicado el {formatFechaSolo(fechaEmision)} · Vigencia: 65 días
            </p>
          )}
        </div>

        <div className={styles.divider} />

        <Alert variant="warn" icon="⚠️">
          El enlace de descarga tiene vigencia de <strong>65 días</strong>. Pasado ese
          plazo, el archivo será eliminado del servidor.
        </Alert>

        <Button fullWidth onClick={handleDescargar} disabled={!linkDescarga}>
          ⬇ Descargar Certificado
        </Button>

        {fechaEmision && (
          <p style={{ textAlign: 'center', fontSize: '12px', color: 'var(--ink-3)', marginTop: '8px' }}>
            También enviado a su email
          </p>
        )}
      </div>
    </div>
  )
}

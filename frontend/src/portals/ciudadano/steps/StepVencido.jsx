/**
 * StepVencido.jsx — Estado terminal: solicitud vencida.
 *
 * Se muestra cuando el estado de la solicitud es VENCIDO o PUBLICADO_VENCIDO.
 * Causas posibles:
 *   - El ciudadano no pagó en el plazo de 60 días (PRD) / 1 día (DEV)
 *   - PlusPagos rechazó el pago (códigos 4, 7, 8, 9, 11)
 *   - El certificado expiró a los 65 días (PUBLICADO_VENCIDO)
 *
 * Ofrece un botón para iniciar una nueva solicitud.
 */

import { Button } from '../../../components/ui/Button.jsx'
import { Alert } from '../../../components/ui/Alert.jsx'
import styles from './Steps.module.css'

export function StepVencido({ nroTramite, isPublicadoVencido = false, onNuevaSolicitud }) {
  return (
    <div className={`${styles.card} fade-up`}>
      <div className={styles.cardTop}>
        <span>❌</span>
        <h2>{isPublicadoVencido ? 'Enlace Expirado' : 'Solicitud Vencida'}</h2>
      </div>
      <div className={styles.cardBody}>
        <div className={styles.centerContent}>
          <div className={styles.bigIcon}>{isPublicadoVencido ? '🗓️' : '⏰'}</div>
          <h3 className={styles.centerTitle} style={{ color: 'var(--red)' }}>
            {isPublicadoVencido ? 'El enlace de descarga expiró' : 'Solicitud vencida'}
          </h3>
          <p className={styles.centerText}>
            {isPublicadoVencido
              ? 'El archivo del certificado fue eliminado del servidor por vencimiento del plazo de 65 días.'
              : 'Esta solicitud fue cerrada porque el plazo de pago venció o el pago fue rechazado por la pasarela.'
            }
          </p>
          <div className={styles.tramiteBox}>
            <p className={styles.tramiteLabel}>Nro. de Trámite</p>
            <p className={styles.tramiteValue} style={{ color: 'var(--red)' }}>{nroTramite}</p>
          </div>
        </div>

        <div className={styles.divider} />

        <Alert variant="warn" icon="ℹ️">
          {isPublicadoVencido
            ? 'Si necesita el certificado, deberá iniciar una nueva solicitud y abonar el arancel nuevamente.'
            : 'Para obtener el certificado, deberá realizar una nueva solicitud.'}
        </Alert>

        <Button fullWidth onClick={onNuevaSolicitud}>
          Iniciar nueva solicitud
        </Button>
      </div>
    </div>
  )
}

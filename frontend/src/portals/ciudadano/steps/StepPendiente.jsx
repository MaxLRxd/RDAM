/**
 * StepPendiente.jsx — Paso 3: Solicitud recibida / esperando certificado.
 *
 * Este step se usa para DOS estados del backend:
 *
 *   A) Estado PENDIENTE — Solicitud recibida, el ciudadano puede pagar.
 *      Muestra un botón "Proceder al pago" y un timeline simple.
 *      Réplica de screen-pendiente de la maqueta.
 *
 *   B) Estado PAGADO — Pago confirmado, esperando que el operador
 *      suba el certificado. Sin botón de pago, muestra timeline actualizado.
 *
 * La prop `isPagado` controla cuál variante se muestra.
 */

import { formatFecha } from '../../../utils/formatters.js'
import { Button } from '../../../components/ui/Button.jsx'
import { Alert } from '../../../components/ui/Alert.jsx'
import styles from './Steps.module.css'

export function StepPendiente({ nroTramite, circunscripcion, fechaCreacion, isPagado = false, onPagar }) {
  return (
    <div className={`${styles.card} fade-up`}>
      <div className={styles.cardTop}>
        <span>{isPagado ? '⏱️' : '⏳'}</span>
        <h2>{isPagado ? 'Pago Confirmado' : 'Solicitud Recibida'}</h2>
      </div>
      <div className={styles.cardBody}>
        {/* Centro con ícono + nro de trámite */}
        <div className={styles.centerContent}>
          <div className={styles.bigIcon}>{isPagado ? '✅' : '📋'}</div>
          <h3 className={styles.centerTitle}>
            {isPagado ? 'Pago procesado exitosamente' : 'Solicitud recibida'}
          </h3>
          <p className={styles.centerText}>
            {isPagado
              ? 'El personal de su circunscripción está generando el certificado. Recibirá una notificación por email cuando esté disponible.'
              : 'Su solicitud fue registrada. Para obtener el certificado debe abonar el arancel de emisión.'
            }
          </p>
          <div className={styles.tramiteBox}>
            <p className={styles.tramiteLabel}>Nro. de Trámite</p>
            <p className={styles.tramiteValue}>{nroTramite}</p>
          </div>
        </div>

        <div className={styles.divider} />

        <h4 className={styles.sectionHeader}>Estado del proceso</h4>
        <ul className={styles.tl}>
          <li className={styles.tlItem}>
            <div className={styles.tlAction}>
              Solicitud creada
              <span className={styles.tlTime}>{formatFecha(fechaCreacion)}</span>
            </div>
            <div className={styles.tlDetail}>
              Email validado · {circunscripcion || 'Circunscripción asignada'}
            </div>
          </li>

          {isPagado ? (
            <li className={styles.tlItem}>
              <div className={styles.tlAction} style={{ color: 'var(--green)' }}>
                Pago confirmado
              </div>
              <div className={styles.tlDetail}>
                Arancel de emisión abonado correctamente
              </div>
            </li>
          ) : null}

          <li className={styles.tlItem}>
            <div className={styles.tlAction} style={{ color: 'var(--amber)' }}>
              {isPagado ? 'Generando certificado' : 'Pendiente de pago'}
              <span className={styles.tlTime}>ahora</span>
            </div>
            <div className={styles.tlDetail}>
              {isPagado
                ? 'El operador está preparando el documento'
                : 'Abone el arancel para que el personal procese su solicitud'
              }
            </div>
          </li>
        </ul>

        {!isPagado && (
          <>
            <Alert variant="info" icon="💳">
              El pago debe realizarse para que el personal pueda procesar y emitir su certificado.
            </Alert>
            <Button fullWidth onClick={onPagar}>
              Proceder al pago →
            </Button>
          </>
        )}
      </div>
    </div>
  )
}

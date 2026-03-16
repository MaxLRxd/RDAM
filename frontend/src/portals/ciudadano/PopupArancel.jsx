/**
 * PopupArancel.jsx — Modal de confirmación de arancel (HU01).
 *
 * Según la especificación v1.2, antes de enviar la solicitud se debe mostrar
 * un popup informando que deberá abonarse un arancel de emisión, con opciones
 * "Cancelar" y "Continuar".
 *
 * Réplica del div#popup de la maqueta rdam-ciudadano.html.
 * Usa backdrop-filter:blur para el efecto visual del fondo.
 */

import styles from './PopupArancel.module.css'
import { Button } from '../../components/ui/Button.jsx'

export function PopupArancel({ onConfirm, onCancel }) {
  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.box}>
        <div className={styles.icon}>💳</div>
        <h3 className={styles.title}>Aviso antes de continuar</h3>
        <p className={styles.text}>
          Al enviar esta solicitud, deberá{' '}
          <strong>abonar el arancel de emisión</strong> para obtener su
          certificado. El pago es obligatorio independientemente del resultado.
          <br /><br />
          ¿Desea continuar con la solicitud?
        </p>
        <div className={styles.actions}>
          <Button variant="ghost" fullWidth onClick={onCancel}>
            Cancelar
          </Button>
          <Button variant="primary" fullWidth onClick={onConfirm}>
            Continuar →
          </Button>
        </div>
      </div>
    </div>
  )
}

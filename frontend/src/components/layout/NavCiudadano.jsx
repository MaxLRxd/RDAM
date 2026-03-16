/**
 * NavCiudadano.jsx — Barra de navegación del portal público.
 *
 * Réplica del <nav> de la maqueta rdam-ciudadano.html.
 * Expone:
 *   - Marca "Sistema RDAM / Provincia de Santa Fe"
 *   - Botón "Mis Solicitudes" (visible siempre)
 *   - Botón "Nueva Solicitud" (visible cuando no estamos en el form)
 */

import styles from './NavCiudadano.module.css'
import { STEPS } from '../../utils/constants.js'

export function NavCiudadano({ currentStep, onNavigate }) {
  return (
    <nav className={styles.nav}>
      <div className={styles.brand}>
        <div className={styles.dot} />
        <div>
          <div className={styles.title}>Sistema RDAM</div>
          <div className={styles.sub}>Provincia de Santa Fe</div>
        </div>
      </div>

      <div className={styles.actions}>
        {currentStep !== STEPS.FORM && currentStep !== STEPS.VALIDAR_EMAIL && (
          <button
            className={styles.link}
            onClick={() => onNavigate(STEPS.FORM)}
          >
            + Nueva solicitud
          </button>
        )}
        <button
          className={styles.link}
          onClick={() => onNavigate(STEPS.MIS_SOLICITUDES)}
        >
          Mis Solicitudes
        </button>
      </div>
    </nav>
  )
}

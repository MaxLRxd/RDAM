/**
 * MisSolicitudes.jsx — Historial de solicitudes del ciudadano (HU03).
 *
 * Réplica de screen-mis-solicitudes de la maqueta.
 *
 * Lee el historial de localStorage (actualizado en cada sesión completada).
 * Permite al ciudadano:
 *   - Ver el estado cached de cada solicitud
 *   - Hacer clic en "Ver" para ir al step correspondiente al estado actual
 *   - Iniciar una nueva solicitud
 *
 * La información de estado en localStorage puede estar desactualizada.
 * Al hacer click en "Ver", el CiudadanoPage re-consulta el backend para
 * obtener el estado real y navegar al step correcto.
 */

import { getHistorial } from '../../utils/storage.js'
import { formatFechaSolo, formatEstado, badgeVariantFromEstado } from '../../utils/formatters.js'
import { Badge } from '../../components/ui/Badge.jsx'
import { Button } from '../../components/ui/Button.jsx'
import { Alert } from '../../components/ui/Alert.jsx'
import styles from './MisSolicitudes.module.css'
import stepStyles from './steps/Steps.module.css'

export function MisSolicitudes({ onVerSolicitud, onNuevaSolicitud }) {
  const historial = getHistorial()

  return (
    <div className={`${stepStyles.card} fade-up`}>
      <div className={stepStyles.cardTop}>
        <span>📂</span>
        <h2>Mis Solicitudes</h2>
      </div>
      <div className={stepStyles.cardBody}>
        <Alert variant="info" icon="🔑">
          El historial se almacena en este dispositivo y navegador.
          Si borró el caché o accede desde otro dispositivo, no aparecerán aquí.
        </Alert>

        {historial.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '32px 0', color: 'var(--ink-3)' }}>
            <div style={{ fontSize: '36px', marginBottom: '10px' }}>📭</div>
            <p style={{ fontSize: '14px' }}>No hay solicitudes registradas en este dispositivo.</p>
          </div>
        ) : (
          historial.map(sol => (
            <div key={sol.nroTramite} className={styles.solItem}>
              <div>
                <div className={styles.solId}>{sol.nroTramite}</div>
                <div className={styles.solDni}>DNI/CUIL {sol.dniCuil}</div>
                <div className={styles.solMeta}>
                  {formatFechaSolo(sol.fechaCreacion)}
                  {sol.circunscripcion ? ` · ${sol.circunscripcion}` : ''}
                  {sol.email ? ` · ${sol.email}` : ''}
                </div>
              </div>
              <div className={styles.solActions}>
                <Badge variant={badgeVariantFromEstado(sol.estadoCache)}>
                  {formatEstado(sol.estadoCache)}
                </Badge>
                {sol.tokenAcceso && (
                  <Button
                    size="sm"
                    onClick={() => onVerSolicitud(sol)}
                  >
                    Ver
                  </Button>
                )}
              </div>
            </div>
          ))
        )}

        <div className={stepStyles.divider} />
        <Button variant="ghost" size="sm" onClick={onNuevaSolicitud}>
          + Nueva solicitud
        </Button>
      </div>
    </div>
  )
}

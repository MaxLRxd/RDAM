/**
 * SidebarInfo.jsx — Panel lateral informativo del portal ciudadano.
 *
 * Réplica exacta del sidebar de la maqueta rdam-ciudadano.html.
 * Contiene tres tarjetas:
 *   1. Información general del certificado (vigencia, arancel)
 *   2. Aclaraciones legales
 *   3. Datos de soporte
 */

import styles from './SidebarInfo.module.css'

export function SidebarInfo() {
  return (
    <aside>
      {/* Tarjeta 1: Información */}
      <div className={styles.sCard}>
        <div className={styles.sHead}>ℹ️ Información</div>
        <div className={styles.sBody}>
          <div className={styles.sRow}>
            <span className={styles.sKey}>Tipo</span>
            <span className={styles.sVal}>Informativo</span>
          </div>
          <div className={styles.sRow}>
            <span className={styles.sKey}>Vigencia enlace</span>
            <span className={styles.sVal}>65 días</span>
          </div>
          <div className={styles.sRow}>
            <span className={styles.sKey}>Código email</span>
            <span className={styles.sVal}>15 min</span>
          </div>
        </div>
      </div>

      {/* Tarjeta 2: Aclaraciones legales */}
      <div className={styles.sCard}>
        <div className={styles.sHead}>⚠️ Aclaraciones legales</div>
        <div className={styles.sBody}>
          <p className={styles.legalText}>
            El arancel se cobra por la <strong>emisión del certificado</strong>, no por
            la deuda. Personas que no figuren en el registro igualmente deben abonarlo.
          </p>
          <p className={styles.legalText} style={{ marginBottom: 0 }}>
            Cualquier persona puede solicitar certificados de terceros. La
            responsabilidad del uso recae en el solicitante.
          </p>
        </div>
      </div>

      {/* Tarjeta 3: Soporte */}
      <div className={styles.sCard}>
        <div className={styles.sHead}>📞 Soporte</div>
        <div className={styles.sBody}>
          <div className={styles.sRow}>
            <span className={styles.sKey}>Web</span>
            <span className={`${styles.sVal} ${styles.sValSmall}`}>rdam.santafe.gob.ar</span>
          </div>
          <div className={styles.sRow}>
            <span className={styles.sKey}>Email</span>
            <span className={`${styles.sVal} ${styles.sValSmall}`}>soporte@rdam.santafe.gob.ar</span>
          </div>
          <div className={styles.sRow}>
            <span className={styles.sKey}>Tel.</span>
            <span className={styles.sVal}>0800-XXX-XXXX</span>
          </div>
          <div className={styles.sRow}>
            <span className={styles.sKey}>Horario</span>
            <span className={styles.sVal}>L–V 8–20hs</span>
          </div>
        </div>
      </div>

      <p className={styles.footer}>
        Gobierno de la Provincia de Santa Fe<br />
        Poder Judicial — Registro RDAM
      </p>
    </aside>
  )
}

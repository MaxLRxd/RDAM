/**
 * InternoDashboard.jsx — Shell del Portal Interno.
 *
 * Orquestador post-login. Gestiona:
 *   1. El panel activo (qué sección ver: dashboard / solicitudes / usuarios / auditoría)
 *   2. El pendingCount para el badge del sidebar (solicitudes PAGADO sin certificado)
 *   3. El montaje/desmontaje de los modales (ModalDetalleSolicitud, ModalNuevoUsuario)
 *
 * Layout:
 *   ┌──────────┬────────────────────────────────┐
 *   │          │ Topbar                         │
 *   │ Sidebar  │────────────────────────────────│
 *   │  220px   │ Panel activo                   │
 *   │  fixed   │                                │
 *   └──────────┴────────────────────────────────┘
 *
 * Decisión: el pendingCount se inicializa en 0 y se actualiza cuando
 * PanelSolicitudes carga los datos. PanelSolicitudes llama a `onPendingCount`
 * con el totalElements del filtro estado=PAGADO.
 */

import { useState, useCallback } from 'react'
import { Sidebar }           from './components/Sidebar.jsx'
import { PanelDashboard }    from './panels/PanelDashboard.jsx'
import { PanelSolicitudes }  from './panels/PanelSolicitudes.jsx'
import { PanelUsuarios }     from './panels/PanelUsuarios.jsx'
import { PanelAuditoria }    from './panels/PanelAuditoria.jsx'
import styles from './InternoDashboard.module.css'

export function InternoDashboard() {
  const [activePanel, setActivePanel]   = useState('dashboard')
  const [pendingCount, setPendingCount] = useState(0)

  const handlePendingCount = useCallback((count) => {
    setPendingCount(count)
  }, [])

  function renderPanel() {
    switch (activePanel) {
      case 'dashboard':
        return (
          <PanelDashboard
            onNavigate={setActivePanel}
            pendingCount={pendingCount}
          />
        )
      case 'solicitudes':
        return (
          <PanelSolicitudes
            onPendingCount={handlePendingCount}
          />
        )
      case 'usuarios':
        return <PanelUsuarios />
      case 'auditoria':
        return <PanelAuditoria />
      default:
        return (
          <PanelDashboard
            onNavigate={setActivePanel}
          />
        )
    }
  }

  return (
    <div className={styles.root}>
      <Sidebar
        activePanel={activePanel}
        onNavigate={setActivePanel}
        pendingCount={pendingCount}
      />

      <main className={styles.main}>
        <div className={styles.panelArea}>
          {renderPanel()}
        </div>
      </main>
    </div>
  )
}

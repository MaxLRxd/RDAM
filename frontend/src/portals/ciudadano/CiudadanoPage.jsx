/**
 * CiudadanoPage.jsx — Orquestador del Portal Ciudadano.
 *
 * Es el componente raíz del portal. Gestiona:
 *   1. El "step activo" (qué pantalla ver)
 *   2. El estado de la sesión activa (via CiudadanoContext)
 *   3. El polling del estado de la solicitud (para detectar cambios de estado
 *      después de que el operador suba el certificado o PlusPagos procese el pago)
 *   4. La recuperación de sesión al recargar la página (desde localStorage)
 *
 * Lógica de inicialización:
 *   Al montar, si existe una sesión guardada (tokenAcceso + nroTramite),
 *   consulta el backend para obtener el estado real y navega al step
 *   correspondiente. Esto maneja el caso de "regreso desde PlusPagos".
 *
 * Polling:
 *   Cuando el step es 'esperando-cert' (estado PAGADO), el componente
 *   sondea la API cada 30 segundos para detectar cuando el operador
 *   publica el certificado (estado → PUBLICADO).
 */

import { useState, useEffect, useCallback, useRef } from 'react'
import { useCiudadano } from '../../hooks/useCiudadano.js'
import { consultarEstado } from '../../api/solicitudesApi.js'
import { useToast } from '../../hooks/useToast.js'
import { updateHistorialEstado } from '../../utils/storage.js'
import {
  STEPS,
  ESTADOS,
  stepFromEstado,
  stepperIndexFromStep,
} from '../../utils/constants.js'

import { NavCiudadano }    from '../../components/layout/NavCiudadano.jsx'
import { Toast }           from '../../components/ui/Toast.jsx'
import { Spinner }         from '../../components/ui/Spinner.jsx'
import { SidebarInfo }     from './SidebarInfo.jsx'
import { StepFormulario }  from './steps/StepFormulario.jsx'
import { StepValidarEmail } from './steps/StepValidarEmail.jsx'
import { StepPendiente }   from './steps/StepPendiente.jsx'
import { StepPago }        from './steps/StepPago.jsx'
import { StepDescarga }    from './steps/StepDescarga.jsx'
import { StepVencido }     from './steps/StepVencido.jsx'
import { MisSolicitudes }  from './MisSolicitudes.jsx'

import styles from './CiudadanoPage.module.css'

const POLL_INTERVAL_MS = 30_000 // 30 segundos

export function CiudadanoPage() {
  const ctx           = useCiudadano()
  const { toast, showToast } = useToast()

  const [step, setStep]                   = useState(STEPS.FORM)
  const [initializing, setInitializing]   = useState(true)
  const [solicitudData, setSolicitudData] = useState(null) // datos del backend
  const pollTimer = useRef(null)

  // ─── Inicialización: recuperar sesión desde localStorage ────────────────

  useEffect(() => {
    async function init() {
      if (ctx.tokenAcceso && ctx.nroTramite) {
        try {
          const data = await consultarEstado(ctx.nroTramite, ctx.tokenAcceso)
          setSolicitudData(data)
          setStep(stepFromEstado(data.estado))
        } catch {
          // Si el token expiró o la solicitud no existe, ir al form
          ctx.clearSesion()
          setStep(STEPS.FORM)
        }
      }
      setInitializing(false)
    }
    init()
    // Solo al montar
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // ─── Polling cuando está esperando el certificado ───────────────────────

  useEffect(() => {
    if (step === STEPS.ESPERANDO_CERT && ctx.tokenAcceso && ctx.nroTramite) {
      pollTimer.current = setInterval(async () => {
        try {
          const data = await consultarEstado(ctx.nroTramite, ctx.tokenAcceso)
          setSolicitudData(data)
          if (data.estado === ESTADOS.PUBLICADO) {
            setStep(STEPS.DESCARGA)
            updateHistorialEstado(ctx.nroTramite, ESTADOS.PUBLICADO)
            showToast('✅ ¡Su certificado está listo para descargar!', 'ok', 6000)
            clearInterval(pollTimer.current)
          } else if (data.estado === ESTADOS.VENCIDO || data.estado === ESTADOS.PUBLICADO_VENCIDO) {
            setStep(STEPS.VENCIDO)
            clearInterval(pollTimer.current)
          }
        } catch {
          // Ignorar errores de polling — reintentar en el siguiente ciclo
        }
      }, POLL_INTERVAL_MS)
    }

    return () => {
      if (pollTimer.current) clearInterval(pollTimer.current)
    }
  }, [step, ctx.tokenAcceso, ctx.nroTramite, showToast])

  // ─── Handlers de cada step ──────────────────────────────────────────────

  /** Después de crear la solicitud (POST /solicitudes) */
  const handleSolicitudCreada = useCallback((data) => {
    ctx.setSolicitudCreada(data)
    setStep(STEPS.VALIDAR_EMAIL)
  }, [ctx])

  /** Después de validar el OTP (POST /solicitudes/{id}/validar) */
  const handleOtpValidado = useCallback((data) => {
    // data = { tokenAcceso, nroTramite, estado }
    ctx.setToken({
      tokenAcceso:     data.tokenAcceso,
      nroTramite:      data.nroTramite,
      circunscripcion: solicitudData?.circunscripcion || '',
      estado:          data.estado,
    })

    const nextStep = stepFromEstado(data.estado)
    setStep(nextStep)

    // Actualizar solicitudData para mostrar en pantallas siguientes
    setSolicitudData(prev => ({
      ...prev,
      nroTramite: data.nroTramite,
      estado:     data.estado,
    }))

    showToast('✅ Email verificado exitosamente', 'ok')
  }, [ctx, solicitudData, showToast])

  /** Al hacer clic en "Proceder al pago" en StepPendiente */
  const handleIrAPagar = useCallback(() => {
    setStep(STEPS.PAGO)
  }, [])

  /** Al volver desde StepPago (botón "← Volver") */
  const handleVolverDePago = useCallback(() => {
    setStep(STEPS.PENDIENTE)
  }, [])

  /** Para "Nueva solicitud" desde Vencido o MisSolicitudes */
  const handleNuevaSolicitud = useCallback(() => {
    ctx.clearSesion()
    setSolicitudData(null)
    setStep(STEPS.FORM)
  }, [ctx])

  /** Para "Ver" una solicitud del historial */
  const handleVerSolicitud = useCallback(async (sol) => {
    // Restaurar la sesión con los datos del historial
    ctx.setSolicitudCreada({
      idSolicitud: sol.idSolicitud,
      nroTramite:  sol.nroTramite,
      email:       sol.email,
      dniCuil:     sol.dniCuil,
    })
    ctx.setToken({
      tokenAcceso:     sol.tokenAcceso,
      nroTramite:      sol.nroTramite,
      circunscripcion: sol.circunscripcion,
      estado:          sol.estadoCache,
    })

    try {
      const data = await consultarEstado(sol.nroTramite, sol.tokenAcceso)
      setSolicitudData(data)
      setStep(stepFromEstado(data.estado))
      updateHistorialEstado(sol.nroTramite, data.estado)
    } catch {
      showToast('No se pudo cargar el estado. El token puede haber expirado.', 'error')
    }
  }, [ctx, showToast])

  // ─── Render del step activo ──────────────────────────────────────────────

  function renderStep() {
    switch (step) {
      case STEPS.FORM:
        return <StepFormulario onSuccess={handleSolicitudCreada} />

      case STEPS.VALIDAR_EMAIL:
        return (
          <StepValidarEmail
            idSolicitud={ctx.idSolicitud}
            email={ctx.email}
            onSuccess={handleOtpValidado}
            onBack={() => setStep(STEPS.FORM)}
          />
        )

      case STEPS.PENDIENTE:
        return (
          <StepPendiente
            nroTramite={ctx.nroTramite || solicitudData?.nroTramite}
            circunscripcion={solicitudData?.circunscripcion}
            fechaCreacion={solicitudData?.fechaCreacion}
            isPagado={false}
            onPagar={handleIrAPagar}
          />
        )

      case STEPS.PAGO:
        return (
          <StepPago
            idSolicitud={ctx.idSolicitud}
            tokenAcceso={ctx.tokenAcceso}
            nroTramite={ctx.nroTramite}
            dniCuil={ctx.dniCuil}
            circunscripcion={solicitudData?.circunscripcion}
            onBack={handleVolverDePago}
          />
        )

      case STEPS.ESPERANDO_CERT:
        return (
          <StepPendiente
            nroTramite={ctx.nroTramite || solicitudData?.nroTramite}
            circunscripcion={solicitudData?.circunscripcion}
            fechaCreacion={solicitudData?.fechaCreacion}
            isPagado={true}
          />
        )

      case STEPS.DESCARGA:
        return (
          <StepDescarga
            nroTramite={ctx.nroTramite || solicitudData?.nroTramite}
            linkDescarga={solicitudData?.linkDescarga}
            fechaEmision={solicitudData?.fechaEmision}
            circunscripcion={solicitudData?.circunscripcion}
          />
        )

      case STEPS.VENCIDO:
        return (
          <StepVencido
            nroTramite={ctx.nroTramite || solicitudData?.nroTramite}
            isPublicadoVencido={solicitudData?.estado === ESTADOS.PUBLICADO_VENCIDO}
            onNuevaSolicitud={handleNuevaSolicitud}
          />
        )

      case STEPS.MIS_SOLICITUDES:
        return (
          <MisSolicitudes
            onVerSolicitud={handleVerSolicitud}
            onNuevaSolicitud={handleNuevaSolicitud}
          />
        )

      default:
        return <StepFormulario onSuccess={handleSolicitudCreada} />
    }
  }

  // ─── Stepper visual ──────────────────────────────────────────────────────

  const stepperIndex = stepperIndexFromStep(step)
  const stepperItems = [
    { label: 'Datos'       },
    { label: 'Email'       },
    { label: 'Pago'        },
    { label: 'Certificado' },
  ]

  // ─── Carga inicial ───────────────────────────────────────────────────────

  if (initializing) {
    return (
      <div className={styles.loadingScreen}>
        <Spinner size="lg" />
      </div>
    )
  }

  return (
    <>
      <NavCiudadano currentStep={step} onNavigate={setStep} />

      {/* Hero */}
      <div className={styles.hero}>
        <div className={styles.heroTag}>🏛 Registro Oficial · Gobierno de Santa Fe</div>
        <h1 className={styles.heroTitle}>
          Certificado del <strong>Registro de<br />Deudores Alimentarios Morosos</strong>
        </h1>
        <p className={styles.heroSub}>
          Consulte si una persona figura en el registro provincial.
          El certificado es informativo y tiene vigencia de 65 días.
        </p>
      </div>

      {/* Stepper (oculto en mis-solicitudes) */}
      {step !== STEPS.MIS_SOLICITUDES && (
        <div className={styles.steps}>
          {stepperItems.map((item, i) => (
            <div key={i} className={styles.stepRow}>
              {i > 0 && <div className={styles.stepLine} />}
              <div className={styles.stepItem}>
                <div className={`${styles.stepNum} ${i < stepperIndex ? styles.stepDone : ''} ${i === stepperIndex ? styles.stepActive : ''}`}>
                  {i < stepperIndex ? '✓' : i + 1}
                </div>
                <div className={styles.stepLabel}>{item.label}</div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Layout principal: contenido + sidebar */}
      <div className={styles.layout}>
        <div>{renderStep()}</div>
        <SidebarInfo />
      </div>

      <Toast message={toast.message} visible={toast.visible} type={toast.type} />
    </>
  )
}

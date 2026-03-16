/**
 * ModalDetalleSolicitud.jsx — Modal de detalle y carga de certificado.
 *
 * Réplica de la sección modal de la maqueta rdam-interno.html.
 *
 * Muestra:
 *   1. Grid de información de la solicitud (nroTramite, DNI, email, circ., estado, fecha)
 *   2. Si el estado es PAGADO: zona de carga de PDF (click o drag & drop)
 *   3. Botón "Publicar certificado" que llama a POST /solicitudes/{id}/certificado
 *      con multipart/form-data (campo "archivo")
 *
 * Para solicitudes en otros estados, solo muestra la info (modo lectura).
 *
 * Props:
 *   solicitud            — objeto del backend con {id, nroTramite, dniCuil, email, ...}
 *   onClose              — () => void
 *   onCertificadoSubido  — () => void: llamado tras éxito del upload
 */

import { useState, useRef } from 'react'
import { useAuth }           from '../../../hooks/useAuth.js'
import { subirCertificado }  from '../../../api/internoApi.js'
import { ESTADOS }           from '../../../utils/constants.js'
import { formatFecha, formatEstado } from '../../../utils/formatters.js'
import { Button }            from '../../../components/ui/Button.jsx'
import { Alert }             from '../../../components/ui/Alert.jsx'
import styles from './Modals.module.css'

const MAX_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB

export function ModalDetalleSolicitud({ solicitud, onClose, onCertificadoSubido }) {
  const { token } = useAuth()

  const [archivo,  setArchivo]  = useState(null)
  const [dragOver, setDragOver] = useState(false)
  const [loading,  setLoading]  = useState(false)
  const [error,    setError]    = useState(null)
  const [success,  setSuccess]  = useState(false)

  const fileInputRef = useRef(null)

  const isPagado = solicitud.estado === ESTADOS.PAGADO

  // ─── Selección de archivo ──────────────────────────────────────────────

  function handleFileChange(e) {
    const file = e.target.files?.[0]
    if (file) validateAndSetFile(file)
  }

  function handleDrop(e) {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files?.[0]
    if (file) validateAndSetFile(file)
  }

  function validateAndSetFile(file) {
    setError(null)
    if (file.type !== 'application/pdf') {
      setError('Solo se aceptan archivos PDF.')
      return
    }
    if (file.size > MAX_SIZE_BYTES) {
      setError('El archivo no puede superar los 10 MB.')
      return
    }
    setArchivo(file)
  }

  // ─── Publicar ─────────────────────────────────────────────────────────

  async function handlePublicar() {
    if (!archivo) {
      setError('Debe seleccionar un archivo PDF primero.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await subirCertificado(solicitud.id, archivo, token)
      setSuccess(true)
      // Esperar 1.5s para que el usuario vea el éxito, luego cerrar
      setTimeout(() => {
        onCertificadoSubido?.()
      }, 1500)
    } catch (err) {
      setError(err.message || 'Error al subir el certificado. Intente nuevamente.')
    } finally {
      setLoading(false)
    }
  }

  // ─── Render ───────────────────────────────────────────────────────────

  return (
    <div className={styles.overlay} onClick={e => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal} role="dialog" aria-modal="true">

        {/* ── Cabecera ── */}
        <div className={styles.modalHead}>
          <span className={styles.modalTitle}>
            {isPagado ? '📄 Cargar certificado' : '🔍 Detalle de solicitud'}
          </span>
          <button className={styles.closeBtn} onClick={onClose} type="button" aria-label="Cerrar">
            ✕
          </button>
        </div>

        {/* ── Cuerpo ── */}
        <div className={styles.modalBody}>
          {/* Info de la solicitud */}
          <div className={styles.infoGrid}>
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Nro. Trámite</span>
              <span className={styles.infoValueMono}>{solicitud.nroTramite}</span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Estado</span>
              <span className={styles.infoValue}>{formatEstado(solicitud.estado)}</span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>DNI / CUIL</span>
              <span className={styles.infoValueMono}>{solicitud.dniCuil}</span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Email</span>
              <span className={styles.infoValue} style={{ fontSize: '12px' }}>{solicitud.email}</span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Circunscripción</span>
              <span className={styles.infoValue}>
                {solicitud.circunscripcion?.nombre ?? solicitud.circunscripcion ?? '—'}
              </span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Fecha creación</span>
              <span className={styles.infoValue}>{formatFecha(solicitud.fechaCreacion)}</span>
            </div>
          </div>

          {/* Solo para PAGADO: zona de upload */}
          {isPagado && !success && (
            <>
              <Alert variant="info" icon="ℹ️">
                El ciudadano ha abonado el arancel. Suba el certificado PDF para publicarlo
                y notificar al ciudadano por email.
              </Alert>

              {/* Zona de drag & drop */}
              <div
                className={`
                  ${styles.uploadArea}
                  ${dragOver   ? styles.dragOver : ''}
                  ${archivo    ? styles.hasFile  : ''}
                `}
                onClick={() => fileInputRef.current?.click()}
                onDragOver={e => { e.preventDefault(); setDragOver(true)  }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleDrop}
                role="button"
                tabIndex={0}
                onKeyDown={e => e.key === 'Enter' && fileInputRef.current?.click()}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="application/pdf"
                  className={styles.fileInput}
                  onChange={handleFileChange}
                />

                {archivo ? (
                  <>
                    <div className={styles.uploadIcon}>✅</div>
                    <div className={styles.uploadFileName}>
                      <span>📄</span>
                      <span>{archivo.name}</span>
                    </div>
                    <span className={styles.uploadSub}>
                      {(archivo.size / 1024).toFixed(0)} KB · Haga clic para cambiar
                    </span>
                  </>
                ) : (
                  <>
                    <div className={styles.uploadIcon}>📤</div>
                    <div className={styles.uploadTitle}>
                      Arrastre el PDF aquí o haga clic para seleccionar
                    </div>
                    <span className={styles.uploadSub}>Solo PDF · Máximo 10 MB</span>
                  </>
                )}
              </div>

              {error && (
                <Alert variant="error" icon="✕">{error}</Alert>
              )}
            </>
          )}

          {/* Éxito */}
          {success && (
            <Alert variant="ok" icon="✅">
              ¡Certificado publicado exitosamente! El ciudadano recibirá un email con el link de descarga.
            </Alert>
          )}

          {/* Para estados distintos de PAGADO: nota informativa */}
          {!isPagado && !success && (
            <p className={styles.hint}>
              Esta solicitud está en estado <strong>{formatEstado(solicitud.estado)}</strong>.
              Solo las solicitudes con pago confirmado (estado PAGADO) permiten subir certificados.
            </p>
          )}
        </div>

        {/* ── Pie ── */}
        <div className={styles.modalFoot}>
          <Button variant="ghost" onClick={onClose} disabled={loading}>
            Cerrar
          </Button>
          {isPagado && !success && (
            <Button
              variant="primary"
              onClick={handlePublicar}
              loading={loading}
              disabled={!archivo || loading}
            >
              📤 Publicar certificado
            </Button>
          )}
        </div>

      </div>
    </div>
  )
}

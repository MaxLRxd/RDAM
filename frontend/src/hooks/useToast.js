/**
 * useToast.js — Hook para el sistema de notificaciones tipo toast.
 *
 * Reemplaza el showToast() de la maqueta con un hook reutilizable.
 * Se usa junto con el componente <Toast> para mostrar mensajes globales.
 *
 * Uso en cualquier componente:
 *   const { showToast } = useToast()
 *   showToast('✓ Código reenviado')
 *   showToast('Error al conectar', 'error')
 */

import { useState, useCallback, useRef } from 'react'

export function useToast() {
  const [toast, setToast] = useState({ message: '', visible: false, type: 'default' })
  const timerRef = useRef(null)

  const showToast = useCallback((message, type = 'default', duration = 3000) => {
    // Cancelar el timer anterior si existe
    if (timerRef.current) clearTimeout(timerRef.current)

    setToast({ message, visible: true, type })

    timerRef.current = setTimeout(() => {
      setToast(t => ({ ...t, visible: false }))
    }, duration)
  }, [])

  const hideToast = useCallback(() => {
    setToast(t => ({ ...t, visible: false }))
  }, [])

  return { toast, showToast, hideToast }
}

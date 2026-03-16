/**
 * App.jsx — Enrutador raíz de la aplicación RDAM.
 *
 * Implementa un router hash minimalista sin dependencias externas.
 * Rutas:
 *   #/          → CiudadanoPage (portal público)
 *   #/ciudadano → CiudadanoPage (portal público)
 *   #/interno   → Portal interno (LoginPage o InternoDashboard según auth)
 *
 * Arquitectura de auth del portal interno:
 *   AuthProvider envuelve toda la ruta #/interno.
 *   Dentro, InternoApp decide si mostrar LoginPage o InternoDashboard
 *   dependiendo de `isAuthenticated` de AuthContext.
 *   Esto asegura que el contexto de auth esté disponible en ambos componentes.
 */

import { useState, useEffect } from 'react'
import { CiudadanoProvider } from './context/CiudadanoContext.jsx'
import { CiudadanoPage }     from './portals/ciudadano/CiudadanoPage.jsx'
import { AuthProvider }      from './context/AuthContext.jsx'
import { useAuth }           from './hooks/useAuth.js'
import { LoginPage }         from './portals/interno/LoginPage.jsx'
import { InternoDashboard }  from './portals/interno/InternoDashboard.jsx'

/** Decide qué renderizar dentro del portal interno según el estado de autenticación */
function InternoApp() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <InternoDashboard /> : <LoginPage />
}

function getRoute(hash) {
  if (!hash || hash === '#' || hash === '#/' || hash === '#/ciudadano') return 'ciudadano'
  if (hash.startsWith('#/interno')) return 'interno'
  return 'ciudadano'
}

export default function App() {
  const [route, setRoute] = useState(() => getRoute(window.location.hash))

  useEffect(() => {
    function onHashChange() {
      setRoute(getRoute(window.location.hash))
    }
    window.addEventListener('hashchange', onHashChange)
    return () => window.removeEventListener('hashchange', onHashChange)
  }, [])

  if (route === 'interno') {
    return (
      <AuthProvider>
        <InternoApp />
      </AuthProvider>
    )
  }

  return (
    <CiudadanoProvider>
      <CiudadanoPage />
    </CiudadanoProvider>
  )
}

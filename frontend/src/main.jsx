/**
 * main.jsx — Entry point de la aplicación.
 *
 * Importa las variables CSS globales (sistema de diseño de las maquetas)
 * antes que cualquier otro estilo, para que --accent, --bg, etc. estén
 * disponibles en todos los componentes.
 */

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/variables.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

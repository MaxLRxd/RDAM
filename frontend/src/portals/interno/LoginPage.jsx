/**
 * LoginPage.jsx — Pantalla de login del Portal Interno.
 *
 * Diseño: fondo azul institucional (--accent) con tarjeta blanca centrada.
 * Réplica fiel de la pantalla "login" de rdam-interno.html.
 *
 * Flujo:
 *   1. El usuario ingresa email y contraseña
 *   2. Se llama a AuthContext.login() → POST /auth/login
 *   3. Si hay error (401), se muestra el mensaje debajo del form
 *   4. Si tiene éxito, AuthContext actualiza `isAuthenticated = true`
 *      y App.jsx renderiza InternoDashboard (ya no LoginPage)
 *
 * Seguridad visual: pie de página con HTTPS / JWT / "Sesión 8 horas"
 * para cumplir el diseño de la maqueta.
 */

import { useState } from 'react'
import { useAuth } from '../../hooks/useAuth.js'
import { Spinner } from '../../components/ui/Spinner.jsx'
import styles from './LoginPage.module.css'

export function LoginPage() {
  const { login, loginError, loginLoading } = useAuth()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    if (!username.trim() || !password) return
    login(username.trim(), password)
  }

  return (
    <div className={styles.root}>
      <div>
        <div className={styles.card}>
          {/* ── Marca ── */}
          <div className={styles.brand}>
            <div className={styles.brandDot}>SF</div>
            <p className={styles.brandTitle}>Sistema RDAM</p>
            <p className={styles.brandSub}>Portal de Gestión Interna · Provincia de Santa Fe</p>
          </div>

          {/* ── Error ── */}
          {loginError && (
            <div className={styles.errorBox} role="alert">
              <span>✕</span>
              <span>{loginError}</span>
            </div>
          )}

          {/* ── Formulario ── */}
          <form className={styles.form} onSubmit={handleSubmit} noValidate>
            <div className={styles.fGroup}>
              <label className={styles.fLabel} htmlFor="username">
                Email institucional
              </label>
              <input
                id="username"
                type="email"
                className={`${styles.fInput} ${loginError ? styles.error : ''}`}
                placeholder="usuario@justiciasantafe.gov.ar"
                value={username}
                onChange={e => setUsername(e.target.value)}
                autoComplete="username"
                required
              />
            </div>

            <div className={styles.fGroup}>
              <label className={styles.fLabel} htmlFor="password">
                Contraseña
              </label>
              <input
                id="password"
                type="password"
                className={`${styles.fInput} ${loginError ? styles.error : ''}`}
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>

            <button
              type="submit"
              className={styles.submitBtn}
              disabled={loginLoading || !username || !password}
            >
              {loginLoading
                ? <><Spinner size="sm" color="white" /> Ingresando...</>
                : 'Ingresar al sistema →'
              }
            </button>
          </form>

          {/* ── Footer de seguridad ── */}
          <div className={styles.secFooter}>
            <span>🔒 HTTPS / TLS 1.3</span>
            <div className={styles.secDot} />
            <span>JWT</span>
            <div className={styles.secDot} />
            <span>Sesión 8 horas</span>
          </div>
        </div>

        <a href="#/" className={styles.backLink}>
          ← Ir al portal ciudadano
        </a>
      </div>
    </div>
  )
}

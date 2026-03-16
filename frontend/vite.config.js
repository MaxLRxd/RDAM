import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo, Vite actúa como proxy inverso hacia el backend Spring Boot.
// Esto evita problemas de CORS: el navegador habla siempre con el mismo origen
// (localhost:5173) y Vite reenvía /api/v1/* al puerto 8080.
// En producción, esta función la cumple Nginx (ver Dockerfile del frontend).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})

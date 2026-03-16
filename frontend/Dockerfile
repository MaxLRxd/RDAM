# ============================================================
# Dockerfile — Frontend RDAM (React + Vite)
# Build multi-etapa: minimiza el tamaño de la imagen final.
#
# Etapa 1 (builder): node:20-alpine
#   - Instala dependencias con `npm ci` (reproducible: usa package-lock.json)
#   - Compila el bundle de producción con `vite build`
#   - Resultado: /app/dist/ con los archivos estáticos optimizados
#
# Etapa 2 (runner): nginx:alpine
#   - Copia solo el /dist/ de la etapa anterior (sin node_modules, sin src)
#   - Copia nginx.conf con la configuración de proxy inverso
#   - Expone el puerto 80
#
# ¿Por qué node:20-alpine y no node:22?
#   Vite 8 y @vitejs/plugin-react ^6 soportan Node ≥ 18.
#   Node 20 LTS es la versión más estable y la que se usa en CI/CD
#   del proyecto. Alpine reduce el tamaño de la imagen base (~5 MB vs ~900 MB).
#
# ¿Por qué `npm ci` y no `npm install`?
#   `npm ci` (clean install) es la variante determinista:
#   - Ignora package.json, instala exactamente lo que dice package-lock.json
#   - Falla si hay discrepancias → detecta derivs no committeadas
#   - Borra node_modules antes de instalar → siempre parte de cero
#   Ideal para CI/CD y Dockerfiles.
# ============================================================


# ──────────────────────────────────────────────────────────────
# ETAPA 1 — Build
# ──────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copiar manifiestos de dependencias primero.
# Docker cachea esta capa si package*.json no cambia, evitando
# reinstalar todo con cada cambio de código fuente.
COPY package.json package-lock.json ./

# Instalar dependencias de forma reproducible.
# --prefer-offline evita descargas si ya hay caché del daemon.
RUN npm ci --prefer-offline

# Copiar el resto del código fuente.
# Se hace DESPUÉS de npm ci para aprovechar la caché de Docker:
# cambios en src/ no invalidan la capa de node_modules.
COPY . .

# Compilar para producción.
# Vite genera /app/dist/ con archivos optimizados:
#   - JS minificado y con hash en el nombre (code splitting)
#   - CSS purificado
#   - Assets copiados y hasheados
RUN npm run build


# ──────────────────────────────────────────────────────────────
# ETAPA 2 — Runtime (imagen final)
# ──────────────────────────────────────────────────────────────
FROM nginx:alpine

# Eliminar la configuración por defecto de nginx.
# La imagen nginx:alpine incluye un servidor demo en /etc/nginx/conf.d/default.conf
# que interferiría con nuestra configuración.
RUN rm /etc/nginx/conf.d/default.conf

# Copiar nuestra configuración personalizada de nginx.
# Incluye: proxy /api/* → backend:8080, SPA fallback, gzip, cache headers.
COPY nginx.conf /etc/nginx/nginx.conf

# Copiar los archivos estáticos compilados por Vite.
# /usr/share/nginx/html es el directorio raíz por defecto de nginx.
COPY --from=builder /app/dist /usr/share/nginx/html

# Puerto que expone el contenedor.
# El docker-compose mapea este 80 al puerto del host (también 80).
EXPOSE 80

# nginx se inicia en modo "daemon off" para que Docker pueda
# monitorear el proceso como PID 1 (si nginx termina, el contenedor termina).
CMD ["nginx", "-g", "daemon off;"]

# CONTEXTO COMPLETO DEL PROYECTO — Sistema RDAM

> **Documento actualizado:** Marzo 2026
> **Propósito:** Proveer todo el contexto necesario para continuar el desarrollo con otra IA o chat.
> **Versión del sistema:** 2.0 MVP — Frontend React implementado y funcionando

---

## 1. CONTEXTO DE NEGOCIO

### ¿Qué es RDAM?

El **Sistema RDAM** (Registro de Deudores Alimentarios Morosos) es una plataforma web gubernamental de la **Provincia de Santa Fe, Argentina**. Permite a ciudadanos solicitar un **certificado digital oficial** que informa si una persona (identificada por DNI/CUIL) figura o no en el registro provincial de deudores alimentarios.

### Conceptos clave del negocio

- El certificado es **INFORMATIVO**, no liberatorio. No extingue obligaciones alimentarias.
- El ciudadano paga un **arancel de emisión** (actualmente $1500 en dev) por cada certificado, independientemente del resultado.
- **Cualquier persona** puede solicitar certificados de terceros. Solo se valida que el email sea accesible.
- El sistema **NO genera certificados PDF**: el operador interno los genera en un sistema judicial externo y los sube manualmente a la plataforma.
- **NO hay integración con API Judicial**. El operador consulta el registro judicial de manera independiente.

### Circunscripciones Judiciales

La Provincia de Santa Fe se organiza en 5 circunscripciones judiciales. Cada solicitud es atendida por el personal de la circunscripción correspondiente al domicilio del solicitante:

| ID | Nombre | Sede |
|----|--------|------|
| 1 | Santa Fe | Sede Santa Fe |
| 2 | Rosario | Sede Rosario |
| 3 | Venado Tuerto | Sede Venado Tuerto |
| 4 | Reconquista | Sede Reconquista |
| 5 | Rafaela | Sede Rafaela |

### Roles del Sistema

| Rol | Descripción |
|-----|-------------|
| **Ciudadano** | Usuario público sin cuenta permanente. Solicita certificados, paga arancel, descarga PDF. Se autentica con token temporal almacenado en Redis. |
| **Operador (Usuario Interno)** | Empleado asignado a una circunscripción. Solo ve solicitudes de su circunscripción. Su única tarea operativa es cargar el certificado PDF una vez confirmado el pago. NO aprueba ni rechaza solicitudes. |
| **Administrador** | Gestión de usuarios internos. Puede ver todas las circunscripciones. Crea/desactiva operadores con asignación de circunscripción. Accede al panel de auditoría. |

### Ciclo de Vida de una Solicitud (Máquina de Estados)

```
[INICIO]
   ↓
PENDIENTE  ← formulario enviado + email validado con OTP
   ↓
   ├──→ PAGADO         ← webhook/callback PlusPagos con estado aprobado
   │       ↓
   │    PUBLICADO      ← operador sube certificado PDF manualmente
   │       ↓
   │    PUBLICADO_VENCIDO (terminal) ← vencimiento 65 días PRD / 2 días DEV
   │
   └──→ VENCIDO (terminal) ← vencimiento 60 días PRD / 1 día DEV sin pago
                            ← pago rechazado por PlusPagos (códigos 4,7,8,9,11)
```

**Transiciones válidas:**
- PENDIENTE → PAGADO, VENCIDO
- PAGADO → PUBLICADO, VENCIDO
- PUBLICADO → PUBLICADO_VENCIDO
- PUBLICADO_VENCIDO y VENCIDO son estados terminales (sin transiciones salientes)

**Mapeo de códigos PlusPagos:**
- Códigos 0, 3 → APROBADO (transición a PAGADO)
- Códigos 4 (rechazada), 7 (expirada), 8 (cancelada), 9 (devuelta), 11 (vencida) → RECHAZADO (transición a VENCIDO)

---

## 2. STACK TECNOLÓGICO

### Backend
- **Java 21** (LTS) con **Spring Boot 3.4.5**
- **Maven** como build tool
- **MySQL 8** — Base de datos relacional
- **Redis 7** — Tokens efímeros (OTP, tokens ciudadano)
- **MinIO** — Almacenamiento de certificados PDF (interfaz S3)
- **Flyway** — Migraciones de base de datos
- **JJWT 0.12.6** — Generación/validación de JWT
- **Lombok** — Reducción de boilerplate
- **Spring Security** — Autenticación y autorización
- **Spring Mail** — Envío de emails (async)
- **Spring Scheduler** — Jobs de vencimiento programados

### Mock de Pasarela de Pagos
- **Node.js 18** con **Express**
- **CryptoJS** — Encriptación AES-256-CBC
- Puerto 3000 (interno Docker: `pluspagos-mock:3000`)

### Frontend (SPA React — implementado)
- **React 19** + **Vite**
- **CSS Modules** — estilos con scope por componente
- **Hash-based routing** — `#/ciudadano` y `#/interno` (sin react-router-dom)
- **Nginx** como servidor estático + reverse proxy
- Dos portales completamente separados dentro de la misma SPA

### Infraestructura (Docker Compose)
7 servicios definidos en `backend/docker-compose.yml`:
- `mysql` — MySQL 8.0 (puerto 3306)
- `redis` — Redis 7 Alpine (puerto 6379)
- `minio` — MinIO (API: 9000, Consola: 9001)
- `createbuckets` — Init container que crea el bucket `rdam-certificados` (se ejecuta una sola vez)
- `mailhog` — MailHog SMTP fake (SMTP: 1025, UI: 8025)
- `backend` — Spring Boot containerizado (accesible desde host en puerto 8080 vía nginx)
- `pluspagos-mock` — Node.js mock (puerto 3000)

**Nota:** El frontend (nginx) puede correrse como contenedor separado con su propio Dockerfile, pero en desarrollo también puede levantarse con `npm run dev` (Vite) apuntando al backend en `localhost:8080`.

### Dependencias Maven principales
```xml
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-data-jpa
spring-boot-starter-data-redis
spring-boot-starter-mail
spring-boot-starter-validation
spring-boot-starter-actuator
jjwt-api / jjwt-impl / jjwt-jackson (0.12.6)
flyway-core / flyway-mysql
mysql-connector-j
minio (8.5.11)
lombok
```

---

## 3. ESTRUCTURA DE CARPETAS

```
Sistema/
├── README.md
├── SPEC.md
├── IMPLEMENTATION.md
├── CONTEXT-FOR-AI.md              # Este archivo
├── RDAM.postman_collection.json
├── .gitignore
│
├── backend/
│   ├── .env                       # Variables de entorno (NO commitear)
│   ├── .env.example               # Template de variables
│   ├── docker-compose.yml         # 7 servicios: MySQL, Redis, MinIO, createbuckets, MailHog, backend, PlusPagos mock
│   ├── Dockerfile                 # Imagen Spring Boot
│   ├── pom.xml                    # Maven: Spring Boot 3.4.5, Java 21
│   ├── mvnw / mvnw.cmd
│   │
│   ├── pluspagos-mock/            # Mock de la pasarela de pagos (Node.js)
│   │   ├── package.json
│   │   ├── server.js              # Express server con endpoints de pago
│   │   ├── crypto.js              # AES-256-CBC encriptación/desencriptación
│   │   └── test_*.js              # Scripts de test de compatibilidad crypto
│   │
│   └── src/
│       ├── main/
│       │   ├── java/com/rdam/backend/
│       │   │   ├── BackendApplication.java          # Entry point (@EnableAsync, @EnableScheduling)
│       │   │   │
│       │   │   ├── config/
│       │   │   │   ├── CacheRequestBodyFilter.java  # Permite leer body múltiples veces (webhooks)
│       │   │   │   └── MinioConfig.java             # Bean de MinioClient
│       │   │   │
│       │   │   ├── controllers/
│       │   │   │   ├── AuditoriaController.java         # GET /auditoria (ADMIN)
│       │   │   │   ├── AuthController.java              # POST /auth/login
│       │   │   │   ├── CertificadoController.java       # GET /certificados/{tokenDescarga}
│       │   │   │   ├── HealthController.java            # GET /health
│       │   │   │   ├── PagoController.java              # POST /solicitudes/{id}/pago/crear
│       │   │   │   ├── SolicitudInternaController.java  # CRUD interno
│       │   │   │   ├── SolicitudPublicaController.java  # Endpoints públicos
│       │   │   │   ├── UsuarioInternoController.java    # ADMIN: crear/desactivar usuarios
│       │   │   │   └── WebhookController.java           # Webhook y callback de PlusPagos
│       │   │   │
│       │   │   ├── domain/
│       │   │   │   ├── dto/
│       │   │   │   │   ├── AuditoriaResponse.java           # DTO respuesta auditoría
│       │   │   │   │   ├── CallbackPlusPagosRequest.java
│       │   │   │   │   ├── CambiarEstadoUsuarioRequest.java
│       │   │   │   │   ├── CrearSolicitudRequest.java
│       │   │   │   │   ├── CrearSolicitudResponse.java
│       │   │   │   │   ├── CrearUsuarioRequest.java
│       │   │   │   │   ├── LoginRequest.java
│       │   │   │   │   ├── LoginResponse.java
│       │   │   │   │   ├── SolicitudEstadoResponse.java
│       │   │   │   │   ├── UsuarioInternoResponse.java
│       │   │   │   │   ├── ValidarOtpRequest.java
│       │   │   │   │   ├── ValidarOtpResponse.java
│       │   │   │   │   └── WebhookPlusPagosRequest.java
│       │   │   │   │
│       │   │   │   └── entity/
│       │   │   │       ├── AuditoriaOperacion.java          # Log inmutable de operaciones
│       │   │   │       ├── Circunscripcion.java
│       │   │   │       ├── Solicitud.java                   # @Version optimistic lock
│       │   │   │       ├── SolicitudHistorialEstado.java
│       │   │   │       └── UsuarioInterno.java              # Implementa UserDetails
│       │   │   │
│       │   │   ├── enums/
│       │   │   │   ├── EstadoSolicitud.java   # PENDIENTE, PAGADO, PUBLICADO, PUBLICADO_VENCIDO, VENCIDO
│       │   │   │   └── RolUsuario.java        # OPERADOR, ADMIN
│       │   │   │
│       │   │   ├── exception/
│       │   │   │   ├── CircunscripcionMismatchException.java
│       │   │   │   ├── EstadoInvalidoException.java
│       │   │   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice (RFC 7807)
│       │   │   │   ├── SolicitudNotFoundException.java
│       │   │   │   └── TokenInvalidoException.java
│       │   │   │
│       │   │   ├── repository/
│       │   │   │   ├── AuditoriaOperacionRepository.java  # findByOperacion(String, Pageable)
│       │   │   │   ├── CircunscripcionRepository.java
│       │   │   │   ├── SolicitudHistorialEstadoRepository.java
│       │   │   │   ├── SolicitudRepository.java
│       │   │   │   └── UsuarioInternoRepository.java
│       │   │   │
│       │   │   ├── security/
│       │   │   │   ├── JwtFilter.java
│       │   │   │   ├── JwtProvider.java
│       │   │   │   ├── SecurityConfig.java
│       │   │   │   ├── TokenCiudadanoFilter.java
│       │   │   │   └── UserDetailsServiceImpl.java
│       │   │   │
│       │   │   ├── service/
│       │   │   │   ├── AuditoriaService.java        # Registro de operaciones + listar con paginación
│       │   │   │   ├── CertificadoService.java      # MinIO upload/download/delete
│       │   │   │   ├── EmailService.java            # Envío async de emails
│       │   │   │   ├── PagoService.java             # Integración PlusPagos
│       │   │   │   ├── SolicitudService.java        # Orquestador principal
│       │   │   │   ├── SolicitudStateMachine.java   # Validación de transiciones de estado
│       │   │   │   ├── TokenService.java            # Redis: OTP y tokens ciudadano
│       │   │   │   ├── UsuarioInternoService.java   # CRUD usuarios internos
│       │   │   │   └── VencimientoScheduler.java    # Job cron de vencimientos
│       │   │   │
│       │   │   └── util/
│       │   │       └── PlusPagosCrypto.java
│       │   │
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/
│       │           ├── V1__crear_tabla_circunscripcion.sql
│       │           ├── V2__crear_tabla_parametros_sistema.sql
│       │           ├── V3__crear_tabla_solicitud.sql
│       │           ├── V4__crear_tablas_auditoria.sql
│       │           ├── V5__crear_tabla_usuario_interno.sql
│       │           └── V6__datos_iniciales.sql
│       │
│       └── test/java/com/rdam/backend/
│           ├── BackendApplicationTests.java
│           └── util/TestCryptoCompatibility.java
│
└── frontend/
    ├── Dockerfile
    ├── nginx.conf                     # Reverse proxy: /api/ → backend:8080, SPA fallback
    ├── vite.config.js
    ├── package.json
    ├── index.html
    └── src/
        ├── main.jsx                   # Entry point
        ├── App.jsx                    # Router hash-based: #/ciudadano | #/interno
        ├── api/
        │   ├── authApi.js             # login()
        │   ├── internoApi.js          # listarSolicitudes, subirCertificado, regenerarToken,
        │   │                          #   crearUsuario, listarUsuarios, cambiarEstadoUsuario,
        │   │                          #   listarAuditoria
        │   └── solicitudesApi.js      # crearSolicitud, validarOtp, consultarEstado, crearOrdenPago
        ├── components/
        │   ├── layout/
        │   │   └── NavCiudadano.jsx
        │   └── ui/
        │       ├── Alert.jsx / .module.css
        │       ├── Badge.jsx / .module.css
        │       ├── Button.jsx / .module.css
        │       ├── Spinner.jsx / .module.css
        │       └── Toast.jsx / .module.css
        ├── context/
        │   ├── AuthContext.jsx        # JWT + usuario interno autenticado
        │   └── CiudadanoContext.jsx   # Estado de sesión ciudadano
        ├── hooks/
        │   ├── useAuth.js
        │   ├── useCiudadano.js
        │   └── useToast.js
        ├── portals/
        │   ├── ciudadano/
        │   │   ├── CiudadanoPage.jsx        # Orquestador de pasos + polling post-pago
        │   │   ├── MisSolicitudes.jsx        # Historial de solicitudes ciudadano
        │   │   ├── PopupArancel.jsx          # Modal de confirmación de arancel
        │   │   ├── SidebarInfo.jsx
        │   │   └── steps/
        │   │       ├── StepFormulario.jsx    # Paso 1: DNI, email, circunscripción
        │   │       ├── StepValidarEmail.jsx  # Paso 2: OTP de 6 dígitos
        │   │       ├── StepPendiente.jsx     # Paso 3: esperando pago + polling retorno
        │   │       ├── StepPago.jsx          # Paso 4: auto-submit form → PlusPagos
        │   │       ├── StepDescarga.jsx      # Paso 5: link de descarga PDF (URL normalizada)
        │   │       └── StepVencido.jsx       # Estado terminal
        │   └── interno/
        │       ├── LoginPage.jsx
        │       ├── InternoDashboard.jsx      # Shell con sidebar + router de panels
        │       ├── components/
        │       │   ├── Sidebar.jsx
        │       │   └── Topbar.jsx
        │       ├── modals/
        │       │   ├── ModalDetalleSolicitud.jsx
        │       │   └── ModalNuevoUsuario.jsx
        │       └── panels/
        │           ├── PanelDashboard.jsx
        │           ├── PanelSolicitudes.jsx  # Listar + subir certificado + regenerar token
        │           ├── PanelUsuarios.jsx     # ADMIN: crear/activar/desactivar usuarios
        │           └── PanelAuditoria.jsx    # ADMIN: log de operaciones con filtros y paginación
        ├── styles/
        │   └── variables.css
        └── utils/
            ├── constants.js
            ├── fetchClient.js         # HTTP client con manejo de auth dual
            ├── formatters.js
            └── storage.js
```

---

## 4. ESQUEMA DE BASE DE DATOS

### Tablas

**circunscripcion** — Datos maestros (5 registros fijos, nunca se modifican desde la API)
- `id_circunscripcion` INT PK AUTO_INCREMENT
- `nombre` VARCHAR(50) NOT NULL
- `sede` VARCHAR(100) NOT NULL

**solicitud** — Entidad central del sistema
- `id_solicitud` BIGINT PK AUTO_INCREMENT
- `nro_tramite` VARCHAR(20) UNIQUE NOT NULL — Formato: RDAM-YYYYMMDD-NNNN
- `dni_cuil` VARCHAR(11) NOT NULL
- `email` VARCHAR(255) NOT NULL
- `id_circunscripcion` INT NOT NULL FK → circunscripcion
- `codigo_validacion` VARCHAR(6) NULL — OTP 6 dígitos (se limpia tras validación)
- `token_acceso` VARCHAR(64) UNIQUE NULL — Token de sesión ciudadano
- `estado` VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE' — CHECK constraint con los 5 estados
- `monto_arancel` DECIMAL(10,2) NULL
- `id_orden_pago` VARCHAR(100) UNIQUE NULL — ID de la orden en PlusPagos
- `sol_fec_pago` TIMESTAMP NULL
- `url_certificado` VARCHAR(500) NULL — Ruta en MinIO
- `token_descarga` VARCHAR(64) UNIQUE NULL — Token criptográfico para descarga
- `sol_fec_emision` TIMESTAMP NULL
- `fecha_creacion` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `version` BIGINT NOT NULL DEFAULT 0 — Optimistic locking

**usuario_interno** — Operadores y Administradores
- `id` BIGINT PK AUTO_INCREMENT
- `username` VARCHAR(100) UNIQUE NOT NULL — Email
- `password_hash` VARCHAR(255) NOT NULL — BCrypt cost=12
- `rol` VARCHAR(20) NOT NULL — CHECK: 'OPERADOR' | 'ADMIN'
- `id_circunscripcion` INT NULL FK → circunscripcion — CHECK: OPERADOR requiere circ, ADMIN no
- `activo` BOOLEAN NOT NULL DEFAULT TRUE
- `fecha_creacion` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**solicitud_historial_estados** — Auditoría de transiciones (append-only)
- `id_historial` BIGINT PK AUTO_INCREMENT
- `id_solicitud` BIGINT NOT NULL FK → solicitud ON DELETE CASCADE
- `estado_anterior` VARCHAR(30) NULL
- `estado_nuevo` VARCHAR(30) NOT NULL
- `id_usuario_interno` BIGINT NULL
- `fecha_cambio` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**auditoria_operaciones** — Log inmutable de operaciones
- `id_auditoria` BIGINT PK AUTO_INCREMENT
- `fecha_hora` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `id_usuario` BIGINT NULL
- `nombre_usuario` VARCHAR(100) NULL
- `operacion` VARCHAR(50) NOT NULL — Ver enum `AuditoriaOperacion.Operaciones`
- `ip_origen` VARCHAR(45) NULL
- `detalle` TEXT NULL
- `entidad_id` BIGINT NULL

Operaciones auditadas (`AuditoriaOperacion.Operaciones`): `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CERTIFICADO_PUBLICADO`, `TOKEN_REGENERADO`, `DESCARGA_CERTIFICADO`, `PAGO_APROBADO`, `PAGO_RECHAZADO`, `SOLICITUD_CREADA`, `USUARIO_CREADO`, `USUARIO_ACTIVADO`, `USUARIO_DESACTIVADO`.

**parametros_sistema** — Configuración persistente (no usada actualmente por el código Java)
- `clave` VARCHAR(50) PK
- `valor` VARCHAR(255) NOT NULL
- `descripcion` VARCHAR(255) NULL

### Datos iniciales (V6)
- 5 circunscripciones (Santa Fe, Rosario, Venado Tuerto, Reconquista, Rafaela)
- Admin por defecto: `admin@rdam.santafe.gob.ar` / `Admin1234!` (BCrypt cost=12)
- Parámetros de sistema: MONTO_ARANCEL=1500, VALIDEZ_DIAS_CERT=65, VENCIMIENTO_PENDIENTE_DIAS=60

---

## 5. API REST — ENDPOINTS

**Base URL:** `http://localhost:8080/api/v1`
**Nota nginx:** En producción/Docker el frontend accede a `/api/v1` (sin host), que nginx proxea a `http://backend:8080/api/v1`.

### Endpoints Públicos (sin autenticación)

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| POST | `/solicitudes` | Crear solicitud | `{ dniCuil, email, idCircunscripcion }` |
| POST | `/solicitudes/{id}/validar` | Validar OTP | `{ codigo }` (6 dígitos) |
| POST | `/auth/login` | Login interno | `{ username, password }` |
| GET | `/health` | Health check | — |
| GET | `/certificados/{tokenDescarga}` | Descargar PDF | — |
| POST | `/webhooks/pluspagos` | Webhook global PlusPagos | `WebhookPlusPagosRequest` |
| POST | `/webhooks/pluspagos/callback` | Callback por transacción | `CallbackPlusPagosRequest` |

### Endpoints Ciudadano (token Redis en header `Authorization` — sin prefijo Bearer)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/solicitudes/{nroTramite}` | Consultar estado de solicitud |
| POST | `/solicitudes/{id}/pago/crear` | Crear orden de pago |

### Endpoints Internos (JWT en header `Authorization: Bearer {token}`)

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| GET | `/solicitudes` | OPERADOR, ADMIN | Listar solicitudes (filtrado auto por circunscripción) |
| POST | `/solicitudes/{id}/certificado` | OPERADOR, ADMIN | Subir certificado PDF (multipart/form-data) |
| POST | `/solicitudes/{id}/certificado/regenerar-token` | OPERADOR, ADMIN | Regenerar token de descarga |
| GET | `/usuarios-internos` | ADMIN | Listar usuarios internos |
| POST | `/usuarios-internos` | ADMIN | Crear usuario interno |
| PATCH | `/usuarios-internos/{id}/estado` | ADMIN | Activar/desactivar usuario |
| GET | `/auditoria` | ADMIN | Listar log de operaciones (paginado, filtro opcional `?accion=`) |

---

## 6. AUTENTICACIÓN Y SEGURIDAD

### Doble esquema de autenticación

**Ciudadanos:** Token opaco almacenado en Redis
- Se genera tras validar el OTP (64 caracteres = 2 UUIDs concatenados sin guiones)
- Se envía en header `Authorization: {token}` (sin prefijo Bearer)
- Redis key: `ciudadano:token:{token}` → valor: `{nroTramite}`
- TTL: 24 horas
- Rol asignado: `ROLE_CIUDADANO`

**Usuarios Internos:** JWT firmado con HMAC-SHA256
- Se genera en `/auth/login`
- Se envía en header `Authorization: Bearer {token}`
- Expiración: 8 horas (28800000 ms)
- Claims: `sub` (username), `rol` (OPERADOR|ADMIN), `circunscripcion` (ID o null), `iat`, `exp`
- Se valida en cada request: firma + expiración + usuario activo en DB

**Orden de filtros:**
1. `JwtFilter` — Intenta autenticar como usuario interno
2. `TokenCiudadanoFilter` — Si no hubo auth JWT, intenta como ciudadano via Redis

### fetchClient.js — Cliente HTTP del Frontend

Wrapper sobre `fetch` nativo ubicado en `frontend/src/utils/fetchClient.js`. Firma de métodos:

```js
fetchClient.get(path, opts)
fetchClient.post(path, body, opts)
fetchClient.patch(path, body, opts)
fetchClient.delete(path, opts)
```

`opts` puede contener: `{ jwtToken, ciudadanoToken, isMultipart }`. El `jwtToken` genera el header `Authorization: Bearer {token}`; el `ciudadanoToken` genera `Authorization: {token}` sin prefijo. **No mezclar**: el `jwtToken` va en `opts`, no en `body`.

### Seguridad adicional
- BCrypt con cost=12 para passwords
- Control optimista con `@Version` en Solicitud (HTTP 409 en conflicto)
- CSRF deshabilitado (API REST stateless)
- Usuarios desactivados son rechazados en tiempo real (se verifica `activo` en cada request)
- Filtrado automático por circunscripción: el Operador solo ve sus solicitudes
- HMAC-SHA256 para validar webhooks de pago (bypass en modo SIM si no hay firma)

---

## 7. INTEGRACIÓN CON PASARELA DE PAGOS (PlusPagos)

### Arquitectura de pago

El flujo usa un esquema de **form POST encriptado** (no API REST directa):

1. **Backend genera formularioDatos** — `POST /solicitudes/{id}/pago/crear` devuelve JSON con campos encriptados
2. **Frontend auto-submit** — `StepPago.jsx` construye un formulario HTML invisible y lo envía al mock via `requestSubmit()`
3. **Mock procesa pago** — Muestra formulario de tarjeta (con auto-formato), procesa y decide resultado
4. **Mock notifica al backend** — Dos mecanismos (ver más abajo), luego redirige al usuario
5. **Frontend detecta retorno** — Extrae `?retorno=pagado|rechazado` del hash y hace polling hasta confirmar el nuevo estado

### Notificaciones del mock al backend

- **Webhook global:** `POST /api/v1/webhooks/pluspagos` (si WEBHOOK_URL está configurada en el dashboard)
- **Callback por transacción:** `POST` a la URL encriptada en `CallbackSuccess`/`CallbackCancel` del formulario

**IMPORTANTE — Race condition resuelta:** El mock ejecuta `await sendCallback(callbackUrl, txn)` **antes** de redirigir al usuario. Sin el `await`, el frontend llegaba a PENDIENTE mientras el backend aún no había procesado el callback.

### URLs de retorno post-pago

```java
// PagoService.java
String urlExito = serverPublicUrl + "/#/ciudadano?retorno=pagado";
String urlError  = serverPublicUrl + "/#/ciudadano?retorno=rechazado";
```

El frontend (`CiudadanoPage.jsx`) parsea `window.location.hash` al montarse, extrae `?retorno=`, limpia la URL con `window.history.replaceState()`, y si está en estado PENDIENTE lanza polling agresivo (cada 2s, máx 10 intentos) hasta detectar el nuevo estado.

### Campos del formulario encriptado

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `Comercio` | Plano | GUID del comercio |
| `TransaccionComercioId` | Plano | nroTramite |
| `Monto` | **Encriptado** AES-256-CBC | Monto en centavos como string |
| `UrlSuccess` | **Encriptado** | URL de redirección si aprobado |
| `UrlError` | **Encriptado** | URL de redirección si rechazado |
| `CallbackSuccess` | **Encriptado** | URL callback backend (aprobado) |
| `CallbackCancel` | **Encriptado** | URL callback backend (rechazado) |
| `Informacion` | Plano | Descripción del pago |

### Encriptación AES-256-CBC (compatible Java ↔ Node.js)

**Formato:** `Base64( IV_16_bytes + AES_Ciphertext )`

- Clave: se deriva con SHA-256 del `secretKey` → 256 bits
- IV: 16 bytes aleatorios por cada encriptación
- Padding: PKCS5/PKCS7
- Clave compartida: `PLUSPAGOS_SECRET_KEY` = `clave-secreta-campus-2026`

### Tarjetas de prueba del mock

| Número | Resultado |
|--------|-----------|
| 4242 4242 4242 4242 | Aprobado |
| 4000 0000 0000 0002 | Rechazado |
| 5555 5555 5555 4444 | Aprobado |
| 5105 1051 0510 5100 | Rechazado |
| 378282246310005 | Aprobado |
| 371449635398431 | Rechazado |
| Cualquier otro número | Rechazado (default seguro) |

El formulario acepta números con espacios o guiones (e.g., `4242-4242-4242-4242`): el campo visible auto-formatea con espacios cada 4 dígitos; el campo oculto envía solo dígitos. El servidor también aplica `replace(/\D/g, '')` como defensa adicional.

El campo de vencimiento auto-inserta `/` al escribir (`1127` → `11/27`).

### HMAC-SHA256 para webhooks/callbacks

- El mock firma el body con `HMAC-SHA256(body, CONFIG.SECRET_KEY)` → header `X-PlusPagos-Signature`
- El backend en SIM mode bypasea la validación si no hay firma
- ⚠️ En producción habría que unificar los secrets entre mock y backend

---

## 8. VARIABLES DE ENTORNO

```env
# Base de datos
DB_URL=jdbc:mysql://mysql:3306/rdam_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=rdam_user
DB_PASSWORD=rdam_pass_local

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET_NAME=rdam-certificados

# SMTP (MailHog en dev)
SMTP_HOST=mailhog
SMTP_PORT=1025
SMTP_AUTH=false
SMTP_STARTTLS=false
MAIL_FROM=noreply@rdam.santafe.gob.ar

# JWT
JWT_SECRET=desarrollo-local-2026-este-secret-es-solo-para-dev

# Pagos
PAYMENT_MODE=sim
PLUSPAGOS_HMAC_SECRET=clave-secreta-campus-2026
PLUSPAGOS_MERCHANT_GUID=test-merchant-001
PLUSPAGOS_SECRET_KEY=clave-secreta-campus-2026
PLUSPAGOS_API_URL=http://pluspagos-mock:3000     # Container-to-container (Spring Boot → mock)
PLUSPAGOS_PUBLIC_URL=http://localhost:3000        # Accesible desde el browser

# Reglas de negocio (valores DEV reducidos para testing)
MONTO_ARANCEL=1500.00
VENCIMIENTO_PENDIENTE_DIAS=1     # PRD: 60
VALIDEZ_CERTIFICADO_DIAS=2       # PRD: 65

# URLs del servidor — arquitectura dual (Docker interno vs browser)
SERVER_BASE_URL=http://backend:8080      # Container-to-container (para callbacks internos)
SERVER_PUBLIC_URL=http://localhost       # Accesible desde el browser (nginx en puerto 80)
```

**Arquitectura de URLs:**
- `SERVER_BASE_URL` — usado por el backend para construir URLs que otros **servicios Docker** consumirán (e.g., callback del mock → backend)
- `SERVER_PUBLIC_URL` — usado para construir URLs que el **browser** navegará (e.g., redirect post-pago, link de descarga en `SolicitudEstadoResponse`)
- `PLUSPAGOS_API_URL` — URL del mock para que el **backend** envíe la orden (container-to-container)
- `PLUSPAGOS_PUBLIC_URL` — URL del mock para que el **browser** haga el form POST

---

## 9. CONFIGURACIÓN DEL APPLICATION.PROPERTIES

```properties
server.port=8080
server.servlet.context-path=/api/v1

spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

rdam.jwt.expiration-ms=28800000

rdam.scheduler.vencimiento-cron=0 0 2 * * *

spring.servlet.multipart.max-file-size=10MB
rdam.negocio.max-upload-size-bytes=10485760

rdam.security.rate-limit-por-minuto=10
```

---

## 10. NGINX — REVERSE PROXY (frontend/nginx.conf)

El frontend usa nginx para:
- Servir los archivos estáticos del build de Vite
- Proxear `/api/` → `http://backend:8080` (preserva el path completo)
- Proxear `/sim/` → `http://backend:8080` (simulador de pago)
- SPA fallback: cualquier ruta no encontrada → `index.html`
- Cache 1 año para assets con hash (`.js`, `.css`, imágenes)
- No-cache para `index.html`
- gzip habilitado para texto/JS/CSS
- `client_max_body_size 15m` (certificados PDF)

**Importante:** El frontend nunca usa URLs absolutas al backend. Todas las llamadas van a rutas relativas (`/api/v1/...`) que nginx intercepta y proxea. Esto es lo que hace que `fetchClient.js` funcione tanto en `npm run dev` (con el proxy de Vite en `vite.config.js`) como en producción Docker (con nginx).

---

## 11. SERVICIOS PRINCIPALES — RESUMEN FUNCIONAL

### SolicitudService (orquestador central)
- `crearSolicitud()` — Crea solicitud PENDIENTE, genera OTP en Redis (TTL 15min), envía email
- `validarOtp()` — Valida OTP (máx 3 intentos), genera token ciudadano en Redis (TTL 24h)
- `consultarEstado()` — Devuelve vista pública; usa `publicUrl` (no `baseUrl`) para generar el link de descarga
- `crearOrdenPago()` — Genera orden encriptada, guarda idOrdenPago en DB
- `procesarCallbackPago()` — Busca por nroTramite, aplica resultado (PAGADO o VENCIDO)
- `procesarWebhookPago()` — Busca por idOrdenPago, aplica resultado
- `subirCertificado()` — Valida estado PAGADO + circunscripción, sube a MinIO, genera token, transiciona a PUBLICADO. Registra `CERTIFICADO_PUBLICADO` en auditoría.
- `regenerarTokenDescarga()` — Nuevo token sin reemplazar archivo. Registra `TOKEN_REGENERADO` en auditoría.
- `listarSolicitudes()` — OPERADOR: solo su circunscripción; ADMIN: todas

### AuditoriaService
- `registrar(operacion, detalle, actorId, actorNombre, entidadId)` — Inserta registro inmutable. Anotado con `@Async` + `@Transactional(Propagation.REQUIRES_NEW)` para no afectar el flujo principal.
- `listar(pageable)` — Devuelve todos los registros paginados (sort `fechaHora DESC`)
- `listarPorOperacion(operacion, pageable)` — Filtra por tipo de operación

### UsuarioInternoService
- `crearUsuario()` — Crea operador/admin con BCrypt. Registra `USUARIO_CREADO` en auditoría.
- `cambiarEstado()` — Activa/desactiva. Registra `USUARIO_ACTIVADO` o `USUARIO_DESACTIVADO` en auditoría.
- El actor (quien ejecuta la operación) se obtiene de `SecurityContextHolder` sin cambiar las firmas de los métodos.

### TokenService (Redis)
- OTP: key `otp:{idSolicitud}`, TTL 15 min, máx 3 intentos (`otp:intentos:{idSolicitud}`)
- Token ciudadano: key `ciudadano:token:{token}`, TTL 24h, valor = nroTramite

### PagoService
- `crearOrdenPago()` — Encripta campos con PlusPagosCrypto, retorna `ResultadoOrdenPago`
- URLs de retorno incluyen query param: `?retorno=pagado` o `?retorno=rechazado`
- `validarFirmaHmac()` — HMAC-SHA256, bypass en SIM mode si no hay firma

### VencimientoScheduler (cron diario 2 AM)
- `vencerSolicitudesSinPago()` — PENDIENTE con fecha > X días → VENCIDO, envía email
- `vencerCertificados()` — PUBLICADO con emisión > X días → PUBLICADO_VENCIDO, elimina de MinIO

### CertificadoService (MinIO)
- Sube PDF a `certificados/{idSolicitud}/{uuid}.pdf`
- Genera token de 64 caracteres (2 UUIDs concatenados sin guiones)

### EmailService (@Async)
- 4 templates: OTP, pago confirmado, certificado disponible, solicitud vencida
- Errores loggeados pero no bloquean el flujo principal

---

## 12. MANEJO DE ERRORES

Todas las excepciones son manejadas por `GlobalExceptionHandler` usando formato **RFC 7807 ProblemDetail**:

| Excepción | HTTP | Causa |
|-----------|------|-------|
| `MethodArgumentNotValidException` | 400 | Validación de DTOs (@Valid) |
| `EstadoInvalidoException` | 400 | Transición de estado inválida |
| `TokenInvalidoException` | 401 | JWT/token expirado o inválido |
| `CircunscripcionMismatchException` | 403 | Operador intenta acceder a otra circunscripción |
| `SolicitudNotFoundException` | 404 | Solicitud no encontrada |
| `ObjectOptimisticLockingFailureException` | 409 | Conflicto de concurrencia (@Version) |
| `Exception` (genérica) | 500 | Error interno (nunca expone stacktrace al cliente) |

---

## 13. FORMATO DE NÚMERO DE TRÁMITE

`RDAM-YYYYMMDD-NNNN`

Ejemplo: `RDAM-20260305-0042`

Generado por `SolicitudService.generarNroTramite()` usando fecha actual y un `AtomicInteger` como contador secuencial.

---

## 14. CONVENCIONES DE REDIS

| Key Pattern | Valor | TTL | Uso |
|-------------|-------|-----|-----|
| `otp:{idSolicitud}` | Código OTP 6 dígitos | 15 min | Validación de email |
| `otp:intentos:{idSolicitud}` | Contador de intentos | 15 min | Límite de 3 intentos |
| `ciudadano:token:{token64chars}` | nroTramite | 24 horas | Sesión ciudadano |

---

## 15. COLECCIÓN POSTMAN

Archivo: `RDAM.postman_collection.json` en la raíz del proyecto.

Variables de la colección:
- `base_url` = `http://localhost:8080/api/v1`
- `jwt_token` — Se setea automáticamente tras login exitoso
- `ciudadano_token` — Se setea automáticamente tras validar OTP
- `id_solicitud` — Se setea automáticamente tras crear solicitud
- `nro_tramite` — Se setea automáticamente
- `otp_code` — Se debe completar manualmente (ver MailHog en http://localhost:8025)

---

## 16. CÓMO LEVANTAR EL PROYECTO

```bash
# 1. Ir al directorio del backend
cd backend/

# 2. Crear .env a partir del template (editar según entorno)
cp .env.example .env

# 3. Levantar toda la infraestructura + backend con Docker Compose
docker compose up -d

# 4. Verificar servicios
docker compose ps

# 5. Verificar que el backend arrancó
curl http://localhost:8080/api/v1/health
```

**Para el frontend (desarrollo local sin Docker):**
```bash
cd frontend/
npm install
npm run dev      # Vite en http://localhost:5173, proxy /api/ → localhost:8080
```

**URLs útiles en desarrollo:**
- Frontend (Vite dev): http://localhost:5173
- Frontend (Docker/nginx): http://localhost
- API Backend: http://localhost:8080/api/v1
- MailHog (ver emails): http://localhost:8025
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin123)
- PlusPagos Mock: http://localhost:3000
- PlusPagos Dashboard: http://localhost:3000/dashboard

**Credenciales de acceso al panel interno:**
- Email: `admin@rdam.santafe.gob.ar`
- Password: `Admin1234!`

---

## 17. ROUTING DEL FRONTEND (Hash-based)

La SPA no usa react-router-dom. El routing se hace parseando `window.location.hash`:

- `#/ciudadano` → Portal del Ciudadano (`CiudadanoPage`)
- `#/interno` → Si hay JWT válido → `InternoDashboard`; sino → `LoginPage`
- `#/ciudadano?retorno=pagado` — Señal post-pago para lanzar polling
- `#/ciudadano?retorno=rechazado` — Señal de pago rechazado

Dentro del dashboard interno, los panels se muestran según el estado del sidebar (no modifica el hash).

---

## 18. ISSUES CONOCIDOS Y PENDIENTES

### Issues técnicos sin resolver
1. **Mismatch de HMAC secrets:** El mock firma con `clave-secreta-campus-2026` pero el backend valida con `PLUSPAGOS_HMAC_SECRET`. Funciona en SIM mode (bypass de validación). En producción habría que configurar el mismo secret en ambos lados.
2. **Rate limiting:** Configurado en `application.properties` pero no implementado como filtro.
3. **Tabla `parametros_sistema`:** Existe en DB con datos iniciales pero el código Java no la lee (usa `application.properties`).
4. **Refresh tokens:** No implementados para usuarios internos (excluido del MVP).

### Pendientes de implementación
1. **Tests unitarios y de integración** — Solo existe `BackendApplicationTests.java` y `TestCryptoCompatibility.java`. Falta cubrir servicios core (SolicitudService, PagoService, etc.).
2. **`MisSolicitudes.jsx`** — El historial de solicitudes del ciudadano puede estar parcialmente implementado; verificar estado funcional.

---

## 19. DECISIONES ARQUITECTÓNICAS IMPORTANTES

1. **Sin aprobación/rechazo manual (v1.2):** El operador no interviene antes del pago. Solo carga el certificado tras confirmación de pago.
2. **Circunscripción en JWT:** El claim permite filtrado automático sin consulta adicional a DB.
3. **Autenticación híbrida:** Token opaco Redis para ciudadanos (sin cuenta) vs JWT para internos.
4. **MinIO como S3 local:** Permite migración futura a AWS S3 sin cambios de código.
5. **Concurrencia optimista (@Version):** Evita que dos operadores trabajen el mismo caso simultáneamente.
6. **Form POST (no API REST) para pagos:** Porque el mock devuelve HTML (formulario de tarjeta), no JSON.
7. **Email async (@Async):** SMTP caído no bloquea el flujo principal.
8. **Flyway + validate:** El schema solo se crea via migraciones, JPA solo valida.
9. **URLs relativas en el frontend:** `fetchClient.js` usa `/api/v1` sin host. Vite proxea en dev; nginx proxea en producción. Nunca hay URLs hardcodeadas al backend en el frontend.
10. **Dual URL architecture:** `SERVER_BASE_URL` para comunicación inter-contenedor; `SERVER_PUBLIC_URL` para links que abre el browser. Necesario porque `http://backend:8080` no es resoluble desde fuera de la red Docker.
11. **`await sendCallback()` en el mock:** Garantiza que el backend actualiza el estado antes de que el browser sea redirigido, evitando la race condition que mostraba PENDIENTE tras volver del pago.
12. **`?retorno=` + polling en el frontend:** Como capa defensiva adicional sobre el callback, el frontend hace polling agresivo al volver del pago, no dependiendo 100% de que el callback llegue antes de la redirección.

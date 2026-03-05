# CONTEXTO COMPLETO DEL PROYECTO — Sistema RDAM

> **Documento generado:** Marzo 2026
> **Propósito:** Proveer todo el contexto necesario para continuar el desarrollo con otra IA o chat.
> **Versión del sistema:** 1.2 MVP — En desarrollo

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
| **Administrador** | Gestión de usuarios internos. Puede ver todas las circunscripciones. Crea/desactiva operadores con asignación de circunscripción. |

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
- Puerto 3000

### Frontend (prototipos HTML estáticos)
- Dos archivos HTML en `frontend/prototipo/`:
  - `rdam-ciudadano.html` — Portal del ciudadano
  - `rdam-interno.html` — Panel interno
- El frontend definitivo aún no está implementado (planificado con React o Vue)

### Infraestructura (Docker Compose)
5 servicios:
- `rdam-mysql` — MySQL 8.0 (puerto 3306)
- `rdam-redis` — Redis 7 Alpine (puerto 6379)
- `rdam-minio` — MinIO (API: 9000, Consola: 9001)
- `rdam-mailhog` — MailHog SMTP fake (SMTP: 1025, UI: 8025)
- `rdam-pluspagos-mock` — Node.js mock (puerto 3000)

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
├── README.md                          # Visión general del proyecto
├── SPEC.md                            # Especificación funcional completa
├── IMPLEMENTATION.md                  # Guía técnica con plan de sprints
├── CONTEXTO-PROYECTO.md               # Este archivo
├── RDAM.postman_collection.json       # Colección Postman para testing
├── .gitignore
│
├── backend/
│   ├── .env                           # Variables de entorno (NO commitear)
│   ├── .env-template                  # Template de variables
│   ├── docker-compose.yml             # 5 servicios: MySQL, Redis, MinIO, MailHog, PlusPagos mock
│   ├── pom.xml                        # Maven: Spring Boot 3.4.5, Java 21
│   ├── mvnw / mvnw.cmd               # Maven wrapper
│   │
│   ├── pluspagos-mock/                # Mock de la pasarela de pagos (Node.js)
│   │   ├── package.json
│   │   ├── server.js                  # Express server con endpoints de pago
│   │   ├── crypto.js                  # AES-256-CBC encriptación/desencriptación
│   │   ├── test_crypto_compatibility.js
│   │   ├── test_actual_module.js
│   │   └── test_fixed_vector.js
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
│       │   │   │   ├── AuthController.java          # POST /auth/login
│       │   │   │   ├── CertificadoController.java   # GET /certificados/{tokenDescarga}
│       │   │   │   ├── HealthController.java        # GET /health
│       │   │   │   ├── PagoController.java          # POST /solicitudes/{id}/pago/crear
│       │   │   │   ├── SolicitudInternaController.java  # CRUD interno (listar, subir cert, regenerar token)
│       │   │   │   ├── SolicitudPublicaController.java  # Endpoints públicos (crear, validar OTP, consultar)
│       │   │   │   ├── UsuarioInternoController.java    # ADMIN: crear/desactivar usuarios
│       │   │   │   └── WebhookController.java           # Webhook y callback de PlusPagos
│       │   │   │
│       │   │   ├── domain/
│       │   │   │   ├── dto/
│       │   │   │   │   ├── CallbackPlusPagosRequest.java    # DTO callback por transacción
│       │   │   │   │   ├── CambiarEstadoUsuarioRequest.java # Activar/desactivar usuario
│       │   │   │   │   ├── CrearSolicitudRequest.java       # Crear solicitud ciudadano
│       │   │   │   │   ├── CrearSolicitudResponse.java      # Respuesta con nroTramite
│       │   │   │   │   ├── CrearUsuarioRequest.java         # Crear usuario interno
│       │   │   │   │   ├── LoginRequest.java                # Login credenciales
│       │   │   │   │   ├── LoginResponse.java               # JWT + metadata
│       │   │   │   │   ├── SolicitudEstadoResponse.java     # Vista pública de estado
│       │   │   │   │   ├── UsuarioInternoResponse.java      # Info de usuario
│       │   │   │   │   ├── ValidarOtpRequest.java           # Código OTP 6 dígitos
│       │   │   │   │   ├── ValidarOtpResponse.java          # Token ciudadano + estado
│       │   │   │   │   └── WebhookPlusPagosRequest.java     # Payload webhook global
│       │   │   │   │
│       │   │   │   └── entity/
│       │   │   │       ├── AuditoriaOperacion.java          # Log inmutable de operaciones
│       │   │   │       ├── Circunscripcion.java             # Datos maestros (5 registros fijos)
│       │   │   │       ├── Solicitud.java                   # Entidad central (@Version optimistic lock)
│       │   │   │       ├── SolicitudHistorialEstado.java    # Historial de transiciones (append-only)
│       │   │   │       └── UsuarioInterno.java              # Implementa UserDetails
│       │   │   │
│       │   │   ├── enums/
│       │   │   │   ├── EstadoSolicitud.java   # PENDIENTE, PAGADO, PUBLICADO, PUBLICADO_VENCIDO, VENCIDO
│       │   │   │   └── RolUsuario.java        # OPERADOR, ADMIN
│       │   │   │
│       │   │   ├── exception/
│       │   │   │   ├── CircunscripcionMismatchException.java  # 403
│       │   │   │   ├── EstadoInvalidoException.java           # 400
│       │   │   │   ├── GlobalExceptionHandler.java            # @RestControllerAdvice (RFC 7807)
│       │   │   │   ├── SolicitudNotFoundException.java        # 404
│       │   │   │   └── TokenInvalidoException.java            # 401
│       │   │   │
│       │   │   ├── repository/
│       │   │   │   ├── AuditoriaOperacionRepository.java
│       │   │   │   ├── CircunscripcionRepository.java
│       │   │   │   ├── SolicitudHistorialEstadoRepository.java
│       │   │   │   ├── SolicitudRepository.java     # Queries custom con @Query
│       │   │   │   └── UsuarioInternoRepository.java
│       │   │   │
│       │   │   ├── security/
│       │   │   │   ├── JwtFilter.java               # Filtro JWT para usuarios internos
│       │   │   │   ├── JwtProvider.java             # Generación/validación JWT (HMAC-SHA256)
│       │   │   │   ├── SecurityConfig.java          # HttpSecurity, BCrypt(12), filtros
│       │   │   │   ├── TokenCiudadanoFilter.java    # Filtro token opaco ciudadano (Redis)
│       │   │   │   └── UserDetailsServiceImpl.java  # Carga usuario desde DB
│       │   │   │
│       │   │   ├── service/
│       │   │   │   ├── CertificadoService.java      # MinIO upload/download/delete
│       │   │   │   ├── EmailService.java            # Envío async de emails
│       │   │   │   ├── PagoService.java             # Integración PlusPagos (encriptación, HMAC)
│       │   │   │   ├── SolicitudService.java        # Orquestador principal del flujo
│       │   │   │   ├── SolicitudStateMachine.java   # Validación de transiciones de estado
│       │   │   │   ├── TokenService.java            # Redis: OTP y tokens ciudadano
│       │   │   │   ├── UsuarioInternoService.java   # CRUD usuarios internos
│       │   │   │   └── VencimientoScheduler.java    # Job programado de vencimientos
│       │   │   │
│       │   │   └── util/
│       │   │       └── PlusPagosCrypto.java         # AES-256-CBC compatible con CryptoJS
│       │   │
│       │   └── resources/
│       │       ├── application.properties           # Configuración centralizada con ${ENV_VARS}
│       │       └── db/migration/
│       │           ├── V1__crear_tabla_circunscripcion.sql
│       │           ├── V2__crear_tabla_parametros_sistema.sql
│       │           ├── V3__crear_tabla_solicitud.sql
│       │           ├── V4__crear_tablas_auditoria.sql
│       │           ├── V5__crear_tabla_usuario_interno.sql
│       │           └── V6__datos_iniciales.sql      # 5 circunscripciones + admin inicial
│       │
│       └── test/java/com/rdam/backend/
│           ├── BackendApplicationTests.java
│           └── util/TestCryptoCompatibility.java    # Test de compatibilidad AES Java↔Node
│
└── frontend/
    └── prototipo/
        ├── rdam-ciudadano.html
        └── rdam-interno.html
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
- `operacion` VARCHAR(50) NOT NULL — DESCARGA_CERTIFICADO, LOGIN_EXITOSO, etc.
- `ip_origen` VARCHAR(45) NULL
- `detalle` TEXT NULL
- `entidad_id` BIGINT NULL

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

### Endpoints Ciudadano (token Redis en header `Authorization`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/solicitudes/{nroTramite}` | Consultar estado de solicitud |
| POST | `/solicitudes/{id}/pago/crear` | Crear orden de pago |

### Endpoints Internos (JWT en header `Authorization: Bearer {token}`)

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| GET | `/solicitudes` | OPERADOR, ADMIN | Listar solicitudes (filtrado auto por circunscripción) |
| POST | `/solicitudes/{id}/certificado` | OPERADOR, ADMIN | Subir certificado PDF (multipart) |
| POST | `/solicitudes/{id}/certificado/regenerar-token` | OPERADOR, ADMIN | Regenerar token de descarga |
| POST | `/usuarios-internos` | ADMIN | Crear usuario interno |
| PATCH | `/usuarios-internos/{id}/estado` | ADMIN | Activar/desactivar usuario |

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

El flujo de pago usa un esquema de **form POST encriptado** (no API REST directa):

1. **Backend genera formularioDatos** — `POST /solicitudes/{id}/pago/crear` devuelve un JSON con campos encriptados
2. **Frontend auto-submit** — El frontend construye un formulario HTML invisible y lo envía al mock
3. **Mock procesa pago** — Muestra formulario de tarjeta, procesa y decide resultado
4. **Mock notifica al backend** — Vía dos mecanismos:
   - **Webhook global:** `POST /api/v1/webhooks/pluspagos` (si hay WEBHOOK_URL configurada)
   - **Callback por transacción:** `POST /api/v1/webhooks/pluspagos/callback` (usando URL encriptada en el form)

### Campos del formulario encriptado

La respuesta de `crearOrdenPago()` incluye `formularioDatos` (Map<String, String>):

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `Comercio` | Plano | GUID del comercio (merchant) |
| `TransaccionComercioId` | Plano | nroTramite de la solicitud |
| `Monto` | **Encriptado** AES-256-CBC | Monto en formato string |
| `UrlSuccess` | **Encriptado** | URL de redirección si pago exitoso |
| `UrlError` | **Encriptado** | URL de redirección si pago falla |
| `CallbackSuccess` | **Encriptado** | URL de callback backend (aprobado) |
| `CallbackCancel` | **Encriptado** | URL de callback backend (rechazado) |
| `Informacion` | Plano | Descripción del pago |

### Encriptación AES-256-CBC (compatible Java ↔ Node.js)

**Formato:** `Base64( IV_16_bytes + AES_Ciphertext )`

- Clave: se deriva con SHA-256 del `secretKey` (string) → 256 bits
- IV: 16 bytes aleatorios por cada encriptación
- Padding: PKCS5/PKCS7
- Clave compartida: `PLUSPAGOS_SECRET_KEY` = `clave-secreta-campus-2026`

Implementaciones idénticas en:
- **Java:** `PlusPagosCrypto.java` (javax.crypto)
- **Node:** `crypto.js` (CryptoJS)

### HMAC-SHA256 para webhooks/callbacks

- El mock firma el body JSON con `HMAC-SHA256(body, SECRET_KEY)` y lo envía en header `X-PlusPagos-Signature`
- El backend valida la firma... **excepto en modo SIM donde acepta requests sin firma**
- ⚠️ **Posible issue:** El mock firma con `CONFIG.SECRET_KEY` (`clave-secreta-campus-2026`) pero el backend valida con `hmacSecret` (`PLUSPAGOS_HMAC_SECRET` = `dev-secret`). Funciona porque en SIM mode se bypasea la validación.

### Tarjetas de prueba del mock

| Número | Resultado |
|--------|-----------|
| 4242 4242 4242 4242 | Aprobado |
| 4000 0000 0000 0002 | Rechazado |
| 5555 5555 5555 4444 | Aprobado |
| 5105 1051 0510 5100 | Rechazado |
| 378282246310005 | Aprobado |
| 371449635398431 | Rechazado |

---

## 8. VARIABLES DE ENTORNO

```env
# Base de datos
DB_URL=jdbc:mysql://localhost:3306/rdam_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=rdam_user
DB_PASSWORD=rdam_pass_local

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET_NAME=rdam-certificados

# SMTP (MailHog en dev)
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_AUTH=false
SMTP_STARTTLS=false
MAIL_FROM=noreply@rdam.santafe.gob.ar

# JWT
JWT_SECRET=desarrollo-local-2026-este-secret-es-solo-para-dev

# Pagos
PAYMENT_MODE=sim
PLUSPAGOS_HMAC_SECRET=dev-secret
PLUSPAGOS_MERCHANT_GUID=test-merchant-001
PLUSPAGOS_SECRET_KEY=clave-secreta-campus-2026
PLUSPAGOS_API_URL=http://localhost:3000
SIM_AUTO_APPROVE=false

# Reglas de negocio (valores DEV reducidos para testing)
MONTO_ARANCEL=1500.00
VENCIMIENTO_PENDIENTE_DIAS=1     # PRD: 60
VALIDEZ_CERTIFICADO_DIAS=2       # PRD: 65

# URL base
SERVER_BASE_URL=http://localhost:8080
```

---

## 9. CONFIGURACIÓN DEL APPLICATION.PROPERTIES

Todas las properties usan `${ENV_VAR:default}`. Las más relevantes:

```properties
server.port=8080
server.servlet.context-path=/api/v1

# JPA: validate (no auto-create, Flyway maneja el schema)
spring.jpa.hibernate.ddl-auto=validate

# Flyway habilitado
spring.flyway.enabled=true

# JWT: 8 horas
rdam.jwt.expiration-ms=28800000

# Scheduler: corre a las 2 AM diario
rdam.scheduler.vencimiento-cron=0 0 2 * * *

# Upload max: 10MB
spring.servlet.multipart.max-file-size=10MB
rdam.negocio.max-upload-size-bytes=10485760

# Rate limiting: 10 req/min por IP
rdam.security.rate-limit-por-minuto=10
```

---

## 10. SERVICIOS PRINCIPALES — RESUMEN FUNCIONAL

### SolicitudService (orquestador central)
- `crearSolicitud()` — Crea solicitud PENDIENTE, genera OTP en Redis (TTL 15min), envía email
- `validarOtp()` — Valida OTP (máx 3 intentos), genera token ciudadano en Redis (TTL 24h)
- `consultarEstado()` — Devuelve vista pública con link de descarga solo si PUBLICADO
- `crearOrdenPago()` — Genera orden encriptada, guarda idOrdenPago en DB
- `procesarCallbackPago()` — Busca por nroTramite, aplica resultado (PAGADO o VENCIDO)
- `procesarWebhookPago()` — Busca por idOrdenPago, aplica resultado
- `subirCertificado()` — Valida estado PAGADO + circunscripción, sube a MinIO, genera token, transiciona a PUBLICADO
- `regenerarTokenDescarga()` — Nuevo token sin reemplazar archivo en MinIO
- `listarSolicitudes()` — OPERADOR: solo su circunscripción; ADMIN: todas (con filtros opcionales)

### TokenService (Redis)
- OTP: key `otp:{idSolicitud}`, TTL 15 min, máx 3 intentos (key `otp:intentos:{idSolicitud}`)
- Token ciudadano: key `ciudadano:token:{token}`, TTL 24h, valor = nroTramite

### PagoService
- `crearOrdenPago()` — Encripta campos con PlusPagosCrypto, retorna `ResultadoOrdenPago` (record con idOrdenPago, urlPago, modoSimulacion, formularioDatos)
- `validarFirmaHmac()` — HMAC-SHA256, bypass en SIM mode si no hay firma
- `interpretarCodigoEstado()` — Mapea códigos PlusPagos a APROBADO/RECHAZADO

### VencimientoScheduler (cron diario)
- `vencerSolicitudesSinPago()` — PENDIENTE con fecha > X días → VENCIDO, envía email
- `vencerCertificados()` — PUBLICADO con emisión > X días → PUBLICADO_VENCIDO, elimina de MinIO

### CertificadoService (MinIO)
- Sube PDF a `certificados/{idSolicitud}/{uuid}.pdf`
- Genera token de 64 caracteres (2 UUIDs concatenados)
- Descarga y eliminación de archivos

### EmailService (@Async)
- 4 templates: OTP, pago confirmado, certificado disponible, solicitud vencida
- Errores loggeados pero no bloquean el flujo principal

---

## 11. MANEJO DE ERRORES

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

## 12. FORMATO DE NÚMERO DE TRÁMITE

`RDAM-YYYYMMDD-NNNN`

Ejemplo: `RDAM-20260305-0042`

Generado por `SolicitudService.generarNroTramite()` usando fecha actual y un `AtomicInteger` como contador secuencial.

---

## 13. CONVENCIONES DE REDIS

| Key Pattern | Valor | TTL | Uso |
|-------------|-------|-----|-----|
| `otp:{idSolicitud}` | Código OTP 6 dígitos | 15 min | Validación de email |
| `otp:intentos:{idSolicitud}` | Contador de intentos | 15 min | Límite de 3 intentos |
| `ciudadano:token:{token64chars}` | nroTramite | 24 horas | Sesión ciudadano |

---

## 14. COLECCIÓN POSTMAN

Archivo: `RDAM.postman_collection.json` en la raíz del proyecto.

Organizada en 3 carpetas:
- **Portal Ciudadano** (4 requests): registro, validar OTP, login, crear solicitud
- **Pasarela de Pagos** (4 requests): crear orden, callback aprobado, callback rechazado, webhook global
- **Panel Interno** (4 requests): login admin, listar solicitudes, aprobar, rechazar

Variables de la colección:
- `base_url` = `http://localhost:8080/api/v1`
- `jwt_token` — Se setea automáticamente tras login exitoso
- `ciudadano_token` — Se setea automáticamente tras validar OTP
- `id_solicitud` — Se setea automáticamente tras crear solicitud
- `nro_tramite` — Se setea automáticamente
- `otp_code` — Se debe completar manualmente (ver MailHog en http://localhost:8025)

---

## 15. CÓMO LEVANTAR EL PROYECTO

```bash
# 1. Ir al directorio del backend
cd backend/

# 2. Levantar infraestructura con Docker Compose
docker compose up -d

# 3. Verificar que todos los servicios estén corriendo
docker compose ps

# 4. Ejecutar la aplicación Spring Boot
./mvnw spring-boot:run

# 5. Verificar que arrancó
curl http://localhost:8080/api/v1/health
```

**URLs útiles en desarrollo:**
- API Backend: http://localhost:8080/api/v1
- MailHog (ver emails): http://localhost:8025
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin123)
- PlusPagos Mock: http://localhost:3000
- PlusPagos Dashboard: http://localhost:3000/dashboard

---

## 16. ISSUES CONOCIDOS Y PENDIENTES

### Issues técnicos
1. **Mismatch de HMAC secrets:** El mock firma con `CONFIG.SECRET_KEY` (`clave-secreta-campus-2026`) pero el backend valida con `PLUSPAGOS_HMAC_SECRET` (`dev-secret`). Funciona en SIM mode porque se bypasea la validación para requests sin firma. En producción habría que unificar.

### Pendientes de implementación
1. **Frontend definitivo** — Solo existen prototipos HTML estáticos. Falta implementar con React o Vue.
2. **Frontend auto-submit form** — El frontend debe consumir `formularioDatos` de la respuesta de crear orden de pago y auto-submit un formulario HTML al mock.
3. **Tests unitarios y de integración** — Solo existe `BackendApplicationTests.java` y `TestCryptoCompatibility.java`. Falta cubrir > 80% de servicios.
4. **Rate limiting** — Configurado en properties pero no implementado como filtro.
5. **Tabla `parametros_sistema`** — Existe en la DB con datos iniciales pero no se usa desde el código Java (los valores se leen de `application.properties`).
6. **Refresh tokens** — No implementados para usuarios internos (excluido del MVP).

---

## 17. DECISIONES ARQUITECTÓNICAS IMPORTANTES

1. **Sin aprobación/rechazo manual (v1.2):** El operador no interviene antes del pago. Solo carga el certificado tras confirmación de pago.
2. **Circunscripción en JWT:** El claim permite filtrado automático sin consulta adicional a DB.
3. **Autenticación híbrida:** Token opaco Redis para ciudadanos (sin cuenta) vs JWT para internos.
4. **MinIO como S3 local:** Permite migración futura a AWS S3 sin cambios de código.
5. **Concurrencia optimista (@Version):** Evita que dos operadores trabajen el mismo caso simultáneamente.
6. **Form POST (no API REST) para pagos:** Porque el mock devuelve HTML (formulario de tarjeta), no JSON.
7. **Email async (@Async):** SMTP caído no bloquea el flujo principal.
8. **Flyway + validate:** El schema solo se crea via migraciones, JPA solo valida.

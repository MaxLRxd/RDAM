# ESPECIFICACIÓN FUNCIONAL: SISTEMA RDAM
## Cliente: Gobierno de la Provincia de Santa Fe

> **Versión:** 1.2 MVP — Febrero 2026
> **Estado:** En desarrollo
> **Autor:** Rojas Máximo
> **Cambios respecto a v1.1:** Incorporación de circunscripción como campo obligatorio de la solicitud. Eliminación del flujo de aprobación/rechazo por parte del operador interno. Simplificación del ciclo de vida de solicitudes. Actualización de roles y responsabilidades del Usuario Interno. Inclusión de popup de confirmación de arancel en el portal ciudadano.

---

### 1. Resumen Ejecutivo

El Sistema RDAM (Registro de Deudores Alimentarios Morosos) es una plataforma web gubernamental que permite a los ciudadanos consultar si una persona figura en el registro provincial de deudores mediante la solicitud de un certificado oficial. El sistema no requiere cuentas de usuario para ciudadanos, utilizando validación por email y tokens temporales de acceso. Incluye integración con la pasarela de pagos PlusPagos y la gestión manual de certificados por parte del personal interno.

Cada Usuario Interno gestiona exclusivamente las solicitudes correspondientes a su **Circunscripción Judicial** asignada. La Provincia de Santa Fe cuenta con cinco circunscripciones: I (Santa Fe), II (Rosario), III (Venado Tuerto), IV (Reconquista) y V (Rafaela).

---

### 2. Arquitectura

```
[Ciudadano Web] <---> [Frontend React/Vue] <---> [Usuario Interno Web]
                              |
                              v
                     [Backend Spring Boot]
                              |
                    +---------+---------+-------+
                    |         |         |       |
                    v         v         v       v
              [PlusPagos  [SMTP    [Base de  [MinIO
               Gateway]  Service]  Datos]  Storage]
```

---

### 3. Roles del Sistema

| Rol | Descripción | Capacidades |
|-----|-------------|-------------|
| **Ciudadano** | Usuario público sin cuenta permanente. | Solicitar certificados, validar email, acceder con token temporal, pagar arancel, descargar certificado. |
| **Usuario Interno** | Empleado con credenciales, asignado a una circunscripción. | Visualizar solicitudes de su circunscripción, cargar manualmente el certificado PDF una vez confirmado el pago. |
| **Administrador** | Gestión básica del sistema. | Alta y baja de usuarios internos con asignación de circunscripción, consulta de auditoría básica. |

> **Nota v1.2:** El Usuario Interno ya **no aprueba ni rechaza** solicitudes. Su única responsabilidad operativa es generar el certificado en el sistema judicial externo y cargarlo en la plataforma una vez que el pago ha sido confirmado.

---

### 4. Historias de Usuario

| ID | Historia | Criterios de Aceptación |
|----|----------|------------------------|
| HU01 | Solicitar certificado RDAM | Formulario con campos DNI/CUIL, email y circunscripción (menú desplegable obligatorio). Validación de formato. Generación de número de trámite único (RDAM-YYYYMMDD-NNNN). Antes de enviar, se muestra un popup informando que el arancel de emisión deberá ser abonado, con opciones "Cancelar" y "Continuar". |
| HU02 | Validar correo electrónico | Envío de código 6 dígitos, validez 15 min, máx 3 intentos. |
| HU03 | Acceder a "Mis solicitudes" | Historial de solicitudes almacenado en LocalStorage vinculado al token de sesión. Incluye visualización de circunscripción por solicitud. |
| HU04 | Pagar arancel de emisión | Redirección a PlusPagos cuando el certificado esté disponible, recibo digital por email tras confirmación de pago. |
| HU05 | Descargar certificado | Botón visible si estado es `publicado`, descarga del archivo subido por el operador, vigencia del enlace de 65 días (PRD) / 80 días (DEV). |
| HU06 | Visualizar solicitudes (Interno) | Listado de solicitudes filtrado por defecto a la circunscripción asignada al usuario. Filtros adicionales por estado. Opción de filtrar por otra circunscripción disponible para el rol Administrador. |
| HU07 | Cargar certificado (Interno) | El operador visualiza las solicitudes en estado `pagado` de su circunscripción. Sube manualmente el archivo PDF del certificado. El sistema lo almacena en MinIO, genera el token de descarga y notifica al ciudadano. |
| HU08 | Gestión de usuarios internos (Admin) | El Administrador puede crear, activar y desactivar usuarios internos. Al crear un usuario, se le asigna una circunscripción. Los JWT de usuarios desactivados son rechazados en tiempo real. |

---

### 5. Modelo de Datos

**SQL Schema (Extracto Principal):**

```sql
CREATE TABLE solicitud (
    id_solicitud        BIGINT PRIMARY KEY DEFAULT gen_random_uuid(),
    nro_tramite         VARCHAR(20) UNIQUE NOT NULL,       -- formato RDAM-YYYYMMDD-NNNN
    dni_cuil            VARCHAR(11) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    circunscripcion     TINYINT NOT NULL,                  -- 1=Santa Fe, 2=Rosario, 3=Venado Tuerto, 4=Reconquista, 5=Rafaela
    codigo_validacion   VARCHAR(6),
    token_acceso        VARCHAR(64),
    estado              VARCHAR(30) NOT NULL DEFAULT 'pendiente',
    monto_arancel       DECIMAL(10,2),
    id_orden_pago       VARCHAR(100),
    sol_fec_pago        TIMESTAMP,
    url_certificado     VARCHAR(500),
    token_descarga      VARCHAR(64),
    sol_fec_emision     TIMESTAMP,
    fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT DEFAULT 0
);

CREATE TABLE usuario_interno (
    id                  BIGINT PRIMARY KEY DEFAULT gen_random_uuid(),
    username            VARCHAR(100) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    rol                 VARCHAR(20) NOT NULL,               -- OPERADOR | ADMIN
    circunscripcion     TINYINT,                            -- NULL para Administradores (acceso total)
    activo              BOOLEAN DEFAULT TRUE,
    fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**JSON Schema (Solicitud):**

```json
{
  "id_solicitud": "BIGINT/Long",
  "nro_tramite": "RDAM-20250214-0042",
  "dni_cuil": "String(11)",
  "circunscripcion": 2,
  "estado": "publicado",
  "pago": {
    "monto": 5000.00,
    "estado": "PAGADO",
    "id_transaccion": "String",
    "fecha_confirmacion": "ISO8601"
  },
  "certificado": {
    "url_descarga": "URL (token seguro)",
    "token_descarga": "String(64)",
    "fecha_emision": "ISO8601",
    "vigencia_dias": 65
  }
}
```

---

### 6. Flujo de Estados

```
[INICIO]
   |
   v
pendiente  <-- (formulario + email validado)
   |
   |-- Webhook PlusPagos OK -------> pagado
   |                                    |
   |                                    |-- Operador carga cert. --> publicado
   |                                    |                               |
   |                                    |                               |-- Vencimiento 65d --> publicado_vencido (terminal)
   |                                    |
   |                                    |-- Pago rechazado ---------> vencido (terminal)
   |
   |-- Vencimiento 60d (sin pago) --> vencido (terminal)
```

> **Nota v1.2:** Se eliminó el estado `rechazado` generado por el operador. El operador ya no puede rechazar solicitudes manualmente. Las únicas causas de cierre negativo son el vencimiento del plazo de pago y el rechazo por parte de PlusPagos.

**Transiciones Principales:**

| Estado Actual | Evento | Estado Siguiente | Validaciones |
|--------------|--------|------------------|--------------|
| — | Formulario enviado + email validado | `pendiente` | Código correcto, < 3 intentos, < 15 min |
| `pendiente` | Webhook PlusPagos OK | `pagado` | Firma HMAC válida, idempotencia por `id_transaccion` |
| `pendiente` | Vencimiento 60 días PRD / 15 días DEV | `vencido` | — |
| `pagado` | Cargar certificado (operador) | `publicado` | Archivo subido correctamente a MinIO |
| `pagado` | Pago rechazado (webhook PlusPagos) | `vencido` | Firma HMAC válida |
| `publicado` | Vencimiento 65 días PRD / 80 días DEV | `publicado_vencido` | Archivo eliminado de MinIO |

**Mapeo de códigos de rechazo PlusPagos:**
- 4 = RECHAZADA, 7 = EXPIRADA, 8 = CANCELADA, 9 = DEVUELTA, 11 = VENCIDA → todos derivan a `vencido`

---

### 7. Integraciones

**1. PlusPagos Gateway**
- **URL Creación:** `POST /api/v1/solicitudes/{id}/pago/crear` (Backend → PlusPagos)
- **Auth:** Credenciales de comercio + Firma HMAC.
- **Operación:** Generar orden de pago y recibir confirmación vía Webhook.
- **Webhook:** `POST /api/v1/webhooks/pluspagos`. Valida firma `X-PlusPagos-Signature` (HMAC-SHA256) e idempotencia por `id_transaccion`.
- **Tiempo máximo de pago:** 60 días (PRD) / 15 días (DEV). Protección contra doble procesamiento.

**2. Almacenamiento de Certificados (MinIO/S3)**
- El operador sube manualmente el archivo del certificado desde el panel interno.
- El sistema almacena el archivo en MinIO en un bucket privado.
- Se genera un `token_descarga` criptográfico de 64 caracteres, válido por 65 días (PRD) / 80 días (DEV).
- Al vencer, el archivo se elimina del servidor. El operador puede regenerar el token si es necesario.

**Nota importante:** No existe integración con API Judicial. El personal interno consulta el registro en el sistema externo judicial correspondiente de manera independiente, fuera de esta plataforma.

---

### 8. Requisitos No Funcionales

- **Performance:** Tiempo de respuesta de API < 300ms en condiciones normales. Soporte estimado de hasta 500 usuarios concurrentes en la versión inicial.
- **Seguridad:** HTTPS/TLS 1.3 obligatorio. Tokens temporales de sesión para ciudadanos. JWT de 8hs para usuarios internos sin refresh. Rate limiting básico en endpoints públicos (máx. 10 req/min por IP). Sanitización de inputs contra XSS e inyección SQL vía JPA.
- **Disponibilidad:** SLA 99% en horario hábil. Backup diario de base de datos.
- **Circunscripciones:** El filtro de circunscripción en el panel interno debe restringir automáticamente los resultados al valor asignado al usuario autenticado. Solo el rol Administrador puede visualizar y filtrar por circunscripciones arbitrarias.

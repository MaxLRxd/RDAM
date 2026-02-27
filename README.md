# Sistema RDAM — Paquete de Producción (v1.2 MVP)

> **Versión:** 1.2 — Febrero 2026
> **Estado:** En desarrollo
> **Cambios respecto a v1.1:** Incorporación de circunscripciones judiciales como eje organizativo del sistema. Eliminación del flujo de aprobación/rechazo manual por parte del operador. Simplificación del ciclo de vida de solicitudes. Popup de confirmación de arancel en el portal ciudadano.

---

## Contenido

| Archivo | Descripción |
|---------|-------------|
| **SPEC.md** | Especificación funcional completa del sistema (v1.2) |
| **IMPLEMENTATION.md** | Guía técnica de implementación con plan de sprints (v1.2) |
| **README.md** | Este archivo — Visión general del proyecto |

---

## ¿Qué es el Sistema RDAM?

El **Sistema RDAM** (Registro de Deudores Alimentarios Morosos) es una plataforma web gubernamental que permite a los ciudadanos de la Provincia de Santa Fe consultar si una persona figura en el registro provincial de deudores alimentarios mediante la solicitud de un **certificado digital oficial**.

La Provincia de Santa Fe se organiza en **cinco circunscripciones judiciales**. Cada solicitud de certificado es atendida por el personal de la circunscripción correspondiente al domicilio del solicitante.

| Circunscripción | Ciudad cabecera |
|-----------------|----------------|
| Circ. I | Santa Fe |
| Circ. II | Rosario |
| Circ. III | Venado Tuerto |
| Circ. IV | Reconquista |
| Circ. V | Rafaela |

Para identificar a qué circunscripción pertenece, el ciudadano puede consultar la [Guía Judicial del Poder Judicial de Santa Fe](https://www.justiciasantafe.gov.ar/index.php/poder-judicial/guia-judicial/).

---

## Propósito del Sistema

**IMPORTANTE — Aclaraciones:**

1. **El certificado es INFORMATIVO**, no liberatorio. Acredita si el DNI/CUIL figura o no en el registro RDAM. NO es un documento que extinga obligaciones alimentarias.

2. **El pago es por EMISIÓN del certificado**, no por deuda. El arancel es el costo administrativo de emitir el documento. Incluso si la persona NO figura en el registro, debe pagar el arancel. Antes de enviar la solicitud, el portal muestra un popup informando esta condición.

3. **Cualquier persona puede solicitar certificados de terceros**. Solo se valida que el email sea accesible. La responsabilidad del uso del certificado recae en el solicitante.

---

## Alcance del Proyecto

**Flujo completo ciudadano:**
- Solicitud de certificado ingresando DNI/CUIL, email y circunscripción judicial
- Confirmación de arancel mediante popup antes del envío
- Validación de email mediante código de 6 dígitos
- Acceso a "Mis solicitudes" vía LocalStorage
- Pago del arancel de emisión mediante PlusPagos
- Descarga del certificado subido por el operador
- Notificaciones por email en cada etapa

**Flujo completo usuario interno:**
- Visualización de solicitudes filtradas por circunscripción asignada
- Carga manual del certificado PDF una vez confirmado el pago
- Gestión básica de usuarios internos con asignación de circunscripción (Administrador)

**Características técnicas:**
- Sin autenticación tradicional para ciudadanos (tokens temporales)
- LocalStorage para historial de solicitudes del ciudadano
- Almacenamiento de certificados en MinIO con tokens de descarga seguros
- El sistema no genera certificados: el operador los sube manualmente
- El sistema no aprueba ni rechaza solicitudes: el único evento interno es la carga del certificado

### Exclusiones (Futuro)

**NO incluido en la versión inicial:**
- Automatización de aprobaciones
- Pasarelas de pago alternativas
- Refresh tokens para sesiones internas
- Integración con API Judicial (el operador usa sistemas externos)

---

## Arquitectura Técnica

### Stack Tecnológico

**Backend:**
- Spring Boot 3.x / Java 17+
- MySQL 8+
- Spring Security + JWT (con claim de circunscripción)
- Redis (tokens efímeros)
- SMTP (envío de emails)

**Frontend:**
- React 18+ / Vue 3+
- React Router / Vue Router

**Infraestructura:**
- Docker + Docker Compose
- MinIO (almacenamiento de certificados)
- CI/CD con GitHub Actions

**Integraciones:**
- PlusPagos (pasarela de pago)

### Características de Seguridad

- 🔒 JWT de 8hs para usuarios internos (incluye claim de circunscripción)
- 🎫 Tokens temporales para ciudadanos (almacenados en Redis)
- 🗂 Filtrado automático por circunscripción en todos los endpoints del panel interno
- 🛡️ Sanitización de inputs (XSS, SQL Injection vía JPA)
- 🔐 Validación HMAC-SHA256 en webhooks de pago
- 🔑 Tokens de descarga criptográficos de 64 caracteres

---

## Estados del Sistema

### Ciclo de Vida de una Solicitud

```
[INICIO]
   ↓
pendiente  ← formulario enviado + email validado
   ↓
   ├──→ pagado         ← webhook PlusPagos OK
   │       ↓
   │    publicado      ← operador sube certificado manualmente
   │       ↓
   │    publicado_vencido (terminal) ← vencimiento 65 días PRD
   │
   └──→ vencido (terminal) ← vencimiento 60 días sin pago
                           ← pago rechazado por PlusPagos
```

> **v1.2:** Se eliminó el estado `rechazado` por acción manual del operador. El operador ya no interviene en la decisión de si se emite o no el certificado: su único rol es cargarlo una vez recibido el pago.

---

## Documentación

### Para Desarrolladores

- **IMPLEMENTATION.md**: Guía completa de implementación
  - Plan de 6 sprints para entrega en 30 días
  - Decisiones técnicas fundamentadas (incluyendo circunscripción en JWT)
  - Estrategia de testing con casos de circunscripción cruzada
  - Deployment con Docker Compose
  - Checklists de entrega por área

### Para Product Owners / Analistas

- **SPEC.md**: Especificación funcional
  - Roles del sistema
  - Historias de usuario con criterios de aceptación
  - Modelo de datos SQL + JSON (incluye campo `circunscripcion`)
  - Flujo de estados simplificado
  - Integraciones externas
  - Requisitos no funcionales

---

## Testing

- **Tests unitarios:** > 80% cobertura en capa de servicios
- **Tests de integración:** Endpoints y repositorios críticos con Testcontainers
- **Tests de seguridad:** Validación de roles, restricción por circunscripción, expiración de tokens, CORS
- **Tests E2E:** Flujo completo Solicitud (selector circunscripción + popup) → Validación → Pago → Carga cert. → Descarga

---

## Soporte y Contacto

### Para Ciudadanos

- **Web:** https://rdam.santafe.gob.ar
- **Email:** soporte@rdam.santafe.gob.ar
- **Teléfono:** 0800-XXX-XXXX
- **Horario:** Lunes a Viernes 8:00–20:00
- **Guía Judicial:** https://www.justiciasantafe.gov.ar/index.php/poder-judicial/guia-judicial/

### Para Equipo Técnico

- **Documentación:** Confluence interno
- **Issues:** Jira / GitHub Issues
- **Slack:** #rdam-dev

---

## Licencia

Uso interno — Gobierno de la Provincia de Santa Fe.

---

**Última actualización:** Febrero 2026
**Versión del documento:** 1.2
**Estado:** En desarrollo

# Sistema RDAM — Entrega Final
**Autor:** Rojas Máximo
**Tecnologías:** Spring Boot · React · Docker · MySQL · Redis · MinIO

---

## ¿Qué es el Sistema RDAM?

El **Sistema RDAM** (Registro de Deudores Alimentarios Morosos) es una plataforma web gubernamental de la Provincia de Santa Fe que permite a ciudadanos solicitar un **certificado digital oficial** que informa si una persona figura en el registro provincial de deudores alimentarios.

La Provincia de Santa Fe se organiza en **cinco circunscripciones judiciales**. Cada solicitud es atendida por el personal de la circunscripción correspondiente al domicilio del solicitante.

| Circunscripción | Ciudad cabecera |
|-----------------|----------------|
| Circ. I | Santa Fe |
| Circ. II | Rosario |
| Circ. III | Venado Tuerto |
| Circ. IV | Reconquista |
| Circ. V | Rafaela |

Para identificar a qué circunscripción pertenece, el ciudadano puede consultar la [Guía Judicial del Poder Judicial de Santa Fe](https://www.justiciasantafe.gov.ar/index.php/poder-judicial/guia-judicial/).

---

## Despliegue con Docker

> El `docker-compose.yml` está configurado de forma autónoma usando `dockerfile_inline`. **No es necesario clonar el repositorio manualmente.** Al ejecutar el comando, Docker descarga el código directamente desde GitHub y construye las imágenes del backend y el frontend de forma automática.
>
> **En caso de error:** [https://github.com/MaxLRxd/RDAM/tree/front-develop](https://github.com/MaxLRxd/RDAM/tree/front-develop)

**Requisitos previos:** Docker Desktop instalado y en ejecución.

**Pasos:**

1. Colocá los archivos `.env` y `docker-compose.yml` en una carpeta vacía.
2. Abrí una terminal en esa carpeta.
3. Ejecutá:

```bash
docker compose up --build
```

El primer arranque tarda varios minutos porque descarga las dependencias de Maven y npm. Los siguientes arranques son mucho más rápidos gracias a la caché de Docker.

---

## Servicios y URLs de acceso

Una vez que todos los contenedores estén iniciados:

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Portal Web** | [http://localhost](http://localhost) | Portal ciudadano (`#/ciudadano`) e interno (`#/interno`) |
| **API Backend** | [http://localhost/api/v1](http://localhost/api/v1) | Endpoint base de la API REST |
| **MailHog** | [http://localhost:8025](http://localhost:8025) | Captura de correos de prueba (OTPs, notificaciones) |
| **MinIO Console** | [http://localhost:9001](http://localhost:9001) | Gestión de archivos S3 — Usuario: `minioadmin` / Contraseña: `minioadmin123` |
| **PlusPagos Mock** | [http://localhost:3000](http://localhost:3000) | Simulador de pasarela de pagos |
| **PlusPagos Dashboard** | [http://localhost:3000/dashboard](http://localhost:3000/dashboard) | Historial de transacciones del simulador |

> **MailHog:** Todos los correos salientes del sistema (códigos OTP, confirmaciones de pago, avisos de disponibilidad del certificado) son capturados aquí. No se envían correos reales.
>
> **MinIO:** El bucket `rdam-certificados` se crea automáticamente al levantar los contenedores. Si no aparece, crearlo manualmente con ese nombre exacto.

---

## Credenciales del panel interno

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Administrador | `admin@rdam.santafe.gob.ar` | `Admin1234!` |

Los usuarios operadores deben ser creados desde el panel de administración.

---

## Tarjetas de prueba — Pasarela PlusPagos

| Número de tarjeta | Resultado |
|-------------------|-----------|
| `4242 4242 4242 4242` | ✅ Aprobada |
| `4000 0000 0000 0002` | ❌ Rechazada |
| `5555 5555 5555 4444` | ✅ Aprobada |
| `5105 1051 0510 5100` | ❌ Rechazada |

El formulario de pago acepta números con espacios o guiones. La fecha de vencimiento se puede ingresar sin `/` (se inserta automáticamente).

---

## Documentación incluida

| Archivo / Documento | Descripción |
|---------------------|-------------|
| `SPEC.md` | Especificación funcional completa del sistema |
| `IMPLEMENTATION.md` | Guía técnica de implementación con plan de sprints |
| `CONTEXT-FOR-AI.md` | Contexto técnico completo para continuación del desarrollo |
| `Manual de Usuario` | Guía paso a paso para ciudadanos, operadores y administradores |
| `Manual Técnico` | Arquitectura, variables de entorno, API y guía de despliegue |
| `Rojas_Maximo-Plan_de_Pruebas-RDAM` | Casos de prueba, metodología y resultados |
| `Sistema-RDAM-collection.json` | Colección Postman con todos los endpoints |

---

## Pruebas con Postman

Importá en Postman los archivos:
- `Sistema-RDAM-collection.json` — colección de requests
- `RDAM-Environment` — variables de entorno (`{{base_url}}`: `http://localhost/api/v1`)

**La colección automatiza el flujo:** tokens, IDs y número de trámite se guardan automáticamente entre requests.

**Pasos manuales requeridos:**
1. Configurar `base_url` en el entorno de Postman: `http://localhost/api/v1`
2. Consultar el OTP en MailHog (`http://localhost:8025`) e ingresarlo en el body de la request de validación
3. Subir un archivo PDF al momento de cargar el certificado

---

## Propósito y aclaraciones importantes

1. **El certificado es INFORMATIVO**, no liberatorio. Acredita si el DNI/CUIL figura o no en el RDAM. No extingue obligaciones alimentarias.

2. **El pago es por EMISIÓN del certificado**, no por deuda. El arancel se cobra incluso si la persona NO figura en el registro. El portal muestra un popup informando esta condición antes de continuar.

3. **Cualquier persona puede solicitar certificados de terceros.** Solo se valida que el email ingresado sea accesible.

4. **El sistema no genera certificados.** El operador interno los genera en un sistema judicial externo y los sube manualmente a la plataforma.

---

## Ciclo de vida de una solicitud

```
[INICIO]
   ↓
PENDIENTE  ← formulario enviado + email validado con OTP
   ↓
   ├──→ PAGADO         ← pago aprobado por PlusPagos
   │       ↓
   │    PUBLICADO      ← operador sube certificado PDF
   │       ↓
   │    PUBLICADO_VENCIDO (terminal) ← vence a los 2 días (DEV) / 65 días (PRD)
   │
   └──→ VENCIDO (terminal) ← sin pago por 1 día (DEV) / 60 días (PRD)
                            ← pago rechazado por PlusPagos
```

---

## Arquitectura técnica

```
[Navegador]
    │
    ▼
[nginx : 80]  ──── /api/* ────►  [Spring Boot : 8080]
    │                                    │
    ▼                         ┌──────────┼──────────┐
[React SPA]               [MySQL]    [Redis]    [MinIO]
                              │
                          [MailHog]  [PlusPagos Mock : 3000]
```

**Stack:**
- **Frontend:** React 19 + Vite, CSS Modules, hash-based routing, servido por nginx
- **Backend:** Spring Boot 3.4.5, Java 21, Spring Security + JWT, Flyway
- **Base de datos:** MySQL 8, Redis 7
- **Almacenamiento:** MinIO (S3-compatible)
- **Pasarela de pagos:** PlusPagos (mock Node.js en desarrollo)
- **Infraestructura:** Docker Compose (7 servicios)

---

*Última actualización: Marzo 2026 — Versión 2.0*
*Estado: Entrega final — Backend + Frontend completos*

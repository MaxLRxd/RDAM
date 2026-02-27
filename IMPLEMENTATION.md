# GUÍA DE IMPLEMENTACIÓN: SISTEMA RDAM

> **Versión:** 1.2 MVP — Febrero 2026
> **Estado:** En desarrollo
> **Cambios respecto a v1.1:** Campo `circunscripcion` incorporado en modelo de datos y formulario ciudadano. Eliminación de la lógica de aprobación/rechazo manual por el operador. Simplificación del flujo de estados (se elimina estado `rechazado` por acción interna). Actualización de endpoints y seguridad relacionados con el filtrado por circunscripción. Popup de confirmación de arancel en el frontend ciudadano.

---

## 1. Visión General

Sistema web gubernamental basado en **Spring Boot 3.x (Java 17+)** para el backend y **React 18+ / Vue 3+** para el frontend. La persistencia se maneja con **MySQL 8+**, el almacenamiento de certificados en **MinIO** (compatible con S3), y **Redis** para caché de tokens efímeros de ciudadanos.

El núcleo del negocio es la gestión de solicitudes de certificados del Registro de Deudores Alimentarios Morosos. Cada solicitud está asociada a una **circunscripción judicial**, y cada Usuario Interno gestiona exclusivamente las solicitudes de su circunscripción asignada. La consulta al registro judicial es responsabilidad del operador interno mediante sistemas externos ajenos a esta plataforma. El sistema no genera certificados PDF: el operador los sube manualmente una vez confirmado el pago.

El flujo simplificado de la v1.2 elimina la etapa de aprobación/rechazo manual: una solicitud con email validado queda en estado `pendiente` hasta que el ciudadano abona el arancel, momento en que pasa a `pagado` y el operador puede cargar el certificado.

---

## 2. Plan de Sprints

### Sprint 0: Preparación (Días 1–3)
- Setup de repositorio Git, CI básico con GitHub Actions y configuración de linters.
- Levantamiento de infraestructura local con Docker Compose (MySQL, Redis, MinIO).
- Definición de variables de entorno y estructura base del proyecto.

### Sprint 1: Modelo de Datos y Seguridad Base (Días 4–7)
- Diseño e implementación del esquema de Base de Datos: tablas `solicitud` (con campo `circunscripcion`) y `usuario_interno` (con campo `circunscripcion`).
- Migraciones con Flyway.
- Configuración de Spring Security: JWT para usuarios internos con claim de circunscripción, filtro de token temporal para ciudadanos.

### Sprint 2: Servicios Core y API Ciudadano (Días 8–12)
- Implementación de `SolicitudService` con lógica de estados y control optimista (`@Version`). Sin lógica de aprobación/rechazo.
- Lógica de "Magic Codes": generación, envío por SMTP (Spring Mail) y validación con expiración en Redis.
- Endpoints públicos: `POST /api/v1/solicitudes` (HU01, incluye campo `circunscripcion`) y `POST /api/v1/solicitudes/{id}/validar` (HU02).

### Sprint 3: Pagos y Panel Interno (Días 13–18)
- Integración con PlusPagos: generación de orden de pago firmada y endpoint Webhook con validación HMAC-SHA256. El webhook mueve la solicitud directamente de `pendiente` a `pagado`.
- Lógica de vencimientos: job programado para marcar solicitudes `pendiente` sin pago vencidas (60d PRD / 15d DEV) y certificados `publicado` vencidos (65d PRD / 80d DEV), eliminando archivos de MinIO en este último caso.
- Endpoints protegidos para listado con filtro automático por circunscripción del usuario autenticado (HU06).

### Sprint 4: Carga Manual de Certificados (Días 19–22)
- Endpoint de carga de archivo: `POST /api/v1/solicitudes/{id}/certificado` — recibe el archivo, valida que la solicitud esté en estado `pagado` y corresponda a la circunscripción del operador, lo sube a MinIO y genera el `token_descarga`.
- Generación de URL de descarga con token criptográfico de 64 caracteres (válido 65 días PRD / 80 días DEV).
- Notificación al ciudadano por email con el enlace de descarga.
- Lógica de regeneración de token por parte del operador.

### Sprint 5: Frontend Ciudadano (Días 23–26)
- Desarrollo de vistas públicas: Landing, Formulario de Solicitud (HU01) con selector de circunscripción y link a la Guía Judicial, Popup de confirmación de arancel previo al envío, Ingreso de Código (HU02), Visualización de Estado (HU03), Redirección a Pago (HU04) y Descarga de Certificado (HU05).
- Gestión del token de sesión en LocalStorage.

### Sprint 6: Frontend Interno y Ajustes Finales (Días 27–30)
- Desarrollo de SPA Interna: Login con JWT, Dashboard con listado filtrado por circunscripción, selector de circunscripción para Administradores, y formulario de carga de certificado.
- Tests de integración sobre flujos críticos, correcciones de bugs y smoke test completo del sistema.

---

## 3. Decisiones Técnicas

- **Autenticación Híbrida:** Tokens de sesión almacenados en Redis para ciudadanos versus JWT de 8hs sin refresh para empleados. Simplifica la implementación manteniendo la seguridad adecuada para cada tipo de usuario.
- **Circunscripción en JWT:** El JWT del usuario interno incluye un claim `circunscripcion`. El backend usa este valor para filtrar automáticamente los resultados de la API en cada endpoint protegido. Los Administradores reciben un claim especial que permite acceso a todas las circunscripciones.
- **Sin aprobación/rechazo manual:** El operador ya no interviene antes del pago. El único rol del operador tras la confirmación del pago es cargar el certificado. Esto simplifica el flujo, elimina la necesidad de un estado intermedio y reduce la superficie de error humano.
- **Sin integración judicial:** El operador consulta el sistema externo judicial por su cuenta. Esta plataforma no realiza ninguna llamada a APIs judiciales.
- **Sin generación de PDF:** El sistema no genera certificados. El operador los sube manualmente como archivo una vez que el pago fue confirmado.
- **Versionado de API:** Prefijo `/api/v1` en todos los endpoints para garantizar compatibilidad futura.
- **Concurrencia:** Control optimista mediante `@Version` de JPA en la entidad `Solicitud` para evitar que dos operadores trabajen el mismo caso simultáneamente.
- **Almacenamiento:** MinIO como implementación local con interfaz S3 genérica, lo que permite migrar a AWS S3 en el futuro sin cambios de código.
- **Vencimientos:** Job programado (`@Scheduled`) que corre diariamente para detectar solicitudes `pendiente` sin pago vencidas (60d PRD / 15d DEV) y certificados `publicado` vencidos (65d PRD / 80d DEV), eliminando archivos de MinIO en este último caso.

---

## 4. Estrategia de Testing

- **Unitarios:** JUnit 5 + Mockito para la capa de servicios (`SolicitudService`, lógica de estados). Cobertura objetivo > 80%.
- **Integración:** `@SpringBootTest` con Testcontainers (MySQL y Redis reales) para repositorios y controladores críticos.
- **Seguridad:** Tests específicos para validar roles (`@WithMockUser`), restricción por circunscripción, expiración de tokens y protección CSRF/CORS.
- **E2E:** Cypress o Playwright simulando el flujo completo: Solicitud (con selector de circunscripción + popup) → Validación Email → Pago (webhook) → Carga de certificado → Descarga por el ciudadano.

---

## 5. Infraestructura y Despliegue

- **Ambiente:** Docker Compose local con MySQL, Redis y MinIO. Imágenes Docker con base Alpine para el backend y Nginx para servir el frontend estático.
- **CI/CD:** GitHub Actions ejecuta build, tests unitarios y análisis de código en cada push a `main`.
- **Configuración:** Gestión estricta mediante Variables de Entorno. Ninguna credencial hardcodeada en código ni en Dockerfiles.
- **Variables críticas:** `DB_URL`, `DB_PASSWORD`, `JWT_SECRET`, `REDIS_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `PLUSPAGOS_API_KEY`, `PLUSPAGOS_HMAC_SECRET`, `SMTP_HOST`, `SMTP_PASSWORD`.

---

## 6. Checklists de Validación y Entrega

### 6.1. Calidad de Código y Backend (Definition of Done)
*Responsable: Tech Lead / Backend Devs*
- [ ] **Cobertura de Tests:** > 80% en capa de servicios y > 90% en lógica de estados.
- [ ] **Manejo de Concurrencia:** Verificar que `@Version` (JPA) retorne HTTP 409 ante ediciones simultáneas sobre la misma solicitud.
- [ ] **Gestión de Errores:** Todos los endpoints retornan `ProblemDetails` (RFC 7807) estandarizados, sin stacktraces expuestos.
- [ ] **Logs Estructurados:** Los logs no exponen datos sensibles (DNI, email) pero sí trazan el `nro_tramite` y la `circunscripcion` en cada operación.
- [ ] **Migraciones de BD:** Scripts Flyway probados sobre base limpia sin errores. Incluye la nueva columna `circunscripcion` en `solicitud` y `usuario_interno`.

### 6.2. Seguridad y Compliance
*Responsable: Tech Lead / Dev Backend*
- [ ] **Validación de Tokens:**
    - [ ] Token de email (ciudadano): expira en 15 min, máx 3 intentos, eliminado de Redis tras uso exitoso.
    - [ ] Token de acceso (ciudadano): almacenado en Redis con TTL adecuado.
    - [ ] JWT Interno: expira en 8hs, contiene claim `circunscripcion`, rechazado si el usuario fue desactivado.
- [ ] **Filtrado por circunscripción:** Los endpoints del panel interno deben rechazar con HTTP 403 cualquier intento de un Operador de acceder o modificar solicitudes de una circunscripción diferente a la suya.
- [ ] **Protección de API:** Rate limiting básico configurado en endpoints públicos (máx. 10 req/min por IP).
- [ ] **OWASP básico:** Uso estricto de JPA/Hibernate (sin SQL crudo), sanitización de inputs, CORS restrictivo al origen del frontend.
- [ ] **Webhook PlusPagos:** Validación de firma HMAC-SHA256 e idempotencia por `id_transaccion` antes de cambiar cualquier estado.

### 6.3. Integraciones Externas
*Responsable: Backend Devs / QA*
- [ ] **Pasarela de Pagos:**
    - [ ] Flujo de pago exitoso: `pendiente` → `pagado` (Happy Path).
    - [ ] Flujo de pago fallido: webhook con estado rechazado (códigos 4/7/8/9/11) → `vencido`.
    - [ ] Protección contra doble procesamiento del webhook.
- [ ] **Carga de Certificados:**
    - [ ] El archivo se sube correctamente a MinIO y la URL de descarga es accesible con token válido.
    - [ ] El sistema rechaza la carga si la solicitud no está en estado `pagado`.
    - [ ] El sistema rechaza la carga si la circunscripción de la solicitud no coincide con la del operador (salvo Admin).
    - [ ] El token de descarga vence correctamente y el archivo se elimina de MinIO.
    - [ ] El operador puede regenerar el token de descarga.
- [ ] **SMTP caído:** Error logueado, no bloquea el flujo principal, se reintenta en background.

### 6.4. Frontend y Experiencia de Usuario (UX)
*Responsable: Frontend Devs*
- [ ] **Responsive Design:** Probado en móviles (Android/iOS) para el flujo ciudadano.
- [ ] **Accesibilidad básica:** Contraste de colores aceptable y etiquetas en formularios.
- [ ] **Selector de circunscripción:** Menú desplegable con las 5 circunscripciones. Incluye link a la Guía Judicial (`justiciasantafe.gov.ar/guia-judicial`) para que el ciudadano identifique a cuál pertenece.
- [ ] **Popup de arancel:** Antes de enviar el formulario de solicitud, se muestra un popup informando que el arancel de emisión deberá abonarse. Los botones "Cancelar" y "Continuar" deben funcionar correctamente.
- [ ] **Feedback al Usuario:**
    - [ ] Indicadores de carga visibles durante llamadas a API.
    - [ ] Mensajes de error amigables y sin información técnica expuesta.
- [ ] **Flujo Sin Login:** Al recargar la página, el ciudadano conserva el contexto si su token en LocalStorage sigue vigente.

### 6.5. Infraestructura y Despliegue (DevOps)
*Responsable: Dev / Tech Lead*
- [ ] **Variables de Entorno:** Ninguna credencial hardcodeada en código, Dockerfiles ni repositorio.
- [ ] **Almacenamiento (MinIO):** Bucket configurado como privado, acceso solo a través de URLs pre-firmadas con expiración.
- [ ] **Cache (Redis):** Política de desalojo configurada (`allkeys-lru`) para evitar saturación de memoria.
- [ ] **HTTPS:** Certificado SSL activo en el ambiente de entrega.
- [ ] **Rollback:** Procedimiento documentado para restaurar la versión anterior mediante `docker compose down && git checkout <tag> && docker compose up`.

### 6.6. Validación Final
*Responsable: Product Owner / Stakeholders*
- [ ] **Smoke Test:** Un usuario interno de prueba realiza el flujo completo: Solicitud (con circunscripción) → Popup confirmación → Validación Email → Pago simulado (webhook) → Carga manual de certificado → Descarga por el ciudadano.
- [ ] **Test de circunscripción:** Verificar que un Operador de la Circ. II (Rosario) no puede cargar certificados de solicitudes de la Circ. I (Santa Fe).
- [ ] **Manual de Operador:** Guía básica de uso entregada al equipo interno (estados, filtros por circunscripción, carga de certificado).
- [ ] **Mesa de Ayuda:** El equipo de soporte sabe cómo buscar un trámite por DNI, número de trámite o circunscripción ante un reclamo ciudadano.

# Fase 2 — Multiempresa: empresas, miembros y roles, migración de OryKai, alta y onboarding

> **Estado (23-oct-2026): implementada en `feat/multiempresa`, sin desplegar.**
> Commits: `73a43b1` aislamiento por empresa (V17, JWT) · `23fb369` alta, invitaciones, `/org` y
> permisos · `b3870c5` app (alta, onboarding, Ajustes) · `a778297` flecos (V18 facturas locales
> solo para OryKai, tickets filtrados en SQL, roles y bajas al momento, barra inferior con «Más»).
> Despliegue: `docs/V17_V18_MULTIEMPRESA_DEPLOYMENT.md`.
>
> Diferencias con el plan: las sesiones de trabajo (temporizador) siguen siendo por persona;
> el borrador del onboarding vive en el servidor (cada paso se guarda) en lugar de en local;
> los tokens se validan contra el estado del usuario con una caché de 30 s.
>
> Pendiente para fases siguientes: verificación de email y recuperación de contraseña (necesitan
> proveedor de email), numeración de tickets por empresa (sigue global `RDS-000001`), selector si
> una persona pertenece a varias empresas, la app móvil (iOS/Android) no tiene alta ni Ajustes,
> y la compilación de iOS no se ha comprobado (solo se compila en macOS).

## Contexto
El plan SaaS aprobado (`docs/product/SAAS_TODO_EN_UNO_PLAN.md` §5, §10) pide que cada empresa tenga su propio espacio. Hoy no existe esa entidad:
- Todo cuelga de `owner_admin_id` (el usuario admin).
- Solo hay dos roles: `CLIENT` y `ADMIN`.
- No hay alta de cuentas, invitaciones ni email.
- La RLS de V7 solo bloquea el acceso externo: no tiene políticas.
- El filtro de propiedad se repite a mano en cada consulta SQL (`? IS NULL OR owner_admin_id = ?`), y **si llega `null` devuelve los datos de todos**.

Objetivo de la fase: que una empresa nueva pueda darse de alta, completar el onboarding, invitar a su equipo con roles, y que sus datos queden aislados de los de las demás empresas. OryKai pasa a ser una empresa más, sin perder datos.

Fuera de esta fase:
- Planes, módulos y cobro (fase 3).
- Facturación Verifactu (fase 4).
- Mover los programas `BUSINESS_*` del portal al espacio. Siguen funcionando por `client_id`, que ya queda aislado dentro de su empresa.

## Hallazgos que definen el diseño
- **Tablas raíz con `owner_admin_id`:** `clients`, `task_labels`, `tasks` y `work_sessions`. El resto cuelga de ellas por `client_id`, `task_id` o `ticket_id`. Basta con añadir la empresa a esas 4 tablas.
- **Emails únicos en todo el sistema:** `clients.email` y `users.email`. Hay que cambiar el de `clients` para que sea único por empresa.
- **Sin aislar todavía:**
  - Las vistas `admin_dashboard_summary` y `ticket_queue`, que cuentan globalmente.
  - Las rutas de tickets, que cargan todos los tickets y filtran en memoria.
  - `ownerAdminIdFor()` en `server/.../utils/RequestAuthContext.kt:63`, que recorre todos los clientes.
- **JWT** (`security/SupportDeskTokenService.kt`): lleva `sub`, `role`, `clientId` y otros datos del usuario. No hay plugin de Authentication: cada archivo de rutas tiene su propio guard.
- **Nombre:** "workspace" ya está usado (`BusinessProjectsWorkspace`, `/client/apps/projects/workspace`). **En código se llamará `Organization`; en la interfaz, "Empresa".**
- **Cliente:**
  - `AuthMapper` convierte cualquier rol desconocido en `CLIENT`.
  - La elección entre admin y portal está en `AdminWorkspaceApp.kt:338`.
  - No existe un componente de pasos ni un campo de texto propio del design system.
  - `AddEditClientDialog` y `ClientBillingValidation` son reutilizables.

## Decisiones (aplicadas por recomendación)
1. **Un usuario pertenece a una sola empresa** en esta fase. La tabla de miembros ya permite varias, pero con `UNIQUE(user_id)`; el selector de empresa llegará más adelante.
2. **`users.role` no cambia de significado:**
   - `ADMIN` = cualquier persona del equipo de una empresa.
   - `CLIENT` = usuario del portal.
   - El rol dentro de la empresa vive en la tabla de miembros: `OWNER`, `ADMIN`, `MEMBER` y `ACCOUNTANT` (gestoría).
   - Así siguen funcionando todos los guards actuales y el trigger `enforce_admin_owner()`.
3. **`owner_admin_id` pasa a significar "creado por"** y se sigue rellenando con el usuario que actúa. **El filtro pasa a ser `organization_id`, siempre obligatorio.** Así desaparece el riesgo del `null` que devuelve todo.
4. **Migración de OryKai:**
   - Se crea una empresa por cada admin que exista, como dueño (OWNER), igual que el aislamiento entre admins que ya hay hoy.
   - La empresa del admin más antiguo se llama "OryKai"; las demás, "Empresa de {nombre}".
5. **Alta pública detrás de un interruptor:**
   - `SUPPORTDESK_SIGNUP_ENABLED`: desactivado por defecto en producción y activado en desarrollo y tests.
   - Motivo: aún no hay proveedor de email (no se pueden verificar correos) y faltan los textos legales (`AJUSTES_LEGAL…` §10).
   - Se guarda `email_verified_at` (vacío) para activarlo después.
6. **Invitaciones sin email:**
   - La dueña o una admin genera un código de un solo uso, con caducidad de 7 días y un rol asignado, y lo comparte a mano (igual que hoy los códigos de acceso de clientes).
   - La persona invitada lo usa en "Unirme con un código".
7. **Permisos de la fase 2:**

   | Rol | Puede |
   |---|---|
   | ACCOUNTANT | Solo lectura en todo |
   | MEMBER | Todo lo operativo, salvo Ajustes de empresa y Usuarios |
   | ADMIN | Todo, salvo transferir o borrar la empresa |
   | OWNER | Todo |

   Se comprueba en servidor con un único guard.
8. La numeración de tickets (`RDS-000001`) sigue siendo global en esta fase. Se apunta para más adelante.

## Cambios

### A. Base de datos — `V17__organizations.sql`
- **Tablas nuevas:**
  - `organizations`:
    - `id`, `name`, `legal_form` (`AUTONOMO`/`SOCIEDAD`), `sector`, `team_size`, `has_accountant`, `onboarding_goals text[]`, `onboarding_completed_at`
    - datos fiscales opcionales: `legal_name`, `tax_id`, `address_line`, `postal_code`, `city`, `province`, `country`
    - `created_at`, `updated_at`
  - `organization_members`: `organization_id`, `user_id` (UNIQUE), `role` CHECK (OWNER/ADMIN/MEMBER/ACCOUNTANT), `created_at`. Tiene que haber al menos un OWNER (lo comprueba el servicio).
  - `organization_invitations`: `id`, `organization_id`, `code_hash` (SHA-256, como `refresh_tokens`), `role`, `created_by`, `expires_at`, `accepted_by`, `accepted_at`, `revoked_at`.
  - `legal_acceptances`: `user_id`, `document` (TERMS/PRIVACY), `version`, `accepted_at`. Lo pide `AJUSTES_LEGAL…`: aceptación versionada y separada.
  - `users.email_verified_at timestamptz NULL`.
- **Columna nueva:** `organization_id uuid` en `clients`, `task_labels`, `tasks` y `work_sessions`.
  - Se rellena a partir de `owner_admin_id`, con una empresa por admin (decisión 4).
  - Después pasa a `NOT NULL`, con FK e índice.
- **Unicidad:**
  - `clients`: se quita `UNIQUE(email)` y se pone `UNIQUE(organization_id, email)`.
  - `task_labels`: `UNIQUE(organization_id, name)`.
  - Se mantiene el índice de "una sesión abierta por usuario".
- **Vistas:** `admin_dashboard_summary` y `ticket_queue` se recrean con `organization_id`.
- **Seguridad:** RLS activada y REVOKE en las tablas nuevas, como en V7–V15.
- **Tests:** `PostgresMigrationIntegrationTest` pasa a esperar 17 migraciones y comprueba el relleno.

### B. Servidor — aislamiento por empresa
- **Modelos** (`server/.../domain/model/ServerModels.kt`):
  - `ServerAuthIdentity` añade `organizationId` y `orgRole`.
  - Nuevos: `Organization`, `OrganizationMember`, `OrganizationInvitation`.
- **JWT:** nuevos claims `orgId` y `orgRole`.
  - Se resuelven al iniciar sesión y al refrescar. Los usuarios del portal reciben el `orgId` de su cliente.
  - Si un token antiguo no los trae, se buscan en la base de datos, de modo que las sesiones abiertas no se rompen.
- **`utils/RequestAuthContext.kt`:**
  - `ownerAdminIdFor()` se sustituye por `organizationIdFor(identity)`, que lee del claim sin recorrer los clientes.
  - Nuevo `requireStaffIdentity(write: Boolean, settings: Boolean = false)`, que aplica la matriz de permisos.
  - Los guards de cada archivo de rutas pasan a apoyarse en él.
- **Repositorio y servicio:**
  - Se renombra `ownerAdminId: String?` → `organizationId: String`, **no nulable**, en `SupportDeskRepository`, `SupportDeskService`, `PostgresSupportDeskRepository` e `InMemorySupportDeskRepository`.
  - En SQL, el predicado `(? IS NULL OR owner_admin_id::text = ?)` pasa a `organization_id = CAST(? AS uuid)`.
  - Los INSERT rellenan `organization_id` (empresa) y `owner_admin_id` (usuario que actúa).
  - Es un cambio mecánico en unos 300 sitios; el compilador y los tests guían el renombrado.
- **Tickets:** se filtran por empresa en SQL, no en memoria.
- **Rutas nuevas:**
  - `routes/AuthRoutes.kt`:
    - `POST /auth/signup` (tras el interruptor, con límite de peticiones). Recibe nombre, email, contraseña (≥ 12 caracteres), nombre de empresa y aceptación de términos y privacidad con su versión. Crea empresa, usuario ADMIN y miembro OWNER, y devuelve la sesión.
    - `POST /auth/join`: código de invitación, nombre, email y contraseña. Crea el usuario con el rol de la invitación.
  - Nuevo `routes/OrganizationRoutes.kt`:
    - `GET /org`, `PATCH /org`: datos de la empresa, respuestas del onboarding y marca de completado.
    - `GET /org/members`, `PATCH /org/members/{userId}` (cambiar rol), `DELETE /org/members/{userId}` (desactiva al usuario; nunca deja la empresa sin OWNER).
    - `POST /org/invitations` (devuelve el código en claro una sola vez), `GET /org/invitations`, `DELETE /org/invitations/{id}`.
- **`PostgresDemoBootstrapper` y semillas en memoria:**
  - Crean la empresa "OryKai" y una segunda empresa, para los tests de acceso cruzado.
  - Añaden un usuario MEMBER y otro ACCOUNTANT de prueba.

### C. Shared (cliente)
- **`core/model`:**
  - `User` añade `organizationId`, `organizationRole: OrganizationRole?` y `organizationName`.
  - Nuevos `Organization`, `OrganizationMember` y `OrganizationRole`.
- **`features/auth`:**
  - `AuthSessionDto` y `AuthMapper` incluyen los campos de empresa.
  - El mapper pasa a fallar ante un rol desconocido en lugar de convertirlo en CLIENT.
  - `AuthRepository` añade `signup()` y `join()`.
  - Las rutas `/auth/signup` y `/auth/join` se excluyen del reintento con refresco en `SupportDeskHttpClient.kt:96`.
- **Nuevo `features/organization`:**
  - Data source, repositorio y `OrganizationViewModel` (estado de empresa, miembros e invitaciones).
  - Sigue el patrón State/Event/Effect y `BaseViewModel.guardedLaunch`.
- **Nuevo `features/onboarding`:**
  - `OnboardingViewModel` con los pasos, un borrador local por si se cierra a mitad y el envío por `PATCH /org`.
  - `recommendedModules(goals)` es una función pura y con tests; la fase 3 la usará para recomendar plan.
- **Factorías** en `SupportDeskSharedModule`.

### D. App (Compose)
- **Login** (`AdminLoginScreen.kt`):
  - Enlaces "Crear cuenta de empresa" (solo si el servidor tiene el alta activa: `GET /auth/signup/status`) y "Unirme con un código".
- **Alta y onboarding:** nuevo `app/onboarding/OnboardingFlow.kt` con 5 pasos, como el documento §5.
  1. Cuenta (es el alta en sí).
  2. Tu negocio.
  3. ¿Qué quieres resolver?
  4. Datos fiscales (opcional; reutiliza `clientTaxIdProblem` e `isSpanishPersonalTaxIdValid`).
  5. Estilo (reutiliza la vista previa de `AppearanceDialog`).

  Detalles:
  - Nuevo componente `designsystem/components/feedback/StepProgress.kt`: barra con "Paso n de 5" y Atrás/Saltar/Siguiente.
  - Errores junto a cada campo y foco en el primer error.
  - Se muestra tras iniciar sesión si `onboarding_completed_at` está vacío y el rol es OWNER. La puerta está en `AdminWorkspaceApp.kt:338`.
- **Inicio:** tarjeta "Primeros pasos" en `AdminDashboardScreen` (crear cliente, registrar horas, invitar a tu equipo, invitar a un cliente al portal), calculada con datos que ya existen.
- **Ajustes:** nuevo destino `AppDestination.Settings(section)`, con entrada en la barra lateral separada abajo.
  - Pestañas: **Empresa** (datos y fiscales), **Usuarios** (miembros, rol, invitar con código para copiar, revocar) y **Apariencia** (el diálogo actual, embebido).
  - Pasos para añadirlo: `AdminWorkspaceApp.kt:368-413`, `AdminContentArea`, `navDestinationFor`, `titleFor` y `subtitleFor`.
- **Permisos en la interfaz:**
  - ACCOUNTANT ve las acciones de escritura desactivadas, con el motivo explicado.
  - MEMBER no ve Usuarios ni los datos de empresa.
  - La cabecera muestra el nombre de la empresa.

## Orden de trabajo (rama `feat/multiempresa`, un commit por bloque)
1. A + B, solo datos y aislamiento: migración, renombrado a `organizationId`, JWT y tests de acceso cruzado. **Este bloque es el riesgo principal y debe quedar en verde antes de seguir.**
2. B, la parte nueva: alta, invitaciones y rutas `/org`.
3. C + D: sesión, alta, onboarding, Ajustes, Usuarios y permisos en la interfaz.
4. Documentación: guía de despliegue (copia de seguridad con `pg_dump` antes de migrar, nuevas variables de entorno) y actualización de `SAAS_TODO_EN_UNO_PLAN.md`.

## Verificación
- **Servidor** (`./gradlew :server:test`):
  - **Acceso cruzado:** un usuario de la empresa B no ve ni modifica clientes, tareas, etiquetas, sesiones, tickets ni el panel de la empresa A (devuelve 404/403). Se prueba en memoria y con Postgres embebido.
  - **Migración:** con datos de dos admins anteriores a V17, cada uno acaba en su propia empresa como OWNER y no se pierde ninguna fila (se cuenta antes y después).
  - **Alta:** crea empresa, OWNER y aceptación legal; responde 404 si el interruptor está apagado; con un email repetido devuelve un error claro.
  - **Invitaciones:** son de un solo uso, caducan y se pueden revocar; asignan el rol indicado.
  - **Permisos:** ACCOUNTANT recibe 403 al escribir; MEMBER recibe 403 en `/org` y en las invitaciones; no se puede quitar al último OWNER.
- **Shared** (`./gradlew :shared:jvmTest`): mapper con roles nuevos, `OnboardingViewModel` (avanzar, retroceder, saltar, validar, borrador) y `recommendedModules`.
- **App:**
  - `./gradlew :composeApp:jvmTest :composeApp:compileDebugKotlinAndroid`
  - Arrancar servidor y app en local. Recorrido completo: crear cuenta → 5 pasos → Inicio con primeros pasos → invitar a un MEMBER → entrar con el código en otra sesión y comprobar que solo ve lo de su empresa.
- **Sin push.** El despliegue en producción requiere copia de seguridad previa de la base de datos y lo decide el usuario.

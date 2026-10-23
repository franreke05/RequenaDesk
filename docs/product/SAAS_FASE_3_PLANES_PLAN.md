# Fase 3 — Planes y módulos por empresa

## Contexto
Desde la fase 2 cada empresa tiene su espacio, pero no hay nada que diga qué puede usar ni cuánto paga. El plan SaaS aprobado (`docs/product/SAAS_TODO_EN_UNO_PLAN.md` §3, §4 y §10) pide:
- Planes Gratis / Autónomo / Pro / Equipo, cada uno con sus módulos.
- Prueba Pro de 14 días sin tarjeta, que al acabar pasa a Gratis y **nunca a un cobro**.
- Límites por plan, con el precio calculado siempre en el servidor.
- Cobro manual en esta fase: OryKai factura por el carril A. Stripe llega en la fase 6.
- Las empresas de la beta siguen gratis hasta una fecha avisada con 60 días.

### Qué hay hoy
- Los programas `BUSINESS_*` (V8, V9 y V15) se contratan **por cliente del portal**: el cliente lo pide y el admin lo aprueba a 0 €. Se comprueban con `hasActiveBusinessEntitlement` (`server/.../application/SupportDeskModule.kt` L253).
- No hay planes de empresa, ni pasarela de pago, ni bloqueo de funciones en el lado del equipo. La única excepción es `local_pdf_invoices` (V18).
- **Fallo encontrado:** las rutas `/admin/program-requests/*/approve|reject` solo comprueban `users.role=ADMIN`, así que **un MEMBER puede aprobar**.
- El repositorio en memoria no incluye `BUSINESS_PROJECTS` en su catálogo, a diferencia de Postgres.

### Alcance
- **Entra:** planes por empresa, prueba, límites, bloqueo por módulo de las pantallas del equipo, solicitud y aprobación manual de cambios de plan, y pantalla "Plan y pagos".
- **Fuera:**
  - Los programas del portal (`BUSINESS_*`) siguen como están, por cliente y en beta. Se reubicarán al reorientar el portal en la fase 5.
  - El límite de "portal para 3 clientes" del plan Gratis, también para la fase 5.
  - Stripe y la facturación Verifactu (fases 6 y 4).

## Decisiones (aplicadas por recomendación)
1. **Claves de módulo:** `CORE`, `WORK`, `SUPPORT`, `SALES`, `BILLING`, `AGENDA`, `DOCUMENTS`, `PORTAL` y `REPORTS`.

   | Pantalla del equipo | Módulo |
   |---|---|
   | Inicio, Clientes, Ajustes | `CORE` |
   | Tareas, Tablero, Etiquetas, horas y temporizador | `WORK` |
   | Tickets | `SUPPORT` |
   | Facturas locales | Sigue la marca V18, solo OryKai |

   `SALES`, `BILLING`, `AGENDA` y `DOCUMENTS` todavía no tienen pantallas del equipo: de momento solo aparecen en las tarjetas de planes.

2. **Planes** (sembrados en la base de datos; el precio se guarda en céntimos y sin IVA):

   | Plan | €/mes · €/mes en anual | Usuarios incluidos (gestoría aparte) | Clientes | Módulos |
   |---|---|---|---|---|
   | `FREE` | 0 | 1 | 25 | `CORE` + 1 a elegir entre `WORK`, `SUPPORT`, `SALES` y `AGENDA` |
   | `AUTONOMO` | 12 · 10 | 1 | sin límite | `CORE`, `WORK`, `SALES`, `BILLING`, `AGENDA`, `PORTAL` |
   | `PRO` ★ | 29 · 24 | 3 | sin límite | Lo de Autónomo + `SUPPORT`, `DOCUMENTS`, `REPORTS` |
   | `TEAM` | 49 · 41 | 8 | sin límite | Lo de Pro (más roles avanzados y fichaje cuando existan) |
   | `BETA` (no se puede contratar) | 0 | sin límite | sin límite | Todos los módulos: es el plan de las empresas que ya existen |

   El usuario adicional cuesta 6 €; solo se muestra el precio, el cobro sigue siendo manual. La gestoría (ACCOUNTANT) **no cuenta** como usuario.

3. **Ciclo de vida, calculado al vuelo y sin tareas programadas:**
   - Alta nueva: `TRIAL` del plan Pro durante 14 días.
   - Al vencer la prueba, la empresa pasa a ser **`FREE` efectivo**. Si no eligió módulo, se le asigna uno según sus objetivos del onboarding (`WORK` por defecto). No se envía ningún cobro.
   - Las empresas existentes pasan a `BETA`, con `beta_ends_on` vacío. Cuando OryKai fije la fecha, se avisará 60 días antes y entonces se aplicará el 50 % de "fundadores". Esa fecha se elige en otra fase; aquí solo se guarda el dato.
4. **Cambios de plan:** la dueña o una admin **solicita** un plan y su periodicidad. Lo aprueba un **operador de la plataforma**: los dueños y admins de una empresa marcada con `is_platform_operator`, que en V19 es OryKai.
   - Al aprobar, el plan queda `ACTIVE` y OryKai emite la factura a mano por el carril A.
   - Bajar a Gratis no necesita aprobación, porque no implica ningún cobro.
   - Nunca se cobra sin aceptación expresa. La solicitud registra quién aceptó, qué precio y cuándo.
5. **Bloqueo por módulo en el servidor:** las rutas de un módulo no incluido responden **402 `Module not included in your plan`**. Los datos no se borran nunca: al volver a un plan que lo incluye, todo sigue ahí.
   - **Límites:** con el máximo de clientes alcanzado no se pueden crear más; con el máximo de usuarios alcanzado no se pueden crear invitaciones ni unirse (las de gestoría siempre se permiten). En ambos casos la respuesta es 409 con un mensaje claro.
6. **Corrección del fallo:** aprobar o rechazar programas del portal exige ser dueño o admin.

## Bloques (rama `feat/planes`, a partir de `feat/multiempresa`; un commit por bloque)

### Bloque 1 — Servidor: planes, estado efectivo, límites y bloqueo
- **`V19__organization_plans.sql`:**
  - `subscription_plans`: `plan_key` (PK), `name`, `monthly_price_cents`, `annual_monthly_price_cents`, `included_users`, `max_clients` (NULL = sin límite), `modules text[]`, `selectable_free_modules text[]`, `is_purchasable`, `sort_order`. Se siembran los 5 planes.
  - `organization_subscriptions`:
    - `organization_id` (PK, FK), `plan_key`.
    - `status` CHECK `TRIAL`/`ACTIVE`/`BETA`.
    - `billing_period` (`MONTHLY`/`ANNUAL`/NULL), `trial_ends_at`, `free_module`, `beta_ends_on`, `founder_discount_percent`, `started_at`, `updated_at`.
  - `organization_plan_events`: auditoría con `action`, `from_plan`, `to_plan`, `actor_user_id`, `price_cents` y `at`.
  - `organizations.is_platform_operator boolean DEFAULT FALSE`, que se pone a TRUE en la empresa con `local_pdf_invoices`.
  - **Relleno:** todas las empresas existentes pasan a `BETA`. RLS y REVOKE como en V17.
- **Dominio** (`server/.../domain/service/OrganizationPlanService.kt`):
  - `effectivePlan(orgId, now)` devuelve `EffectivePlan(planKey, status, modules, limits, usage, trialDaysLeft, effectiveAfterTrial)`. La regla de expiración es una función pura y con tests.
  - Repositorio nuevo `OrganizationPlanRepository`: Postgres más memoria, delegado igual que `OrganizationRepository`.
- **Alta:** `signupOrganization` crea también la suscripción `TRIAL` Pro, con `trial_ends_at` a +14 días.
- **Bloqueo:** helper `requireModule(identity, module)` en `utils/RequestAuthContext.kt`. Se llama en las rutas de `TaskRoutes`, `TaskBoardRoutes`, `LabelRoutes`, `TimeLogRoutes` y `WorkSessionRoutes` (`WORK`) y en `TicketRoutes` (`SUPPORT`).
  - El estado efectivo se guarda en caché 30 s, como la identidad, y se invalida al cambiar de plan.
  - Los usuarios del portal (CLIENT) no se bloquean aquí; los programas del portal siguen con su comprobación actual.
- **Límites:**
  - Clientes: en `SupportDeskService.createdClient`.
  - Usuarios: en `OrganizationService.createdInvitation` y en `join`. Cuentan los miembros activos que no son gestoría más las invitaciones pendientes que no son de gestoría.
- **Ruta nueva:** `GET /org/plan`, para cualquier persona del equipo. Devuelve el plan efectivo, los módulos, los límites y su uso, los días de prueba que quedan y el catálogo de planes contratables con sus precios.
- **Corrección** en `routes/ProgramRoutes.kt`: approve y reject exigen `OrganizationRoles.canManage`.
- **Repositorio en memoria:** se añade `BUSINESS_PROJECTS` al catálogo de programas para que coincida con Postgres.

### Bloque 2 — Servidor: cambios de plan y aprobación por el operador
- **`V19`** (en el mismo fichero) crea `organization_plan_requests`:
  - `id`, `organization_id`, `plan_key`, `billing_period`.
  - `quoted_monthly_price_cents`: el precio calculado en el servidor en el momento de pedirlo.
  - `status` `REQUESTED`/`APPROVED`/`REJECTED`/`CANCELLED`.
  - `requested_by`, `accepted_terms_at`, `decided_by`, `decided_at`, `note`.
  - Índice único para que solo haya una solicitud `REQUESTED` por empresa.
- **Rutas de la empresa** (dueña o admin):
  - `POST /org/plan/requests`: pide `planKey` y `billingPeriod` y exige `acceptPrice=true`. Bajar a `FREE` se aplica al momento.
  - `DELETE /org/plan/requests/{id}`: cancela la solicitud.
  - `PATCH /org/plan/free-module`: elige el módulo del plan Gratis.
- **Rutas del operador** (dueña o admin de una empresa con `is_platform_operator`):
  - `GET /platform/plan-requests?status=`
  - `POST /platform/plan-requests/{id}/approve`: deja el plan `ACTIVE`, guarda el evento y vacía la caché.
  - `POST /platform/plan-requests/{id}/reject`
- Ninguna ruta mueve dinero. La respuesta de la aprobación recuerda que la factura se hace a mano (carril A).

### Bloque 3 — App
- **Shared:** `features/plan`, con DTOs, `PlanRepository` y `PlanViewModel` (plan efectivo, catálogo, solicitudes de operador, eventos).
  - Más la función pura `recommendedPlan(goals, teamSize)` (por ejemplo, Soporte → Pro; equipo de 6 o más → Equipo), con tests.
  - Más los textos en español de los errores 402 y 409.
- **Ajustes › Plan y pagos** (`app/admin/screens/AdminPlanScreen.kt`):
  - Plan actual con su estado y un aviso de prueba ("Te quedan N días de Pro").
  - Uso frente a límites.
  - Selector Mensual/Anual y tarjetas de los 4 planes, con Pro destacado y precios del servidor.
  - "Solicitar este plan", con confirmación del precio y el texto "Sin cobro automático: OryKai te enviará la factura".
  - Solicitud pendiente con opción de cancelarla, y elección del módulo del plan Gratis.
  - Solo la dueña y los admins pueden pedir cambios; los demás ven el plan sin poder tocarlo.
- **Operador:** pestaña "Solicitudes de plan" en Ajustes, visible solo si `isPlatformOperator`, con Aprobar y Rechazar.
- **Navegación por módulos:** los módulos no incluidos aparecen atenuados con la etiqueta "Incluido en Pro" (en lugar de ocultarse, plan §6) y llevan a Plan y pagos. Si el servidor responde 402, la app lo muestra con un mensaje claro y un enlace al plan.
- **Onboarding:** el paso 3 añade "Te recomendamos el plan X". Al terminar, Inicio muestra la prueba en curso.

### Bloque 4 — Documentación
- `docs/V19_PLANES_DEPLOYMENT.md`, con copia de seguridad, verificación (todas las empresas existentes en `BETA`, OryKai como operador) y vuelta atrás.
- Actualización de `SAAS_TODO_EN_UNO_PLAN.md` y del estado de este plan.

## Verificación
- **Servidor** (`./gradlew :server:test`):
  - Estado efectivo: la prueba vence y pasa a `FREE` con el módulo según objetivos; `BETA` incluye todos los módulos; un plan de pago activo incluye los suyos.
  - Bloqueo: una empresa en Gratis con `SUPPORT` elegido recibe 402 en `/admin/tasks` y 200 en `/tickets`; tras aprobarle Pro, recibe 200 en los dos.
  - Límites: el cliente número 26 en Gratis devuelve 409; un usuario por encima del límite devuelve 409; la gestoría siempre se permite.
  - Solicitudes: una por empresa; solo el operador puede aprobar; otra empresa recibe 403 o 404; bajar a Gratis se aplica al momento; el precio que se guarda es el del servidor.
  - Corrección: un MEMBER recibe 403 al aprobar programas del portal.
  - Migración V19 sobre Postgres embebido: todas las empresas en `BETA`, OryKai como operador, sin perder filas.
- **Shared y app:** `./gradlew :shared:jvmTest :composeApp:jvmTest :composeApp:compileDebugKotlinAndroid`, con `recommendedPlan`, mapeo de errores, la regla de navegación por módulos y `PlanViewModel` con un repositorio falso.
- **Manual:** servidor local con Postgres desechable, igual que en la fase 2. Recorrido:
  1. Alta → prueba Pro.
  2. Adelantar `trial_ends_at` por SQL → pasa a Gratis y Tickets aparece atenuado.
  3. Solicitar Pro → aprobar como OryKai → Tickets vuelve.
- **Sin push ni despliegue** hasta que el usuario lo pida.

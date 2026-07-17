# AI Session State

## Current task
Publish the server release that includes SBS credential regeneration, final CRM client routes and the V7 RLS security baseline.

## Files touched this session
- `docs/product/CLIENT_CRM_UX_AI_STRATEGY.md`
- `docs/AI_CONTEXT_MAP.md`
- `docs/AI_TOKEN_INDEX.md`
- `docs/AI_SESSION_STATE.md`
- `docs/AI_DECISION_LOG.md`
- `shared/.../core/model/ClientPortalComponent.kt` and client data/domain/presentation flow
- `server/.../V5__client_component_entitlements.sql`, client routes, repositories and server models
- `composeApp/.../app/client/*` and `AdminClientsScreen.kt`
- Client data-source and PostgreSQL migration integration tests
- `server/.../security/ClientAccessCodeGenerator.kt`, server client routes/models/repositories
- shared client DTO/domain/repository/ViewModel flow and `AdminClientsScreen.kt`
- `shared/.../features/invoices/data/storage/InvoicePdfStorage*`, invoice ViewModel/state/event, tests and `AdminInvoicesScreen.kt`
- `composeApp/.../AdminClientsScreen.kt` and `ClientsViewModel.kt`
- `server/.../ClientCrmRoutes.kt`, `ClientPortalRoutes.kt`, CRM repository/models, API JSON and Ktor registration
- `server/.../V6__client_crm_contacts_and_activities.sql` and server migration/API tests
- `server/.../V7__secure_public_schema_with_rls.sql`, request rate limiting and proxy security headers

## Decisions made
- The first paid module is `SERVICE_SLA`; entitlement activation is manual in the admin client screen.
- Module entitlement is server-authorized and client-scoped. Existing Priority/VIP clients are backfilled by V5; future components must not infer access solely from service tier.
- The client portal now has five primary destinations: Inicio, Trabajo, Solicitar, Servicio and Cuenta. Tickets, tasks, board and activity remain available under Trabajo.
- A client without Service and SLA can still use core support. The portal explains the optional module and routes to a contextual request.
- No external AI provider is configured. AI remains a planned, provider-neutral follow-up pending credentials, consent and retention policy.
- Client creation now atomically provisions the portal account with a generated `SBS-XXXX-XXXX-XXXX` code. PostgreSQL stores only its BCrypt hash; plaintext returns once to the authenticated admin.
- The credential tab no longer accepts a manually chosen password. It regenerates a code after confirmation and revokes refresh sessions. Changing a client email also synchronizes the linked portal login email.
- Invoice deletion remains desktop-local: the UI confirms the action, the storage boundary validates the filename remains inside the invoice folder, deletes that one PDF and refreshes the library.
- Regenerated SBS credentials appear both in the global one-time dialog and directly in the selected client's Credenciales tab; the success notice also includes the code as a fallback.
- V6 is the last planned CRM feature migration. V7 is a later, explicitly requested security-only migration: it enables RLS on every live table and blocks direct Supabase API access because the product authenticates through Ktor rather than Supabase Auth.
- Login and refresh are limited to 8 requests per minute per originating IP; admin credential change/regeneration is limited to 5 requests per 10 minutes per originating IP. Credentials are not accepted through URL query parameters.

## Pending work
- Add the next commercial module after validating Service and SLA usage (recommended: Roadmap and approvals).
- Define client members/roles, recurring billing and the approved AI provider/data policy.
- Decide later whether to remove the legacy, admin-only manual credentials endpoint after any external API consumers have migrated to regeneration.
- Deploy/restart the current Ktor server process after publishing. The active public backend is healthy but the project has no remote deployment connection or CI configuration in this workspace. The restart is required to run V7 on the production Supabase database and to expose the SBS regenerate route.

## Commands run
- Applied local `.claude` project-context, Ktor and Compose state skills.
- `./gradlew.bat :shared:compileKotlinJvm :composeApp:compileKotlinJvm :server:compileKotlin --no-daemon` (passed).
- `./gradlew.bat :server:test :shared:jvmTest :composeApp:compileKotlinJvm --no-daemon` (passed; includes real PostgreSQL migration integration).
- `./gradlew.bat :shared:jvmTest :composeApp:compileKotlinJvm --no-daemon` (passed after local invoice deletion change).
- `./gradlew.bat :shared:compileKotlinJvm :composeApp:compileKotlinJvm --no-daemon` (passed after SBS display redundancy change).
- Confirmed the application route and its server tests use `POST /admin/clients/{id}/credentials/regenerate`; no local Ktor process is listening on port `8080` and no local backend URL is configured.
- `./gradlew.bat :server:compileKotlin --no-daemon` (passed after V6 CRM route implementation).
- `./gradlew.bat :server:test --no-daemon` (passed; includes embedded PostgreSQL applying V6 and exercising contacts/activities).
- `./gradlew.bat :server:test :shared:jvmTest :composeApp:compileKotlinJvm :server:installDist --no-daemon` (passed; distribution generated under `server/build/install/server`).
- `./gradlew.bat :server:test :server:installDist --no-daemon` (passed after V7; embedded PostgreSQL verifies RLS for all 15 live tables and denies direct `anon`/`authenticated` table access).
- Published `2aeed84` to `origin/main`.

## Failures / blockers
- The currently contacted backend returns 404 because it is an older deployment. The source route is present and covered by passing server tests, but this workspace has no server configuration, SSH target or CI deployment automation to restart the public process directly.
- The public endpoint still returned HTTP 404 after the Git push. The project has no local Ktor listener on port `8080` and no `.github` deployment workflow; a terminal/session on the actual server is required to run the updated artifact.

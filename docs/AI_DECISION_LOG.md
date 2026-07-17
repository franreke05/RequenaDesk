# AI Decision Log

## 2026-07-17 — Mixed invoice lines remain local
Manual activities are transient invoice lines and are stored only in the generated desktop PDF, preserving the project’s no-invoice-persistence constraint.

## 2026-07-17 — Shared calculations, JVM PDF layout
Invoice item classification and totals live in `commonMain`; the PDF visual layout remains a PDFBox concern in `jvmMain`.

## 2026-07-17 — Issuer profile stays local
Issuer fiscal details are read from the ignored desktop `supportdesk.properties` file, keeping them out of source control, APIs and invoice persistence.

## 2026-07-17 - Client CRM proposal remains unapproved
The CRM, subscription-component and AI strategy is recorded in `docs/product/CLIENT_CRM_UX_AI_STRATEGY.md` for review. It deliberately does not alter existing schema, billing behavior, client authentication or AI data processing until product decisions are approved.

## 2026-07-17 - First client CRM vertical approved and implemented
`SERVICE_SLA` is a client-scoped entitlement managed manually by admins, persisted in PostgreSQL and surfaced in the Centro de Cliente portal. Existing Priority/VIP clients are backfilled; service tier is not the long-term authorization mechanism.

## 2026-07-17 - Live AI remains deferred
No AI provider is called until credentials, client consent and a retention policy are approved. The implemented portal changes do not transmit client data outside the current application.

## 2026-07-17 - Portal credentials are app-generated SBS codes
Client provisioning and regeneration generate a secure `SBS-XXXX-XXXX-XXXX` code, display it once to an admin and persist only its BCrypt hash. Regeneration revokes refresh sessions and the client email remains the login identifier.

## 2026-07-17 - Invoice deletion stays in the local PDF library
Deleting a factura requires confirmation, validates the selected file remains inside `Facturas OryKai`, and removes only that local PDF. It does not add backend or database persistence.

## 2026-07-17 - SBS regeneration requires the current backend deployment
The desktop client calls `POST /admin/clients/{id}/credentials/regenerate`. A 404 is not a credential-display failure: it means the backend serving the request has not been updated with the route (or the client points to the wrong server). Do not fall back to locally generated credentials, because the server must hash the code and revoke the old refresh sessions atomically.

## 2026-07-17 - V6 closes the CRM feature schema delivery
V6 adds internal client contacts and follow-up activities, while dedicated `/client/*` routes provide a safe portal read model. Contacts, activities and ticket internal comments remain admin-only.

## 2026-07-17 - V7 establishes the Supabase public-schema security baseline
The explicit security request reopens migrations solely for security. V7 enables RLS on every live application table and removes direct `anon`, `authenticated` and `PUBLIC` access to public tables, views, sequences and functions. The app uses its own Ktor JWT authentication rather than Supabase Auth, so it intentionally defines no browser-facing RLS policies; all application access remains through Ktor's backend database role.

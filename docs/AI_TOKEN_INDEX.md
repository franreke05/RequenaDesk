# AI Token Index

| Path | Contains | Read when |
|---|---|---|
| `CLAUDE.md` | Invoice-locality rules | Changing any invoice behavior |
| `shared/.../features/invoices/domain/model/InvoiceInputs.kt` | Invoice creation models | Adding invoice fields or item types |
| `composeApp/.../AdminInvoicesScreen.kt` | Admin invoice form | Changing invoice interaction/UI |
| `shared/.../InvoicePdfStorage.jvm.kt` | PDFBox renderer and safe local library deletion | Changing PDF layout, validation or local files |
| `shared/.../InvoicesViewModel.kt` | Local PDF generation, deletion, state and effects | Changing submission or library behavior |
| `shared/.../TasksUiState.kt` | Task time aggregation | Billing task hours |
| `shared/.../SupportDeskSharedModule.kt` | ViewModel construction | Changing invoice dependencies |
| `shared/src/*Test/.../features/invoices` | Invoice tests | Verifying invoice feature changes |
| `supportdesk.properties.example` | Desktop-local configuration pattern | Adding non-versioned desktop settings |
| `server/.../ClientRoutes.kt` | Admin client creation and SBS credential regeneration endpoints | Changing portal access provisioning |
| `server/.../PostgresSupportDeskRepository.kt` | Atomic PostgreSQL client-user provisioning and BCrypt credentials | Changing portal login persistence |
| `server/.../security/ClientAccessCodeGenerator.kt` | Secure `SBS-` access-code generator | Changing portal credential format or entropy |
| `shared/.../features/auth` | Login request and session state | Changing client login inputs |
| `composeApp/.../AdminClientsScreen.kt` | One-time credential display and admin regeneration action | Changing client portal credential UX |
| `docs/product/CLIENT_CRM_UX_AI_STRATEGY.md` | Reviewed client CRM, UX/UI, modules and AI proposal | Planning or implementing client-portal expansion |
| `composeApp/.../app/client/ClientPortalScreen.kt` | Current client portal navigation and screen composition | Changing client information architecture |
| `composeApp/.../app/client/screens/ClientHomeScreen.kt` | Current portal home and action cards | Redesigning client landing experience |
| `composeApp/.../app/client/screens/ClientServiceScreen.kt` | Current support hours and service metrics | Building service/SLA experience |
| `server/.../routes/ClientRoutes.kt` | Client identity scope and admin provisioning | Adding members, entitlements or portal access |
| `server/.../db/migration/V1__supportdesk_schema.sql` | Current client/users/tickets/tasks schema | Adding client CRM or module schema |
| `server/.../db/migration/V5__client_component_entitlements.sql` | Client-scoped Service and SLA entitlement | Adding or migrating product modules |
| `server/.../db/migration/V6__client_crm_contacts_and_activities.sql` | Final CRM schema migration: contacts and internal follow-ups | Maintaining the client CRM data model |
| `server/.../routes/ClientCrmRoutes.kt` | Admin-only contact and activity CRUD | Extending CRM operations for a client |
| `server/.../routes/ClientPortalRoutes.kt` | Sanitized, identity-scoped client read API | Connecting the customer portal without admin routes |
| `shared/.../core/model/ClientPortalComponent.kt` | Supported client add-on enum and display copy | Adding a new contracted component |
| `server/.../ClientRoutes.kt` | Admin endpoint for client components | Changing entitlement authorization |
| `composeApp/.../AdminClientsScreen.kt` | Admin component activation tab | Changing manual subscription management |
| `composeApp/.../app/client/screens/ClientWorkScreen.kt` | Grouped client work experience | Changing client work information architecture |

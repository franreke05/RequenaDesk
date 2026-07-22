# Client Portal Free-Tier UX Pass

Structural/content review of the free-tier client portal (13 files under
`app/client/*`). Visual/comic restyling is intentionally out of scope here —
that is a separate pass. This is the audit trail for that follow-up and for
the maintainer.

Scope note: invoices/facturas were not touched anywhere (per `CLAUDE.md`),
and nothing under `app/admin/*` or `app/client/screens/business/*` was
touched.

## 1. ClientPortalScreen.kt

**Assessment:** Already solid — clear nav, sensible destination routing, a
calm "acceso pendiente" empty state that doesn't dead-end the user (offers
sign-out), and a compact/wide layout split that keeps the primary "Solicitar
ayuda" action reachable in both. No structural changes made; the shell was
judged fit for purpose. Left untouched.

## 2. ClientPortalDesign.kt (components)

**Assessment:** Shared card/header/metric primitives are well-scoped and
consistent (`ClientPortalSurfaceCard`, `ClientPortalSectionCard`,
`ClientPortalPageHeader`, `ClientPortalMetric`, `ClientPortalSectionTitle`).
No copy or API gaps found — every screen composes these cleanly. Left
untouched to avoid destabilizing the shared contract ahead of the visual
pass.

## 3. ClientHomeScreen.kt

**Assessment:** Good information hierarchy already: priority card (pending
ticket needing a response) is the most prominent slot, service/program
snapshot second, metrics third, recent activity/today's tasks last. Empty
and loading states are meaningful ("Todo el contexto... ordenado para
decidir el siguiente paso" vs. a clear "hay una actualización que necesita
tu respuesta" when blocked). No changes needed.

## 4. ClientWorkScreen.kt

**Assessment:** Concise hub that surfaces the pending-response state up top
("Hay una respuesta pendiente") and defers full detail to Tickets/Tasks.
Structure judged correct as-is. No changes made.

## 5. ClientNewTicketScreen.kt

**What was weak:** On successful submission, the form silently cleared
(`subject`/`description`/`priority` reset) with no visible confirmation.
A client who just submitted a ticket had no signal it worked short of
noticing the fields went blank — easy to read as "did that actually
submit?" and re-click.

**What changed:** Added a `justCreatedTicketId` saved state that is set
when `lastCreatedTicketId` changes, and an animated `ClientNotice` ("Ticket
enviado. El equipo lo revisará y te avisaremos en cuanto haya novedades.")
shown at the top of the form. It clears as soon as the client starts typing
a new subject/description, so it reads as a one-time confirmation rather
than a stale banner.

**Why:** Closing the feedback loop on the primary conversion action of this
screen (submitting a ticket) is a basic trust signal for a support portal —
users should never have to guess whether their request went through.

## 6. ClientTicketsScreen.kt

**What was weak:** The ticket detail panel's closing section ("Estado de
cierre" via `ClientClosure`) only distinguished resolved vs. not-resolved,
collapsing `OPEN`, `IN_PROGRESS`, and `PENDING_CLIENT` into one generic
"El ticket está pendiente de resolución." That's the least actionable
moment for exactly the ticket state (`PENDING_CLIENT`) where the client
needs to *do* something.

**What changed:** `ClientClosure` now branches on all five `TicketStatus`
values with a distinct title and message per state, notably calling out
`PENDING_CLIENT` explicitly ("Esperando tu respuesta... El equipo necesita
una confirmación tuya para poder continuar") and giving `RESOLVED`/`CLOSED`
a clear next step (open a new ticket, or check Actividad for history).

**Why:** Status legibility at a glance was the task's explicit bar — a
single ambiguous "pending" bucket hid the one state that actually blocks
the client's own team.

## 7. ClientTasksScreen.kt

**Assessment:** Daily-limit framing, progress bar, and the "Hoy" vs.
"Anteriores" split are clear and the empty state points at the form. Judged
sufficient as-is. No changes made.

## 8. ClientBoardScreen.kt

**Assessment:** Kanban board has correct loading/error/empty states
(`LoadingState`, `ErrorState` with retry, `EmptyState` with a next step),
and urgent tickets get a visible indicator per column. No changes made.

## 9. ClientServiceScreen.kt

**What was weak:** The free-tier lock state (`isEnabled = hasServiceSla ==
false`) read as a dead end: title "Servicio y SLA no esta activo" is a
negative status statement, not an invitation, and the single line of body
copy didn't say what the client would actually get by asking for it.

**What changed:** Reframed as an upsell: title is now "Desbloquea el panel
de consumo", subtitle explains the value (hours tracking, monthly trends,
SLA priority), and the body lists three concrete benefits (monthly hours
with month-over-month comparison, a 6-month trend with category breakdown,
priority handling on urgent tickets) before the CTA, now labeled "Solicitar
activación" instead of the more passive "Solicitar información". Added a
reassurance line that nothing in their current history is lost while
waiting.

**Why:** This is exactly the free-vs-paid boundary the review brief called
out — it should read as enticing rather than a locked door, per the spec's
explicit UX bar for `ClientServiceScreen`'s `isEnabled` gate.

## 10. ClientActivityScreen.kt

**Assessment:** Filter chips (Todo/Semana/Mes), summary badges (event
count, time logged, status changes), and grouped-by-date list with typed
icons per event type are already clear and well organized. No changes
made.

## 11. ClientAccountScreen.kt

**Assessment:** Profile/plan split, service preferences, enabled
components, and a monthly activity summary are ordered sensibly (identity
first, then plan, then details, then activity). Copy is clear and the
account status badge is legible. No changes made.

## 12. ClientSettingsScreen.kt

**Assessment:** Already strong — clearly separates the paid CRM
subscription from the "Facturación" business program (explicitly
documented in the KDoc), and every legal/privacy placeholder panel is
honest about being "pendiente de publicación" rather than faking content.
This is a good pattern; left untouched.

## 13. ClientProgramsScreen.kt

**Assessment:** The catalog/upsell screen already does the enticing-not-
dead-end job well: per-program state badges (Activo / Pendiente de
autorización / En tu selección / Próximamente / Disponible para solicitar),
a running selection panel before submission, and clear request-status
history. No changes made.

## Summary of edits

| File | Change |
| --- | --- |
| `ClientNewTicketScreen.kt` | Added a post-submit success confirmation notice that clears on further edits. |
| `ClientTicketsScreen.kt` | `ClientClosure` now gives a distinct, actionable message per `TicketStatus` instead of a binary resolved/pending split. |
| `ClientServiceScreen.kt` | Reworded the free-tier lock state as a benefits-led upsell instead of a negative status message. |

All other files in scope were reviewed and judged structurally sound; no
changes were made to avoid unnecessary churn ahead of the visual pass.

## Visual pass (comic-editorial)

Brings the client portal in line with the "editorial paper" comic system
already shipped on the admin side (warm paper surfaces, true-ink text, one
vermillion accent, hairline borders instead of blurred Material elevation,
and a `hardOffsetShadow` "printed panel" treatment reserved for one hero
element per screen). Started by an agent that hit a session-usage limit
after finishing file 1; the rest was completed directly, same system.

- **`ClientPortalDesign.kt`** (shared base, done first so it cascades
  everywhere): `ClientPortalSurfaceCard`/`ClientPortalSectionCard` gained an
  `emphasized: Boolean` param — off by default (1dp `outlineVariant`
  hairline border, matching the admin `SectionCard`), on for a 2dp ink
  (`onSurface`) outline plus a hard, unblurred offset accent shadow (a
  locally-duplicated `clientPortalHardShadow`, kept independent from the
  admin card system on purpose). Card radius unified to 8dp (was 16dp).
  `ClientPortalMetric` border/shape brought in line with the same 8dp
  hairline language (was a separate 12dp shape at half-opacity border).
- **`ClientPortalScreen.kt`** — the "acceso pendiente de configurar"
  empty state (`SectionCard`, the admin component reused here) is now
  `emphasized = true`: it's a full-screen dead-end state, the clearest
  possible hero.
- **`ClientHomeScreen.kt`** — `ClientPriorityCard` is `emphasized` only
  when there's a `pendingTicket` needing a response: the ink-outline/shadow
  doubles as an urgency cue instead of being decoration from a cold start.
- **`ClientWorkScreen.kt`** — same dynamic-urgency pattern on
  `RequestsOverview` (`emphasized = pendingClientTickets > 0`).
- **`ClientNewTicketScreen.kt`** — `TicketPreviewCard` becomes emphasized
  once the form completeness score hits ≥90%, rewarding a finished form
  rather than styling from the first keystroke (peak/reward moment).
- **`ClientTicketsScreen.kt`** — `TicketGridCard` swapped from
  `ElevatedCard` (blurred Material elevation, growing with selection) to a
  flat `Card` with an animated hairline/ink/accent border depending on
  selected/hover state — no more glassy shadow. `ClientClosure` is
  emphasized when `TicketStatus.PENDING_CLIENT` (the state that actually
  needs the client's action).
- **`ClientTasksScreen.kt`** — the "Hoy" card is emphasized when
  `todayDone == todayTasks.size`: a small celebratory beat for clearing the
  day's list.
- **`ClientBoardScreen.kt`** — `KanbanTicketCard` swapped from
  `ElevatedCard` (blurred shadow growing on hover) to a flat `Card` with a
  hover-reactive hairline/ink border, consistent with the tickets grid card.
- **`ClientServiceScreen.kt`** — the free-tier upsell card
  (`isEnabled == false`) is emphasized — it's the single most important
  thing on the screen when locked, per the UX pass's reframing of it as an
  invitation rather than a dead end.
- **`ClientActivityScreen.kt`** — `ActivityItemRow` gained a 1dp
  `outlineVariant` border (was a borderless tinted `Surface`), for
  consistency with the rest of the card language; no hero here, it's a feed.
- **`ClientAccountScreen.kt`** — `ProfileCard` is emphasized (the client's
  own identity card, the natural hero of the Cuenta screen); its avatar
  circle gained a 2dp ink ring, a small "letterpress stamp" touch.
- **`ClientSettingsScreen.kt`** — `SettingsEntry` (the repeated nav-list
  row, used ~7 times) gained a hairline/danger border (was borderless) and
  its radius was tightened to 8dp for consistency. The "Programas durante
  la beta" card is emphasized — the free-beta framing is the headline fact
  of the billing panel.
- **`ClientProgramsScreen.kt`** — `ProgramSelectionPanel` (the review-
  before-send cart) is emphasized: the clearest "about to commit" moment on
  the screen. Catalog cards (`ProgramCatalogCard`) were left at the default
  hairline treatment — already carry enough visual weight via per-program
  status badges and bold "Gratis durante la beta" pricing text; adding
  emphasis to every card in a grid would have defeated the "one hero per
  screen" restraint the system depends on.

Guardrails followed throughout: no new hex colors (everything pulled from
`MaterialTheme.colorScheme` / `SupportDeskThemeTokens`), no halftone
textures or cartoon motifs, at most one `emphasized` element per screen
(several tied to real state — urgency/completion/commitment — rather than
applied unconditionally), and `./gradlew :composeApp:compileKotlinJvm -q` /
`:composeApp:compileDebugKotlinAndroid -q` both pass with these changes.

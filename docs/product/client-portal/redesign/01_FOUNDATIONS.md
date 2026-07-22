# Wave 0 — Fundamentos (decisión reconciliada)

Este documento es la única fuente de verdad para las decisiones de Wave 0. Las 21 specs por pantalla (`specs/<pantalla>.md`) lo referencian en vez de repetirlo.

Proceso: 4 expertos de dominio (UX Architect, UI Designer, Mobile App Builder, Code Reviewer) analizaron en paralelo, sin verse entre sí. Reality Checker verificó cada afirmación leyendo el código directamente (no confió en los resúmenes) y recalculó la aritmética disputada de forma independiente. Yo concilié los 3 conflictos reales que surgieron. Nada de lo que sigue es "porque un agente lo dijo" — cada decisión tiene su verificación.

---

## 1. Escala de breakpoints unificada

**Estructura** (reemplaza los 4 tokens `client*` en el mismo objeto, sin anidar — ratificado por Responsive, verificado por Reality Checker):

```kotlin
val clientMedium: Dp = 640.dp
val clientWide: Dp = 960.dp                     // antes 900.dp
val clientUltraWide: Dp = 1400.dp                // antes clientMultiColumn, mismo valor
val clientSidebarWidth: Dp = 284.dp              // nombra el literal suelto de NavigationComponents.kt:61
val clientShellExpandedThreshold: Dp = 1280.dp   // derivado: clientWide(960)+clientSidebarWidth(284)=1244, redondeado por seguridad de redondeo de densidad
```

Se retiran los nombres `clientBoardWide` y `clientStacked` (no se mantienen como alias).

**Por qué 1280 y no 1244 exacto:** `BoxWithConstraints.maxWidth` es un valor medido en tiempo real sujeto a redondeo de densidad en pantallas con escala no entera — un límite de igualdad exacta arriesga parpadeo de un píxel justo en el cruce. Verificado además que a escalados de Windows habituales (125%/150%), el mínimo de ventana de escritorio (1024px físicos) puede corresponder a menos de 1024dp lógicos — los niveles `compact`/`medium` son alcanzables en escritorio, no son un caso raro de móvil.

## 2. El bug estructural (shell vs. contenido) y su arreglo

**Causa raíz verificada dos veces (Responsive + Reality Checker, con aritmética independiente):** `ClientPortalScreen.kt`'s `BoxWithConstraints` mide el ancho completo de la ventana y decide sidebar-vs-compacto; el sidebar (`AppSidebar`) mide 284dp fijos; pero cada pantalla hija mide su propio ancho de *contenido restante* (ya descontado el sidebar) contra el mismo valor de token. Ventana de 1000dp de ejemplo: shell ya muestra sidebar (1000≥900 antiguo), deja 716dp de contenido — por debajo de 820dp — Tickets renderiza modo compacto bajo un chrome que ya se cree "escritorio ancho". Afecta a 6 archivos, no solo a Tickets: `ClientAccountScreen.kt:80`, `ClientNewTicketScreen.kt:216`, `ClientHomeScreen.kt:67`, `ClientWorkScreen.kt:61`, `ClientBoardScreen.kt:109`, `BusinessCustomersScreen.kt:93`.

**Arreglo:** el shell (`ClientPortalScreen.kt:454`) es la ÚNICA comparación que debe usar `clientShellExpandedThreshold` (1280dp) en vez de `clientWide` — porque es la única que mide ventana completa en vez de contenido. Todas las demás pantallas simplemente migran su valor de 900→960 (mismo mecanismo, sin cambio estructural).

## 3. Conflicto resuelto — Tickets: split de 3 paneles vs. grid de 2 columnas

**Las dos posturas:** UX quería activar el modo ancho de Tickets en `medium` (640dp) para no perder la vista rica que hoy ya funciona desde 700-820dp. Responsive, con matemática verificada (recalculada de forma independiente por mí y por Reality Checker, ambos llegando a **87.28dp/columna** en el umbral actual de 820dp y **44.08dp/columna** si se bajara a 640dp), argumentó que bajar el corte rompería el grid.

**La resolución no es "gana uno":** `stacked` (mostrar 3 paneles: filtros+grid+detalle) y `cols` (columnas dentro del grid) son hoy la MISMA variable, ambas ancladas a 820dp (`ClientTicketsScreen.kt:96-101`). Se desacoplan:
- El split de 3 paneles activa en `clientMedium` (640dp) — satisface a UX, nada se pierde antes de tiempo.
- `cols=2` sigue anclado cerca de 820-900dp — idealmente calculado desde el ancho real medido del panel de grid (con su propio `BoxWithConstraints`), no desde el `maxWidth` del contenedor exterior. Esto también cierra un bug preexistente (el acoplamiento a `maxWidth` exterior) que ninguno de los dos expertos había señalado como su foco principal pero que Responsive documentó.
- Kanban (`ClientBoardScreen.kt:109`) no tiene este problema (scroll horizontal absorbe cualquier ancho) — ambos expertos coincidieron en bajarlo a `clientMedium` (640dp) sin objeción.

## 4. Snackbar / efectos — arquitectura y estilo

**Dónde vive el estado vs. dónde se dibuja (resuelto, no es un conflicto real):** `AdminWorkspaceApp.kt:206` es el único sitio de toda la app donde se invoca `ClientPortalScreen(...)`, y `AdminAppModule` es el único lugar con referencia simultánea a las 9 ViewModels relevantes (Tickets/Tasks/Programs + 6 de negocio) — verificado por grep exhaustivo, no por asunción. `ClientPortalScreen.kt` en cambio solo recibe Tickets/Tasks/Programs como `UiState`+lambda, nunca la ViewModel. Patrón estándar de Compose: `SnackbarHostState` (un simple contenedor de estado, no una ViewModel) se crea con `remember` en `AdminWorkspaceApp.kt`, se alimenta desde ~8 nuevos `LaunchedEffect(vm) { vm.effects.collect { ... } }`, y se pasa como parámetro simple a `ClientPortalScreen`. El `SnackbarHost` (el composable visual) se dibuja DENTRO de `ClientPortalScreen`, como hermano de `AnimatedContent` dentro de `portalContent` (así sobrevive el cambio de destino) y anclado a los límites del panel de contenido, no de la ventana completa — evita el desplazamiento de ~166dp hacia el sidebar que se midió a 900dp de ancho total.

**Hallazgo adicional confirmado:** hoy `AdminWorkspaceApp.kt` ya colecciona 4 efectos (auth/clients/invoices/tickets) pero los vuelca en un `var statusMessage` que se renderiza como badge de header PERMANENTE que nunca se limpia — y ese badge solo se muestra en la rama no-CLIENT, así que incluso el efecto de tickets que sí es relevante para el cliente se descarta en silencio hoy. Los otros 8 (Tasks, Programs, 6 de negocio) no se coleccionan en ningún sitio del código, ni admin ni cliente.

**Diseño visual del Snackbar (ratificado, con una corrección hecha durante la implementación):**
- Forma: `MaterialTheme.shapes.medium` (8dp), sin borde, sin `tonalElevation` — mismo tratamiento que `ErrorState`/`SkeletonCard`. Nunca el tratamiento `emphasized` (ese es exclusivo de una tarjeta héroe persistente, no de feedback transitorio del sistema).
- Texto del cuerpo: `bodyMedium` normal, sin el tratamiento "sello" (mayúsculas+cursiva+negrita) de `SupportDeskBadge` — ese tratamiento degrada la legibilidad en frases largas, y la prioridad explícita de este rediseño es "fácil de entender" antes que decoración.
- **Corrección respecto al diseño original:** el plan original pedía relleno `semanticColors.success`/`danger` a color completo según severidad (con matemática de contraste ya verificada — ver `onSuccess`/`onDanger`, añadidos a `SupportDeskSemanticColors.kt` y conservados para uso futuro). Al implementar se descubrió que `SnackbarHostState.showSnackbar(String)` no lleva ningún campo de severidad, y los 5 tipos de efecto que ya existen (`TicketsUiEffect`/`TasksUiEffect`/`ProgramsUiEffect`/`BusinessFinanceUiEffect`/`BusinessSalesUiEffect`) usan `ShowMessage(String)` para éxito Y error indistintamente en ~30 sitios de emisión repartidos por ViewModels que incluyen funciones **fuera** del portal de cliente (Auth, Clients, Invoices, Dashboard, Notifications). Añadir un campo `isError` real habría significado tocar esos archivos no relacionados solo para poder pintar un Snackbar del color correcto — desproporcionado para Wave 0. Se implementó en su lugar **un único tratamiento neutro tinta/papel** (`colorScheme.onSurface` de fondo, `colorScheme.background` de texto — cero color nuevo, mismo par invertido que ya usa la identidad "sello de tinta" de la app) para las 8 fuentes existentes, y se documenta el color éxito/error ya verificado como el objetivo para cuando cada ViewModel gane su propio campo `isError` — el momento natural es la próxima vez que esa ViewModel se toque por su propia lógica (p. ej. `TasksUiEffect` durante el piloto de Wave 0.5).
- Icono: `Lucide.Info` (neutro, junto al mensaje siempre, no solo por color).
- Acción (si la hay, ej. "Reintentar"): texto en negrita simple, sin el chrome completo de `PrimaryButton`/`SecondaryButton` (que impondría 44dp+ de alto y un borde de tinta que competiría visualmente) — no implementado aún en Wave 0 (ningún caso actual lo necesita todavía).
- Duración/entrada-salida: usa el comportamiento por defecto de `SnackbarHost` de Material3 por ahora; la asimetría `SupportDeskMotion.emphasized`/`quick` propuesta originalmente queda como refinamiento pendiente, no bloqueante.

**Qué NO cambia:** `ClientNotice` no se retira. Sigue siendo el canal correcto para condiciones persistentes ligadas a estado actual (límite diario alcanzado, prioridad urgente activa) — se distingue del Snackbar por una prueba simple: ¿la condición tiene un estado final visible que la cierra (campos se reactivan, badge cambia), o pasó una vez y terminó? Lo primero es `ClientNotice`, lo segundo es Snackbar. `ErrorState`/`EmptyState`/`LoadingState` tampoco cambian — siguen siendo el canal para "el contenido principal de la pantalla falló/está vacío/está cargando", nunca se pliegan en el Snackbar (un toast perdido en una carga inicial fallida deja al usuario mirando una pantalla en blanco sin explicación).

## 5. `emphasized` en breakpoint compacto — sin cambios

Verificado dos veces (Visual + Reality Checker, con las mismas cifras: padding de página `spacing.xl`=24dp en las 5 pantallas que usan `emphasized=true`, shadow offset 4dp, borde 2dp — deja ~20dp de margen, invariante al ancho de pantalla porque ambos valores son fijos, no relativos). No se reduce en ningún breakpoint. Se amplía el KDoc de `ClientPortalSurfaceCard` para prohibir explícitamente `emphasized` dentro de un `Row`/grid multi-columna (nunca ocurre hoy, pero no estaba prohibido por escrito).

## 6. Patrón canónico de doble-envío

**Ratificado:** `Mutex.tryLock()` síncrono + `finally { unlock() }` (ya usado en `BusinessInvoicingViewModel`/`BusinessAccountingViewModel`, verificado genuinamente atómico porque `tryLock()` no es suspend y corre en el hilo llamador antes de que exista la corrutina). Se extrae a `BaseViewModel.guardedLaunch(mutex, block)`.

**Aplicación en Wave 0:** solo se toca `BaseViewModel.kt` (añadir el helper, cambio aditivo, no rompe nada existente) y `OperationsViewModel`/`BusinessOperationsFeature.kt` (ver §7 — necesario porque Wave 0 le añade su primer flujo de efectos). El resto de ViewModels con el patrón racy (`ProgramsViewModel`, 3 de Sales) o sin ningún guard (`TasksViewModel` en sus otras 5 funciones más allá de `createTask`) migran a `guardedLaunch` en su propia ola (Wave 0.5 para Tasks, Wave 1 para Programs, Wave 4 para Sales) — no se toca ahora para mantener Wave 0 acotado a fundamentos compartidos.

## 7. Alcance de Wave 0 para lógica — qué se arregla ahora vs. qué espera

Reality Checker distinguió dos hallazgos que sonaban igual de urgentes pero no lo son:

- **`OperationsUiState` (Bookings+Documents comparten `isLoading`/`isSaving`/`message`): SE ARREGLA AHORA.** Hoy está dormido (ninguna pantalla lee esos campos), pero Wave 0 es exactamente lo que le añade su primer flujo de efectos (§4) — si no se separa antes, Wave 0 es la ola que convierte un bug dormido en uno activo (un mensaje de Documentos podría mostrarse mientras el cliente mira Reservas, porque la ViewModel es un singleton de sesión cuyo scope sobrevive a la navegación). Se separa el estado en dos slices con un discriminante de sub-feature.
- **`BusinessFinanceUiContract` (`isLoading`/`isSaving`/`accessDenied`/`errorMessage` como 4 campos independientes): SE DOCUMENTA AHORA, SE REFACTORIZA EN WAVE 3.** Este bug ya está vivo hoy sin depender de nada de Wave 0 (confirmado reproducible: un `accessDenied=true` sobrevive sin resetear cuando un reintento falla por otra causa, y la UI muestra un error genérico seguido de un panel de "pide autorización" aunque el problema real fuera transitorio). Como no depende de Wave 0 para manifestarse, no bloquea Wave 0. Pero como Wave 0 ya toca estas dos ViewModels para añadirles su primer colector de efectos, se incluye el arreglo mínimo de una línea (resetear `accessDenied=false` en las 3 ramas `else`/`refreshOverviewAfterMutation` que no lo hacían) para que el nuevo Snackbar no amplifique un estado ya confuso. El refactor completo a un `sealed class BusinessFinanceStatus` (la arquitectura objetivo) se documenta aquí pero se implementa en Wave 3, cuando Facturación/Contabilidad se tocan a fondo.

## 8. Hallazgos adicionales confirmados, no bloqueantes para Wave 0 (van a su ola correspondiente)

- **Bug de bypass del límite diario de tareas** (`TasksViewModel.createTask()` nunca activa `isLoading` durante la llamada de red real; el guard visual de `ClientTasksScreen.kt` está correctamente cableado pero observa un flag que nunca cambia en la ventana vulnerable) — confirmado por 2 revisiones independientes. Refuerza que Tasks sea el piloto de Wave 0.5, ya planeado.
- **`TasksViewModel` tiene 3 `var` planas (`categories`/`tasks`/`logs`) escritas desde 7 corrutinas independientes sin coordinación** — mismo arreglo (`workspaceMutex` compartido) cierra esto y el bug de doble-envío a la vez. Wave 0.5.
- Mojibake confirmado más extenso de lo inicialmente citado (no solo 3 líneas por archivo) en los 3 archivos de Ventas. Wave 4, como ya estaba planeado.
- 8 sitios de `Button` crudo confirmados exactos en los 3 archivos de Operaciones. Wave 2, como ya estaba planeado.
- `TicketsUiEffect.TicketCreated` es código muerto hoy (un único consumidor en `AdminWorkspaceApp.kt` que no hace nada con él) — anotado para quien toque ese flujo en Wave 1, sin acción en Wave 0.

## 9. Checklist de implementación Wave 0

1. `SupportDeskBreakpoints.kt` — nuevos tokens (§1).
2. `NavigationComponents.kt:61` — referenciar `clientSidebarWidth` en vez del literal `284.dp`.
3. `SupportDeskSemanticColors.kt` — añadir `onSuccess`, `onDanger` (§4).
4. Migrar los 6 archivos con el bug estructural (§2) + desacoplar `stacked`/`cols` en `ClientTicketsScreen.kt` (§3).
5. Migrar los 5 literales sueltos (`ClientProgramsScreen.kt` ×4, `BusinessCustomersScreen.kt` ×1) a los tokens nuevos.
6. `ClientPortalDesign.kt` — ampliar KDoc de `emphasized` (§5), sin cambios de valores.
7. `BaseViewModel.kt` — añadir `guardedLaunch` (§6).
8. `BusinessOperationsFeature.kt` — separar estado Bookings/Documents, añadir `OperationsUiEffect` (§7).
9. `BusinessInvoicingViewModel.kt` / `BusinessAccountingViewModel.kt` — reset de `accessDenied` en 3 sitios (§7).
10. `AdminWorkspaceApp.kt` — `SnackbarHostState`, ~8 nuevos colectores de efectos, pasar el host a `ClientPortalScreen`.
11. `ClientPortalScreen.kt` — recibir el host, renderizar `SnackbarHost` anclado al panel de contenido, arreglo del umbral del shell (§2).
12. Compilar `:composeApp:compileKotlinJvm` y `:composeApp:compileDebugKotlinAndroid`.

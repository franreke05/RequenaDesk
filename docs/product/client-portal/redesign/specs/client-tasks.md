# ClientTasksScreen — Wave 0.5 (piloto)

## 1. Identidad
- Archivo: `composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/client/screens/ClientTasksScreen.kt`
- ViewModel: `shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/presentation/viewmodel/TasksViewModel.kt`
- Ruta: `ClientDestination.TASKS`
- Nivel: gratuito

## 2. Hallazgos (de la investigación previa, todos confirmados por lectura directa)
- `[LOGIC]` `TasksViewModel.createTask()` nunca ponía `isLoading=true` durante la llamada de red real — el botón "Añadir" ya estaba correctamente cableado a `state.isLoading` en la UI, pero observaba un flag que nunca cambiaba en la ventana vulnerable. Dos taps rápidos podían crear dos tareas y superar el límite diario anunciado.
- `[LOGIC]` `categories`/`tasks`/`logs` eran `var` planas escritas por `loadWorkspace()`, invocado desde 8 corrutinas independientes sin coordinación entre sí.
- `[UX]` Ninguna confirmación de éxito visible (existía `statusMessage`/`TasksUiEffect.ShowMessage` pero nada lo leía en el portal de cliente).
- `[UX]` Sin indicador de carga inicial — "Sin tareas" era indistinguible de "aún no ha cargado".

## 3. Máquina de estados
| Estado | Antes | Ahora |
|---|---|---|
| idle | Formulario habilitado | Sin cambio |
| cargando (inicial) | Sin indicador — pantalla en blanco | `LoadingState` si `isLoading && clientTasks.isEmpty() && categories.isEmpty()` (distingue de un create en curso sobre lista vacía, porque el botón "Añadir" exige una categoría cargada para habilitarse) |
| guardando (crear tarea) | Botón nunca se deshabilitaba realmente | `isLoading=true` real durante la llamada; botón deshabilitado + spinner (ya estaba bien cableado en la UI) |
| éxito | Sin confirmación visible | `TasksUiEffect.ShowMessage("Tarea creada")` ahora llega al Snackbar (colector añadido en Wave 0, `AdminWorkspaceApp.kt`) |
| error | `ClientNotice` inline | Sin cambio, ya era correcto |
| vacío | `EmptyState` genérico | Sin cambio |

## 4. Arreglos de lógica implementados
- `TasksViewModel.kt`: añadido `workspaceMutex: Mutex` compartido; `loadWorkspace()` convertido de función auto-lanzada a `suspend fun` (para que el mutex cubra toda la secuencia crear→refrescar, no solo el primer tramo); las 8 funciones mutantes (`Load`, `createLabel`, `updateLabel`, `deleteLabel`, `createTask`, `executeTaskUpdate`, `deleteTask`, `stopTimer`) ahora usan `guardedLaunch(workspaceMutex)` en vez de `launch` crudo — cierra doble-envío y la carrera de las `var` planas con un solo mecanismo, siguiendo el patrón canónico ratificado en Wave 0 (`BaseViewModel.guardedLaunch`).
- `createTask()` ahora pone `isLoading=true` en el mismo `_state.update` donde ya limpiaba `errorMessage`/`lastCreatedTaskId`.

## 5. Cambios en la pantalla
- `ClientTasksScreen.kt`: añadido `LoadingState(itemCount = 3)` gated como se describe arriba. El botón "Añadir" no necesitó cambios — ya estaba bien construido, solo le faltaba una señal real.

## 6. Responsive
Sin cambios — esta pantalla no tenía branching de ancho y no lo necesitaba (formulario + dos listas, ya de una sola columna en todos los tamaños).

## 7. Consolidación de componentes compartidos
`TaskRow` sigue siendo un componente a medida (no usa `ClientPortalSurfaceCard`) — queda para Wave 1 si se decide consolidar.

## 8. Verificación
`./gradlew :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid` — ambos limpios tras el cambio.

## 9. Backlog (no accionado esta ronda)
- El límite diario de tareas solo se valida en cliente (`todayTaskCount` derivado de datos ya cargados); no se verificó si el servidor lo revalida de forma independiente — documentado en la auditoría de backend original, sin tocar el servidor esta ronda.

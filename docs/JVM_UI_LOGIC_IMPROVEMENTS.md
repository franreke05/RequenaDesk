# Mejora integral de UI y lógica JVM

Fecha de entrega: 2026-07-14

## Objetivo

Hacer funcional y coherente la aplicación Compose Desktop de OryKai software de extremo a extremo: autenticación, navegación, pantallas administrativas, portal cliente, acceso a Ktor y persistencia PostgreSQL.

La revisión se realizó con las skills locales de Compose, estado y efectos, UI móvil/escritorio, revisión visual y conexión de interfaces administrativas a datos reales. Una auditoría secundaria recorrió las pantallas y los contratos Ktor para localizar rutas rotas, datos simulados y problemas de autorización antes de implementar los cambios.

## Resultado

- La clase de entrada JVM es `com.requena.supportdesk.MainKt`. Está configurada tanto para Compose `run` como para Kotlin `jvmRun`, que es la tarea usada por Android Studio en el error original.
- La ventana abre a 1440 x 900 y mantiene un mínimo de 1024 x 720 para proteger las composiciones densas.
- Administradores y clientes pueden iniciar sesión desde el mismo formulario y reciben su espacio según el rol devuelto por Ktor.
- Clientes, tareas, etiquetas, tickets, actividad, horas y facturas consumen datos reales del servidor.
- El servidor limita cada identidad a sus propios clientes y recursos. El cliente no puede falsificar un `clientId` en el cuerpo.
- Los formularios conservan el borrador durante los errores y solo navegan o se limpian tras una respuesta satisfactoria.
- Se eliminaron rutas inexistentes, mapeos con usuarios/fechas fijos y acciones de demostración expuestas en producción.

## Sistema de interfaz

La UI JVM usa una base común de Material 3 con estas decisiones:

- Paleta neutral de alto contraste, verde para acción principal, azul para información y colores semánticos para éxito, aviso y error.
- Superficies planas, sin fondos degradados decorativos.
- Radios entre 4 y 8 dp, espaciado compartido y dimensiones estables para navegación, calendarios, tarjetas y botones.
- Iconografía Lucide CMP en navegación, búsquedas, visibilidad de contraseña, actividad y acciones conocidas.
- Botones con objetivo mínimo de 44 dp, estado de carga y bloqueo de doble envío.
- Estados explícitos de carga, vacío, error recuperable y contenido.
- Navegación adaptativa: sidebar expandida, rail intermedio y barra inferior compacta.

La integración de iconos usa `com.composables:icons-lucide-cmp:2.2.1`. Referencias: [Compose Icons](https://github.com/composablehorizons/compose-icons) y [accesibilidad en Compose](https://developer.android.com/develop/ui/compose/accessibility).

## Pantallas administrativas

| Pantalla | Comportamiento entregado |
| --- | --- |
| Inicio de sesión | Validación de correo, contraseña visible/oculta, acciones de teclado, carga, error y acceso por rol. |
| Dashboard | Clientes reales, tareas y horas del mes actual, calendario estable y métricas sin duplicados. |
| Clientes | Listado, búsqueda, alta, selección y estados de carga/error; el formulario solo se reinicia tras éxito. |
| Tareas | Filtros, calendario, alta, edición, cambio de cliente y registro de horas. Una edición produce una única actualización Ktor. |
| Tickets | Cola con filtros, alta, detalle, conversación, estado y prioridad. Se muestran solicitante, fechas, mensajes, comentarios, eventos y adjuntos persistidos. |
| Etiquetas | Gestión conectada a Ktor y al propietario administrativo de los datos. |
| Facturas | Carga bajo demanda, listado y detalle tanto en ancho expandido como compacto, alta validada, actualización de estado y apertura del PDF. |

## Pantallas del cliente

| Pantalla | Comportamiento entregado |
| --- | --- |
| Inicio | Resumen obtenido de todos los tickets del cliente, accesos rápidos y estados de carga correctos. |
| Nuevo ticket | Identidad impuesta por el servidor, límite diario urgente, validación, carga y conservación del borrador hasta éxito. |
| Tickets | Búsqueda, filtros, detalle y conversación con datos persistidos; los filtros no alteran las métricas globales. |
| Tareas | Solo tareas propias, creación limitada por día y cambio de completado autorizado por el servidor. |
| Tablero | Columnas estables, estado vacío/error y navegación al ticket sin scroll anidado ilimitado. |
| Mi servicio | Métricas de tickets y horas reales, sin entradas de tiempo inventadas. |
| Actividad | Eventos reales agrupados por fecha, filtros temporales e iconos semánticos accesibles. |
| Mi cuenta | Empresa, contacto, producto, plan, estado, canal y métricas procedentes del cliente autenticado. |
| Facturas | Solo facturas emitidas o pagadas del cliente, detalle real, error recuperable y descarga de PDF. |

La carga de facturas se ejecuta al entrar en su pantalla. Así, una indisponibilidad puntual de ese módulo no contamina el dashboard ni el resto del portal.

## Contratos Ktor y permisos

| Recurso | Administrador | Cliente |
| --- | --- | --- |
| Clientes | Consulta y mutación dentro de su propiedad | Consulta únicamente su ficha |
| Etiquetas | Consulta y mutación | Consulta de las etiquetas del propietario |
| Tareas | Consulta y mutación | Consulta/alta propia y cambio de completado propio |
| Horas | Consulta y alta | Consulta de sus propias horas |
| Tickets | Consulta, alta, respuesta, estado y prioridad | Consulta, alta y respuesta únicamente sobre tickets propios |
| Facturas | Consulta, alta y cambio de estado | Consulta y PDF únicamente de facturas propias |

Las reglas sensibles se aplican en el servidor, no solo en la UI:

- Máximo de 5 tareas creadas por cliente y día.
- Máximo de 3 tickets urgentes creados por cliente y día.
- El `clientId` enviado por un cliente se sustituye por el de su identidad autenticada.
- Las respuestas, detalles y PDFs verifican propiedad antes de devolver datos.
- La descarga en navegador acepta el token de sesión como parámetro porque `Desktop.browse` no permite añadir `Authorization`. Como endurecimiento futuro conviene cambiarlo por un token firmado de un solo uso y vida corta.

## Datos de tickets

El contrato compartido y las respuestas Ktor incluyen ahora:

- `clientId`, solicitante, asignado, fechas reales y pasos para reproducir.
- Mensajes, comentarios internos, eventos y adjuntos persistidos.
- Recarga del ticket tras responder para evitar mensajes locales que no existen en base de datos.
- Hidratación equivalente en repositorios PostgreSQL e in-memory para desarrollo y pruebas.

## Verificación

Comandos ejecutados:

```powershell
.\gradlew.bat :composeApp:compileKotlinJvm :server:compileKotlin :server:test --rerun-tasks
.\gradlew.bat test --stacktrace
git diff --check
```

Resultado:

- Compilación Compose Desktop: correcta.
- Compilación Ktor: correcta.
- Pruebas del servidor: 32 pruebas, 0 fallos, 0 errores.
- Migraciones PostgreSQL verificadas con PostgreSQL embebido.
- Pruebas nuevas para acceso del portal cliente y prevención de suplantación de `clientId`.
- Diff sin errores de espacios en blanco.
- Smoke test de `:composeApp:jvmRun`: proceso `MainKt`, ventana visible de 1440 x 900 y render no vacío.

El 2026-07-14 se comprobó que el servidor público configurado en el equipo (`https://crm.franciscorequena.cloud`) todavía devuelve 404 para `/admin/invoices`, aunque clientes, etiquetas, tareas, horas y tickets responden 200. La ruta de facturas existe y está probada en este código; para habilitarla en el dominio hay que desplegar/reiniciar esta versión del servidor Ktor detrás de Caddy.

Se mantienen dos avisos de migración futura del toolchain: compatibilidad de Kotlin Multiplatform con Android Gradle Plugin 9 y clases `expect/actual` todavía marcadas como beta. No bloquean el build actual.

## Ejecución local

1. Crear `supportdesk.properties` a partir de `supportdesk.server.properties.example` y completar base de datos, secreto de autenticación y credenciales iniciales.
2. Iniciar Ktor:

```powershell
.\start-ktor-server.ps1 -Development
```

3. Iniciar Compose Desktop:

```powershell
.\gradlew.bat :composeApp:jvmRun
```

La configuración detallada de PostgreSQL, migraciones y Caddy está en [KTOR_POSTGRES_CADDY_REPAIR.md](./KTOR_POSTGRES_CADDY_REPAIR.md).

## Archivos principales

- `composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/AdminWorkspaceApp.kt`
- `composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/`
- `composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/client/`
- `composeApp/src/commonMain/kotlin/com/requena/supportdesk/designsystem/`
- `shared/src/commonMain/kotlin/com/requena/supportdesk/features/`
- `server/src/main/kotlin/com/requena/supportdesk/server/routes/`
- `server/src/main/kotlin/com/requena/supportdesk/server/data/repository/`
- `server/src/test/kotlin/com/example/crmfreelance/ApplicationTest.kt`

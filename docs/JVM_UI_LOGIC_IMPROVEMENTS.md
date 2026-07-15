# Mejora integral de UI y lógica JVM

Fecha de entrega: 2026-07-14

## Objetivo

Hacer funcional y coherente la aplicación Compose Desktop de OryKai software de extremo a extremo: autenticación, navegación, pantallas administrativas, portal cliente, acceso a Ktor y persistencia PostgreSQL.

La revisión se realizó con las skills locales de Compose, estado y efectos, UI móvil/escritorio, revisión visual y conexión de interfaces administrativas a datos reales. Una auditoría secundaria recorrió las pantallas y los contratos Ktor para localizar rutas rotas, datos simulados y problemas de autorización antes de implementar los cambios.

## Resultado

- La clase de entrada JVM es `com.requena.supportdesk.MainKt`. Está configurada tanto para Compose `run` como para Kotlin `jvmRun`, que es la tarea usada por Android Studio en el error original.
- La ventana abre a 1440 x 900 y mantiene un mínimo de 1024 x 720 para proteger las composiciones densas.
- Administradores y clientes pueden iniciar sesión desde el mismo formulario y reciben su espacio según el rol devuelto por Ktor.
- Clientes, tareas, etiquetas, tickets, actividad y horas consumen datos reales del servidor. La creación y biblioteca de facturas son exclusivamente locales.
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
| Facturas | Biblioteca de PDFs locales, formulario bajo demanda, una o varias tareas por cliente, horas redondeadas hacia arriba y apertura con el visor PDF del sistema. |

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
| Facturas | PDF local generado bajo demanda, sin persistir facturas ni líneas en la base de datos. |

La biblioteca de facturas lee la carpeta local al crear el ViewModel y cuando el usuario pulsa `Actualizar`. Crear, listar y abrir facturas no requiere que Ktor esté disponible.

## Contratos Ktor y permisos

| Recurso | Administrador | Cliente |
| --- | --- | --- |
| Clientes | Consulta y mutación dentro de su propiedad | Consulta únicamente su ficha |
| Etiquetas | Consulta y mutación | Consulta de las etiquetas del propietario |
| Tareas | Consulta y mutación | Consulta/alta propia y cambio de completado propio |
| Horas | Consulta y alta | Consulta de sus propias horas |
| Tickets | Consulta, alta, respuesta, estado y prioridad | Consulta, alta y respuesta únicamente sobre tickets propios |
| Facturas (fuera de Ktor) | Genera PDFs locales desde tareas seleccionadas, sin endpoint, CRUD ni persistencia | Sin persistencia de facturas ni líneas |

Las reglas sensibles se aplican en el servidor, no solo en la UI:

- Máximo de 5 tareas creadas por cliente y día.
- Máximo de 3 tickets urgentes creados por cliente y día.
- El `clientId` enviado por un cliente se sustituye por el de su identidad autenticada.
- Las respuestas y detalles de recursos remotos verifican propiedad antes de devolver datos.

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
.\gradlew.bat :shared:jvmTest :composeApp:compileKotlinJvm --no-daemon
.\gradlew.bat :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid --no-daemon
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
- Prueba JVM de factura con dos tareas: crea el PDF sin servidor, redondea 1,1 horas a 2, verifica el total, extrae y comprueba su texto y vuelve a encontrarlo al listar la carpeta.
- Prueba del ViewModel: el evento `GenerateInvoice` guarda localmente, refresca la biblioteca y entrega un único efecto de éxito.
- Compilación de los contratos `expect/actual` de facturas para JVM y Android: correcta.

El 2026-07-15 se confirmó que el error de descarga procedía de mantener la creación ligada a `/admin/invoices/pdf` y a un fallback HTML del servidor. Ambas dependencias se eliminaron del cliente: el formulario entrega los datos directamente al almacenamiento JVM, PDFBox genera el documento y una escritura temporal seguida de movimiento atómico lo deja en la carpeta local. También se creó con éxito una factura de humo real en `Escritorio/Facturas OryKai` con dos tareas.

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

## Facturas locales (decisión de producto)

Las facturas no son entidades persistidas. Se generan bajo demanda como PDF y se guardan solo en `Escritorio/Facturas OryKai`; la pantalla lee esa carpeta local y abre el archivo seleccionado con el visor PDF predeterminado de Windows. Tampoco se conserva una secuencia o contador de factura en PostgreSQL: la numeración efímera se crea en el escritorio con el año y un identificador aleatorio.

El formulario permite incluir una o varias tareas del cliente elegido. Cada tarea crea su propia línea usando las horas registradas, mostradas únicamente como horas completas y redondeadas siempre hacia arriba. No se guardan en base de datos la factura, sus líneas, la ruta local ni la URL temporal de descarga.

### Flujo de creación

1. `AdminInvoicesScreen` permite elegir el cliente y añadir una, varias o todas las tareas que tengan tiempo registrado.
2. La UI convierte los segundos registrados en horas facturables completas mediante redondeo hacia arriba y construye un `CreateInvoiceInput` con todas las líneas.
3. `InvoicesViewModel` recibe `GenerateInvoice` y llama únicamente a `InvoicePdfStorage.saveInvoice`; no construye URL, no usa sesión y no llama a un repositorio remoto.
4. `DesktopInvoicePdfStorage` valida la entrada, calcula importes, genera el PDF con PDFBox y lo mueve atómicamente a la carpeta de facturas.
5. El ViewModel vuelve a leer esa carpeta y la pantalla muestra el PDF. `Abrir PDF` delega en `Desktop.open`, que usa el visor predeterminado de Windows.

No existe fallback HTTP para facturas. Los antiguos data source, DTO, repositorio y caso de uso remoto de facturas se eliminaron del cliente compartido.

### Logs de diagnóstico

Cada operación emite logs estructurados sin volcar el contenido completo del formulario:

- `invoice_pdf.create.start`: comienza la creación e indica `operationId` y número de líneas.
- `invoice_pdf.create.write`: informa del archivo de destino antes de escribir.
- `invoice_pdf.create.success`: confirma nombre y tamaño del PDF.
- `invoice_pdf.create.failure`: conserva el mismo `operationId` y adjunta la excepción completa.
- `invoice_pdf.list.start/success/failure`: diagnostica la lectura de la biblioteca.
- `invoice_pdf.open.start/success/failure`: diagnostica la apertura en el visor del sistema.

El `operationId` permite seguir un intento concreto desde el clic hasta el resultado final en la consola de Android Studio o en la salida del proceso JVM.

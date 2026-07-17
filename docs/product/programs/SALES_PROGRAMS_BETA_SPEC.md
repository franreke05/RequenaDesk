# Especificación beta — Programas de gestión comercial

**Estado:** especificación aprobable; no implementa código, rutas ni migraciones.  
**Ámbito:** tres programas gratuitos y solicitables desde el portal: **Clientes y contactos**, **Presupuestos y ventas**, y **Catálogo de productos, servicios y stock**.  
**Fecha:** 2026-07-17.

## 1. Decisión de producto

Durante la beta los tres programas son gratuitos: **0 €/mes**, sin cargo retroactivo, sin renovación automática de pago y sin añadir líneas a facturas existentes. El cliente solicita cada programa y el administrador responsable de la cuenta autoriza o rechaza la activación. La autorización crea un acceso activo; no es una venta ni una aceptación de condiciones de cobro.

Los productos se identificarán por claves estables, sin `BETA` en la clave para que no haya que migrar datos al salir de beta:

| Clave | Nombre visible | Propósito beta | Dependencia |
|---|---|---|---|
| `CUSTOMER_CONTACTS` | Clientes y contactos | Agenda comercial de la empresa usuaria. | Ninguna |
| `QUOTES_SALES` | Presupuestos y ventas | Crear presupuestos, seguir su estado y convertir un aceptado en venta interna. | Ninguna; puede enlazar clientes y catálogo si están activos. |
| `CATALOG_STOCK` | Catálogo, servicios y stock | Productos, servicios y movimiento de stock trazable. | Ninguna |

El término **empresa usuaria** designa al `client_id` del portal de OryKai. El término **cliente comercial** designa a una persona o empresa que esa empresa usuaria gestiona dentro del programa. Esta distinción es obligatoria: la tabla actual `clients` representa los clientes de OryKai y `client_contacts` (V6) son contactos internos del CRM de OryKai; ninguno de los dos debe reutilizarse ni exponerse para esta beta.

### Límites intencionales de la beta

- No hay facturación electrónica, cobros, pagos, e-signature, contabilidad, IVA liquidable, compras, proveedores, multi-almacén, lotes, números de serie ni reservas de stock.
- Un presupuesto es un documento comercial interno y no una factura ni un contrato. El cliente debe revisarlo antes de enviarlo por sus propios canales.
- Una venta confirmada puede descontar stock; un presupuesto o una venta en borrador no reservan stock.
- No se habilita exportación masiva, subida de ficheros ni envío de email en la beta. Se decidirán tras validar retención, permisos y tratamiento de datos.
- No habrá conversión automática de beta a pago. Al terminar, el acceso se suspende o se ofrece una conversión explícita, con precio y aprobación separados.

## 2. Punto de partida inspeccionado

| Capa | Base disponible | Implicación para la beta |
|---|---|---|
| Portal Compose | `ClientPortalScreen` ya prioriza `Inicio`, `Trabajo`, `Programas`, `Cuenta`; `Solicitar` y `Servicio` son destinos internos. | Los programas beta viven dentro de `Programas`, no añaden destinos primarios. |
| Shared KMP | `features/programs` ya usa DTO → mapper → repositorio → casos de uso → `ProgramsViewModel` con `StateFlow`, eventos y efectos. | Cada programa debe seguir la misma forma y mantener modelos de dominio fuera de Compose. |
| Ktor | `ProgramRoutes` devuelve catálogo, solicitudes y suscripciones; el cliente pide y el administrador decide. | Se extiende el guard de entitlement; la UI nunca es la autorización. |
| PostgreSQL | V8 introduce `product_catalog`, solicitudes, suscripciones y auditoría. V7/V8 habilitan RLS y revocan acceso directo de roles Supabase. | La siguiente migración sólo añadirá tablas de datos beta y endurecerá las mismas protecciones. |
| Seguridad | JWT propio de Ktor incorpora `clientId`; Caddy es proxy local de confianza; operaciones sensibles tienen rate limit. | Toda lectura/escritura deriva el tenant de la identidad, nunca del cuerpo o un parámetro del cliente. |
| Facturas | Las facturas PDF se generan y guardan localmente en escritorio; no son un dominio de suscripciones. | La beta no toca facturas ni el preview de facturación. |

El patrón coincide con las guías locales de KMP: estado inmutable, eventos unidireccionales, repositorios con modelos de dominio y lógica compartida en `commonMain`. El cliente Ktor existente conserva autenticación y mapea datos en el límite de repositorio; no se añadirá una segunda pila de red.

## 3. Flujos de usuario

### 3.1 Solicitud y autorización gratuita

```text
Cliente del portal
  → Programas: selecciona uno o varios programas beta
  → revisa “Beta gratuita · 0 €/mes · sin cobro automático”
  → envía solicitud opcionalmente comentada
  → solicitud REQUESTED

Administrador propietario de la cuenta
  → cola de solicitudes
  → aprueba o rechaza con nota opcional
  → al aprobar: suscripción ACTIVE con precio 0 y evento de auditoría
  → el portal actualiza el entitlement y habilita la utilidad
```

Reglas:

1. El servidor obtiene el `client_id` del JWT del cliente; el cliente no puede pedir un programa para otra empresa.
2. El administrador sólo decide solicitudes de empresas que le pertenecen según la relación existente de propietario administrativo.
3. Para un producto `BETA_FREE`, el precio no viaja desde la app ni se puede editar en la pantalla de aprobación. El servidor fija `0` y registra el modo beta.
4. Una solicitud `REQUESTED` por pareja `(client_id, product_key)` es única. Reintentos idénticos deben devolver el resultado idempotente o un conflicto legible, nunca crear duplicados.
5. Rechazar no borra la solicitud ni el evento. El cliente puede crear una nueva solicitud posterior, con nuevo motivo si aplica.
6. La aceptación de beta no autoriza a la plataforma a cobrar. Cualquier conversión futura requiere una solicitud de producto de pago independiente.

### 3.2 Clientes y contactos

1. Con `CUSTOMER_CONTACTS` activo, el usuario abre **Clientes y contactos** desde Programas.
2. Ve búsqueda, filtros Activos/Archivados y una lista de clientes comerciales con contacto principal y última actualización.
3. Crea un cliente comercial con nombre; los datos de contacto son opcionales y mínimos.
4. Desde el detalle añade, edita, marca un contacto principal o archiva el cliente comercial.
5. Archivar conserva la trazabilidad de presupuestos y ventas; no hay borrado físico desde la interfaz beta.

### 3.3 Catálogo, servicios y stock

1. Con `CATALOG_STOCK` activo, el usuario abre **Catálogo y stock**.
2. Puede crear un producto con SKU opcional, o un servicio sin seguimiento de stock.
3. Un producto con seguimiento de stock muestra existencias, mínimo y últimos movimientos; un servicio nunca muestra stock.
4. La persona añade stock inicial o realiza un ajuste indicando motivo. El sistema crea un movimiento inmutable, no una edición silenciosa de la existencia.
5. Si se confirma una venta con líneas de producto, el sistema registra el movimiento de salida dentro de la misma transacción.

### 3.4 Presupuestos y ventas

1. Con `QUOTES_SALES` activo, el usuario abre **Presupuestos y ventas** y ve borradores, enviados, aceptados, rechazados y ventas confirmadas.
2. Crea un borrador con cliente comercial si `CUSTOMER_CONTACTS` está activo; de lo contrario puede guardar el nombre y datos de comprador como una instantánea en el documento, sin crear una agenda paralela.
3. Añade líneas manuales o selecciona productos/servicios si `CATALOG_STOCK` está activo. La línea guarda descripción, precio, descuento e impuesto como instantánea.
4. El servidor recalcula importes; el cliente no envía subtotal, impuestos ni total autoritativos.
5. Sólo un borrador se puede editar. El usuario lo marca como enviado, aceptado, rechazado o caducado según el flujo permitido.
6. Un presupuesto aceptado se convierte una única vez en venta interna. Si existen líneas de stock, la conversión se bloquea con explicación si no hay unidades suficientes. Si tiene éxito, venta, líneas y movimientos de stock se guardan atómicamente.

## 4. Datos y fuente de verdad

### 4.1 Entitlements beta

`product_catalog` debe expresar de forma autoritativa el modo comercial del producto: `BETA_FREE` o `PAID`. Durante la beta los tres productos son `BETA_FREE`, disponibles y solicitables, con precio mensual `0`. La lógica de autorización consulta catálogo y suscripción activa, no copia una bandera de acceso a Compose.

La aprobación de beta necesita permitir precio cero sólo cuando el catálogo declara `BETA_FREE`. La validación actual para aprobaciones de pago no debe relajarse de forma global. Un producto pagado conserva precio explícito y su flujo actual.

### 4.2 Entidades de negocio propuestas

| Grupo | Entidad | Campos y reglas esenciales |
|---|---|---|
| Agenda | Cliente comercial | ID UUID, `client_id` tenant, razón/nombre comercial, identificador fiscal opcional, email/teléfono/dirección opcionales, estado activo/archivado, autor y fechas. Nombre obligatorio; identificación fiscal no se exige en beta. |
| Agenda | Contacto comercial | ID UUID, `client_id`, cliente comercial, nombre, cargo opcional, email/teléfono opcionales, bandera de principal, activo/archivado y auditoría. Un único contacto principal activo por cliente comercial. |
| Catálogo | Producto o servicio | ID UUID, `client_id`, tipo `PRODUCT`/`SERVICE`, nombre, SKU opcional normalizado, descripción, unidad, precio en céntimos EUR, activo, `tracks_stock`, mínimo de stock opcional. Un servicio no puede seguir stock. |
| Inventario | Movimiento de stock | ID UUID, `client_id`, producto, tipo `INITIAL`/`ADJUSTMENT`/`SALE`/`RETURN`, delta decimal, motivo o referencia de documento, actor y fecha. Es inmutable; una corrección crea otro movimiento. |
| Ventas | Presupuesto | ID UUID, `client_id`, número por tenant, comprador comercial opcional, datos de comprador instantáneos, estado, moneda EUR, emisión/validez, notas, totales calculados, autor y fechas de transición. |
| Ventas | Línea de presupuesto | Presupuesto y `client_id`, posición, producto fuente opcional, descripción instantánea, cantidad decimal, precio unitario en céntimos, descuento/impuesto permitidos, subtotales calculados. |
| Ventas | Venta y líneas | Conversión única desde presupuesto aceptado o venta manual controlada; conserva instantáneas de comprador y líneas, estado y fecha de confirmación. Las salidas de stock enlazan la venta. |
| Infraestructura | Secuencia documental | Contador por `client_id` y tipo de documento para números de presupuesto/venta sin colisiones. |
| Infraestructura | Idempotencia | Clave de operación por tenant para creación de presupuesto, ajustes y conversión a venta; conserva respuesta segura el tiempo de retención definido. |
| Auditoría | Evento de programa | Tenant, tipo/ID de entidad, actor, acción, fecha y metadatos no sensibles (campos cambiados, no valores completos). |

Todas las entidades de negocio guardan `client_id` aunque puedan derivarlo de una cabecera. Las relaciones compuestas o verificaciones equivalentes deben impedir que una línea, contacto o movimiento enlace una entidad de otro tenant.

### 4.3 Reglas de integridad y cálculo

- Importes: enteros en céntimos y moneda `EUR`; nunca `Float`/`Double`. Cantidades decimales se transportan como texto decimal con máximo tres decimales y se procesan con precisión decimal en servidor.
- La línea mantiene una instantánea de descripción, precio, descuento e impuesto. Editar el catálogo no altera presupuestos ni ventas pasados.
- La existencia se calcula a partir del libro de movimientos o de un resumen mantenido transaccionalmente; no es un campo editable de saldo.
- No se permiten saldos negativos en beta. Un ajuste negativo o venta que dejaría existencias por debajo de cero falla con conflicto y detalle de producto/cantidad disponible.
- Convertir un presupuesto a venta bloquea la cabecera y productos afectados en la misma transacción. La operación es idempotente: una segunda llamada devuelve la venta ya creada, no duplica salidas de stock.
- Los números de documento se asignan en el servidor dentro de la transacción y son únicos por tenant y tipo; nunca se derivan de un contador de Compose.
- Archivar preserva referencias históricas. La eliminación física queda fuera de beta y requiere política de retención.

## 5. API propuesta

Todos los payloads siguen el sobre de respuesta actual y serialización Kotlin existente. Las listas se paginan con cursor, límite acotado y filtro allowlist; no se exponen datos por `client_id` aportado por el llamador.

### 5.1 Activación de programas

| Método y ruta | Actor | Resultado |
|---|---|---|
| `GET /client/programs` | Cliente autenticado | Catálogo con modo comercial, estado de solicitud/suscripción y distintivo `Beta gratuita`. |
| `POST /client/program-requests` | Cliente autenticado | Solicita hasta ocho claves distintas; para beta no incluye precio. |
| `GET /admin/program-requests` | Admin propietario | Cola filtrable por estado y producto, con contexto de empresa. |
| `POST /admin/program-requests/{requestId}/approve` | Admin propietario | Activa beta a 0 € sólo si catálogo es `BETA_FREE`; para pago conserva validación explícita de precio. |
| `POST /admin/program-requests/{requestId}/reject` | Admin propietario | Marca rechazo con nota opcional y evento auditado. |

### 5.2 Clientes y contactos

| Método y ruta | Programa requerido | Operación |
|---|---|---|
| `GET /client/apps/customers` | `CUSTOMER_CONTACTS` | Búsqueda, estado y paginación de clientes comerciales. |
| `POST /client/apps/customers` | `CUSTOMER_CONTACTS` | Crea cliente comercial. |
| `GET /client/apps/customers/{customerId}` | `CUSTOMER_CONTACTS` | Detalle sólo del tenant propio. |
| `PATCH /client/apps/customers/{customerId}` | `CUSTOMER_CONTACTS` | Edita campos permitidos y control de versión. |
| `POST /client/apps/customers/{customerId}/archive` | `CUSTOMER_CONTACTS` | Archivo reversible en beta, no borrado. |
| `POST /client/apps/customers/{customerId}/contacts` | `CUSTOMER_CONTACTS` | Añade contacto. |
| `PATCH /client/apps/customers/{customerId}/contacts/{contactId}` | `CUSTOMER_CONTACTS` | Edita, archiva o cambia principal. |

### 5.3 Catálogo y stock

| Método y ruta | Programa requerido | Operación |
|---|---|---|
| `GET /client/apps/catalog/items` | `CATALOG_STOCK` | Lista productos/servicios con filtros y cursor. |
| `POST /client/apps/catalog/items` | `CATALOG_STOCK` | Crea producto o servicio. |
| `PATCH /client/apps/catalog/items/{itemId}` | `CATALOG_STOCK` | Edita datos de catálogo, nunca movimientos históricos. |
| `POST /client/apps/catalog/items/{itemId}/archive` | `CATALOG_STOCK` | Archiva el ítem; no modifica documentos previos. |
| `GET /client/apps/catalog/stock` | `CATALOG_STOCK` | Resumen de existencias y bajo mínimo. |
| `GET /client/apps/catalog/items/{itemId}/movements` | `CATALOG_STOCK` | Libro paginado de movimientos. |
| `POST /client/apps/catalog/items/{itemId}/adjustments` | `CATALOG_STOCK` | Crea ajuste con clave de idempotencia, delta y motivo. |

### 5.4 Presupuestos y ventas

| Método y ruta | Programa requerido | Operación |
|---|---|---|
| `GET /client/apps/sales/quotes` | `QUOTES_SALES` | Lista de presupuestos por estado, búsqueda y cursor. |
| `POST /client/apps/sales/quotes` | `QUOTES_SALES` | Crea borrador y líneas; el servidor calcula totales. |
| `GET /client/apps/sales/quotes/{quoteId}` | `QUOTES_SALES` | Detalle propio. |
| `PATCH /client/apps/sales/quotes/{quoteId}` | `QUOTES_SALES` | Sólo borrrador y con versión esperada. |
| `POST /client/apps/sales/quotes/{quoteId}/mark-sent` | `QUOTES_SALES` | Transición a enviado. |
| `POST /client/apps/sales/quotes/{quoteId}/mark-accepted` | `QUOTES_SALES` | Transición confirmada por la empresa usuaria; no es firma externa. |
| `POST /client/apps/sales/quotes/{quoteId}/mark-rejected` | `QUOTES_SALES` | Rechaza con motivo opcional. |
| `POST /client/apps/sales/quotes/{quoteId}/convert-to-sale` | `QUOTES_SALES` | Conversión idempotente y, si procede, salida de stock transaccional. |
| `GET /client/apps/sales/sales` | `QUOTES_SALES` | Lista ventas internas confirmadas/canceladas. |
| `GET /client/apps/sales/sales/{saleId}` | `QUOTES_SALES` | Detalle propio y referencia al presupuesto. |

Los identificadores de ruta se validan como UUID. Un error de pertenencia devuelve `404` o `403` según la convención ya usada, sin revelar que el identificador exista en otro tenant.

## 6. Permisos, RLS y privacidad

### 6.1 Matriz de permisos

| Acción | Cliente del portal | Admin propietario de OryKai | Sin sesión / otro tenant |
|---|---|---|---|
| Solicitar beta para su empresa | Sí | No necesaria | Denegar |
| Aprobar/rechazar programa | No | Sí, sólo empresas propias | Denegar |
| Leer/escribir sus datos comerciales | Sí, sólo con programa activo y `client_id` del token | No por defecto | Denegar |
| Ver uso agregado de beta | Sí, de su empresa | Sí, de sus empresas | Denegar |
| Ver contenido comercial de una empresa | Sí, propio | No por defecto; futuro acceso de soporte explícito y auditado | Denegar |

La separación de contenido comercial y CRM interno es deliberada. El administrador autoriza el programa y ve la activación, pero no obtiene acceso automático a las agendas, precios o ventas de la empresa usuaria. Si se necesita soporte asistido en el futuro, requerirá consentimiento contextual, alcance, caducidad y auditoría aparte.

### 6.2 Controles obligatorios

1. Cada handler obtiene identidad y `clientId` del JWT antes de cargar recursos. No acepta `client_id`, precio, total, stock ni identidad de actor como autoridad desde el cuerpo.
2. Un guard reutilizable de servidor comprueba suscripción `ACTIVE` para la clave requerida antes de cada ruta de negocio. Ocultar una tarjeta Compose no equivale a permiso.
3. El guard se ejecuta también en rutas auxiliares, conversiones y búsquedas de catálogo; no basta con proteger la pantalla principal.
4. Cada consulta SQL filtra por `client_id` derivado. Las mutaciones usan `WHERE id = ? AND client_id = ?` y comprueban filas afectadas.
5. Los administradores comprueban pertenencia al propietario antes de decidir una solicitud o consultar métricas. No se acepta un `ownerAdminId` procedente de HTTP.
6. Las operaciones de creación, ajuste de stock, cambio de estado y conversión usan rate limit sensible, límites de tamaño y claves de idempotencia.
7. Las tablas beta activan RLS y revocan `PUBLIC`, `anon` y `authenticated`, igual que V7/V8. La aplicación usa autenticación Ktor propia, no Supabase Auth; por tanto no se inventan políticas `auth.uid()` que no correspondan a esta arquitectura.
8. Las vistas, funciones y secuencias nuevas se revocan igual que las tablas. Ningún rol de Data API recibe privilegios directos.
9. Los logs, analítica y auditoría no guardan emails, teléfonos, notas completas, descripciones de líneas ni tokens. Guardan IDs, acción, actor, fecha y metadatos mínimos.

PostgreSQL exige habilitar RLS por tabla y las políticas restringen las filas visibles o modificables; los propietarios pueden eludir RLS salvo configuración adicional. En esta aplicación, el bloqueo de roles públicos y la autorización Ktor por tenant son el límite de seguridad efectivo, y deben verificarse juntos. [PostgreSQL: Row Security Policies](https://www.postgresql.org/docs/15/ddl-rowsecurity.html)

OWASP recomienda denegar por defecto y validar autorización en cada petición, incluso si la UI ya oculta una acción. [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)

### 6.3 Datos personales

Los nombres, emails, teléfonos y direcciones de contactos comerciales son datos personales. Antes del piloto se publicará aviso de privacidad específico que indique responsable, finalidad, roles (OryKai/proveedor y empresa usuaria), retención, exportación/borrado y canal de ejercicio de derechos. La beta captura sólo datos necesarios, permite corrección/archivo y no usa estas agendas para entrenamiento de IA ni marketing.

La minimización, limitación de finalidad, limitación de conservación e integridad/confidencialidad son principios de tratamiento de datos del artículo 5 RGPD. [EUR-Lex, RGPD artículo 5](https://eur-lex.europa.eu/eli/reg/2016/679/art_46/pnt_c/oj) [Comisión Europea, principios RGPD](https://commission.europa.eu/law/law-topic/data-protection/rules-business-and-organisations/principles-gdpr/overview-principles/what-data-can-we-process-and-under-which-conditions_en)

## 7. UX/UI responsive

### 7.1 Navegación

`Programas` conserva su posición primaria actual. Al estar activo un programa beta, su tarjeta abre una subruta interna de gestión; no añade un quinto o sexto acceso global. En móvil, el usuario entra en `Programas` y navega dentro de una lista; en escritorio, catálogo/lista y detalle pueden convivir.

| Superficie | Compacta | Media | Ancha |
|---|---|---|---|
| Catálogo de programas | Una columna, estado y CTA de 48 dp. | Dos columnas. | Tres columnas; resumen de beta visible. |
| Clientes y contactos | Lista → detalle a pantalla completa. CTA fija inferior `Nuevo cliente`. | Lista y detalle alternables. | Patrón lista-detalle con panel de detalle persistente. |
| Catálogo/stock | Pestañas Productos, Servicios, Stock; filtros en sheet. | Tabla/tarjetas de dos columnas. | Tabla con columnas configuradas y panel lateral de movimiento. |
| Presupuestos/ventas | Lista de documentos, filtros en sheet, editor por pasos. | Lista y vista previa. | Lista, editor y resumen en paneles, sin perder teclado/ratón. |

No se requiere una dependencia adaptativa nueva en la beta: se reutilizan `SupportDeskBreakpoints`, los tokens y las superficies ya presentes. Si en una fase posterior se adopta un scaffold adaptativo, se valida compatibilidad KMP antes de introducirlo. Las guías oficiales recomiendan el patrón lista-detalle en ventanas expandida y la navegación de una sola vista en compacta. [Android Developers: Adaptive apps](https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps?hl=en)

### 7.2 Pantallas y estados

| Pantalla | Jerarquía | Estados obligatorios |
|---|---|---|
| Programa beta no activo | Valor concreto, distintivo `Beta gratuita · 0 €/mes`, capacidades y `Solicitar acceso`. | Disponible, solicitud enviada, aprobada pendiente de actualización, rechazada con nota, no disponible. |
| Clientes y contactos | Búsqueda, filtro, lista; detalle con contactos principales y CTA visible. | Cargando skeleton, vacío guiado, sin resultados, error con reintento, archivado. |
| Catálogo/stock | Alertas de bajo stock primero, búsqueda/filtros, producto/servicio y movimientos. | Sin ítems, sin stock, ajuste enviado, conflicto de existencias, error. |
| Presupuestos y ventas | Métricas pequeñas, filtros de estado, lista y detalle; el siguiente estado permitido es inequívoco. | Sin presupuestos, borrador, enviado, aceptado, rechazado, venta creada, stock insuficiente, conflicto de versión. |
| Confirmaciones | Resumen de impacto antes de archivar, ajustar o convertir a venta. | Enviando, éxito, recuperación por reintento; no usar doble toque. |

Principios de diseño:

- Reutilizar `SupportDeskThemeTokens`, botones, badges, feedback y componentes del portal. Evitar colores y espaciados ad hoc.
- Usar acciones primarias en la zona inferior alcanzable de móvil; los formularios largos se dividen por secciones, no por un modal interminable.
- Todo estado tiene texto e icono además de color. Los controles interactivos respetan mínimo 48 dp, semántica y etiquetas visibles. [Android Developers: Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults?hl=en)
- Las tablas no son obligatorias en móvil: deben tener alternativa legible por tarjeta. El usuario puede buscar y filtrar sin depender de hover.
- El foco va al encabezado/detalle al navegar; al fallar un formulario, al primer error. Texto ampliado, modo oscuro, teclado y lector de pantalla son escenarios de aceptación.
- No hay datos ficticios de ventas/stock. Un catálogo vacío comunica claramente que todavía no hay registros.

## 8. Validaciones y transacciones

| Área | Validación de servidor | Protección contra duplicados/conflictos |
|---|---|---|
| Solicitud beta | Claves allowlist, máximo ocho, comentario hasta 500 caracteres, producto beta disponible. | Índice de solicitud pendiente + resultado idempotente. |
| Cliente comercial | Nombre limpio y no vacío hasta 160 caracteres; email/teléfono dentro de límites; estado allowlist. | Identificador UUID scoped; control de versión en edición. |
| Contacto | Nombre hasta 160, cargo 120, email/teléfono opcionales; principal único. | Restricción de principal activo y mutación scoped. |
| Producto/servicio | Nombre 160, SKU hasta 80 y único por tenant si se informa, precio 0…límite de negocio, sólo EUR, tipo allowlist. | Archivo en vez de borrado si hay referencias. |
| Stock | Delta decimal no cero con tres decimales máximo, motivo 240, sólo producto con `tracks_stock`. | Idempotencia por ajuste, bloqueo de producto y saldo no negativo. |
| Presupuesto | 1…100 líneas, posiciones únicas, cantidades positivas, importes en céntimos y fechas ISO válidas. | Totales recalculados en servidor, versión esperada para borrador, secuencia transaccional. |
| Venta | Sólo presupuesto aceptado no convertido; salida de stock sólo si hay cantidad. | Bloqueo transaccional de presupuesto/productos y clave de conversión. |

La validación usa allowlists para enumeraciones y formatos estructurados; no se pretende detectar caracteres “maliciosos” mediante denylists. [OWASP Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)

## 9. Criterios de aceptación

### Activación beta

- [ ] Los tres programas aparecen como `Beta gratuita · 0 €/mes` y no se incluyen en ningún preview ni PDF de factura.
- [ ] Un cliente puede solicitar uno o varios programas de su propia empresa, pero no fija precio, tenant ni estado.
- [ ] Un admin propietario aprueba/rechaza; aprobar beta activa acceso a precio cero y deja evento de auditoría.
- [ ] Un admin no propietario, cliente de otra empresa y usuario sin token reciben denegación sin información transversal.
- [ ] Un producto beta no puede activar un cargo ni convertirse a pago por tarea programada, reinicio o actualización de catálogo.

### Clientes y contactos

- [ ] La empresa con permiso puede crear, buscar, editar y archivar sólo sus clientes comerciales y contactos.
- [ ] El contacto principal es único por cliente comercial.
- [ ] Archivar no rompe presupuestos/ventas históricos ni permite acceso cruzado.

### Catálogo y stock

- [ ] Producto y servicio se diferencian; sólo producto puede tener movimientos/existencias.
- [ ] Cada ajuste queda visible como movimiento inmutable con actor, motivo y fecha.
- [ ] No se puede confirmar ajuste/venta que deje stock negativo; el mensaje identifica el conflicto sin exponer otro tenant.

### Presupuestos y ventas

- [ ] El total mostrado coincide con el cálculo del servidor, incluso si el cliente manipula el payload.
- [ ] Sólo borradores se editan; las transiciones inválidas se rechazan.
- [ ] Convertir dos veces el mismo presupuesto devuelve una venta única y una sola salida de stock.
- [ ] Un cambio de catálogo posterior no altera líneas de documentos existentes.

### Calidad transversal

- [ ] Carga, vacío, error, reintento, acceso suspendido y conflicto de versión se muestran de forma comprensible.
- [ ] Controles accesibles con teclado/lector y targets mínimos; móvil pequeño, tableta, escritorio, texto aumentado y modo oscuro mantienen la tarea principal.
- [ ] La aplicación no llama directamente a Supabase Data API ni almacena tokens, datos comerciales o notas sensibles fuera de sus límites actuales.

## 10. Estrategia de pruebas

| Nivel | Cobertura | Ubicación esperada |
|---|---|---|
| Dominio KMP | Reglas de transición, cálculo de líneas, snapshots, permisos de programa, estados de beta y validadores. | `shared/commonTest` con `kotlin.test`. |
| Repositorio/Ktor client | DTO↔dominio, serialización de decimales, 401/403/409/422/429, cancelación y reintentos. | `shared/commonTest` con `MockEngine`/fakes. |
| ViewModel | Carga→éxito, vacío, error→reintento, selección, submit doble, conflicto, efectos únicos. | `shared/commonTest` con fakes y dispatcher controlado. |
| PostgreSQL/Flyway | Integridad tenant, índices únicos, restricciones de stock, RLS habilitado y privilegios revocados para cada tabla/vista/secuencia nueva. | `server/src/test`; integración PostgreSQL existente. |
| Rutas Ktor | Guard de entitlement, JWT client/admin, propietario admin, IDs adivinados, validaciones, idempotencia y códigos HTTP. | `server/src/test`. |
| Concurrencia | Dos conversiones simultáneas y dos ajustes/ventas sobre el mismo producto; se obtiene un documento/movimiento correcto y nunca stock negativo. | Integración PostgreSQL, sin `sleep`. |
| Compose | Estados visibles y acciones semánticas en catálogo, contactos, stock y presupuesto; layouts compacto/ancho. | Pruebas CMP compartidas si la infraestructura actual lo habilita; pruebas JVM/screenshot complementarias. |
| Manual de aceptación | VoiceOver/TalkBack, teclado escritorio, texto aumentado, modo oscuro y reconexión lenta. | Checklist de release beta. |

Las pruebas KMP compartidas usan `kotlin.test`; los dobles de repositorio son fakes y los flujos asíncronos se sincronizan de forma determinista. Compose Multiplatform permite `runComposeUiTest`, aunque su API sigue marcada experimental y debe validarse contra la versión del proyecto antes de adoptarla. [Kotlin: Compose Multiplatform UI testing](https://kotlinlang.org/docs/multiplatform/compose-test.html?c=loopfyx)

## 11. Orden de implementación recomendado

1. **Entitlement beta y guard de servidor.** Añadir los tres productos beta al catálogo, permitir aprobación gratuita estrictamente para ese modo, exponer el modo comercial y comprobar el acceso por ruta. No crear pantallas de datos hasta que esto esté probado.
2. **Vertical Clientes y contactos.** Modelo tenant, CRUD/archivo seguro, rutas y pantalla lista-detalle. Es el menor riesgo y valida el patrón de datos del cliente.
3. **Vertical Catálogo y stock.** Producto/servicio, movimientos inmutables y ajustes idempotentes; integrar alertas de bajo stock. No crear venta todavía.
4. **Vertical Presupuestos y ventas.** Borradores, líneas instantáneas, transiciones y conversión de presupuesto aceptado a venta con stock transaccional.
5. **Pulido y beta controlada.** Auditoría, métricas agregadas sin PII, accesibilidad, pruebas de concurrencia, feedback de 3–5 empresas y política de retención antes de abrir la beta.

## 12. Prompts de implementación

### Prompt A — Entitlements beta y seguridad

> Implementa sólo el vertical de autorización beta para `CUSTOMER_CONTACTS`, `QUOTES_SALES` y `CATALOG_STOCK`. Conserva el catálogo/suscripción/solicitud V8 como fuente de verdad, añade un modo comercial `BETA_FREE` sin precio ni facturación y no modifiques facturas locales ni previews de pago. El cliente solicita; el admin propietario aprueba o rechaza; sólo una aprobación beta activa una suscripción a 0 €. Introduce un guard Ktor reutilizable que derive `clientId` del JWT y exija suscripción `ACTIVE` en cada ruta futura. Mantén RLS/revokes de V7/V8 para todas las tablas nuevas. Añade pruebas de tenant cruzado, admin no propietario, payload que intenta fijar precio, reintento duplicado y acceso tras suspensión. Sigue la arquitectura existente de Ktor, repositorio PostgreSQL y tests de migración; no uses Supabase Auth ni políticas `auth.uid()`.

### Prompt B — Clientes y contactos

> Implementa el programa `CUSTOMER_CONTACTS` como vertical completo, sin tocar `clients` ni `client_contacts` existentes: esos modelos pertenecen a OryKai y su CRM interno. Crea modelos de dominio/DTO/mappers/repositorio/ViewModel en el patrón `features/programs` o una feature cohesionada, rutas Ktor con guard de entitlement y datos aislados por `clientId` del token. La empresa usuaria puede crear, buscar, editar y archivar sus clientes comerciales y contactos; el administrador sólo aprueba acceso y ve métricas agregadas, no contenido por defecto. Usa estado inmutable, efectos de una sola vez y Compose responsive lista-detalle. Incluye validación, límite de entradas, archivo en lugar de borrado, contacto principal único, RLS/revokes y pruebas KMP, Ktor y PostgreSQL. No añadas IA, exportación, ficheros, email ni cobro.

### Prompt C — Catálogo, servicios y stock

> Implementa `CATALOG_STOCK` después de que el guard de beta esté probado. Modela productos y servicios por tenant; sólo los productos con `tracksStock` admiten movimientos. Representa inventario como libro inmutable de movimientos `INITIAL`, `ADJUSTMENT`, `SALE`, `RETURN`, usando cantidades decimales precisas y dinero en céntimos EUR, nunca `Double`. Un ajuste exige motivo e idempotencia; el servidor bloquea/consolida de forma transaccional y no permite stock negativo. Construye Compose KMP con pestañas Productos/Servicios/Stock, lista/card responsive, detalle, ajuste confirmado y estados vacío/error/conflicto. Reutiliza tokens y componentes existentes. Añade pruebas de operaciones concurrentes, tenant cruzado, servicios sin stock, SKU por tenant y RLS. No implementes compras, proveedores, multi-almacén, lotes, reservas ni facturación.

### Prompt D — Presupuestos y ventas

> Implementa `QUOTES_SALES` como vertical independiente y enlázalo opcionalmente con los programas de contactos y catálogo cuando estén activos. Un presupuesto guarda comprador y líneas como instantáneas, usa dinero en céntimos EUR, cantidades decimales precisas y totales recalculados exclusivamente por Ktor. Sólo `DRAFT` se edita; transiciones enviadas/aceptadas/rechazadas se validan en servidor. Convertir un presupuesto aceptado a venta debe ser idempotente y, si hay productos de stock, crear movimientos de salida en la misma transacción sin permitir saldo negativo. Diseña Compose móvil por pasos y escritorio lista-detalle, con estados de conflicto de versión y stock insuficiente. Añade tests de cálculo, snapshots, rutas, doble conversión, concurrencia, aislamiento tenant y accesibilidad. No produzcas factura, PDF fiscal, pago, firma, email ni contabilidad.

### Prompt E — Revisión de calidad beta

> Revisa la beta de programas comerciales contra esta especificación. No amplíes alcance. Verifica que la autorización está en Ktor y no en Compose, que RLS/revokes cubren todas las tablas nuevas, que no existe cobro automático ni integración con facturas PDF locales, que todas las listas están paginadas y que los datos de otros tenants no se filtran por IDs adivinados. Ejecuta pruebas de shared, servidor, migraciones y Compose disponibles; añade sólo pruebas de alto valor que falten. Comprueba móvil compacto, escritorio, teclado, lector de pantalla, modo oscuro y texto aumentado. Entrega fallos con evidencia y correcciones pequeñas y cohesionadas.

## 13. Riesgos y decisiones pendientes

| Riesgo | Mitigación / decisión necesaria |
|---|---|
| Confusión entre los clientes de OryKai y los clientes comerciales de la empresa usuaria. | Mantener entidades separadas, nomenclatura explícita y pruebas de aislamiento. |
| La aprobación beta actual exige precio positivo. | Diseñar `BETA_FREE` de forma explícita; no convertir “cualquier precio cero” en válido. |
| Facturas locales pueden interpretarse como billing recurrente. | Excluir por contrato la beta de `InvoicesViewModel`, PDF y preview de pago. |
| Datos personales de contactos. | Aviso de privacidad, minimización, retención y acceso de soporte explícito antes de piloto. |
| Doble conversión o carrera de stock. | Idempotencia, bloqueo transaccional, ledger inmutable e integración PostgreSQL concurrente. |
| Un único usuario cliente por empresa limita colaboración. | Beta asume una cuenta de empresa; miembros/roles es iniciativa previa a compartir datos entre personas. |
| Stock se interprete como ERP completo. | Límites de beta visibles; no ofrecer reservas, compras, multi-almacén ni valoraciones de inventario. |
| Dependencia entre programas. | Integración opcional: ventas puede usar snapshots manuales si contactos/catálogo no están activos. |
| Catálogo beta cambia a pago. | No autoactivar cobro; requerir comunicación y aceptación/solicitud pagada posterior. |
| Datos o endpoints incompletos en despliegue parcial. | DTO tolerante a campos nuevos, feature gating en servidor y UI con estado no disponible/reintentar. |

## 14. Fuentes primarias y técnicas

- [PostgreSQL — Row Security Policies](https://www.postgresql.org/docs/15/ddl-rowsecurity.html)
- [Ktor 3.3 API — RateLimitConfig](https://api.ktor.io/3.3.x/ktor-server-rate-limit/io.ktor.server.plugins.ratelimit/-rate-limit-config/index.html)
- [OWASP — Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
- [OWASP — Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- [Android Developers — Adaptive apps](https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps?hl=en)
- [Android Developers — Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults?hl=en)
- [Kotlin — Compose Multiplatform UI testing](https://kotlinlang.org/docs/multiplatform/compose-test.html?c=loopfyx)
- [EUR-Lex — RGPD, artículo 5](https://eur-lex.europa.eu/eli/reg/2016/679/art_46/pnt_c/oj)

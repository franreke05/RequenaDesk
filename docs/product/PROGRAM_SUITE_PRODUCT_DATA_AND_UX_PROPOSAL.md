# Beta de utilidades de negocio: producto, datos y UX

**Estado:** propuesta reescrita para aprobación. No crea tablas, rutas,
pantallas, migraciones ni suscripciones.

**Objetivo de la beta:** comprobar que las empresas quieren usar RequenaDesk no
solo para relacionarse con nosotros, sino también como una caja de herramientas
simple para llevar su negocio. La beta contiene siete utilidades generales; no
pretende sustituir un ERP completo ni una asesoría.

## 1. Qué ya incluye el CRM base

El portal actual se entrega al contratar el CRM y **no cuenta como programa**.
Su propósito es que el cliente siga el trabajo que RequenaDesk realiza para él.

| Incluido en los 30 EUR/mes | Qué significa |
|---|---|
| Acceso seguro al portal | Credenciales, cuenta, empresa y miembros autorizados. |
| Inicio | Resumen de prioridad, servicio y actividad relevante. |
| Solicitudes y tickets | Crear, responder, revisar estados y adjuntos de soporte. |
| Trabajo | Tareas relacionadas con el servicio contratado, tablero y actividad. |
| Soporte y SLA | Consumo, seguimiento y revisiones del servicio que prestamos. |
| Programas | Elegir, solicitar y abrir las utilidades de negocio activas. |

Las tareas, tickets, tablero, actividad, soporte y SLA no se vuelven a vender.
Son la relación cliente-RequenaDesk. Tampoco se confunden con las facturas que
el cliente emitirá a sus propios compradores.

## 2. Modelo comercial

| Concepto | Precio mensual | Regla |
|---|---:|---|
| CRM RequenaDesk | 30,00 EUR | Portal base y **un programa beta elegido**. |
| Programa beta adicional | 15,00 EUR | Cada programa activo después del incluido. |
| Suite completa de 7 | 110,00 EUR | Oferta al contratar los siete. |

```text
1 programa:     30 EUR
2 programas:    45 EUR
3 programas:    60 EUR
4 programas:    75 EUR
5 programas:    90 EUR
6 programas:   105 EUR
7 programas:   120 EUR -> precio Suite beta: 110 EUR
```

La oferta se aplica automáticamente en servidor si hay siete programas activos.
El primer programa cuenta como incluido; no hay acumulación de descuentos. El
cliente pide el cambio y el administrador lo aprueba. Por seguridad y claridad
de facturación, en la beta los cambios se hacen efectivos en la siguiente
renovación mensual, sin prorrateo.

## 3. Los siete programas beta

Estos son útiles tanto para un autónomo como para una empresa de servicios,
comercio o pequeño equipo. Todos se guardan por empresa, no por ticket ni por
tarea de soporte.

| Clave | Programa | Problema que resuelve | Resultado para el negocio |
|---|---|---|---|
| `BUSINESS_INVOICING` | Facturación | Crear y controlar facturas sin hojas sueltas. | Sabe qué ha emitido, cobrado, vencido o anulado. |
| `BUSINESS_ACCOUNTING` | Contabilidad y gastos | Tener ingresos, gastos y una visión de caja comprensible. | Controla rentabilidad y prepara información para su asesoría. |
| `BUSINESS_CUSTOMERS` | Clientes y contactos | Conservar clientes, proveedores, contactos y seguimiento comercial propio. | Evita perder oportunidades y contexto de cada relación. |
| `BUSINESS_QUOTES` | Presupuestos y ventas | Convertir una consulta en presupuesto, aceptación y futura factura. | Reduce copia manual y acelera el cierre de ventas. |
| `BUSINESS_CATALOG` | Productos, servicios y stock | Mantener lo que vende, sus precios y, cuando aplique, existencias. | Reutiliza líneas fiables en presupuestos y facturas. |
| `BUSINESS_BOOKINGS` | Agenda y reservas | Dar huecos de cita, confirmar reuniones y evitar solapes. | Menos mensajes para agendar y un calendario de negocio ordenado. |
| `BUSINESS_DOCUMENTS` | Documentos y firmas | Guardar contratos, presupuestos aceptados, recibos y archivos de cliente. | Una fuente de verdad versionada y compartible. |

Los módulos elegidos son coherentes con las suites de negocio consolidadas:
contactos, presupuestos, catálogo/inventario, facturación, gastos, agenda y
documentos aparecen como capacidades separadas porque resuelven trabajos
distintos. [Zoho CRM](https://www.zoho.com/crm/features.html),
[Zoho Inventory](https://help.zoho.com/portal/en/kb/crm/manage-inventory/overview/articles/inventory),
[Pipedrive Smart Docs](https://www.pipedrive.com/en/features/smart-docs).

### 3.1. Facturación (`BUSINESS_INVOICING`)

**Por qué está en la beta:** cobrar bien es indispensable y visible para casi
cualquier negocio. El creador de facturas será independiente de las facturas
que RequenaDesk emita a sus clientes por la suscripción.

**Primera versión:** borrador, numeración configurable por serie, cliente,
líneas de catálogo o líneas libres, cantidades, descuentos, impuestos,
vencimiento, PDF, estados `DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`,
`OVERDUE`, `VOID`, y registro manual de cobros.

**No incluye en beta:** presentación fiscal automática, conexión bancaria,
contabilización oficial ni envío legal certificado. Antes de comercializarla
como factura válida se revisarán los requisitos fiscales del mercado objetivo.

### 3.2. Contabilidad y gastos (`BUSINESS_ACCOUNTING`)

**Por qué está en la beta:** después de facturar, el cliente necesita saber qué
entra, qué sale y cuánto queda. Es más útil que un gráfico decorativo.

**Primera versión:** categorías, gastos manuales con recibo, movimientos de
caja, periodos mensuales, impuestos introducidos explícitamente, ingresos
derivados de cobros de factura y resumen de ingresos/gastos/resultado.

**Límite de la beta:** es control financiero y preparación para asesoría; no se
presenta como sustituto de un programa contable homologado ni como asesoramiento
contable/fiscal.

### 3.3. Clientes y contactos (`BUSINESS_CUSTOMERS`)

**Por qué está en la beta:** el CRM base describe al cliente de RequenaDesk. Un
cliente que usa el producto necesita, además, gestionar a *sus propios*
compradores, proveedores y contactos sin mezclarlos con soporte.

**Primera versión:** empresas/personas, contactos, direcciones, etiquetas,
notas, fuente, estado comercial, siguiente acción, actividades y archivo
lógico. Las facturas, presupuestos, citas y documentos se vinculan a este
cliente de negocio.

### 3.4. Presupuestos y ventas (`BUSINESS_QUOTES`)

**Por qué está en la beta:** es el puente entre una oportunidad y un cobro. Un
presupuesto debe reutilizar los datos del cliente y del catálogo, no copiarse a
mano cada vez.

**Primera versión:** borrador, líneas, impuestos, vigencia, PDF, versión al
enviar, estado `DRAFT`, `SENT`, `VIEWED`, `ACCEPTED`, `REJECTED`, `EXPIRED`, y
aceptación interna auditada. Las líneas aceptadas pueden crear un borrador de
factura, pero nunca una factura emitida automáticamente.

Las propuestas comerciales suelen usar catálogo, cantidades, precios y
condiciones; mantener esos datos como líneas versionadas reduce errores.
[Zoho Quotes](https://help.zoho.com/portal/en/kb/crm/manage-inventory/quotes/articles/quotes)

### 3.5. Productos, servicios y stock (`BUSINESS_CATALOG`)

**Por qué está en la beta:** tanto una tienda como un profesional venden algo.
Para servicios basta con el catálogo y su tarifa; para productos se puede
activar control de existencias.

**Primera versión:** productos/servicios, SKU opcional, precio, impuesto por
defecto, coste opcional, unidades, variantes simples, proveedor opcional y
movimientos de stock `IN`, `OUT`, `ADJUSTMENT`, `RESERVED`. Un servicio tiene
`tracks_stock = false`, por lo que no obliga a una empresa de servicios a ver
inventario inútil.

### 3.6. Agenda y reservas (`BUSINESS_BOOKINGS`)

**Por qué está en la beta:** peluquerías, clínicas, asesorías, formación,
instaladores y consultores comparten una necesidad: reservar sin cadenas de
mensajes.

**Primera versión:** tipo de cita, duración, disponibilidad semanal, bloqueos,
zona horaria, reserva, asistente, confirmación, cancelación y recordatorio
interno. Las citas se pueden asociar a un cliente de negocio y a una propuesta,
pero no dependen de tickets de soporte.

### 3.7. Documentos y firmas (`BUSINESS_DOCUMENTS`)

**Por qué está en la beta:** contratos, PDFs de factura, presupuestos, recibos
y documentos entregados necesitan orden, versión y control de acceso.

**Primera versión:** carpetas, archivo privado, versión, metadatos, enlace a
cliente/factura/presupuesto/gasto, plantillas y solicitud de confirmación.

**Límite de la beta:** la confirmación auditada de lectura/aceptación no se
publicita como firma electrónica cualificada. Una integración de firma legal se
decidirá después con proveedor y revisión jurídica.

## 4. UX del portal: separar soporte y negocio

La navegación principal permanece limpia: **Inicio**, **Trabajo**,
**Programas**, **Cuenta**. `Trabajo` sigue mostrando exclusivamente el trabajo
que hacemos para el cliente. `Programas` abre el espacio de negocio del cliente.

```text
Inicio              -> servicio de RequenaDesk
Trabajo             -> tickets, tareas, tablero y actividad de RequenaDesk
Programas
  ├─ Mi suscripción
  ├─ Utilidades activas
  │   ├─ Facturación
  │   ├─ Contabilidad y gastos
  │   ├─ Clientes y contactos
  │   ├─ Presupuestos y ventas
  │   ├─ Productos, servicios y stock
  │   ├─ Agenda y reservas
  │   └─ Documentos y firmas
  └─ Explorar los 7 programas
Cuenta
```

### Pantalla `Programas`

1. Cuota actual y próxima renovación.
2. Bloque `Tu programa incluido`: cuál usa sin coste extra y opción de cambiar
   para el siguiente ciclo.
3. `Mi negocio`: accesos a los programas activos con datos reales, por ejemplo
   facturas vencidas, saldo del mes o próxima cita; no tarjetas vacías.
4. `Explorar`: las siete tarjetas muestran problema resuelto, ejemplos de uso,
   precio `+15 EUR/mes` y estado de la solicitud.
5. Panel de selección: lista exacta, fecha efectiva y precio calculado por
   servidor. Al elegir los siete, muestra `Suite completa: 110 EUR/mes`.

### Responsive

- **Móvil (hasta 599dp):** una columna, acciones de 48dp, listado de facturas
  y clientes como tarjetas, resumen de compra fijo antes de la zona segura y
  nunca tablas con scroll horizontal.
- **Tableta (600-899dp):** dos columnas para catálogo/metricas y filtros en
  línea; los formularios largos usan secciones plegables.
- **Escritorio (900dp+):** barra lateral actual, tablas de facturas o contactos
  con panel de detalle, y resumen de suscripción lateral fijo.
- Todas las pantallas tendrán etiquetas visibles, contraste AA, foco de teclado,
  estados de carga/error/vacío, guardado visible, confirmación antes de anular o
  borrar y texto+icono para estados semánticos.

El diseño seguirá el sistema visual ya creado para cliente: superficies neutras,
una acción principal por vista, jerarquía de información y componentes
reutilizables; no se introducirán siete estilos diferentes.

## 5. Suscripción, precio y autorización

```text
Cliente configura programas
        ↓
Ktor valida identidad, catálogo y precio
        ↓
Solicitud pendiente de administrador
        ↓
Administrador aprueba/rechaza
        ↓
Cambio programado para próxima renovación
        ↓
Nuevo ciclo: acceso activo + snapshot de cuota
```

Reglas:

1. El CRM base exige seleccionar uno de los siete programas antes de activar la
   suscripción comercial.
2. De uno a seis se calcula `3000 + (cantidad - 1) * 1500` céntimos.
3. Con siete se guarda `pricing_mode = ALL_ACCESS_BETA` y total `11000`
   céntimos.
4. Los importes se calculan en Ktor; la app solo muestra el resultado.
5. El precio y los programas de un ciclo ya emitido no se reescriben.
6. Una anulación o suspensión bloquea nuevas escrituras del programa, conserva
   los datos y deja auditoría; no borra facturas, movimientos ni documentos.

## 6. Diseño de datos y migraciones propuestas

### 6.1. Compatibilidad con la base actual

V8 ya creó `product_catalog`, `client_product_subscriptions`,
`client_program_requests` y `client_subscription_events`. Se reutilizan para
catálogo, derecho de uso, solicitud y auditoría, pero la beta necesita cuota
base, programa incluido y ciclos mensuales inmutables.

`SERVICE_SLA` y `SHEETS` se retiran del catálogo comercial beta mediante una
migración nueva: se marcan no solicitables para altas nuevas y se conservan para
clientes históricos hasta que el administrador los migre o cancele. No se edita
V8 ni se borra historial.

### 6.2. Migración V9: modelo comercial

| Cambio / tabla | Campos esenciales | Propósito |
|---|---|---|
| `product_catalog` (alterar) | `included_eligible`, `additional_monthly_price_cents`, `beta`, `is_requestable`, `is_available` | Registra los siete productos a 1.500 céntimos adicionales. |
| `subscription_offers` | `code`, `base_price_cents`, `included_program_count`, `bundle_program_count`, `bundle_price_cents`, `currency`, `active` | Fuente de precios para `CRM_BETA_FLEX` y `CRM_BETA_ALL_ACCESS`. |
| `billing_subscriptions` | `id`, `client_id`, `offer_code`, `status`, `included_product_key`, `starts_on`, `renews_on`, `current_total_cents`, `currency` | Un contrato comercial activo por empresa. |
| `billing_subscription_changes` | `id`, `subscription_id`, `requested_product_keys`, `status`, `effective_on`, `pricing_snapshot_json`, `requested_by`, `decided_by`, `admin_note` | Solicitud y aprobación de cambios con el cálculo que se vio. |
| `billing_cycles` | `id`, `subscription_id`, `period_start`, `period_end`, `total_cents`, `status`, `generated_at` | Snapshot mensual que alimentará la factura de RequenaDesk. |
| `billing_cycle_lines` | `cycle_id`, `line_type`, `product_key`, `description`, `quantity`, `unit_cents`, `total_cents` | Línea base, programa adicional o descuento Suite. |

`client_product_subscriptions` añade `billing_subscription_id`, `effective_on`,
`scheduled_end_on`, `billing_price_contribution_cents` y `change_request_id`.
Su valor es el derecho de uso efectivo; no se duplicará el mismo estado en una
tabla de UI.

### 6.3. Tablas comunes de negocio

Todas están ligadas a `client_id`, que representa la empresa que usa el CRM. Se
usan UUID, `created_at`, `updated_at`, actor de creación cuando corresponda y
moneda/importe en céntimos.

| Tabla | Uso |
|---|---|
| `business_memberships` | Rol `OWNER`, `MEMBER` o `VIEWER` por usuario de empresa. |
| `business_files` | Metadatos de archivo privado, checksum, MIME, tamaño y estado de escaneo. |
| `business_audit_events` | Quién cambió qué recurso, sin guardar secretos ni contenido sensible. |
| `business_sequences` | Series seguras para numeración de factura y presupuesto. |
| `business_tags` / `business_entity_tags` | Etiquetas reutilizables sin duplicar texto en clientes, productos o gastos. |

### 6.4. Persistencia por programa

| Programa | Tablas | Datos que se guardan bien |
|---|---|---|
| Facturación | `business_invoices`, `business_invoice_lines`, `business_invoice_payments`, `business_invoice_versions` | Serie/número, destinatario, líneas, impuestos, totales, PDF, vencimiento, cobros y estado. |
| Contabilidad y gastos | `business_expense_categories`, `business_expenses`, `business_cash_movements`, `business_financial_periods`, `business_tax_summaries` | Fecha, proveedor, categoría, importe, impuesto, recibo, método de pago y cierre mensual. |
| Clientes | `business_customers`, `business_customer_contacts`, `business_customer_notes`, `business_customer_activities` | Empresa/persona, NIF/campos fiscales si procede, direcciones, contacto, consentimiento, notas y siguiente acción. |
| Presupuestos | `business_quotes`, `business_quote_lines`, `business_quote_versions`, `business_quote_acceptances` | Número, cliente, vigencia, condiciones, líneas, impuestos, PDF, versión enviada y aceptación. |
| Catálogo/stock | `business_catalog_items`, `business_price_lists`, `business_inventory_locations`, `business_inventory_movements` | SKU, servicio/producto, precios, costes, impuesto, existencias, reserva y ajustes. |
| Agenda | `business_booking_services`, `business_availability_rules`, `business_availability_exceptions`, `business_appointments`, `business_appointment_attendees` | Duración, disponibilidad, zona IANA, cliente, asistentes, hora, estado y cancelación. |
| Documentos | `business_document_folders`, `business_documents`, `business_document_versions`, `business_document_links`, `business_document_confirmations` | Carpeta, archivo, versión, relación con entidad, metadatos y confirmación auditada. |

Integridad obligatoria:

- FK siempre al mismo `client_id`; una factura no puede apuntar a un cliente de
  negocio de otra empresa.
- Índices `(client_id, status, issued_on)`, `(client_id, customer_id)` y
  `(client_id, created_at)` según la consulta.
- Totales calculados y validados en servidor; las líneas de factura/presupuesto
  se congelan al emitir/enviar.
- Stock sin negativos salvo ajuste explícito autorizado; reservas y salidas se
  ejecutan en transacción.
- Citas sin solapes mediante transacción y restricción por recurso/intervalo.
- Archivos en bucket privado, URL firmada corta emitida por Ktor; nunca URL
  pública permanente ni binario dentro de PostgreSQL.

## 7. Seguridad y arquitectura

1. RLS activado en cada tabla nueva y privilegios revocados a `PUBLIC`, `anon`
   y `authenticated`; el portal no lee tablas directamente.
2. Ktor obtiene `clientId` y usuario desde JWT. Nunca confía en un identificador
   recibido desde pantalla para decidir la empresa propietaria.
3. Una única autorización de servidor:
   `requireBusinessProgramAccess(clientId, programKey, capability)`. Todas las
   rutas de programa la usan antes de leer, crear, editar o descargar.
4. La interfaz sigue UDF/MVI: composable -> evento -> state holder -> repositorio
   -> servicio Ktor -> nuevo `UiState`. El estado visual no es fuente durable.
5. `commonMain` contiene modelos, eventos y contratos; carga de archivo,
   selector de documento o calendario nativo se mantienen en el borde de cada
   plataforma.
6. Las operaciones de cobro, solicitud, aprobación, emisión, aceptación y carga
   final usan idempotency key y dejan evento de auditoría.

## 8. Plan de construcción tras aprobar

| Fase | Entrega | Comprobación |
|---|---|---|
| 0 | Catálogo beta, textos, permisos y pruebas diseñadas | Se aprueba este documento. |
| 1 | V9 comercial, precios, RLS, administración y UI de selección | 30/45/.../110 EUR se calculan y persisten correctamente. |
| 2 | Clientes + catálogo + presupuestos | Un negocio puede registrar clientes, vender y generar una propuesta consistente. |
| 3 | Facturación + gastos | Facturas, cobros y gastos guardan historial y resumen mensual. |
| 4 | Agenda + documentos | Reservas sin solape y archivos privados versionados. |
| 5 | Revisión de seguridad, migración, responsive y piloto | Los siete programas activos tienen datos reales, no mocks. |

Cada fase tendrá migración Flyway nueva, pruebas de migración/RLS, autorización
positiva/negativa, validación de precio, pruebas de state holder y revisión en
móvil, tableta y escritorio.

## 9. Criterios de aceptación de la beta

- El portal base funciona aunque un programa esté suspendido o no contratado.
- El cliente puede elegir uno de los siete y ver 30 EUR/mes; cada extra suma 15
  EUR; los siete muestran 110 EUR/mes.
- La app no permite modificar precios, estado de suscripción ni `client_id`.
- Cada programa guarda, recupera, edita y audita sus datos para la empresa
  correcta; no hay datos ficticios ni compartidos entre clientes.
- Facturas, presupuestos y documentos conservan versión e historial.
- Móvil, tableta y escritorio no tienen scroll horizontal ni controles pequeños.
- Una anulación/borrado pide confirmación y preserva la trazabilidad necesaria.

## 10. Decisiones que necesito confirmar antes de implementar

1. Aprobar estos siete nombres y claves beta.
2. Confirmar que Facturación y Contabilidad se presentan como herramientas de
   control, no como cumplimiento fiscal/asesoría hasta hacer revisión legal.
3. Confirmar que cualquier cambio de programa sigue requiriendo aprobación del
   administrador y se aplica en la siguiente renovación.
4. Confirmar si la Agenda debe enviar correo/WhatsApp desde la primera beta o
   solo guardar reservas y mostrar recordatorios internos.
5. Elegir política de retención para documentos y datos de un cliente cancelado.
6. Confirmar que `SERVICE_SLA` y `SHEETS` dejan de mostrarse como programas
   comerciales nuevos y se conservan solo para migración histórica.

## Fuentes de producto

- [Zoho: contactos, ventas, finanzas, agenda y documentos](https://www.zoho.com/crm/features.html)
- [Zoho: catálogo, stock, presupuestos, pedidos y facturas](https://help.zoho.com/portal/en/kb/crm/manage-inventory/overview/articles/inventory)
- [Zoho: presupuestos y sus líneas](https://help.zoho.com/portal/en/kb/crm/manage-inventory/quotes/articles/quotes)
- [Pipedrive: documentos, propuestas y contratos](https://www.pipedrive.com/en/features/smart-docs)

# Especificación beta gratuita — Facturación comercial y Contabilidad y gastos

> Estado: diseño de implementación. No hay migraciones ni código aplicados.
>
> Alcance: **BUSINESS_INVOICING** y **BUSINESS_ACCOUNTING**. Ambos se solicitan desde el portal y solo quedan activos cuando un administrador los aprueba con cuota de **0 €**. La beta no genera cobros, facturas de RequenaDesk ni aceptación de un precio futuro.

## 1. Objetivo y límites de la beta

Los dos programas añaden utilidades generales al negocio del cliente y se mantienen separados del CRM ya incluido (tickets, tareas, tablero, actividad y soporte).

| Programa | Clave de catálogo | La beta permite | La beta no permite |
| --- | --- | --- | --- |
| Facturación comercial | BUSINESS_INVOICING | Clientes propios, borradores/proformas, actividades con cantidad/precio, cálculos y PDF de prueba. | Emitir facturas fiscales, SIF/VERI*FACTU, QR, AEAT, factura electrónica B2B o asesoramiento fiscal. |
| Contabilidad y gastos | BUSINESS_ACCOUNTING | Registro operativo de ingresos/gastos, categorías, contrapartes, comprobantes y caja mensual. | Doble partida oficial, libros legalizados, impuestos, SII o asesoramiento contable/fiscal. |

Principios obligatorios:

1. El servidor concede el acceso. Ocultar o mostrar un botón nunca autoriza.
2. Gratis no es automático: cliente solicita, administrador decide y la decisión queda auditada.
3. Los datos de negocio se aíslan por tenant; no se mezclan con tickets/tareas de RequenaDesk.
4. Dinero en céntimos bigint y porcentajes en puntos básicos/numeric, nunca Double o Float.
5. La beta es honesta: todo PDF/editor dice **“BORRADOR DE PRUEBA — NO VÁLIDO COMO FACTURA FISCAL”**.
6. Datos mínimos, adjuntos privados, sin PII en logs y sin información real en pruebas.

## 2. Investigación normativa y consecuencia de diseño

El [RD 1619/2012](https://www.boe.es/eli/es/rd/2012/11/30/1619) exige conservar justificantes y regula la facturación. Su [artículo 6](https://www.boe.es/buscar/act.php?id=BOE-A-2012-14696&p=20231206&tn=1) exige, entre otros, número/serie correlativa, identificación/NIF, domicilio, descripción, base, tipo/cuota de IVA y fecha; el artículo 8 exige autenticidad, integridad y legibilidad.

Por ello, la beta solo produce borradores y proformas. No existe estado de documento “emitido” ni numeración fiscal. Antes de emitir se necesitarán inmutabilidad, rectificativas, conservación, series, auditoría, modalidad SIF y revisión fiscal profesional.

La AEAT indica actualmente que el RRSIF/SIF será obligatorio desde el **1 de enero de 2027** para contribuyentes del Impuesto sobre Sociedades y el **1 de julio de 2027** para el resto de obligados incluidos; el período previo es de pruebas ([FAQ AEAT](https://sede.agenciatributaria.gob.es/Sede/iva/sistemas-informaticos-facturacion-verifactu/preguntas-frecuentes.html)). VERI*FACTU remite registros; la modalidad no VERI*FACTU exige, entre otras garantías, firma de sistema y registro de eventos ([AEAT](https://sede.agenciatributaria.gob.es/Sede/iva/sistemas-informaticos-facturacion-verifactu/cuestiones-generales/modalidades-cumplimiento-obligaciones.html)). No se anunciará conformidad en beta.

El [Plan General de Contabilidad](https://www.boe.es/buscar/act.php?id=BOE-A-2007-19884&p=20241221&tn=0) obliga a las empresas en los términos de la norma; su cuadro de cuentas es una referencia flexible, pero los criterios de registro y valoración exigen análisis. Por eso la beta proporciona categorías operativas y, opcionalmente, una sugerencia de cuenta PGC; no genera asientos ni cuentas anuales.

Facturas y tickets pueden contener datos personales. La AEPD exige finalidad, minimización, conservación limitada y seguridad ([principios](https://www.aepd.es/derechos-y-deberes/cumple-tus-deberes/principios)), además de privacidad desde el diseño y por defecto ([AEPD](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/9-analisis-de-riesgos/FAQ-0224-que-es-la-proteccion-de-datos-desde-el-diseno-y-por-defecto)). Antes de producción deben estar definidos DPA, subencargados, región de almacenamiento y conservación.

## 3. Autorización gratuita administrada

| Actor | Puede hacer | No puede hacer |
| --- | --- | --- |
| Cliente | Solicitar, ver su decisión y usar solo productos ACTIVE. | Aprobarse, decidir precio o leer otro tenant. |
| Administrador propietario | Ver la cola de sus clientes, aprobar a 0 €, rechazar, suspender/cancelar. | Leer por defecto los datos financieros privados del cliente. |
| Backend Ktor | Validar identidad, titularidad, entitlement y entradas; auditar. | Confiar en clientId recibido desde la app. |

Flujo:

1. El cliente selecciona uno o ambos productos y envía una nota opcional de hasta 500 caracteres.
2. La infraestructura V8 crea la solicitud REQUESTED y evita duplicados pendientes por cliente/producto.
3. El administrador confirma “Aprobar beta sin coste”, enviando monthlyPriceCents igual a 0.
4. En una transacción, el servidor marca APPROVED, crea/actualiza la suscripción ACTIVE a 0 y registra los eventos.
5. Al refrescar Programas se muestra “Beta activa sin coste” y “Abrir programa”. Una suscripción SUSPENDED/CANCELLED recibe 403 y no entrega datos.
6. La aprobación gratis no se convierte jamás en cargo futuro sin una solicitud/aceptación comercial nueva.

Cambios mínimos sobre V8:

- Añadir BUSINESS_INVOICING y BUSINESS_ACCOUNTING al catálogo con coste 0, disponibles y solicitables.
- Relajar solo los controles que exigen importe > 0: constraint de solicitudes, ApproveClientProgramRequest, ProgramRoutes y formulario administrativo. Deben aceptar 0..Int.MAX_VALUE.
- El precio persiste exclusivamente desde el backend/admin; el cliente no lo puede calcular ni alterar.
- No modificar ni reasignar SERVICE_SLA o SHEETS.

## 4. Modelo de datos

Todas las tablas nuevas incluyen client_id UUID con referencia a clients, timestamps UTC y actor. Se habilita RLS y se revocan privilegios de PUBLIC, anon y authenticated igual que V7/V8. Ktor mantiene un filtro client_id obligatorio en cada consulta además de esa barrera.

### Núcleo común

| Tabla | Campos principales | Reglas |
| --- | --- | --- |
| business_counterparties | id, client_id, tipo CUSTOMER/SUPPLIER/BOTH, nombre legal, NIF, email, teléfono, domicilio, país, archivada | Nombre obligatorio. No reutiliza client_contacts. Se archiva si tiene relaciones. |
| business_finance_categories | id, client_id, dirección INCOME/EXPENSE, nombre, color, cuenta PGC sugerida, orden, archivada | Plantillas básicas por tenant; la sugerencia no crea asiento. |
| business_file_attachments | id, client_id, clave privada, nombre, MIME, bytes, SHA-256, estado de análisis, actor | Objeto fuera de PostgreSQL; PENDING_SCAN no puede descargarse. |
| business_audit_events | tenant, producto, agregado, acción, actor, timestamp, metadatos | Sin NIF, nombre, notas, importes completos ni URL. |

Crear índices de lista por client_id/updated_at/id y por estado/fecha. Validar UUID y limitar páginas a 1..100.

### Facturación comercial

| Tabla | Campos principales | Invariante beta |
| --- | --- | --- |
| business_issuer_profiles | emisor, moneda, is_default | Un perfil por defecto de tenant, sin pretensión de validez fiscal. |
| business_sales_documents | clase DRAFT_INVOICE/PROFORMA, estado DRAFT/ARCHIVED/VOID, referencia visible no fiscal, emisor, contraparte, fechas, notas, moneda, totales, versión | No existe ISSUED y la referencia no es serie/número fiscal. |
| business_sales_document_lines | descripción, quantity numeric(12,3), precio en céntimos, IVA/desc. en puntos básicos, importes calculados, orden | Cantidad > 0, precio >= 0, IVA y descuento entre 0 y 10000 pb. |
| business_sales_document_events | documento, versión, acción, actor, hashes before/after | Cada cambio aumenta versión; PATCH requiere If-Match. |

La fuente de verdad es el documento estructurado. El PDF se genera bajo demanda, con la marca beta y sin acción de emitir/enviar.

**No conectar automáticamente tareas u horas de soporte a ventas.** Esos registros representan el trabajo de RequenaDesk para el cliente, no el trabajo que el cliente vende a terceros. El programa usa actividades manuales con descripción, cantidad y precio. Un futuro programa de partes de trabajo podrá aportar fuentes facturables propias.

### Contabilidad y gastos

| Tabla | Campos principales | Invariante beta |
| --- | --- | --- |
| business_finance_entries | dirección INCOME/EXPENSE, estado DRAFT/RECORDED/VOID, fecha, contraparte, categoría, descripción, base/IVA/total en céntimos, pago, referencia externa, versión | Backend recalcula importes. Un RECORDED se anula con motivo, no se borra. |
| business_finance_entry_attachments | entry_id, attachment_id, tipo RECEIPT/INVOICE/OTHER | Solo adjuntos CLEAN; relación conservada si se anula. |
| business_finance_entry_events | entrada, versión, acción, actor, metadatos | Auditabilidad de alta, edición, registro, anulación y adjuntos. |

El resumen muestra ingresos, gastos, pendiente/cobrado y flujo neto. Las sumas de IVA dicen “informativas; no es una liquidación tributaria”. Se excluyen conciliación bancaria, amortizaciones, cierre, asientos, retenciones, SII y modelos tributarios.

## 5. Contrato API y reglas de acceso

Prefijo: **/client/business**. Un helper requireClientProgramEntitlement(identity, productKey) se ejecuta antes de toda lectura/escritura y valida bearer, rol CLIENT, clientId y suscripción ACTIVE. Ninguna ruta toma clientId de la UI.

| Ruta | Función |
| --- | --- |
| GET /invoicing/bootstrap | Emisor, contrapartes y documentos recientes paginados. |
| GET/POST /invoicing/documents | Lista/crea borradores; POST requiere Idempotency-Key. |
| GET/PATCH /invoicing/documents/{id} | Lee/edita solo el tenant actual; If-Match y 409 ante conflicto. |
| POST /invoicing/documents/{id}/archive | Archiva un borrador, con evento. |
| GET /invoicing/documents/{id}/preview | PDF beta con Cache-Control: no-store. |
| GET/POST/PATCH /invoicing/counterparties | Gestiona compradores propios; archiva los referenciados. |
| GET /accounting/overview?period=YYYY-MM | Métricas operativas mensuales. |
| GET/POST /accounting/entries | Lista/crea registros paginados e idempotentes. |
| GET/PATCH /accounting/entries/{id} | Lee/edita borrador con versión. |
| POST /accounting/entries/{id}/record | Consolida un registro operativo. |
| POST /accounting/entries/{id}/void | Anula con motivo, nunca borra. |
| POST /accounting/attachments | Sube comprobante privado a análisis. |
| GET /accounting/attachments/{id}/download | Descarga temporal si es propio y CLEAN. |
| GET/POST/PATCH /accounting/categories | Categorías del tenant. |

El administrador usa las rutas V8 solo para autorización. No se añade acceso administrativo general a información financiera del cliente.

## 6. Validaciones, ficheros y operación

- EUR como única moneda de beta. Multidivisa requiere fase con fecha/tipo de cambio/redondeo.
- Fechas ISO válidas y vencimiento posterior o igual a emisión. Textos con trim, tamaño máximo y renderizado sin HTML.
- quantity > 0, unit_price_cents >= 0, IVA 0..10000 pb, descuento 0..10000 pb. El backend calcula base, IVA y total; ignora o rechaza totales manipulados de la UI.
- POST de crear/registrar/archivar/anular usa Idempotency-Key por tenant+acción. PATCH usa versión/If-Match.
- Adjuntos solo PDF/JPEG/PNG; 10 MB inicial configurable, nombre saneado, clave aleatoria privada, SHA-256 y análisis antivirus/cuarentena. Prohibir SVG, HTML, macros y ejecutables.
- Bucket privado, URL corta, Content-Disposition attachment y X-Content-Type-Options nosniff. No loguear URL o nombre de archivo.
- Si no existe análisis seguro, el botón de adjuntar queda deshabilitado con explicación; no se guardan archivos inseguros de forma temporal.
- Rate limit de mutaciones/cargas, auditoría de cambios y métricas agregadas sin PII.

## 7. UI/UX responsive

Reutilizar ClientPortalPageHeader, ClientPortalSurfaceCard, botones y SupportDeskThemeTokens. KMP compartido en commonMain; los composables no acceden a repositorios.

- Diseño móvil primero a 375 dp, sin scroll horizontal, cuadrícula de 8/4 dp, contraste AA y objetivos táctiles >=44 dp.
- Dos columnas solo desde aproximadamente 720 dp. En móvil, las tablas pasan a tarjetas.
- Estados obligatorios: carga de esqueleto, vacío guiado, error con reintento, guardado bloqueado ante doble toque y acceso denegado con enlace a Programas.

**Facturación:** inicio “Borradores y proformas”, badge Beta de prueba y CTA “Crear borrador”; editor por secciones de documento, partes y líneas; resumen de base/IVA/total persistente; validación junto al campo y teclado decimal; la vista previa solo permite “Descargar prueba”.

**Contabilidad:** inicio mensual con máximo cuatro métricas (ingresos, gastos, flujo neto, pendientes); filtros en hoja inferior móvil; alta mediante selector Gasto/Ingreso, importe, fecha, categoría, contraparte y comprobante opcional; “Guardar borrador” es la acción principal y “Registrar” pide confirmación.

## 8. Arquitectura KMP/Ktor y estado

- **Servidor:** migración posterior a V8, DTOs serializables, BusinessProgramsRoutes, métodos acotados de SupportDeskService, contratos de repositorio y variantes PostgreSQL/InMemory. Una transacción escribe entidad, líneas y evento.
- **Shared:** modelos dominio distintos de DTO, datasource Ktor, mapper, repositorio, use case solo para reglas reales, UiState inmutable, UiEvent, UiEffect y ViewModel. Mapear errores en repositorio y propagar CancellationException.
- **Compose:** rutas activas solo con entitlement; pantallas pequeñas/stateless y el estado de Programas sigue siendo fuente de verdad del acceso.

Estados mínimos:

    InvoicingUiState: loading, documentsPage, editor, counterparties,
      issuerProfiles, validationErrors, saveState, accessState, errorMessage
    AccountingUiState: selectedPeriod, summary, entriesPage, filters,
      entryEditor, categories, uploadStatesByAttachment, saveState,
      accessState, errorMessage

Navegación, descarga y mensajes son efectos transitorios. La última recarga gana; una mutación sobre la misma entidad bloquea repeticiones hasta terminar.

## 9. Criterios de aceptación

- [ ] El cliente solicita una o dos betas, sin duplicar solicitud pendiente ni aprobarse manipulando UI/tráfico.
- [ ] El admin aprueba los dos productos a 0; se crea entitlement/evento y no hay impacto en facturación de RequenaDesk.
- [ ] Sin entitlement o tenant correcto las rutas devuelven 403/404 sin datos.
- [ ] Borrador con cantidad decimal y cálculos correctos en servidor; PDF siempre marcado como prueba y sin emisión fiscal.
- [ ] Dos PATCH simultáneos causan 409 recuperable, no pérdida silenciosa.
- [ ] Un gasto/ingreso afecta al resumen; un registro RECORDED se anula, no desaparece.
- [ ] Adjuntos ajenos, pendientes, no permitidos o grandes no se descargan/persisten como utilizables.
- [ ] Los textos son explícitos sobre beta y control operativo, sin promesas de cumplimiento fiscal/contable.

## 10. Pruebas exigidas

| Capa | Cobertura |
| --- | --- |
| shared/commonTest | Céntimos, IVA/descuentos, validadores, mappers y transiciones UI con kotlin.test y fakes. |
| server/test | Entitlement activo/suspendido, aislamiento tenant, aprobación a cero, idempotencia, transacciones, 409 y páginas. |
| Flyway/PostgreSQL | Migración limpia desde V8, constraints, índices, RLS/revocaciones y datos V8 intactos. |
| Archivos | MIME/tamaño/hash, cuarentena, autorización de descarga y limpieza tras fallo. |
| Compose | Semántica, errores/vacío/forbidden, 375 dp/escritorio y screenshots de superficies estables. |
| E2E | Solicitar → aprobar a 0 → refrescar → crear borrador → registrar gasto → comprobar segundo tenant aislado. |

No usar sleep, servicios externos o datos reales. Clientes, NIF y comprobantes de prueba son sintéticos.

## 11. Riesgos pendientes

1. No habilitar emisión ni afirmar cumplimiento fiscal hasta un proyecto SIF dedicado.
2. No habilitar archivos productivos sin análisis, DPA, subencargados y conservación final.
3. Mantener separado el cliente CRM de la contraparte comercial del negocio.
4. No convertir coste cero en un precio futuro sin aceptación nueva.
5. Mantener doble barrera: Ktor/tenant y RLS/revocaciones.
6. Si soporte debe leer datos financieros, diseñar permiso temporal, motivo, consentimiento y auditoría antes de hacerlo.

## 12. Prompt para la siguiente fase

    Actúa como equipo senior KMP/Ktor/PostgreSQL. Antes de editar lee completos:
    .claude/skills/kotlin-project-feature-implementation/SKILL.md,
    .claude/skills/kmp-ktor/SKILL.md, .claude/skills/kotlin-testing-kmp/SKILL.md y
    .claude/skills/mobile-app-ui-design/SKILL.md. Inspecciona V7, V8, ProgramRoutes,
    SupportDeskService, repositorios PostgreSQL/InMemory, shared/features/programs y el
    portal de cliente. Sigue docs/product/programs/FINANCE_PROGRAMS_BETA_SPEC.md.

    Implementa solo BUSINESS_INVOICING y BUSINESS_ACCOUNTING como beta gratuita. Cliente
    solicita y admin aprueba a 0 céntimos; servidor autoritativo con bearer, rol CLIENT,
    clientId y entitlement ACTIVE en cada endpoint. No cobres, no generes facturas de
    RequenaDesk y no alteres V8 salvo lo imprescindible para aprobar precio 0.

    Facturación es borrador/proforma: PDF/UI con “BORRADOR DE PRUEBA — NO VÁLIDO COMO
    FACTURA FISCAL”. No implementes emisión, factura electrónica, VERI*FACTU, QR, AEAT ni
    mensajes de conformidad. Contabilidad es control operativo, no doble partida/impuestos.
    No importes automáticamente tareas u horas de soporte a ventas del negocio del cliente.

    Crea migración posterior a V8 con RLS/revocaciones, dinero bigint y porcentajes
    numeric/puntos básicos. Cálculos solo en backend, transacciones/eventos, Idempotency-Key
    para mutaciones e If-Match/version en PATCH. Los registros consolidados se anulan.
    Adjuntos solo PDF/JPEG/PNG privados, con tamaño/hash/análisis/URL temporal; si el
    análisis seguro no existe, no actives subida.

    En commonMain añade DTO/dominio/mappers/repositorio/UiState/Event/Effect/ViewModel,
    propagando CancellationException. En Compose reutiliza portal, mobile-first, sin scroll
    horizontal y con loading/error/empty/retry/forbidden. Añade pruebas de cálculos,
    autorización, aislamiento, aprobación a cero, concurrencia, idempotencia, RLS,
    adjuntos y UI. Antes de cambios enumera archivos; después ejecuta:
    .\gradlew.bat :shared:jvmTest :server:test :composeApp:compileKotlinJvm
    No hagas push ni despliegue. Informa archivos, pruebas y riesgos.

## 13. Fuentes primarias consultadas el 17 de julio de 2026

- [BOE — Reglamento de facturación, RD 1619/2012](https://www.boe.es/eli/es/rd/2012/11/30/1619).
- [BOE — artículo 6 y contenido de factura](https://www.boe.es/buscar/act.php?id=BOE-A-2012-14696&p=20231206&tn=1).
- [AEAT — FAQ SIF/VERI*FACTU](https://sede.agenciatributaria.gob.es/Sede/iva/sistemas-informaticos-facturacion-verifactu/preguntas-frecuentes.html).
- [AEAT — modalidades VERI*FACTU/no VERI*FACTU](https://sede.agenciatributaria.gob.es/Sede/iva/sistemas-informaticos-facturacion-verifactu/cuestiones-generales/modalidades-cumplimiento-obligaciones.html).
- [BOE — Plan General de Contabilidad](https://www.boe.es/buscar/act.php?id=BOE-A-2007-19884&p=20241221&tn=0).
- [AEPD — principios de tratamiento](https://www.aepd.es/derechos-y-deberes/cumple-tus-deberes/principios).
- [AEPD — privacidad desde el diseño y por defecto](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/9-analisis-de-riesgos/FAQ-0224-que-es-la-proteccion-de-datos-desde-el-diseno-y-por-defecto).

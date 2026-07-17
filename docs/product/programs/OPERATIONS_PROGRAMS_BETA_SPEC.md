# Especificación de programas operativos beta

**Estado:** propuesta lista para implementación; no se han creado migraciones ni código.

**Alcance:** `BUSINESS_BOOKINGS` (Agenda y reservas) y
`BUSINESS_DOCUMENTS` (Documentos y confirmaciones). Ambos son programas beta
gratuitos: el usuario del portal los solicita y un administrador de
RequenaDesk los autoriza. La aprobación activa el acceso, pero no crea ningún
cobro, línea de factura ni modificación del pack comercial.

## 1. Decisiones de producto y compatibilidad con V8

V8 ya proporciona una base útil y segura: `product_catalog`, solicitudes
pendientes únicas por cliente, suscripciones activas, eventos de auditoría,
RLS habilitado y rutas separadas de cliente y administración. El servidor, no
la pantalla, decide catálogo, estado, acceso y precio.

| Aspecto | Decisión beta |
|---|---|
| Claves de catálogo | `BUSINESS_BOOKINGS` y `BUSINESS_DOCUMENTS`. |
| Solicitud | Cualquier usuario de portal cliente activo puede solicitarla, con una nota opcional. |
| Autorización | Solo el administrador propietario del cliente puede aprobar o rechazar. |
| Precio | `0` céntimos, EUR, modo `BETA_FREE`, fijado por servidor. No es editable desde cliente ni administrador. |
| Activación | Tras aprobar, se crea/activa la suscripción y el primer propietario operativo se asigna explícitamente. |
| Cobro | Ninguno: no se incluye en preview comercial ni en factura de suscripción. |
| Acceso | Solo con suscripción `ACTIVE` y permiso operativo; una tarjeta visible nunca sustituye esta comprobación. |

### Ajuste necesario de V8 antes de activar la beta

V8 acepta `monthly_price_cents = 0` en la suscripción, pero la ruta/servicio
actual exige un importe positivo al aprobar y la regla de la solicitud solo
acepta presupuesto positivo. La siguiente migración debe introducir un modo de
facturación servidor (`BETA_FREE`, `PAID`; `INCLUDED` si se usa después),
conservarlo como snapshot en la suscripción y permitir importe cero **solo**
para un producto de catálogo `BETA_FREE`.

La ruta de aprobación no recibirá un precio para esta modalidad. Validará la
clave de catálogo, aplicará `0/EUR/BETA_FREE` internamente y registrará
`BETA_ACTIVATED`. Así no se puede convertir un programa de pago en gratuito
alterando JSON, URL o UI. El cálculo de facturación filtrará expresamente las
suscripciones cuyo modo sea `BETA_FREE`.

`SERVICE_SLA` y `SHEETS` se mantienen como datos históricos de V8; no se
reutilizan como autorización para estos dos programas.

## 2. Arquitectura y fuente de verdad

La implementación conserva el patrón observado:

```text
Compose sin lógica de negocio
  -> UiEvent
  -> ViewModel con StateFlow inmutable + SharedFlow de efectos
  -> caso de uso / repositorio de dominio
  -> RemoteDataSource Ktor
  -> Ktor + servicio + Postgres
```

- Los modelos, DTOs, mapeadores, repositorios, casos de uso, estado y eventos
  compartidos viven en `shared/src/commonMain`; no se filtran DTOs a Compose.
- Las pantallas y navegación viven en `composeApp/src/commonMain` y reutilizan
  `ClientPortalSurfaceCard`, `ClientPortalPageHeader`, tokens y botones del
  portal. El selector nativo de archivos queda en el borde de cada plataforma.
- Ktor es la única API de datos. Los clientes no consultan tablas ni Storage
  directamente.
- Postgres/Storage son la fuente durable. El `UiState` solo representa una
  proyección recargable; no hay actualización optimista de reservas ni de
  versiones de documento.
- Cada escritura lleva clave de idempotencia y actor autenticado. Los reintentos
  devuelven el resultado original, no duplican una cita, versión o aceptación.

## 3. Autorización, pertenencia y aislamiento

### Roles operativos propuestos

Los roles existentes `ADMIN` y `CLIENT` no expresan el acceso interno a los
datos de una empresa. Añadir una pertenencia por cliente:

| Rol | Agenda | Documentos | Administración |
|---|---|---|---|
| `OWNER` | Configura servicios/disponibilidad, crea y administra citas. | Gestiona carpetas, versiones, destinatarios y confirmaciones. | Gestiona miembros y configuración. |
| `MEMBER` | Crea/edita citas permitidas. | Sube y consulta contenido compartido; confirma lo asignado. | No gestiona miembros. |
| `VIEWER` | Solo consulta agenda permitida. | Solo lee documentos limpios compartidos y confirma los asignados. | Sin cambios. |
| Administrador RequenaDesk | Aprueba programa. | Aprueba programa. | **No** lee citas ni archivos por defecto. |

Al aprobar por primera vez, el administrador selecciona un usuario cliente
activo como `OWNER`; por defecto puede proponerse quien solicitó la activación,
pero el servidor verifica que sigue activo y vinculado al cliente. No se debe
otorgar propiedad implícita a un administrador de RequenaDesk.

### Regla común de servidor

Toda ruta de ambos programas comienza con:

```text
requireBusinessProgramAccess(identity.clientId, identity.userId, productKey, capability)
```

La función obtiene `clientId` y `userId` del JWT, comprueba cuenta cliente
activa, suscripción `ACTIVE` y pertenencia/capacidad. Nunca toma el tenant de
un cuerpo, query o ruta controlada por el usuario. Las capacidades mínimas son
`BOOKINGS_READ`, `BOOKINGS_WRITE`, `BOOKINGS_CONFIG`, `DOCUMENTS_READ`,
`DOCUMENTS_WRITE`, `DOCUMENTS_MANAGE`, `DOCUMENTS_CONFIRM` y
`DOCUMENTS_DOWNLOAD`.

Todas las tablas nuevas siguen el patrón V8: RLS activado, privilegios
revocados a `PUBLIC`, `anon` y `authenticated`, y sin acceso directo desde
Supabase. Ktor sigue verificando el tenant incluso si usa un rol de servidor.
Las FKs que relacionen recursos llevarán `client_id` compuesto para impedir que
una cita/documento de una empresa apunte a datos de otra. Las pruebas deben
demostrar aislamiento positivo y negativo.

## 4. Programa 1 — Agenda y reservas

### Objetivo beta y límites

Permite a una empresa ordenar disponibilidad, crear citas, evitar solapes y
conservar un historial claro. La primera beta es un calendario privado para el
equipo del cliente: no expone una página pública de reserva, no integra
WhatsApp/correo y no recoge pagos.

Esto reduce superficie de abuso, consentimiento de marketing, enumeración de
huecos y exposición de datos. Las comunicaciones externas, enlaces públicos,
sincronización Google/Outlook y pagos son una fase posterior con revisión
propia.

No se diseñará como historia clínica. Por defecto la cita guarda el mínimo
necesario: nombre/contacto si hacen falta, servicio, hora y estado; el campo de
nota no debe pedir síntomas, diagnósticos ni otros datos de categorías
especiales. Si un cliente sanitario quiere emplearlo, necesita análisis y flujo
sectorial independiente. La AEPD recomienda en citas sanitarias revelar la
mínima información y no mencionar la causa de la cita en comunicaciones.
[Guía AEPD](https://www.aepd.es/guias/guia-profesionales-sector-sanitario.pdf)

### Flujo de negocio

1. Usuario cliente solicita **Agenda y reservas** desde Programas.
2. Administrador aprueba gratis y asigna el `OWNER`; el portal muestra
   “Agenda activa”.
3. Owner crea servicios, recursos y disponibilidad semanal, con excepciones
   (vacaciones, cierre o horario extraordinario).
4. Member/Owner elige servicio, recurso, día y una franja devuelta por el
   servidor; completa asistentes mínimos y confirma.
5. El servidor revalida la franja dentro de una transacción. Si ya no existe,
   responde conflicto y la UI pide elegir otro hueco.
6. La cita puede confirmarse, completarse, marcarse como no presentada o
   cancelarse con motivo opcional. El original y cada cambio dejan auditoría.

### Datos que se persistirán

| Recurso | Campos esenciales | Reglas de integridad |
|---|---|---|
| `business_booking_services` | `id`, `client_id`, nombre, duración, buffer, recurso opcional, activo. | Nombre único activo por cliente; duración positiva y acotada. |
| `business_booking_resources` | `id`, `client_id`, nombre, tipo (`PERSON`, `ROOM`, `EQUIPMENT`), activo. | No se borra si tiene citas futuras; se archiva. |
| `business_availability_rules` | cliente/recurso, día semanal, inicio/fin local, zona IANA, vigencia. | Fin posterior a inicio; rangos no solapados por recurso/día. |
| `business_availability_exceptions` | recurso, intervalo `timestamptz`, tipo (`BLOCKED`, `OPEN`), motivo opcional. | Intervalo válido; prioridad sobre regla semanal. |
| `business_appointments` | cliente, servicio, recurso, inicio/fin UTC, zona IANA al crear, estado, contacto mínimo, cancelación, creador. | Intervalo UTC válido; exclusión contra solapes en estados que bloquean. |
| `business_appointment_attendees` | cita, nombre, email/teléfono opcional, tipo. | Datos mínimos; sin identidad documental ni notas médicas. |
| `business_audit_events` | actor, recurso, acción, fecha, hash/redacción segura de cambios. | Append-only; no guarda contenido sensible ni secretos. |

`start_at` y `end_at` siempre se guardan como `timestamptz`; la zona IANA se
conserva para presentar la cita como el negocio la programó. No se derivan
horas desde cadenas locales ambiguas en cambios de horario de verano.

Para los estados `HELD` y `CONFIRMED`, una exclusión de rango por
`(client_id, resource_id, intervalo)` y una transacción de creación constituyen
la garantía definitiva contra solapes. Un chequeo visual de la app no basta.

### Endpoints propuestos

Todos bajo `/client/business/bookings` y con autorización de programa.

| Método y ruta | Capacidad | Resultado |
|---|---|---|
| `GET /overview?from=&to=` | READ | Próximas citas, configuración mínima y agenda. |
| `GET /availability?serviceId=&resourceId=&date=` | WRITE | Franjas válidas calculadas en servidor. |
| `POST /appointments` | WRITE | Crea una cita idempotente; `409` si la franja se agotó. |
| `PATCH /appointments/{id}` | WRITE | Solo transición y campos permitidos por rol/estado. |
| `POST /appointments/{id}/cancel` | WRITE | Cancela; no borra el historial. |
| `GET/POST/PATCH /services` | CONFIG | Lista y configura servicios. |
| `GET/POST/PATCH /resources` | CONFIG | Lista y configura recursos. |
| `GET/PUT /availability-rules` | CONFIG | Reemplazo validado de reglas semanales. |
| `GET/POST/PATCH /availability-exceptions` | CONFIG | Gestiona bloqueos y aperturas excepcionales. |

Los endpoints de configuración no aceptan `clientId`. Las colecciones se
paginarán y filtrarán por rango temporal; nunca se descarga una agenda completa
sin límite.

### Validaciones

- Zona horaria IANA permitida; la zona predeterminada pertenece a la empresa,
  no al dispositivo del usuario.
- Servicio activo, recurso activo y franja dentro de disponibilidad calculada.
- Inicio anterior a fin; duración/buffer positivos; fecha válida en DST.
- Estado: `DRAFT -> HELD/CONFIRMED/CANCELLED`,
  `CONFIRMED -> COMPLETED/NO_SHOW/CANCELLED`; no reabrir una cancelada.
- Nombre, correo y teléfono opcionales con longitudes razonables; correo solo
  si se va a usar, normalizado pero no verificado por la app beta.
- Nota de cancelación limitada y sin datos innecesarios; mostrar advertencia de
  no introducir salud, identidad o tarjetas.
- Idempotency key por creación/cancelación; límite de tasa en escrituras.

## 5. Programa 2 — Documentos y confirmaciones

### Posicionamiento legal correcto

El nombre comercial puede seguir siendo **Documentos y firmas**, pero toda la
interfaz beta debe denominar su acción como **confirmación autenticada de
lectura o aceptación**, no como “firma avanzada”, “firma cualificada” ni
“equivalente a manuscrita”. La beta no recoge un trazo manuscrito, DNI,
certificado, selfie, biometría ni credenciales de firma.

Al confirmar, se conserva: sesión autenticada, usuario, organización, versión
exacta, checksum del fichero, texto de la declaración, fecha/hora de servidor y
evento de auditoría inmutable. Es evidencia de una acción electrónica y puede
tener valor probatorio según el contexto, pero no promete nivel eIDAS ni
idoneidad para un contrato concreto.

El Reglamento eIDAS impide negar valor jurídico a una firma electrónica solo
por ser electrónica; una firma cualificada sí equivale a la manuscrita. Una
firma avanzada exige vínculo único, identificación, control exclusivo y
detección de cambios. [eIDAS, arts. 25 y 26](https://eur-lex.europa.eu/legal-content/ES/ALL/?uri=celex%3A32014R0910)

| Nivel | Alcance de esta beta |
|---|---|
| Confirmación autenticada / firma simple | Sí: evidencia de portal autenticado y versión inmutable. No prometer equivalencia manuscrita. |
| Firma avanzada | No: no se declarará ni se simulará. Requiere cumplir íntegramente art. 26 y revisión jurídica/técnica. |
| Firma cualificada | No: requiere prestador cualificado, certificado cualificado y dispositivo cualificado. Integración futura con proveedor de lista oficial y validación legal. |

### Flujo de negocio

1. Cliente solicita **Documentos y firmas**; administrador lo activa gratis y
   asigna `OWNER`.
2. Owner crea una carpeta y solicita una intención de carga. Ktor valida nombre,
   MIME, tamaño, cuota y permiso; devuelve una URL firmada, breve y de un único
   objeto privado.
3. La plataforma de Storage recibe el binario. El servidor finaliza la carga,
   calcula/verifica checksum, inicia análisis antimalware y crea versión
   `PENDING_SCAN`.
4. Solo al pasar a `CLEAN` se puede descargar, compartir internamente o pedir
   confirmación. Un archivo rechazado se aísla y no aparece como descargable.
5. Una nueva carga sobre el documento genera una versión correlativa y no
   modifica la anterior. Owner puede archivar, no destruir evidencia.
6. Owner pide lectura o aceptación de una versión limpia a un usuario portal
   concreto; ese usuario revisa el documento y confirma mediante su sesión.
7. Si se crea una nueva versión, las confirmaciones previas quedan vinculadas a
   la anterior y no se trasladan.

La primera beta no envía invitaciones a destinatarios externos ni crea enlaces
públicos. Esa fase exigirá autenticación de destinatario, controles de entrega,
información de privacidad y revisión de firma.

### Datos que se persistirán

| Recurso | Campos esenciales | Reglas de integridad |
|---|---|---|
| `business_document_folders` | cliente, nombre, carpeta padre nullable, creador, archivada. | FK compuesta de padre; sin ciclos; borrado lógico. |
| `business_documents` | cliente, carpeta, título, tipo, estado, creador, archivado. | Título normalizado; no contiene binario ni URL pública. |
| `business_document_versions` | documento/cliente, número, object key privado, nombre, MIME, bytes, SHA-256, estado de escaneo, creador. | Único `(document_id, version_number)`; checksum obligatorio antes de `CLEAN`. |
| `business_document_links` | documento/versión, entidad destino futura, cliente. | FK compuesta al mismo cliente; V1 solo enlaces a recursos ya existentes. |
| `business_document_confirmation_requests` | versión, destinatario interno, tipo `READ/ACCEPT`, texto visible, creador, expiración. | Solo versión `CLEAN`; único pendiente por versión/destinatario/tipo. |
| `business_document_confirmations` | solicitud, versión, usuario, checksum, texto, fecha servidor, método. | Append-only; único por solicitud; nunca mutable. |
| `business_audit_events` | actor, acción, metadatos seguros. | Sin URL firmada, binario, token ni texto completo del documento. |

El bucket es privado. `object_key` no se entrega a Compose; la descarga se
autoriza en Ktor y devuelve una URL firmada de duración corta solo tras comprobar
tenant, permiso, estado limpio y versión solicitada.

### Endpoints propuestos

Todos bajo `/client/business/documents` y con autorización de programa.

| Método y ruta | Capacidad | Resultado |
|---|---|---|
| `GET /folders` / `POST /folders` / `PATCH /folders/{id}` | READ/WRITE/MANAGE | Navega y gestiona estructura sin ciclos. |
| `GET /documents?folderId=&cursor=` | READ | Lista paginada de metadatos, nunca binarios. |
| `POST /upload-intents` | WRITE | Valida y genera carga privada de un uso. |
| `POST /upload-intents/{id}/complete` | WRITE | Verifica carga/checksum y inicia escaneo. |
| `POST /documents/{id}/versions` | WRITE | Crea versión desde carga limpia/verificada. |
| `GET /documents/{id}` | READ | Detalle, versiones, estado y confirmaciones permitidas. |
| `POST /versions/{id}/download-url` | DOWNLOAD | URL breve; prohibido si no está `CLEAN`. |
| `POST /versions/{id}/confirmation-requests` | MANAGE | Solicita lectura/aceptación interna. |
| `POST /confirmation-requests/{id}/confirm` | CONFIRM | Registra confirmación idempotente de la versión exacta. |
| `POST /documents/{id}/archive` | MANAGE | Archivo lógico con auditoría; sin borrar versiones. |

### Validaciones de contenido y seguridad

- V1 permite una lista corta de MIME seguros y verificables por servidor
  (PDF, PNG/JPEG, DOCX, XLSX); rechaza ejecutables, HTML/SVG activo, macros y
  tipos inconsistentes con el contenido. El límite beta y cuota se definen por
  configuración de servidor, no por UI.
- Nombre/título/carpeta: normalización Unicode, longitud máxima, sin rutas,
  control characters ni extensiones dobles engañosas.
- La carga no es legible hasta `CLEAN`; analizar también archivos comprimidos
  si se permiten en el futuro. Caducar y limpiar cargas incompletas.
- Cada versión tiene número correlativo y SHA-256 calculado/verificado por
  backend; no se sobrescribe un objeto existente.
- La petición y confirmación llevan versión y checksum; si cambia el archivo,
  exige una nueva petición. No hay “aceptación global del documento”.
- La confirmación exige sesión activa y usuario asignado; no se acepta un ID de
  usuario aportado por cliente. Repetir la operación es idempotente.
- No recoger firma dibujada ni copia de identidad en beta. Mostrar enlace a los
  términos/privacidad del negocio cuando sea aplicable.

## 6. Diseño de UI responsive

La navegación principal permanece en **Programas**; no se añaden pestañas
globales. Al estar activo, cada tarjeta muestra `Abrir agenda` o `Abrir
documentos`. El router del portal añade destinos secundarios internos y conserva
el botón volver a Programas. Una ruta secundaria sigue verificando acceso en
servidor al cargar, no solo al navegar.

### Programas

- Solicitud: tarjeta de ambos programas con etiqueta `Beta gratuita`, precio
  `Sin coste durante la beta` y CTA `Solicitar autorización`.
- Pendiente: botón desactivado `En revisión por el administrador`.
- Activo: CTA principal `Abrir`; no se muestra incremento mensual.
- Rechazado: motivo administrativo si existe y CTA de soporte, nunca reintento
  automático que duplique la solicitud.

### Agenda

| Anchura | Diseño |
|---|---|
| 375–559 dp | Lista de “Hoy” y próximos; selector de fecha horizontal accesible, formulario paso a paso, CTA fijo inferior. Sin rejilla semanal comprimida. |
| 560–999 dp | Calendario de días + panel de próximas citas; formularios a una o dos columnas. |
| >=1000 dp | Semana/agenda principal, detalle lateral y filtros; el CTA mantiene jerarquía clara. |

Flujo de creación: **Servicio → fecha y franja → asistente mínimo → revisar y
confirmar**. Estados vacíos guían al Owner a crear el primer servicio o la
disponibilidad. Un `409` se explica como “Ese hueco acaba de ocuparse” y ofrece
recargar, no como fallo genérico.

### Documentos

| Anchura | Diseño |
|---|---|
| 375–559 dp | Breadcrumb corto, tarjetas de archivo, CTA `Subir` en zona del pulgar, detalle en pantalla completa. |
| 560–999 dp | Panel de carpetas y lista de documentos; acciones agrupadas por documento. |
| >=1000 dp | Árbol de carpetas, tabla/lista de metadatos y panel de detalle/versiones. |

El detalle enseña título, versión, fecha, autor, estado de análisis y
confirmaciones. `Descargar` y `Solicitar confirmación` no aparecen activos hasta
que el archivo esté limpio. Antes de confirmar: nombre de documento, versión,
checksum abreviado, declaración exacta y CTA “Confirmar lectura” o “Aceptar
esta versión”; nunca una etiqueta engañosa de firma cualificada.

Usar el sistema visual ya existente: fondo neutro, tarjetas de superficie,
espaciado por tokens, jerarquía tipográfica corta, estados semánticos con texto
además de color, controles de al menos 44 dp y foco/semántica accesible. No hay
scroll horizontal, ni tablas obligatorias en móvil, ni datos sensibles como
contenido de archivo en tarjetas.

## 7. Privacidad, conservación y notificaciones

- El cliente que gestiona sus contactos/citas/documentos normalmente actúa como
  responsable; RequenaDesk actúa como encargado para esos datos, según DPA y
  tratamiento real. Cuenta, seguridad y cobro propio son tratamientos separados.
- Aplicar minimización por defecto: solo datos necesarios, mínimo acceso y
  mínimo plazo. [AEPD: protección de datos por defecto](https://www.aepd.es/derechos-y-deberes/cumple-tus-deberes/medidas-de-cumplimiento/proteccion-de-datos-por-defecto)
- No incluir notas clínicas, documentos de identidad, tarjetas o categorías
  especiales en datos de demostración, analítica, logs ni errores.
- La política de retención del cliente se configura y se refleja en el DPA. Al
  cancelar, exportar o devolver datos antes de suprimir/bloquear conforme a las
  obligaciones aplicables; las evidencias y auditoría no se alteran.
- Recordatorios internos no requieren proveedor externo. Para correo/SMS en una
  fase posterior: proveedor como subencargado, registro de entregas, contenido
  mínimo, canal alternativo y base jurídica/documentación del cliente.

## 8. Criterios de aceptación

1. Un usuario cliente puede solicitar cada programa una sola vez mientras está
   pendiente; no puede activarlo ni alterar precio/estado.
2. Un administrador autorizado puede aprobar o rechazar; la aprobación beta
   deja precio cero y nunca entra en preview/factura comercial.
3. Un cliente no autorizado, otra empresa, un usuario inactivo y un admin ajeno
   reciben `403/404` sin metadatos del recurso.
4. Agenda calcula franjas en servidor, no permite solapes bajo peticiones
   concurrentes y presenta correctamente cambios DST.
5. Citas canceladas quedan auditadas; no se borran silenciosamente.
6. Un archivo privado no se lee/descarga hasta escaneo limpio; una URL expira y
   no funciona para otro tenant.
7. Una versión nueva no altera contenido, checksum ni confirmación de una
   versión anterior.
8. La confirmación beta registra usuario, versión, checksum, texto y hora de
   servidor, y no se anuncia como firma avanzada/cualificada.
9. En 375 dp, tableta y escritorio se completan solicitud, cita, subida y
   confirmación sin scroll horizontal y con estados carga/error/vacío.
10. Todos los textos de seguridad, revisión y error son claros, localizables y
    no exponen detalles internos.

## 9. Plan de pruebas

### Servidor y Postgres

- Flyway sobre Postgres limpio y sobre una base ya migrada a V8.
- Integración con dos clientes, dos admins y dos miembros: autorización positiva
  y negativa de cada endpoint, RLS/privilegios sin acceso directo y FKs de
  tenant compuesto.
- Catálogo beta: solo las dos claves con modo `BETA_FREE` permiten aprobación a
  cero; un producto `PAID` con cero falla; precio enviado por HTTP se ignora.
- Paralelismo: dos transacciones intentando la misma cita producen una sola
  confirmada; reintento con igual idempotency key no duplica.
- DST: zona `Europe/Madrid`, día de salto y día repetido; almacenamiento UTC y
  presentación local esperada.
- Documentos: upload intent de otro tenant, MIME falso, checksum incorrecto,
  archivo `PENDING/REJECTED`, URL caducada, versión duplicada, archivo archivado
  y confirmación de versión no limpia fallan.
- Auditoría: petición, aprobación, cita, cancelación, carga, escaneo, versión y
  confirmación dejan actor/fecha/acción sin secretos.

### Shared KMP y UI

- `commonTest` con `kotlin.test`: validadores de disponibilidad/transiciones,
  modelos/mapeadores, cálculo de estado de tarjeta y reducers de ViewModel.
- Fakes de repositorio para carga, éxito, vacío, error, retry, doble toque y
  respuesta obsoleta; no mocks profundos ni sleeps.
- `MockEngine` para `Remote*DataSource`: ruta, autorización, payload y error
  Ktor. Mantener `CancellationException` fuera del mapeo de errores.
- Compose Multiplatform UI tests por semántica: CTA según estado de programa,
  mensaje de conflicto de franja, documento no descargable y confirmación con
  versión visible.
- Capturas puntuales para los tres breakpoints y estados `PENDING_SCAN`,
  `CONFLICT` y vacío; complementan, no sustituyen, pruebas de comportamiento.

## 10. Prompts de implementación por vertical

### A. Servidor y datos

> Implementa únicamente la vertical backend de `BUSINESS_BOOKINGS` y
> `BUSINESS_DOCUMENTS` siguiendo V8. Añade una migración nueva, no reescribas
> V8. Introduce un modo de facturación servidor para que solo productos
> `BETA_FREE` se aprueben con 0 céntimos y queden fuera de facturación. Implementa
> `requireBusinessProgramAccess` con JWT, suscripción activa y membership por
> cliente. Habilita RLS/revoca accesos como V8 y crea FKs compuestas de tenant.
> Agenda usa `timestamptz`, zona IANA y garantía transaccional de no solape.
> Documentos usan bucket privado, intención de carga, escaneo, checksum y URLs
> firmadas cortas. No implementes enlaces públicos, correo externo ni firma
> avanzada/cualificada. Añade pruebas Postgres/Flyway/autorización/concurrencia.

### B. Shared KMP

> Implementa en `shared/commonMain` modelos de dominio, DTOs, mapeadores,
> repositorios, casos de uso y dos ViewModels MVI para agenda/documentos. Mantén
> DTOs fuera de UI y un StateFlow inmutable + efectos transitorios. Define
> estados explícitos de carga, vacío, error, conflicto de reserva, escaneo y
> confirmación. Usa Ktor con errores específicos, no captures cancelación, y
> pruebas `commonTest` con fakes y MockEngine. No añadas dependencias de
> plataforma a commonMain.

### C. Portal Compose

> Amplía el portal cliente sin modificar la navegación principal: Programa
> muestra las tarjetas beta gratuitas solicitables/pendientes/activas y abre
> destinos secundarios para Agenda y Documentos. Reutiliza componentes y tokens
> del portal. Diseña móvil primero: agenda como lista/formulario por pasos,
> documentos como lista de tarjetas; usa paneles en tableta/escritorio. CTA de
> 44 dp, sin scroll horizontal, semántica accesible y estados claros. Nombra la
> acción de documentos como “confirmación autenticada”; nunca afirmes firma
> avanzada, cualificada o equivalente manuscrita.

### D. Verificación de seguridad y UX

> Revisa el flujo completo solicitud→aprobación gratuita→apertura→creación de
> cita/subida→confirmación. Prueba un tenant/usuario/admin no autorizado, doble
> clic, reintento, carrera de franja, URL vencida, archivo no limpio y cambio de
> versión. Verifica que logs, estados, capturas y errores no revelan datos ni
> URLs firmadas. Ejecuta pruebas compartidas y servidor, y valida tres
> breakpoints del portal.

## 11. Riesgos abiertos y decisiones necesarias

| Riesgo | Mitigación / decisión requerida |
|---|---|
| Uso sanitario o datos especiales en reservas | Excluirlo de beta; análisis legal/EIPD sectorial antes de admitirlo. |
| Confusión entre confirmación y firma | Copy fijo y revisión legal; integración de proveedor cualificado solo en fase posterior. |
| Malware y fuga por Storage | Bucket privado, escaneo obligatorio, URLs breves, cuota y logs sin URL. |
| Solapes/DST | Constraint/transacción de BD y pruebas en zonas/hitos reales. |
| Administrador ve documentos del cliente | Aprobar no concede lectura; cualquier soporte excepcional requiere política y auditoría. |
| Avisos externos | No incluir en V1; añadir tras DPA, privacidad y gestión de proveedor. |
| Precio beta al finalizar | Comunicar fecha y política antes de cambiar a pago; cambio de catálogo no modifica aprobaciones históricas. |
| Retención y baja | Definir con el titular la matriz de conservación, exportación y borrado/bloqueo antes del piloto. |

## 12. Fuentes oficiales consultadas

- [Reglamento eIDAS 910/2014, arts. 3, 25 y 26 (EUR-Lex)](https://eur-lex.europa.eu/legal-content/ES/ALL/?uri=celex%3A32014R0910)
- [Comisión Europea: identidad digital y servicios de confianza](https://commission.europa.eu/topics/digital-economy-and-society/european-digital-identity_en)
- [AEPD: protección de datos por defecto](https://www.aepd.es/derechos-y-deberes/cumple-tus-deberes/medidas-de-cumplimiento/proteccion-de-datos-por-defecto)
- [AEPD: guía para profesionales del sector sanitario](https://www.aepd.es/guias/guia-profesionales-sector-sanitario.pdf)
- [AEPD: contenido del contrato de encargo](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/8-responsable-y-encargado-del-tratamiento/FAQ-0238-cual-seria-el-contenido-del-contrato-de-encargo-de-tratamiento)

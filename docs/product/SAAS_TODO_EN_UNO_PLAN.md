# Plan: RequenaDesk como SaaS "todo en uno" para empresas que empiezan

> Estado: aprobado por el usuario el 23-oct-2026. Documento de producto; cada fase tendrá su propio plan de implementación.

## 0. Contexto y decisiones tomadas

RequenaDesk (este repo) es hoy la herramienta interna de OryKai: app de escritorio de admin (CRM, tickets, tareas con temporizador, tablero diario, facturas PDF locales), portal de cliente con 8 "programas" de negocio en beta gratuita y servidor Ktor + Postgres. El objetivo es venderlo como SaaS a autónomos y pymes que empiezan, sin perder nada de lo que ya funciona, y mejorar su aspecto.

Decisiones del usuario (23-oct-2026):
1. **Cada empresa tiene su propio espacio de trabajo.** Lo que hoy es la app de admin pasa a ser el producto. OryKai será un espacio más y sus datos actuales se migran a él.
2. **Facturas en dos carriles.** *Carril A*: las facturas propias de OryKai siguen como hoy (PDF local en `Escritorio/Facturas OryKai`, sin BD ni HTTP). *Carril B*: módulo nuevo en servidor, que cumple Verifactu, para las empresas del SaaS. Para permitirlo, `CLAUDE.md` se modifica por escrito (ver §8).
3. **Planes por empresa** (Gratis / Autónomo / Pro / Equipo), con usuarios incluidos y extras sueltos. Sustituyen el modelo "30 € + 15 €/programa".
4. **Tres estilos visuales que elige cada usuario: Artesanal, Minimalista y Glass.** Artesanal es el estilo editorial actual y será el predeterminado.

## 1. Mercado: lo que se vende y cómo se cobra

Precios sin IVA, según comparadores de 2026. Solo se comprobaron en la web oficial Odoo y Bitrix24.

| Referente | Modelo | Precio orientativo | Lección |
|---|---|---|---|
| Holded | Por empresa, por tramos (usuarios y facturas/año) | 15 / 29 / 59 / 99 / 199 €; usuario extra 10 €; extras de 25 € (inventario, TPV, reservas) | Facturación + impuestos es la puerta de entrada; los módulos se venden como extras |
| Quipu | Por empresa | ~17 / 30 / 59 € | Los modelos 303/130 entran en todos los planes |
| Billin | Por empresa | ~8 / 12,5 / 20 € | El ancla barata del autónomo; Verifactu y TicketBAI incluidos |
| Odoo | Por usuario; una app gratis | 0 / 14,90 / 27,90 € por usuario | "Una app gratis" funciona para captar |
| Zoho Bigin / One | Por usuario o por empleado | Bigin 7–21 €; One ~37 $ por empleado | El CRM sencillo se cobra barato |
| Bitrix24 | Precio fijo con tramos de usuarios | Gratis; 49 / 99 / 199 $ | Un plan gratis amplio con CRM y tareas |
| HubSpot Starter | Por licencia | ~20 € (promos desde 7 €) | El CRM gratis engancha y la IA va por créditos |
| Factorial | Por empleado | desde ~5,50 € (mínimo ~27,50 €) | Fichaje y RR. HH. se cobran por empleado |

**Qué se demanda.** Facturas y presupuestos con Verifactu, gastos con OCR, conexión bancaria, modelos 303/130 y acceso para la gestoría (la mitad de los autónomos tiene una). Detrás van CRM, proyectos, reservas y fichaje.

**Hueco de mercado.** Ningún software español de facturación trae tickets de soporte y portal de cliente, y ningún CRM trae Verifactu. RequenaDesk ya tiene tickets, portal y control de horas. **Posicionamiento:** "el todo en uno para negocios de servicios: clientes, trabajo, horas y facturas en un mismo sitio, con portal para tus clientes".

**Regulación que marca el calendario**
- **Verifactu.** La norma vigente (RDL 15/2025) obliga a las sociedades desde el 1-ene-2027 y a los autónomos desde el 1-jul-2027. Hacienda anunció el 5-oct-2026 un retraso a oct-2028, pero aún no está en el BOE. **Se planifica con la fecha de 2027.**
- **Lo que exige Verifactu al software.** Registro inalterable con hash encadenado, QR y envío a la AEAT. El propio fabricante firma una declaración responsable (Orden HAC/1177/2024). Sanción al fabricante: hasta 150.000 €. Hay que revisarlo con un asesor.
- **Factura electrónica B2B** (RD 238/2026, Orden HAC/1028/2026). Empresas de más de 8 M€: 6-oct-2027. Resto y autónomos: 6-oct-2028. Formato UBL / EN 16931.
- **Registro horario digital.** El nuevo RD no está aprobado. Sigue vigente la obligación de 2019 en formato libre.

## 2. Inventario: lo que existe y no se puede perder

| Servicio actual | Dónde vive hoy | Módulo destino |
|---|---|---|
| CRM de clientes (nivel de servicio, notas, horas/mes, credenciales de portal, pestañas de ficha) | Admin · `AdminClientsScreen.kt`, `ClientRoutes.kt` | Núcleo · Clientes |
| Contactos y actividades CRM (V6, solo en servidor) | `ClientCrmRoutes.kt` | Núcleo · Clientes (por fin con UI) |
| Datos fiscales del cliente (V16, rama `feat/cliente-datos-fiscales`) | `ClientBillingProfile.kt` | Núcleo · Clientes → Facturación |
| Tickets (mensajes, prioridad, adjuntos, notas internas, horas, eventos) | Admin + portal · `AdminTicketsScreen.kt`, `TicketRoutes.kt` | Soporte |
| Servicio y SLA (`SERVICE_SLA`) | Portal · `ClientServiceScreen.kt` | Soporte (extra de SLA) |
| Tareas, etiquetas y temporizador de sesiones (start/pause/checkpoint/stop) | Admin + móvil · `AdminTasksScreen.kt`, `WorkSessionRoutes.kt` | Trabajo y horas |
| Tablero diario de tareas | Admin · `AdminPinboardScreen.kt` | Trabajo y horas |
| Panel de admin (KPIs, calendario, métricas por cliente) | `AdminDashboardScreen.kt` | Núcleo · Inicio + Informes |
| Facturas PDF locales (numeración local, IRPF, perfil de emisor) | Admin · `AdminInvoicesScreen.kt`, `InvoiceBillingLedger.kt` | **Carril A** (solo OryKai, sin cambios) |
| Facturación del portal (borradores/proformas) | Portal · `BusinessFinanceScreens.kt` | Facturación (**Carril B**: evoluciona a Verifactu) |
| Contabilidad y gastos | Portal · `SV/business/finance` | Facturación · Gastos |
| Clientes y contactos del negocio, Catálogo y stock, Presupuestos → venta | Portal · `business/sales/*` | Ventas |
| Agenda y reservas (servicios, recursos, disponibilidad, citas) | Portal · `BookingsOperationsScreen.kt` | Agenda |
| Documentos y confirmaciones (carpetas, versiones; falta el almacenamiento real) | Portal · `DocumentsOperationsScreen.kt` | Documentos |
| Proyectos (V15: proyectos, tareas, tablero, sesiones) | Portal · `BusinessProjectsScreen.kt` | Trabajo y horas (se fusiona con las tareas del admin) |
| Portal de cliente (Inicio, Trabajo, Solicitudes, Programas, Cuenta, Ajustes) | `ClientPortalScreen.kt` | Portal de cliente |
| App móvil (Resumen, Tareas, Clientes, Etiquetas) | `MobileWorkspaceApp.kt` | Se mantiene, ya con espacios |
| Registro de push | `DeviceRoutes.kt` | Núcleo · Avisos |
| Catálogo, suscripciones y peticiones de programas (V8/V9) | `ProgramRoutes.kt` | Pasa a planes y módulos por espacio |

Hoy falta: entidad empresa/espacio (todo cuelga de `owner_admin_id`), usuarios por empresa con roles, alta propia, cobro, centro de notificaciones e IA.

## 3. Catálogo de módulos

| Módulo | Contenido | Estado |
|---|---|---|
| **Núcleo** (siempre incluido) | Espacio, usuarios y roles, Clientes CRM con contactos, actividad y datos fiscales, Inicio con primeros pasos, avisos, ajustes, estilos | Existe en parte; se añaden espacio, roles y alta |
| **Ventas** | Presupuestos (aceptación desde el portal) → factura, catálogo y stock, clientes del negocio; más adelante, embudo de ventas | Existe (beta) |
| **Facturación y gastos** | Facturas Verifactu (carril B), rectificativas, series, gastos, resumen informativo de 303/130, exportación para la gestoría, acceso de gestoría | Hay borradores; Verifactu es nuevo |
| **Trabajo y horas** | Tareas, temporizador, tablero diario, proyectos, etiquetas, **horas → línea de factura** (redondeo hacia arriba, como hoy) | Existe; se unifica |
| **Soporte** | Tickets, adjuntos, notas internas, SLA (extra), base de conocimiento (más adelante) | Existe |
| **Agenda y reservas** | Calendario propio (nuevo), tipos de cita, disponibilidad, reservas desde el portal | Hay reservas; calendario nuevo |
| **Documentos** | Carpetas, versiones, confirmación autenticada; almacenamiento real | Falta el almacenamiento |
| **Portal de cliente** | Los clientes de cada empresa ven sus tickets, trabajo, presupuestos, facturas y documentos | Existe; se reorienta al espacio |
| **Equipo y fichaje** (fase posterior) | Registro horario a partir de las sesiones, ausencias | Nuevo |
| **Asistente IA** (fase posterior) | "¿Cómo va mi negocio?", resumen semanal, borrador de presupuesto | Nuevo |

## 4. Planes y precios (sin IVA)

| Plan | €/mes (anual) | Usuarios | Incluye |
|---|---|---|---|
| **Gratis** | 0 € | 1 | Núcleo + 1 módulo a elegir; hasta 25 clientes; portal para 3 clientes |
| **Autónomo** | 12 € (10 €) | 1 + gestoría | Núcleo, Ventas, Facturación (Verifactu cuando esté listo), Trabajo y horas, Agenda, portal ilimitado |
| **Pro** ★ recomendado | 29 € (24 €) | 3 + gestoría | Todo lo de Autónomo + Soporte con SLA, Documentos, informes, portal con tu logo |
| **Equipo** | 49 € (41 €) | 8 + gestoría | Todo lo de Pro + roles avanzados, fichaje (cuando exista), auditoría |

- **Extras:** usuario adicional 6 €; fichaje 2 €/empleado; pack de IA por créditos.
- **Reglas:**
  - Prueba Pro de 14 días sin tarjeta; al acabar pasa a Gratis, nunca a un cobro.
  - La gestoría entra gratis.
  - Cambios de plan al instante con prorrateo (cuando haya pasarela).
  - El precio se calcula siempre en el servidor.
  - Ningún cobro sin que el cliente lo acepte de forma expresa (se mantiene la regla de la beta).
- **Clientes de la beta actual:**
  - Siguen gratis hasta una fecha que se avisa con 60 días.
  - Después tienen 12 meses al 50 % ("fundadores").
- **Cobro:**
  - Fase 3: aprobación y factura manuales (OryKai factura por carril A).
  - Fase 6: Stripe Billing con tarjeta y SEPA.

## 5. Alta de una empresa nueva (onboarding)

Cinco pasos, cada uno con Atrás y Saltar, barra de progreso y borrador guardado.

1. **Cuenta:** email y contraseña, y aceptación de términos (casillas separadas y sin marcar, como pide `AJUSTES_LEGAL…`).
2. **Tu negocio:** forma jurídica (autónomo/sociedad), sector, número de personas y si trabajas con gestoría.
3. **¿Qué quieres resolver?** (multiselección: cobrar y facturar · organizar trabajo y horas · atender clientes · agenda de citas · documentos). Con esto se recomiendan plan y módulos.
4. **Datos fiscales** (opcional): NIF, dirección, IRPF por defecto y serie. Reutiliza `InvoiceProfileStore` y el perfil fiscal.
5. **Elige tu estilo:** Artesanal, Minimalista o Glass, con vista previa en vivo de un panel de muestra, en claro u oscuro.

Al terminar se llega a **Inicio** con una lista de "Primeros pasos": crear tu primer cliente, registrar horas con el temporizador, enviar un presupuesto, invitar a un cliente al portal e invitar a tu gestoría. Cada paso lleva un estado vacío útil y un botón de acción.

## 6. Navegación

**Espacio de empresa (escritorio ≥ 1280dp: barra lateral; medio: rail; móvil: barra inferior de 5)**
- Inicio
- Clientes
- Ventas › Presupuestos · Catálogo
- Facturación › Facturas · Gastos · Impuestos
- Trabajo › Tareas · Tablero · Proyectos
- Agenda
- Soporte › Tickets
- Documentos
- Informes
- *(separado abajo)* Ajustes › Empresa · Usuarios · Plan y pagos · Apariencia · Cerrar sesión

En móvil, la barra inferior es: Inicio · Clientes · Trabajo · Facturas · Más.

Reglas:
- Los módulos no contratados aparecen atenuados con "Probar 14 días" en lugar de ocultarse, para que se entienda por qué no están.
- Buscador global con Ctrl+K (clientes, tareas, facturas, tickets).
- Cada sección tiene su ruta en `AppDestination`.
- El elemento activo siempre está marcado.
- Al volver se conserva el filtro y la posición del scroll.

**Portal de cliente:** Inicio · Trabajo · Solicitudes · Documentos y facturas · Cuenta. La pestaña "Programas" desaparece del portal: los módulos ahora se contratan en el espacio. Mientras se migra, sigue disponible para los clientes beta.

## 7. Sistema visual: tres estilos a elegir

**Arquitectura**
- Se añade `enum SupportDeskStyle { ARTESANAL, MINIMAL, GLASS }`, independiente de claro/oscuro.
- `SupportDeskDesignTheme(style, useDarkTheme)` entrega por estilo:
  - `ColorScheme`
  - `Shapes`
  - tipografía
  - `SupportDeskElevations`
  - un token nuevo, `SupportDeskSurfaceTreatment` (borde, sombra, translucidez, desenfoque)
- Los componentes de `designsystem/components` y `ClientPortalDesign.kt` solo leen tokens: así se cambia de estilo sin tocar pantallas.
- **Antes hay que auditar los colores fijos en las pantallas.**
- **Preferencia:** cada usuario la guarda en su perfil (se sincroniza en servidor) y se cachea en local. La empresa define el estilo por defecto para usuarios nuevos y para su portal.

| | **Artesanal** (actual, por defecto) | **Minimalista** | **Glass** |
|---|---|---|---|
| Idea | Papel editorial cálido, tinta y un acento | Limpio, rápido, denso, para muchos datos | Capas translúcidas con profundidad, moderno |
| Fondo / superficie | `#F3EFE4` / `#ECE5D4` | `#FAFAF9` / `#FFFFFF` | Degradado suave cálido (`#F6E7D8`→`#EADCF2`) / blanco al 60–70 % |
| Texto | Tinta `#1B1712` | `#111111` | `#1B1712` sobre vidrio claro |
| Acento | Bermellón `#C1391F` + bronce `#8A6E3E` | Bermellón `#C1391F` como único color | Bermellón `#C1391F` + brillo cálido |
| Bordes y sombras | Línea fina 1dp; tarjeta destacada con borde de tinta de 2dp y sombra desplazada 4dp | Borde de 1dp, sin sombras | Borde blanco de 1dp al 40 %, desenfoque de 16–20dp, sombra muy suave |
| Radios | 8dp | 6dp | 16dp |
| Tipografía | Serif en títulos + Sans en texto (actual) | Solo Sans (Inter); números tabulares | Sans geométrica (Inter / Plus Jakarta Sans) |
| Densidad | Media | Alta (escala 4–8dp) | Media-baja |
| Oscuro | El actual (`#17130F`) | `#0F0F0F` / `#1A1A1A` | Tinta `#14110E` con vidrio blanco al 8 % |

**Reglas para todos los estilos**
- Contraste de texto ≥ 4.5:1 en claro y en oscuro.
- Foco visible.
- Áreas de pulsación ≥ 44–48dp.
- Iconos Lucide (nunca emojis).
- Micro-animaciones de 150–300 ms, respetando "reducir movimiento".
- El color nunca es la única señal.

**Glass en Compose**
- El desenfoque de fondo usa la librería Haze (`dev.chrisbanes.haze`), que funciona en Compose Multiplatform.
- Solo se aplica al marco: barra lateral, barra superior, modales y tarjeta principal.
- Nunca se aplica a tablas ni formularios, donde la superficie es casi opaca (≥ 85 %).
- En equipos lentos o con "reducir transparencia" activado, el desenfoque se sustituye por un tono sólido.

## 8. Cambio propuesto en CLAUDE.md (se aplica en la fase 0)

Las reglas actuales de "Facturas" pasan a llamarse **"Facturas propias de OryKai (carril A)"** y no cambian. Se añade:

> **Facturación SaaS (carril B).** El módulo de facturación que usan los espacios de empresa del SaaS sí guarda las facturas en el servidor, porque Verifactu lo exige: registro inalterable con hash encadenado, numeración correlativa por serie y ejercicio, QR, rectificativas y envío a la AEAT. Vive en paquetes propios (`features/billing/…`, `SV/billing/…`) y nunca se usa para las facturas propias de OryKai ni comparte código de almacenamiento con el carril A.

## 9. Pantallas prioritarias

- **P0:**
  - Alta (5 pasos)
  - Inicio del espacio (primeros pasos y KPIs)
  - Ajustes › Apariencia (selector de estilo con vista previa)
  - Plan y pagos (planes y módulos)
  - Ficha de cliente con datos fiscales y portal
  - Tareas y temporizador
  - Presupuesto → factura
- **P1:** facturas Verifactu (emitir, rectificar, QR), Agenda (calendario propio), Tickets, portal de cliente renovado, Usuarios y roles.
- **P2:** Informes, Gastos e impuestos, Documentos con almacenamiento real, Fichaje, Asistente IA.

## 10. Hoja de ruta

| Fase | Qué | Por qué en este orden |
|---|---|---|
| **0. Preparar** (1–2 semanas) — ✅ hecha el 23-oct-2026 salvo la fecha de fin de la beta | Hacer commit de `fix/factura-legal-autonomo`; integrar `feat/cliente-datos-fiscales` (rebase y quitar el commit `wip: snapshot`); aplicar el cambio de CLAUDE.md (§8); unificar las claves de programa (`BUSINESS_*` frente a `CUSTOMER_CONTACTS`…); fijar la fecha de fin de la beta | Partir de una base limpia |
| **1. Estilos** (2–3 semanas) — ✅ hecha el 23-oct-2026 (Glass sin Haze por ahora) | Auditar colores fijos → `SupportDeskStyle` + `SurfaceTreatment`, los 3 estilos en claro y oscuro, Ajustes › Apariencia, preferencia por usuario | Valor visible enseguida, poco riesgo, no depende de la multiempresa |
| **2. Multiempresa** (4–6 semanas) | Tablas `workspaces` y `workspace_members` (roles OWNER/ADMIN/MEMBER/GESTORÍA); migrar `owner_admin_id` → `workspace_id` (los datos de OryKai pasan a su espacio); RLS por espacio; los usuarios de portal pasan a pertenecer a los clientes del espacio; alta y onboarding | Es la base del SaaS: sin esto no se puede vender |
| **3. Planes y módulos** (3 semanas) | Catálogo de planes, extras y entitlements por espacio (se generalizan V8/V9 de cliente a espacio); comprobación de permisos en servidor y en la UI; navegación por módulos; Plan y pagos; migración de los clientes beta; cobro manual | Poder cobrar |
| **4. Facturación Verifactu, carril B** (6–8 semanas; listo antes de abr-2027) | Registro inalterable, series y ejercicio, hash encadenado, QR, rectificativas, envío a la AEAT, declaración responsable, revisión por asesor; horas → factura; exportación para la gestoría; preparar UBL para 2028 | Fecha legal de 2027 y es la principal razón de compra |
| **5. Agenda, portal e informes** | Calendario propio, portal reorientado (presupuestos, facturas, documentos), informes, almacenamiento real de documentos | Completa el "todo en uno" |
| **6. Crecimiento** | Stripe Billing (tarjeta y SEPA), conexión bancaria, fichaje, asistente IA, embudo de ventas | Más ingresos por cliente |

## 11. Riesgos

- **Responsabilidad legal de Verifactu** (hasta 150.000 €): revisión por asesor fiscal antes de lanzar; si el plazo aprieta, valorar un proveedor certificado como respaldo.
- **Fuga de datos entre empresas:** RLS por `workspace_id` y tests de acceso cruzado obligatorios en cada ruta.
- **Rendimiento de Glass** en Android modesto: alternativa sin desenfoque y tests visuales.
- **Migración de datos de OryKai y de los clientes beta:** migración reversible, probada antes con una copia de la BD.
- **Pendientes legales** de `AJUSTES_LEGAL…` §10 (DPA, cookies, proveedores): bloquean el lanzamiento público, no el desarrollo.

## 12. Verificación

- **Este documento:** aprobación del usuario. Después se guarda en `docs/product/SAAS_TODO_EN_UNO_PLAN.md` y se commitea solo cuando el usuario lo pida.
- **Cada fase posterior** tendrá su propio plan de implementación con:
  - `./gradlew :shared:jvmTest :server:test`
  - Fase 2: tests de acceso cruzado entre espacios
  - Fase 1: capturas de los 3 estilos × claro/oscuro a 375dp y 1280dp, y comprobación de contraste ≥ 4.5:1
  - Fase 4: tests de la cadena de hash y la numeración correlativa, y validación del XML contra el esquema de la AEAT en su entorno de pruebas

# Ajustes del portal cliente: especificación legal, privacidad y facturación

> **Estado:** borrador de implementación para RequenaDesk.  
> **Jurisdicción de partida:** España y Unión Europea; servicio B2B.  
> **Publicación:** requiere completar los campos marcados `[PENDIENTE]` y revisión final de un abogado y un asesor fiscal españoles. No se deben publicar documentos contractuales con datos inventados.

## 1. Decisión de producto

`Ajustes` es el centro de confianza del portal cliente. Reúne únicamente la relación entre la organización cliente y RequenaDesk: cuenta, seguridad, privacidad, contrato, suscripción, cobro y baja. No es una pantalla de configuración técnica dispersa.

La navegación propuesta es:

1. **Cuenta y seguridad**: identidad de la organización, usuarios autorizados, contraseña, sesiones, MFA cuando esté disponible y preferencias de comunicaciones.
2. **Facturación**: plan CRM, programas, datos fiscales, próxima renovación, método de pago e historial de facturas emitidas por RequenaDesk.
3. **Privacidad y datos**: política de privacidad, derechos RGPD, exportación, DPA, subencargados y transferencias.
4. **Legal**: aviso legal, términos de uso y suscripción, política de privacidad, cookies y versiones aceptadas.
5. **Dar de baja**: cancelación de renovación, cierre de cuenta, exportación y tratamiento de datos posterior.

En móvil se muestra una columna y cada opción tiene un área táctil de al menos 44 dp. La zona de baja nunca se oculta ni se convierte en un laberinto; sí exige una confirmación clara antes de cambiar el estado de la suscripción.

## 2. Dos ámbitos de facturación que nunca se mezclan

| Ubicación | Quién emite la factura | Para qué sirve |
|---|---|---|
| `Ajustes > Facturación` | RequenaDesk | Cobrar la suscripción SaaS al cliente. |
| `Programas > Facturación` | El negocio cliente | Gestionar las facturas que el cliente emite a sus compradores. |

Esto evita mezclar series, numeración, datos fiscales, permisos, documentos PDF y obligaciones normativas. Las facturas de RequenaDesk solo se consultan y descargan desde Ajustes. El programa de facturación del cliente tendrá su propia base de datos, permisos y cumplimiento fiscal.

## 3. Oferta comercial beta

Todos los importes de la interfaz se comunicarán como **sin IVA; impuestos aplicables calculados antes del cobro**, salvo que el contrato final autorice comunicar legalmente precios con impuestos incluidos.

| Programas activos | Precio mensual neto |
|---:|---:|
| CRM con un programa incluido | 30,00 EUR |
| Dos programas | 45,00 EUR |
| Tres programas | 60,00 EUR |
| Cuatro programas | 75,00 EUR |
| Cinco programas | 90,00 EUR |
| Seis programas | 105,00 EUR |
| Los siete programas | 110,00 EUR |

Regla canónica, siempre en céntimos y nunca en `Float`:

```text
if activeProgramCount == 7:
    monthlySubtotalCents = 11000
else:
    monthlySubtotalCents = 3000 + max(0, activeProgramCount - 1) * 1500
```

La beta no aplica prorrateos: las altas, bajas y cambios aprobados entran en vigor el inicio del siguiente período. La vista previa debe indicar fecha, precio neto, impuestos estimados y precio que dejará de aplicarse. Una factura finalizada conserva una instantánea de producto, precio, impuesto y destinatario; no se recalcula con tarifas futuras.

## 4. Pantalla `Ajustes > Facturación`

La pantalla usa una cabecera breve, información actual antes del histórico y un único CTA primario por bloque:

```text
Ajustes / Facturación
Plan CRM · Activo · Renovación el [fecha]

[45,00 EUR/mes + impuestos]       [Gestionar programas]
CRM RequenaDesk                    Próxima factura
Incluye 1 programa                 Fecha / base / impuestos / total

Método de pago                     Datos de facturación
Proveedor seguro · **** 4242       Razón social, NIF/VAT, dirección, email

Facturas de RequenaDesk
Número · período · fecha · total · estado · Descargar PDF

Zona de baja
[Solicitar baja al final del período]
```

Requisitos de interfaz:

- Mostrar estado con texto e icono, no solo color: `Activa`, `Pago pendiente`, `Cancelación programada`, `Suspendida`, `Cancelada`.
- El botón `Gestionar programas` lleva a Programas. Nunca permite editar un precio en el navegador.
- El método de pago enseña únicamente marca, tipo, últimos cuatro dígitos y caducidad si el proveedor la entrega. RequenaDesk no almacena PAN ni CVC.
- Si faltan datos fiscales, se muestra `Completar datos de facturación` antes de iniciar el cobro.
- En móvil, el historial se convierte en tarjetas; cada una incluye el botón accesible `Descargar PDF`.
- Una factura emitida no tiene botón Borrar. Las correcciones se realizan con una factura rectificativa enlazada.

Microcopy aprobado para la interfaz:

- `Tu plan seguirá activo hasta el [fecha].`
- `No se realizará ningún cargo hoy. El cambio se aplicará en la próxima renovación.`
- `Esta factura corresponde a tu suscripción a RequenaDesk. Las facturas que emites a tus clientes se gestionan en el programa Facturación.`
- `No guardamos los datos completos de tu tarjeta.`
- `Los impuestos se calculan según tus datos fiscales vigentes al emitir la factura.`

## 5. Baja, cierre y datos: acciones distintas

| Acción | Efecto | No implica |
|---|---|---|
| Cancelar renovación | Finaliza el servicio al terminar el período ya pagado. | Borrado inmediato de información. |
| Cerrar cuenta | Inicia el cierre operativo de la organización. | Eliminar facturas, contrato o evidencias que deban conservarse. |
| Solicitar supresión RGPD | Evalúa y ejecuta el derecho de supresión de datos personales cuando proceda. | Eliminar datos sujetos a obligación legal de conservación. |
| Darse de baja de marketing | Detiene comunicaciones comerciales. | Cancelar el servicio contratado. |

Flujo de baja:

1. El administrador lee la fecha efectiva, accesos y programas afectados.
2. Puede solicitar una exportación de datos antes de esa fecha.
3. Confirma: `Entiendo que cancelar la suscripción no equivale a suprimir inmediatamente toda la información.`
4. El servidor crea una cancelación `CANCELLING`, conserva el servicio hasta el final pagado y permite revertirla hasta la fecha definida en contrato.
5. Tras finalizar, aplica la matriz de devolución, supresión o bloqueo prevista en el DPA y en la normativa aplicable.

El motivo de baja es opcional y nunca bloquea la acción.

## 6. Privacidad y documentos legales

### Roles de privacidad

- RequenaDesk es normalmente **responsable del tratamiento** para cuentas, autenticación, seguridad, soporte propio, contratación, cobro y facturas de su SaaS.
- Respecto de datos que el cliente incorpore a sus programas —contactos, agenda, documentos, presupuestos o facturas— RequenaDesk suele ser **encargado del tratamiento**, siguiendo instrucciones del cliente.
- El contrato de encargo (DPA) es independiente de la política de privacidad. Debe describir objeto, duración, finalidad, categorías, instrucciones, subencargados, seguridad, apoyo en derechos, brechas, devolución y supresión.

### Documentos y evidencia

Los documentos se publican con versión, idioma, fecha de vigencia, URL canónica y huella SHA-256. Una versión publicada es inmutable: un cambio publica otra versión y conserva la anterior para prueba.

| Documento | Acción de usuario | Evidencia requerida |
|---|---|---|
| Aviso legal | Consultar/descargar | Documento publicado. |
| Términos y suscripción | Aceptación contractual expresa por administrador autorizado | Organización, usuario, versión, hash, fecha, canal y declaración de autoridad. |
| Política de privacidad | Informar; acuse opcional | No utilizar como consentimiento genérico. |
| Marketing | Consentimiento independiente o base legítima documentada | Finalidad, versión, fecha y retirada. |
| Cookies no esenciales | Elección granular no premarcada | Preferencias y versión de política. |
| DPA | Aceptación contractual por la organización | Versión, autoridad y fecha. |

La casilla de términos no estará premarcada. La aceptación de términos debe ser separada de marketing y de cookies. La política de privacidad explica el tratamiento; no sustituye el consentimiento cuando este sea necesario.

## 7. Modelo de datos para la última migración de Ajustes

Todas las fechas se guardan en UTC, los importes en céntimos y la moneda en ISO 4217. La aplicación y RLS verifican siempre que la organización solicitante es propietaria del recurso.

```text
legal_documents
- id, document_key, locale, version, title, effective_at, published_at
- content_markdown_or_html, canonical_url, content_sha256
- status (DRAFT/PUBLISHED/RETIRED), replaces_document_id
- created_by, approved_by, approved_at

legal_acceptances
- id, organization_id, user_id, legal_document_id, document_version, content_sha256
- acceptance_type (TERMS/DPA/PRIVACY_NOTICE_ACK)
- accepted_at, channel, authority_attestation, evidence_id

privacy_consents
- id, organization_id nullable, user_id, purpose, legal_document_id, version
- granted_at, withdrawn_at, collection_method, evidence_id

cookie_preferences
- id, pseudonymous_browser_or_device_id, policy_version
- essential, analytics_choice, marketing_choice, selected_at, withdrawn_at, expiry_at

data_subject_requests
- id, requester_type, requester_reference, organization_id nullable, controller_scope
- right_type, received_at, due_at, status, identity_verification_status, decision, response_at

subprocessor_registry
- id, name, service, processing_purpose, location, data_categories
- transfer_mechanism, dpa_reference, active_from, active_to, customer_notice_version

billing_profiles
- id, organization_id UNIQUE, legal_name, trade_name, tax_id, vat_id, tax_country
- billing_email, billing_contact_name, address_line1, address_line2
- postal_code, city, region, country_code, validation_status, updated_at, updated_by

subscription_contracts
- id, organization_id, status, currency, interval, period_start_at, period_end_at
- cancel_at_period_end, canceled_at, provider_customer_id, provider_subscription_id
- accepted_terms_version, accepted_at, created_at

subscription_items
- id, subscription_id, program_code
- item_kind (BASE/INCLUDED_PROGRAM/ADDON/ALL_PROGRAMS_BUNDLE)
- quantity, unit_amount_cents, effective_from, effective_until, price_catalog_version, status

subscription_change_requests
- id, organization_id, subscription_id, requested_by, requested_at
- requested_items_snapshot_json, price_preview_cents, tax_preview_json
- effective_at, status, reviewed_by, reviewed_at, admin_reason

billing_invoices
- id, organization_id, subscription_id, issuer_snapshot_json, recipient_snapshot_json
- series, sequence_number, invoice_number UNIQUE, issue_date
- service_period_start, service_period_end, currency, subtotal_cents, tax_cents, total_cents
- tax_breakdown_json, status, provider_invoice_id, pdf_storage_key, finalized_at

billing_invoice_lines
- id, invoice_id, position, item_snapshot_json, description, quantity
- unit_amount_cents, subtotal_cents, tax_rate_basis_points, tax_amount_cents, total_cents

payment_attempts
- id, invoice_id, provider_payment_id UNIQUE, status, amount_cents, currency
- payment_method_type, card_brand, card_last4, failure_code, attempted_at, paid_at

billing_webhook_events
- provider_event_id UNIQUE, event_type, received_at, processed_at, payload_hash, processing_status

subscription_cancellations
- id, organization_id, requested_by, requested_at
- type (RENEWAL_CANCEL/ACCOUNT_CLOSE), effective_at, reason nullable
- export_requested_at, confirmation_at, contract_version, status

legal_audit_events / billing_audit_log
- id, organization_id nullable, actor_type, actor_id, action/event_type
- target_type, target_id, before_json, after_json, immutable_payload_hash, occurred_at
```

Invariantes que se aplican en API y base de datos:

- Facturas y documentos legales finalizados son inmutables; no se eliminan desde el portal.
- Rectificaciones crean un documento enlazado, no reescriben el original.
- Los webhooks de pago son firmados e idempotentes por `provider_event_id`.
- La numeración de facturas se asigna con transacción y bloqueo por serie y ejercicio.
- Las instantáneas de precio y datos fiscales permanecen junto a cada factura.
- Los logs de auditoría son append-only. IP y agente de usuario se minimizan, se protegen y solo se retienen si existe una necesidad de seguridad o prueba documentada.
- El cliente solo consulta su propia organización. El administrador puede aprobar cambios, pero no editar facturas finalizadas ni aceptaciones históricas.

## 8. Seguridad y cumplimiento antes del lanzamiento

- RLS real por organización más autorización en backend: RLS no sustituye comprobar el rol en cada endpoint.
- MFA para administradores y elevación de sesión para exportar datos, cambiar datos fiscales, gestionar pagos o solicitar baja.
- Contraseñas con hash fuerte; las claves SBS solo se muestran de forma controlada al generarse y nunca se registran completas.
- TLS, secretos fuera del repositorio, copias cifradas, prueba periódica de restauración, minimización de logs y registro de acciones sensibles.
- Registro de accesos, cambios fiscales, cambios de programa, exportaciones, aceptaciones, aprobaciones y bajas.
- Procedimiento de incidentes y evaluación de brechas. Cuando proceda, el responsable debe notificar a la autoridad de control en 72 horas.
- Inventario real de cookies, SDKs, analítica, IA, almacenamiento, correo, soporte y subencargados antes de publicar la política.

## 9. Alcance prudente para facturación y fiscalidad

La primera beta debe limitarse a empresas españolas B2B, precios netos, una moneda, sin prorrateos y sin descuentos manuales. Los impuestos se calculan con un motor fiscal y datos validados, no con un 21 % fijado en la interfaz.

El programa comercial `Facturación` no debe comercializarse como conforme con VERI*FACTU ni emitir documentos fiscales en nombre de un cliente hasta completar un diseño específico de integridad, conservación, accesibilidad, legibilidad, trazabilidad e inalterabilidad. La función histórica `borrar factura` se limita a borradores; una factura emitida se rectifica, no se borra.

## 10. Información pendiente que bloquea la publicación, no el diseño

Antes de activar los textos y el cobro se debe aportar:

1. Razón social, NIF, domicilio, Registro Mercantil si aplica, email, teléfono y dominio legal.
2. Países de venta, idioma, ley y jurisdicción aplicables, y confirmación B2B exclusivo o flujo B2C.
3. Renovación, fecha efectiva de baja, impago, reembolsos, promociones, impuestos y quién factura.
4. Proveedor de pago, proveedor de alojamiento, correo, soporte, analítica, IA y sus transferencias internacionales/DPA.
5. Inventario de datos, finalidades, bases jurídicas, plazos de conservación y medidas de seguridad realmente implantadas.
6. Contacto de privacidad, decisión sobre DPO y política de incidentes.
7. Inventario de cookies/SDKs y procedimiento de exportación, devolución, supresión y bloqueo tras baja.
8. Revisión de abogado y asesor fiscal del contenido de `DRAFT_TERMINOS_DE_USO_ES.md` y `DRAFT_POLITICA_DE_PRIVACIDAD_ES.md`.

## 11. Fuentes oficiales y técnicas

- [Ley 34/2002 de servicios de la sociedad de la información (LSSI)](https://www.boe.es/buscar/act.php?id=BOE-A-2002-13758): identificación del prestador, precios e información previa/posterior a la contratación electrónica.
- [RGPD, Reglamento (UE) 2016/679](https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=es): información al interesado, bases jurídicas, derechos, responsabilidad proactiva y seguridad.
- [AEPD: contenido del contrato de encargo](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/8-responsable-y-encargado-del-tratamiento/FAQ-0238-cual-seria-el-contenido-del-contrato-de-encargo-de-tratamiento): contenido mínimo del DPA.
- [AEPD: información no equivale a consentimiento](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/6-el-deber-de-informacion/FAQ-0248-sobre-si-el-usuario-tiene-que-dar-consentimiento-a-clausula-de-privacidad): separación entre deber de informar y consentimiento.
- [AEPD: ejercicio de derechos](https://www.aepd.es/derechos-y-deberes/ejerce-tus-derechos): derechos y plazos de respuesta.
- [AEPD: guía sobre cookies](https://www.aepd.es/es/documento/guia-cookies.pdf): consentimiento y tecnologías no esenciales.
- [Código de Comercio, art. 30](https://www.boe.es/buscar/act.php?id=BOE-A-1885-6627): conservación mercantil general de libros y justificantes.
- [LOPDGDD, art. 32](https://www.boe.es/buscar/act.php?id=BOE-A-2018-16673): bloqueo de datos.
- [Real Decreto 1619/2012](https://www.boe.es/buscar/act.php?id=BOE-A-2012-14696): requisitos de facturación y rectificación.
- [Real Decreto 1007/2023](https://www.boe.es/buscar/act.php?id=BOE-A-2023-24840): requisitos de sistemas informáticos de facturación.
- [AEAT: tipos de IVA](https://sede.agenciatributaria.gob.es/Sede/ayuda/manuales-videos-folletos/manuales-practicos/manual-iva-2025/capitulo-04-sujetos-pasivos-repercusion-impositivo/tipo-impositivo.html): tipos de IVA y no codificar un único supuesto fiscal.
- [AEAT: prestaciones de servicios](https://sede.agenciatributaria.gob.es/Sede/iva/iva-operaciones-comercio-exterior/prestaciones-servicios.html): particularidades de operaciones B2B transfronterizas.
- [Reglamento Delegado (UE) 2018/389](https://eur-lex.europa.eu/eli/reg_del/2018/389/oj/eng): autenticación reforzada en pagos.
- [Stripe Customer Portal](https://docs.stripe.com/customer-management): referencia técnica para gestión segura de métodos de pago e historial, si se eligiera ese proveedor.

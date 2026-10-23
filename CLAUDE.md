# Instrucciones del proyecto

## Facturas propias de OryKai (carril A)

Estas reglas aplican a las facturas que OryKai emite a sus clientes desde la app de admin (`AdminInvoicesScreen`, `features/invoices/…`).

- Las facturas son documentos PDF generados bajo demanda y guardados localmente en `Escritorio/Facturas OryKai`.
- No se deben persistir facturas, sus lineas, metadatos ni URLs de descarga en PostgreSQL ni en ninguna otra base de datos.
- No crear migraciones, tablas, secuencias, repositorios de persistencia ni endpoints CRUD para facturas. La numeracion no escribe en la base de datos: el correlativo vive solo en un fichero local junto a los PDF.
- La creacion, listado y apertura de facturas no pueden usar Ktor, HTTP, endpoints del servidor, tokens de sesion ni fallbacks de descarga. El PDF se genera directamente en el proceso JVM con los datos del formulario.
- La pantalla de creacion debe permitir seleccionar una o varias tareas del mismo cliente. Cada linea usa las horas registradas de esa tarea, redondeadas siempre hacia arriba a horas completas.
- La biblioteca de facturas debe leer exclusivamente los PDF locales y abrirlos con el visor PDF predeterminado del sistema.

## Facturación SaaS (carril B)

- El módulo de facturación que usan los espacios de empresa del SaaS sí guarda las facturas en el servidor, porque Verifactu lo exige: registro inalterable con hash encadenado, numeración correlativa por serie y ejercicio, QR, rectificativas y envío a la AEAT.
- Vive en paquetes propios (`features/billing/…` en `shared`, `server/…/billing/…` en el servidor) y nunca se usa para las facturas propias de OryKai.
- No comparte código de almacenamiento ni de numeración con el carril A, y el carril A no puede depender de él.
- Las facturas emitidas no se borran ni se editan: solo se corrigen con una factura rectificativa enlazada.
- Plan de producto: `docs/product/SAAS_TODO_EN_UNO_PLAN.md` (§8 y fase 4).

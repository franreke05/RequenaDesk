# Loop de ejecución: siete programas beta gratuitos

## Decisión innegociable de beta

Los siete programas se pueden solicitar desde el portal, pero permanecen **gratuitos** y sin facturación mientras se prueban. El acceso solo se concede después de que un administrador autorice la solicitud.

```text
Cliente solicita programa
      ↓
Servidor registra REQUESTED + auditoría
      ↓
Administrador revisa y decide
      ↓
REJECTED ──→ no hay acceso
APPROVED ──→ suscripción ACTIVE a 0 EUR
      ↓
El guard de servidor concede las rutas del programa
```

La aplicación cliente puede ocultar o desactivar botones por UX, pero no concede acceso: cada API protege sus recursos verificando el `clientId` del token y el entitlement `ACTIVE` del programa.

## Catálogo beta

| Clave | Programa | Equipo responsable |
|---|---|---|
| `BUSINESS_INVOICING` | Facturación (borradores/proformas) | Finanzas |
| `BUSINESS_ACCOUNTING` | Contabilidad y gastos | Finanzas |
| `BUSINESS_CUSTOMERS` | Clientes y contactos | Ventas |
| `BUSINESS_QUOTES` | Presupuestos y ventas | Ventas |
| `BUSINESS_CATALOG` | Productos, servicios y stock | Ventas |
| `BUSINESS_BOOKINGS` | Agenda y reservas | Operaciones |
| `BUSINESS_DOCUMENTS` | Documentos y confirmaciones | Operaciones |

`V9__business_programs_free_beta_catalog.sql` es la frontera comercial temporal: establece los siete productos a `0 EUR`, desactiva los dos productos comerciales heredados del catálogo y permite registrar una aprobación gratuita. El backend rechaza cualquier aprobación con importe distinto de cero.

## Bucle de trabajo por vertical

Cada equipo repite este ciclo antes de abrir el siguiente vertical:

1. **Investigar:** necesidad real, normativa y riesgos del programa.
2. **Especificar:** flujo de usuario, modelo de datos, permisos, UX, estados, errores y pruebas.
3. **Construir:** modelo y migración, API Ktor con guard de entitlement, repositorio/state holder, pantalla Compose responsive.
4. **Verificar:** pruebas de creación/edición/listado, acceso sin autorización `403`, aislamiento de otro `clientId`, validación de entrada y reintentos sin duplicados.
5. **Revisar:** prueba manual móvil/escritorio con datos vacíos, largos, erróneos y con carga; corregir antes de pasar al siguiente programa.

No se promociona un programa a disponible para clientes reales mientras no haya pasado las cinco etapas.

## Contratos comunes de implementación

- **Fuente de verdad:** PostgreSQL y rutas Ktor; el cliente solo refleja su estado.
- **Aislamiento:** toda tabla de negocio contiene `client_id`; cada consulta y mutación filtra ese identificador de la sesión autenticada.
- **Autorización:** cada endpoint llama a un guard de entitlement del programa antes de leer o escribir. El administrador conserva exclusivamente la decisión de acceso en la cola existente.
- **Auditoría:** guardar creación, actualización, cambio de estado y actor; las confirmaciones y versiones de documentos no se sobrescriben.
- **Dinero:** importes en céntimos y moneda ISO; no usar `Float`.
- **Fechas:** UTC para auditoría; fechas/horas de reserva con zona IANA y representación local explícita.
- **Archivos:** referencias de almacenamiento privado y hash; nunca rutas públicas ni binarios no verificados en la base de datos.
- **Accesibilidad:** áreas táctiles de 44 dp, texto no truncado para datos críticos, estados no comunicados solo por color, formularios con errores próximos al campo.
- **Errores y concurrencia:** solicitudes idempotentes donde proceda; inventario con revisión/actualización atómica; reservas sin solape en servidor.

## Límites explícitos de la beta

- Facturación: borradores/proformas comerciales, claramente marcados como **no válidos como factura fiscal**. No se afirma cumplimiento VERI*FACTU/SIF.
- Contabilidad: control operativo de ingresos/gastos; no libros contables oficiales, impuestos ni asesoría.
- Firma: aceptación autenticada y evidencias básicas; no se presenta como firma electrónica avanzada ni cualificada.
- Cobro: no hay pasarela, factura de suscripción, prorrateo ni cargo durante esta fase.

## Prompts y especificaciones de los equipos

- [Finanzas: Facturación y contabilidad](FINANCE_PROGRAMS_BETA_SPEC.md)
- [Ventas: clientes, presupuestos y catálogo](SALES_PROGRAMS_BETA_SPEC.md)
- [Operaciones: reservas y documentos](OPERATIONS_PROGRAMS_BETA_SPEC.md)

Cada documento incluye el prompt de su vertical, fuentes de investigación, modelo de datos, rutas, criterios de aceptación, pruebas y riesgos. Esta página es el contrato de integración entre los tres equipos.

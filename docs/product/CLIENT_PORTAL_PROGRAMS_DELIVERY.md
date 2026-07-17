# Centro de Cliente: Trabajo y Programas

## Entrega V8

El portal de cliente queda organizado en cuatro destinos principales:

| Destino | Propósito |
|---|---|
| Inicio | Estado general, tareas y soporte contextual. |
| Trabajo | Seguimiento de tareas y solicitudes. |
| Programas | Catálogo de utilidades, programas activos e historial de solicitudes. |
| Cuenta | Datos de empresa, plan y acceso. |

`Solicitar ayuda` se conserva como una acción visible y `Servicio` como una vista interna. Esto evita competir con el trabajo diario en la navegación principal.

## Flujo comercial

1. El cliente selecciona uno o varios programas del catálogo y puede añadir una nota.
2. Ktor identifica el `clientId` desde el token, valida el catálogo y crea una solicitud `REQUESTED`; el cliente no envía ni decide el precio.
3. En la ficha del cliente, el administrador ve la solicitud, introduce la cuota mensual en EUR y aprueba o rechaza.
4. Una aprobación crea/activa la suscripción, fija el precio acordado y registra el evento de auditoría.
5. La ficha del cliente muestra la cuota mensual activa. Al crear una factura, administración puede importarla como actividades sugeridas al PDF local.

El catálogo inicial incluye `SERVICE_SLA` y `SHEETS`. El portal muestra únicamente lo que el servidor autoriza; una tarjeta activa no finge que existe un espacio de trabajo que todavía no se ha implementado.

## Límites deliberados

- No hay cobro automático ni integración de pagos en esta entrega.
- Las facturas, sus líneas y PDFs permanecen locales; el servidor solo conserva catálogo, solicitudes, suscripciones y su auditoría.
- El cliente nunca puede aprobar, modificar precio ni consultar las solicitudes de otra empresa.
- Las nuevas tablas tienen RLS habilitado y se revoca acceso directo de `PUBLIC`, `anon` y `authenticated`; el acceso de producto pasa por Ktor y su JWT.

## Datos y seguridad

V8 incorpora `product_catalog`, `client_product_subscriptions`, `client_program_requests` y `client_subscription_events`. Las cuotas se guardan como céntimos enteros con moneda EUR, y las decisiones requieren un precio mayor que cero.

El backend aplica autorización en todas las rutas de programas, limita las mutaciones sensibles y controla que cada administrador solo gestione clientes de su ámbito. La auditoría conserva quién solicitó, aprobó o rechazó y cuándo.

## Próxima ampliación recomendada

Antes de ofrecer un nuevo programa, debe existir una utilidad funcional detrás de él (por ejemplo, una hoja de datos colaborativa o un módulo de aprobaciones), su permiso de servidor y pruebas de aislamiento. Los pagos recurrentes y la IA siguen siendo iniciativas separadas con sus propias decisiones de proveedor, consentimiento, privacidad y retención.

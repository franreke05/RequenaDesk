# Despliegue V19: planes y módulos por empresa (fase 3 del plan SaaS)

Rama `feat/planes`, que va **después** de `feat/multiempresa`: despliega primero V17–V18
(`docs/V17_V18_MULTIEMPRESA_DEPLOYMENT.md`) o ambas a la vez; Flyway las aplica en orden al
arrancar Ktor. Flyway no revierte migraciones: la vuelta atrás es restaurar la copia de
seguridad.

## Qué cambia

- **V19**:
  - Catálogo de planes `FREE`, `AUTONOMO`, `PRO`, `TEAM` y `BETA` (este último no se puede contratar).
  - Una suscripción por empresa, un historial de cambios y las solicitudes de cambio de plan.
  - `organizations.is_platform_operator`, activado solo para OryKai.
- **Empresas existentes**: pasan al plan **BETA**, que incluye todo y es gratis. **Nada cambia para OryKai ni
  para las empresas de la beta.**
- **Empresas nuevas** (alta): 14 días de prueba **Pro**. Al terminar pasan a **Gratis**:
  Núcleo más un módulo, según su elección o sus respuestas del onboarding. **Nunca se cobra
  automáticamente.**
- **Bloqueo por módulo** (solo en las pantallas del equipo):
  - Tareas, Tablero, Etiquetas, horas y temporizador forman parte de **Trabajo y horas**.
  - Tickets forma parte de **Soporte**.
  - Si el plan no incluye el módulo, el servidor responde 402 y la app muestra la sección con "Incluido en…" y lleva a Plan y pagos.
  - Los datos no se borran nunca.
- **Límites**: Gratis tiene 25 clientes y 1 usuario; Autónomo, 1 usuario; Pro, 3; Equipo, 8. La gestoría no cuenta.
- **Cambios de plan**:
  - La empresa (dueño o admin) lo solicita desde Ajustes › Plan y pagos, aceptando el precio del servidor.
  - OryKai lo aprueba en Ajustes › Solicitudes de plan.
  - **La factura la emites tú a mano** (carril A); no hay pasarela de pago.
  - Pasar a Gratis se aplica al momento y no necesita aprobación.
- **Corrección de seguridad**: aprobar o rechazar programas del portal pasa a exigir ser dueño o admin. Antes podía hacerlo cualquier miembro.
- No hay variables de entorno nuevas y el `Caddyfile` no cambia.

## 1. Validación local

```bash
cd /c/Users/franr/AndroidStudioProjects/CRMFreelance
./gradlew.bat :server:test :shared:jvmTest :composeApp:jvmTest :composeApp:compileKotlinJvm :server:installDist --no-daemon
```

## 2. Copia de seguridad (obligatoria)

Supabase › Database › Backups › backup manual, y opcionalmente:

```bash
pg_dump "$SUPABASE_DATABASE_URL" --format=custom --no-owner --file=requenadesk-antes-v19-$(date +%Y%m%d-%H%M).dump
```

## 3. Despliegue

Después de integrar en `main`:

```bash
ssh deploy@srv1289731
cd /opt/requenadesk/RequenaDesk
git status --short
git fetch origin
git pull --ff-only origin main
/home/deploy/deploy-requenadesk.sh
```

## 4. Verificación

```bash
sudo journalctl -u requenadesk -n 200 --no-pager | grep -Ei 'version "19|organization plans|successfully applied|error'
curl -i http://127.0.0.1:8080/health/ready
```

En Supabase:

```sql
-- Todas las empresas existentes en BETA; OryKai es el operador.
SELECT o.name, s.plan_key, s.status, o.is_platform_operator
FROM organizations o JOIN organization_subscriptions s ON s.organization_id = o.id
ORDER BY o.created_at;

-- Cinco planes con sus precios (céntimos/mes, sin IVA).
SELECT plan_key, monthly_price_cents, annual_monthly_price_cents, included_users, max_clients
FROM subscription_plans ORDER BY sort_order;
```

## 5. Prueba manual mínima

1. Entra como OryKai. En Ajustes › Plan y pagos debe aparecer **Beta**, con todo incluido, y debe existir la pestaña
   **Solicitudes de plan**.
2. Con el alta activada en un entorno de pruebas, crea una empresa: verás "Estás probando Pro: te quedan 14 días".
3. Para simular el fin de la prueba en ese entorno:
   `UPDATE organization_subscriptions SET trial_ends_at = NOW() - INTERVAL '1 minute' WHERE organization_id = '<id>';`
   En menos de 30 segundos la empresa pasa a Gratis y Tareas o Tickets aparecen como "Incluido en…".
4. Solicita Pro desde esa empresa y apruébalo como OryKai: la sección vuelve en menos de 30 segundos.

## Cuándo termine la beta

`beta_ends_on` y `founder_discount_percent` ya existen, pero **todavía no se aplican**. Cuando
fijes la fecha, avisa con 60 días. El cambio de BETA a un plan de pago con el 50 % de
"fundadores" queda para una fase posterior. No hay ninguna tarea programada que cambie planes por
su cuenta.

## Vuelta atrás

Para el servicio, restaura el backup del paso 2 y vuelve a la versión anterior desde
`/opt/requenadesk/app-dist.backup-<fecha>`. Sin restaurar la base de datos el servidor
anterior también funcionaría, porque V19 solo añade tablas y una columna. Aun así, restaurar es lo seguro.

# Despliegue V17–V18: multiempresa (fase 2 del plan SaaS)

Rama: `feat/multiempresa`. Ejecuta los comandos desde Git Bash (local) y por SSH en el
servidor, igual que en `V15_BUSINESS_PROGRAMS_DEPLOYMENT.md`. Flyway aplica las
migraciones solo al arrancar Ktor; **no ejecutes SQL de migración a mano** y **Flyway no
las revierte**: la vuelta atrás es restaurar la copia de seguridad.

## Qué cambia

- **V17 `organizations`**: empresas, miembros con rol (OWNER/ADMIN/MEMBER/ACCOUNTANT),
  invitaciones, aceptaciones legales y `organization_id` en `clients`, `task_labels`,
  `tasks` y `work_sessions`.
  - Cada admin existente pasa a dueño de su propia empresa; la del admin más antiguo se
    llama **OryKai**.
  - El email de cliente pasa a ser único **por empresa** (antes, en todo el sistema).
- **V18 `local_pdf_invoices`**: solo la empresa de OryKai sigue viendo la pantalla de
  Facturas en PDF local (carril A de `CLAUDE.md`).
- **Seguridad**: todas las consultas filtran por empresa y fallan en cerrado; los tokens se
  comprueban contra el estado actual del usuario (bajas y cambios de rol al momento).
- **Sesiones abiertas**: los tokens emitidos antes de V17 no llevan empresa y se rechazan
  con 401. La app ya refresca la sesión sola ante un 401, así que **nadie tiene que volver a
  iniciar sesión**. Las versiones antiguas de la app de escritorio siguen funcionando.
- **Rutas nuevas**: `/auth/signup`, `/auth/signup/status`, `/auth/join`, `/org`,
  `/org/members`, `/org/invitations`.
- **Variable nueva** `SUPPORTDESK_SIGNUP_ENABLED`: el alta pública de empresas. **Déjala
  sin poner** (apagada) hasta tener proveedor de email y textos legales definitivos
  (`docs/legal/AJUSTES_LEGAL_Y_FACTURACION_ES.md` §10). Las invitaciones con código
  funcionan aunque el alta esté apagada.
- El `Caddyfile` no cambia.

## 1. Validación local

```bash
cd /c/Users/franr/AndroidStudioProjects/CRMFreelance
./gradlew.bat :server:test :shared:jvmTest :composeApp:jvmTest :composeApp:compileKotlinJvm :server:installDist --no-daemon
git diff --check
```

Los tests incluyen la migración V17/V18 sobre Postgres embebido con datos de dos admins
(ninguna fila se pierde ni cambia de empresa) y el aislamiento entre empresas.

## 2. Comprobación previa en producción (solo lectura)

En Supabase › SQL Editor. Todas deben devolver **0**; si alguna no, para y revísalo antes
de desplegar, porque V17 no podría asignar esas filas a una empresa y la migración se
desharía entera:

```sql
SELECT 'clients' AS tabla, COUNT(*) FROM clients c LEFT JOIN users u ON u.id = c.owner_admin_id WHERE u.role IS DISTINCT FROM 'ADMIN'
UNION ALL
SELECT 'task_labels', COUNT(*) FROM task_labels l LEFT JOIN users u ON u.id = l.owner_admin_id WHERE u.role IS DISTINCT FROM 'ADMIN'
UNION ALL
SELECT 'tasks', COUNT(*) FROM tasks t LEFT JOIN users u ON u.id = t.owner_admin_id WHERE u.role IS DISTINCT FROM 'ADMIN'
UNION ALL
SELECT 'work_sessions', COUNT(*) FROM work_sessions w LEFT JOIN users u ON u.id = w.owner_admin_id WHERE u.role IS DISTINCT FROM 'ADMIN';
```

Y anota cuántas filas hay ahora, para compararlas después:

```sql
SELECT (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM clients) AS clients,
       (SELECT COUNT(*) FROM tasks) AS tasks, (SELECT COUNT(*) FROM task_labels) AS labels,
       (SELECT COUNT(*) FROM work_sessions) AS sessions, (SELECT COUNT(*) FROM tickets) AS tickets;
```

## 3. Copia de seguridad (obligatoria)

- Supabase Dashboard › Database › Backups › crear un backup manual (o confirmar que el
  automático es de hoy).
- Recomendado además un volcado propio desde una máquina con `pg_dump` 17:

```bash
pg_dump "$SUPABASE_DATABASE_URL" --format=custom --no-owner --file=requenadesk-antes-v17-$(date +%Y%m%d-%H%M).dump
```

## 4. Despliegue del servidor

Después de integrar `feat/multiempresa` en `main`:

```bash
ssh deploy@srv1289731
cd /opt/requenadesk/RequenaDesk
git status --short
git fetch origin
git pull --ff-only origin main
/home/deploy/deploy-requenadesk.sh
```

El script pide confirmar el backup y guarda la versión anterior en
`/opt/requenadesk/app-dist.backup-<fecha>`.

## 5. Verificación

```bash
git log -1 --oneline
sudo journalctl -u requenadesk -n 200 --no-pager \
  | grep -Ei 'version "18|organizations|local pdf invoices|successfully applied|error'
curl -i http://127.0.0.1:8080/health/ready
curl -s https://crm.franciscorequena.cloud/auth/signup/status   # debe decir "enabled":false
```

En Supabase:

```sql
-- Una empresa por admin; OryKai con las facturas locales.
SELECT o.name, o.local_pdf_invoices, u.email AS owner
FROM organizations o
JOIN organization_members m ON m.organization_id = o.id AND m.role = 'OWNER'
JOIN users u ON u.id = m.user_id
ORDER BY o.created_at;

-- Ninguna fila sin empresa (deben ser 0) y los totales iguales a los del paso 2.
SELECT (SELECT COUNT(*) FROM clients WHERE organization_id IS NULL) +
       (SELECT COUNT(*) FROM tasks WHERE organization_id IS NULL) AS sin_empresa;
```

## 6. Prueba manual mínima

1. Abre la app de escritorio con tu sesión de siempre: debe entrar sin pedir contraseña
   (se refresca sola) y mostrar **OryKai** y tu rol en la barra lateral.
2. Como dueño de OryKai verás el **onboarding** (4 pasos cortos). Complétalo o sáltalo; si
   prefieres no verlo, márcalo como hecho:
   `UPDATE organizations SET onboarding_completed_at = NOW() WHERE name = 'OryKai';`
3. Ajustes › Usuarios: crea una invitación de **Miembro**, entra con ella en otro equipo
   ("Tengo un código de invitación") y comprueba que ve los clientes de OryKai pero no
   Ajustes › Usuarios.
4. Quita el acceso a ese miembro y comprueba que su sesión deja de funcionar enseguida.
5. Un usuario del portal de cliente entra y ve sus tickets como siempre.
6. Facturas sigue apareciendo para OryKai y genera el PDF en `Escritorio/Facturas OryKai`.

## Vuelta atrás

1. `sudo systemctl stop requenadesk`.
2. Restaura el backup del paso 3 (Supabase › Backups › Restore, o `pg_restore` del volcado).
3. Vuelve a poner la versión anterior desde `/opt/requenadesk/app-dist.backup-<fecha>` y
   `sudo systemctl start requenadesk`.

Sin restaurar la base de datos no se puede volver a la versión anterior del servidor: el
código antiguo no conoce `organization_id` y la unicidad de emails de cliente ha cambiado.

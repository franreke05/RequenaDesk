# Actualizar el servidor Ktor en producción

> Los pasos mecánicos de esta guía (git pull, detener, arrancar, comprobar
> salud) están automatizados en [`update-server.ps1`](../../update-server.ps1)
> en la raíz del proyecto. Ejecuta `.\update-server.ps1 -DryRun` primero para
> ver qué haría, y `Get-Help .\update-server.ps1 -Full` para ver todos los
> parámetros (servicio de Windows vs. proceso de consola, smoke test del
> endpoint nuevo, etc.). El backup de la base de datos (Paso 1) sigue siendo
> manual: el script solo te pide confirmarlo.

Esta guía asume el mismo montaje que `public-windows-server.md` y
`KTOR_POSTGRES_CADDY_REPAIR.md`: un PC/servidor Windows con el repositorio
clonado, Ktor escuchando en `127.0.0.1:8080` y Caddy publicando
`crm.franciscorequena.cloud` en `443`. No es un VPS Linux con systemd/Docker;
es Windows + `gradlew.bat :server:run`.

## Qué cambia en esta actualización concreta

- Migración nueva `V13__work_sessions_and_segments.sql`: crea `work_sessions`
  y `work_segments`, añade columnas a `time_logs` (`session_id`,
  `is_historic_migrated`), y hace un backfill idempotente de horas
  históricas. Flyway la aplica sola al arrancar Ktor, igual que las
  anteriores — **no ejecutes SQL a mano**.
- Endpoints nuevos: `/admin/work-sessions*`, `GET /admin/tasks/{id}/work-sessions`.
- Endpoint retirado: `POST /admin/time-logs` (creación manual de tiempo). El
  `GET` se mantiene.
- El `Caddyfile` **no cambia** (sigue apuntando a `127.0.0.1:8080`); no hace
  falta tocar Caddy para este despliegue.

## Paso 0 — Identifica cómo tienes Ktor corriendo ahora mismo

```powershell
Get-CimInstance Win32_Process -Filter "CommandLine like '%server:run%'" |
    Select-Object ProcessId, CommandLine
netstat -ano | findstr :8080
```

- Si aparece un proceso `java`/`gradlew` con `:server:run` en la línea de
  comandos: lo estás ejecutando como en `start-ktor-server.ps1` (consola o
  sesión RDP). Sigue la **Opción A** más abajo.
- Si no aparece nada con Gradle pero el puerto 8080 sí está escuchando:
  probablemente lo registraste tú mismo como servicio de Windows (NSSM, Task
  Scheduler, etc. — no hay ningún script de este tipo en el repo). Sigue la
  **Opción B** y adapta el nombre del servicio/tarea al que hayas creado.

## Paso 1 — Copia de seguridad antes de migrar

Esta actualización sí toca el esquema (V13). Antes de aplicar nada:

- Supabase Dashboard → tu proyecto → **Database → Backups** → crear un
  backup manual (o confirmar que el backup automático más reciente es de
  hoy).
- Si tienes acceso directo a `psql`/`pg_dump`, un dump adicional no sobra.

## Paso 2 — Trae el código nuevo al servidor

```powershell
cd C:\ruta\a\CRMFreelance
git status
```

Si `git status` muestra cambios locales sin commitear en esa máquina
(algo editado directamente en producción), resuélvelos primero — no los
descartes a ciegas.

```powershell
git fetch origin
git pull origin main
```

## Paso 3 — Confirma la configuración (no cambia en esta actualización)

`supportdesk.properties` no necesita valores nuevos para esta versión
(mismo host, puerto y secreto). Solo confirma que sigue ahí:

```powershell
Test-Path .\supportdesk.properties
```

Si por lo que sea no existiera, regenéralo con `create-server-config.ps1`
(ver `public-windows-server.md`, sección 2) — pero si el servidor ya
funciona hoy, este archivo ya debería existir y no hace falta tocarlo.

## Paso 4 — Detén el proceso actual de Ktor

**Opción A — lo tienes corriendo vía `gradlew :server:run`:**

Si tienes la ventana de consola a la vista, `Ctrl+C` ahí mismo es lo más
limpio (deja que Ktor cierre HikariCP ordenadamente). Si no tienes acceso a
esa ventana (p.ej. sesión RDP distinta):

```powershell
Get-CimInstance Win32_Process -Filter "CommandLine like '%server:run%'" |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

**Opción B — lo tienes como servicio de Windows:**

```powershell
Stop-Service <nombre-del-servicio>
# o, si lo registraste con sc.exe:
sc.exe stop <nombre-del-servicio>
```

Confirma que el puerto queda libre antes de continuar:

```powershell
netstat -ano | findstr :8080
```

## Paso 5 — Arranca la nueva versión

```powershell
.\start-ktor-server.ps1
```

Vigila el log de arranque. Debe mostrar Flyway aplicando la migración 13
(`Successfully applied 1 migrations ... now at version "13"` o similar) sin
errores, y después el mensaje de Ktor escuchando en `127.0.0.1:8080`.

Si usabas la Opción B (servicio), reinicia ese servicio en vez de correr el
script directamente, y revisa su log configurado.

## Paso 6 — Verifica el backend local (sin pasar por Caddy)

```powershell
Invoke-RestMethod http://127.0.0.1:8080/health/live
Invoke-RestMethod http://127.0.0.1:8080/health/ready
```

Ambas deben responder 200 y `ready` debe indicar la base disponible.

Comprobación específica de esta actualización — el endpoint nuevo debe
responder (no 404/405):

```powershell
$credenciales = @{ email = "admin@tu-dominio"; password = "..." } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/auth/login `
    -Body $credenciales -ContentType "application/json"
Invoke-RestMethod -Uri http://127.0.0.1:8080/admin/work-sessions/active `
    -Headers @{ Authorization = "Bearer $($login.accessToken)" }
```

Con ningún temporizador activo debe devolver `"data": null`, estado 200.

## Paso 7 — Verifica en público (a través de Caddy)

```powershell
Invoke-RestMethod https://crm.franciscorequena.cloud/health/live
Invoke-RestMethod https://crm.franciscorequena.cloud/health/ready
```

No hace falta recargar Caddy: el `Caddyfile` no cambió con esta
actualización (sigue apuntando a `127.0.0.1:8080`).

## Paso 8 — Prueba real desde la app de escritorio

Abre la app Desktop apuntando a la URL pública e inicia un temporizador en
una tarea real, pausa, reanuda, detén, y comprueba que el historial y el
calendario reflejan lo esperado. Intenta iniciar un segundo temporizador en
otra tarea mientras el primero sigue activo: debe rechazarse con un mensaje
indicando cuál está activa.

## Si algo falla: cómo revertir

1. Detén Ktor (Paso 4).
2. Vuelve al commit anterior:

   ```powershell
   git log --oneline -5
   git checkout <commit-anterior>
   ```

   Si hay trabajo local sin commitear en esa máquina que no quieres perder,
   usa `git stash` antes en vez de `checkout` a ciegas.

3. **Flyway no revierte migraciones automáticamente.** Si V13 ya se aplicó
   y necesitas deshacerla, las opciones son: (a) restaurar el backup del
   Paso 1, o (b) escribir y aplicar una migración de bajada nueva. No hay
   downgrade automático — por eso el Paso 1 no es opcional.
4. Reinicia con el código anterior (Paso 5) y repite las verificaciones del
   Paso 6.

## Notas de seguridad

- No compartas ni subas al repositorio el contenido de
  `supportdesk.properties` (ya está en `.gitignore`).
- Esta actualización no cambia `SUPPORTDESK_AUTH_SECRET`; las sesiones
  activas de los clientes no se invalidan.

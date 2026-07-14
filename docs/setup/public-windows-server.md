# Publicar OryKai software en un PC Windows

Esta guia deja Ktor accesible desde internet de forma mas segura:

- Ktor escucha solo en `127.0.0.1:8080`.
- Caddy publica el dominio en `80/443` con HTTPS.
- El router solo expone `80/443`.

## 1. Requisitos

- Un dominio o subdominio que controles.
- Un registro DNS o DDNS que apunte a tu IP publica.
- La IP local del PC servidor fijada o reservada en el router.
- Puertos `80` y `443` redirigidos en el router al PC servidor.
- Un proyecto Supabase ya operativo.

## 2. Configuracion obligatoria

Copia `supportdesk.server.properties.example` como `supportdesk.properties` en la raiz del proyecto y rellena:

- `SUPPORTDESK_AUTH_SECRET`
- `SUPABASE_DATABASE_URL`

Opcionales pero recomendadas:

- `SUPPORTDESK_AUTH_ISSUER=crm.example.com`
- `SUPPORTDESK_AUTH_AUDIENCE=crm.example.com`
- `SUPPORTDESK_ACCESS_TOKEN_LIFETIME_MINUTES=480`
- `SUPPORTDESK_REFRESH_TOKEN_LIFETIME_DAYS=30`
- `SUPPORTDESK_SERVER_HOST=127.0.0.1`
- `SUPPORTDESK_SERVER_PORT=8080`
- `SUPPORTDESK_DB_MAX_POOL_SIZE=5`

Para el primer arranque, o para resetear intencionadamente los usuarios demo:

- `SUPPORTDESK_BOOTSTRAP_DEMO_DATA=true`
- `SUPPORTDESK_BOOTSTRAP_ADMIN_PASSWORD=<contrasena-admin-real>`
- `SUPPORTDESK_BOOTSTRAP_CLIENT_PASSWORD=<contrasena-cliente-real>`

Las contrasenas deben tener al menos 12 caracteres. Tras confirmar el login,
cambia `SUPPORTDESK_BOOTSTRAP_DEMO_DATA=false`, elimina las dos contrasenas del
archivo y reinicia Ktor. Los usuarios y sus hashes permanecen en PostgreSQL.

Tambien puedes generar el archivo sin BOM y pedir las contrasenas de forma
interactiva:

```powershell
.\create-server-config.ps1 `
  -SupabaseDatabaseUrl "postgresql://..." `
  -PublicDomain "crm.example.com" `
  -BootstrapDemoData
```

Si no usas `SUPABASE_DATABASE_URL`, define estas:

- `SUPABASE_DB_HOST`
- `SUPABASE_DB_PORT`
- `SUPABASE_DB_NAME`
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`

## 3. Generar un secreto fuerte

PowerShell:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Usa ese valor como `SUPPORTDESK_AUTH_SECRET`.

Tambien puedes usar variables de entorno con los mismos nombres.

## 4. Arrancar Ktor en local

En PowerShell:

```powershell
.\start-ktor-server.ps1
```

Ktor ejecuta las migraciones Flyway antes de abrir la API. No ejecutes
`docs/setup/postgresql-init.sql` ni la migracion de facturas a mano.

Comprobacion local del proceso y de PostgreSQL:

```powershell
Invoke-WebRequest http://127.0.0.1:8080/health/live
Invoke-WebRequest http://127.0.0.1:8080/health/ready
```

## 5. Poner Caddy delante

Instala Caddy en Windows y crea un `Caddyfile` con el contenido de [Caddyfile.example](./Caddyfile.example).

La configuracion incluida añade compresion, cabeceras defensivas, logs y health
checks activos contra Ktor. Valida el archivo antes de arrancar o recargar:

```powershell
caddy fmt --overwrite .\Caddyfile
caddy validate --config .\Caddyfile --adapter caddyfile
caddy reload --config .\Caddyfile --adapter caddyfile
```

Cuando el DNS ya apunte a tu casa y el router tenga abiertos `80/443`, Caddy emitira y renovara HTTPS automaticamente.

En produccion, instala Caddy como servicio de Windows para que sobreviva a
reinicios. Ejecuta como administrador y sustituye las rutas:

```powershell
sc.exe create caddy start= auto binPath= '"C:\caddy\caddy.exe" run --config "C:\caddy\Caddyfile" --adapter caddyfile'
sc.exe start caddy
```

## 6. Router y firewall

- Reserva la IP local del PC servidor en el router.
- Redirige `TCP 80` y `TCP 443` a esa IP.
- No publiques `8080` al exterior.
- En el Firewall de Windows permite entrada a Caddy en `80/443`.
- Si quieres ser mas estricto, limita `8080` a loopback o red local solamente.

## 7. Configuracion del PC de tu colega

En el PC cliente crea:

- `%USERPROFILE%\.supportdesk\supportdesk.properties`

Contenido:

```properties
supportdesk.baseUrl=https://crm.example.com
```

Tambien puedes pasarlo por variable de entorno:

```powershell
$env:SUPPORTDESK_BASE_URL="https://crm.example.com"
```

O genera el archivo con:

```powershell
.\create-desktop-client-config.ps1 -PublicBaseUrl "https://crm.example.com"
```

## 8. Comprobaciones finales

- `https://crm.example.com/` debe responder `status=running`.
- `https://crm.example.com/health/live` debe responder `alive`.
- `https://crm.example.com/health/ready` debe responder `database=ready`.
- El login de escritorio debe funcionar con la URL HTTPS.
- Tras cerrar y abrir la app, la sesion debe restaurarse.
- Si cambias la IP publica de casa, el DDNS debe actualizarse.

## 9. Riesgos que siguen existiendo

- Si tu ISP bloquea `80/443`, no podras publicar asi sin cambiar de estrategia.
- Si el PC servidor se apaga o entra en suspension, tu colega se quedara sin backend.
- Debes usar contrasenas reales, no las de bootstrap por defecto.
- Si cambias `SUPPORTDESK_AUTH_SECRET`, invalidaras todas las sesiones activas.

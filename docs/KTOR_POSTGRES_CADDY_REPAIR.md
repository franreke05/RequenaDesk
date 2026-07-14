# Reparación de Ktor, PostgreSQL y Caddy

Fecha de verificación: 14 de julio de 2026.

## Resultado

El backend queda preparado para arrancar con una base PostgreSQL vacía o con el esquema legado, ejecutar las migraciones automáticamente, validar que la base está disponible, crear datos iniciales de forma transaccional y servir la API detrás de Caddy.

La solución se ha verificado con PostgreSQL 14.22 real, no con dobles de prueba. Se probaron Flyway, HikariCP, autenticación y operaciones HTTP de lectura y escritura.

El dominio público todavía ejecuta la versión anterior: la raíz responde, pero `/health/live` y `/health/ready` devuelven 404. Por tanto, los cambios de este documento deben desplegarse en el servidor antes de considerar reparado el entorno público.

## Arquitectura resultante

```text
Cliente Compose
    |
    | HTTPS + JSON
    v
Caddy :443
    |
    | reverse_proxy + health check
    v
Ktor 127.0.0.1:8080
    |
    | HikariCP
    v
PostgreSQL / Supabase
    ^
    |
Flyway V1 + V2 al arrancar Ktor
```

## Problemas encontrados

1. El SQL de instalación contenía `CHECK` con subconsultas, una construcción no admitida por PostgreSQL.
2. Había dependencias circulares entre usuarios y clientes, además de inserciones iniciales en un orden incompatible con claves foráneas reales.
3. El disparador de adjuntos podía dejar `ticket_id` y `message_id` informados simultáneamente, incumpliendo la restricción XOR.
4. Las facturas dependían de una migración manual ajena al arranque de la aplicación.
5. HikariCP no se cerraba al detener Ktor y no tenía límites ni tiempos operativos suficientes.
6. No existía una comprobación de disponibilidad de base de datos para Caddy.
7. El resumen administrativo tenía columnas `client_id` ambiguas después de varios `JOIN`, causando respuestas HTTP 500.
8. Las excepciones internas podían exponerse al cliente.
9. El generador de configuración podía escribir BOM en Windows PowerShell y romper la primera propiedad Java.
10. El cliente de inicio de sesión necesitaba enviar las credenciales exclusivamente en JSON y representar correctamente validación, carga y errores.

## Cambios realizados

### Migraciones y modelo de datos

- Se añadió Flyway al módulo `server`, incluido el módulo específico de PostgreSQL.
- `V1__supportdesk_schema.sql` crea y repara el esquema principal, restricciones, claves foráneas, índices, vistas, funciones y disparadores.
- `V2__invoices.sql` instala facturas, secuencias, índices, restricciones y RLS.
- Se configuró `baselineOnMigrate` con versión base `0` para incorporar una base existente no vacía y aplicar V1/V2 a continuación.
- Los antiguos scripts manuales ahora remiten a las migraciones canónicas para evitar dos fuentes de verdad.
- Se añadieron índices de claves foráneas y compuestos para los recorridos habituales.

Archivos principales:

- `server/src/main/resources/db/migration/V1__supportdesk_schema.sql`
- `server/src/main/resources/db/migration/V2__invoices.sql`
- `docs/setup/postgresql-init.sql`
- `docs/database/migration_invoices.sql`

### Conexión PostgreSQL

- El pool Hikari tiene nombre, máximo configurable, mínimo inactivo y tiempos de conexión, validación, inactividad, vida máxima y keepalive.
- El tamaño predeterminado del pool es 5 y el rango aceptado es 1-20.
- Se aceptan URLs `postgresql://`, `postgres://` y `jdbc:postgresql://`.
- El puerto 6543 desactiva prepared statements del driver mediante `prepareThreshold=0`, compatible con un pooler en modo transacción.
- El pool se cierra con el evento `ApplicationStopped`.
- El arranque falla de forma explícita ante configuración incompleta, secreto débil, contraseñas iniciales débiles o esquema no disponible.

### Arranque y datos iniciales

- Las migraciones se ejecutan antes de registrar el repositorio y las rutas.
- El bootstrap se ejecuta dentro de una transacción.
- Primero se crean administradores; después clientes, usuarios y entidades dependientes usando los UUID devueltos por PostgreSQL.
- Se eliminaron UUID fijos y la posibilidad de asociar datos a un identificador distinto del usuario existente.
- El bootstrap está desactivado por defecto y exige contraseñas de al menos 12 caracteres.

### Salud y seguridad HTTP

- `GET /health/live`: confirma que el proceso Ktor está vivo.
- `GET /health/ready`: comprueba conexión y tablas críticas; responde 503 si PostgreSQL no está preparado.
- Los errores inesperados se registran completos en el servidor, pero el cliente recibe un mensaje genérico.
- Caddy usa `/health/ready` como control activo del upstream.
- La configuración de Caddy añade compresión, log de acceso, HSTS, `nosniff`, política de referencia y tiempos de proxy.

### Autenticación del cliente

- El login envía correo y contraseña como cuerpo JSON; no aparecen en la URL.
- Los fallos HTTP y de red se convierten en errores de dominio comprensibles.
- La cancelación de corrutinas no se transforma en un error falso.
- Las pantallas validan campos, bloquean envíos duplicados y muestran el estado de carga.
- Se añadieron pruebas JVM del datasource, repositorio y ViewModel de autenticación.

### Configuración operativa

- `create-server-config.ps1` genera un secreto criptográfico aleatorio de 48 bytes.
- El archivo se escribe como UTF-8 sin BOM.
- `-BootstrapDemoData` solicita las contraseñas de forma segura y valida su longitud.
- `start-ktor-server.ps1` admite `DATABASE_URL` como alternativa y valida que el secreto tenga al menos 32 caracteres.
- `supportdesk.server.properties.example` y `server/.env.example` documentan las variables actuales sin contener secretos.

## Despliegue en Windows

Antes del primer arranque sobre producción, realizar una copia de seguridad de PostgreSQL. Flyway aplica cambios de esquema hacia delante; este trabajo no incorpora un downgrade automático.

1. Generar la configuración en la raíz del proyecto:

```powershell
.\create-server-config.ps1 `
  -SupabaseDatabaseUrl "postgresql://USUARIO:CLAVE@HOST:5432/postgres?sslmode=require" `
  -PublicDomain "crm.franciscorequena.cloud" `
  -BootstrapDemoData
```

2. Arrancar Ktor. En el primer arranque se aplicarán V1 y V2 y, si se activó el bootstrap, se crearán los usuarios iniciales:

```powershell
.\start-ktor-server.ps1
```

3. Verificar el backend sin pasar por Caddy:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/health/live
Invoke-RestMethod http://127.0.0.1:8080/health/ready
```

Las dos respuestas deben tener estado HTTP 200. `ready` debe indicar que la base está disponible.

4. Después del primer arranque, establecer `SUPPORTDESK_BOOTSTRAP_DEMO_DATA=false` y retirar las dos contraseñas de bootstrap de `supportdesk.properties`. Reiniciar Ktor y confirmar otra vez `/health/ready`.

5. Instalar o actualizar `Caddyfile` a partir de `docs/setup/Caddyfile.example`, sustituyendo el dominio de ejemplo. Validar y recargar:

```powershell
caddy validate --config .\Caddyfile --adapter caddyfile
caddy reload --config .\Caddyfile --adapter caddyfile
```

6. Verificar el recorrido público completo:

```powershell
Invoke-RestMethod https://crm.franciscorequena.cloud/health/live
Invoke-RestMethod https://crm.franciscorequena.cloud/health/ready
```

7. Ejecutar Ktor y Caddy mediante servicios de Windows, con reinicio automático y cuentas de servicio con permisos mínimos. La guía detallada está en `docs/setup/public-windows-server.md`.

## Verificación ejecutada

Comando final, forzando la recompilación y repetición de pruebas:

```powershell
.\gradlew.bat :server:test :shared:jvmTest :composeApp:compileKotlinJvm :server:installDist --rerun-tasks
```

Resultado: `BUILD SUCCESSFUL`, 20 tareas ejecutadas.

La prueba de integración realizó lo siguiente:

- Arranque de PostgreSQL nativo 14.22 en un puerto temporal.
- Simulación de un esquema legado no vacío.
- Baseline y aplicación de las dos migraciones Flyway.
- Segunda ejecución sin migraciones pendientes, comprobando idempotencia.
- Comprobación de readiness y cierre limpio de HikariCP/PostgreSQL.
- Bootstrap transaccional y autenticación real.
- GET de tickets, clientes, etiquetas, tareas, tiempos, dashboard y facturas.
- POST de cliente, etiqueta, tarea, tiempo y factura, todos con HTTP 201.
- Compilación de Compose JVM y pruebas compartidas de autenticación.
- Generación de `server/build/install/server`.
- Confirmación de que el JAR contiene V1 y V2.
- Validación sintáctica del Caddyfile y de los scripts PowerShell.

## Skills aplicadas

- `kmp-ktor`: estructura de Ktor, lifecycle, configuración y rutas de salud.
- `kotlin-project-architecture-review`: separación entre configuración, datasource, repositorio y bootstrap.
- `kotlin-testing-kmp`: pruebas de autenticación e integración JVM reproducibles.
- `supabase-postgres-best-practices`: tamaño del pool, compatibilidad con pooler, restricciones, claves foráneas, índices y upserts.
- Auditoría local `.claude/SKILLS_AUDIT.md`: selección y trazabilidad de las skills del proyecto.

## Estado pendiente del entorno público

No se ha desplegado esta distribución ni se han ejecutado migraciones contra la base pública porque esta máquina no dispone de las credenciales del servidor, de `supportdesk.properties` de producción ni de acceso al servicio remoto. El código y el runbook están listos, pero el login público seguirá dependiendo de la versión anterior hasta completar los pasos de despliegue.

No deben añadirse al repositorio `supportdesk.properties`, URLs con contraseña, secretos JWT ni contraseñas de bootstrap.

## Referencias técnicas

- [Flyway Java API](https://documentation.red-gate.com/flyway/reference/usage/api-java)
- [Flyway: soporte de PostgreSQL](https://documentation.red-gate.com/fd/postgresql-database-277579325.html)
- [Caddy como servicio](https://caddyserver.com/docs/running)
- [Caddy reverse_proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy)
- [Zonky Embedded PostgreSQL](https://github.com/zonkyio/embedded-postgres)

# Local Setup

## Requisitos

- Android Studio con JBR disponible.
- Gradle wrapper descargado o acceso de red para descargarlo.
- SDK Android configurado para `composeApp`.
- PostgreSQL accesible y un `supportdesk.properties` valido para ejecutar `server`.

## Comandos esperados

Windows:

```powershell
.\gradlew.bat :composeApp:run
.\gradlew.bat :composeApp:assembleDebug
.\start-ktor-server.ps1
.\gradlew.bat :server:test
```

En el primer arranque, Ktor aplica automaticamente las migraciones Flyway de
`server/src/main/resources/db/migration`. Comprueba el proceso y la base de
datos por separado:

```powershell
Invoke-WebRequest http://127.0.0.1:8080/health/live
Invoke-WebRequest http://127.0.0.1:8080/health/ready
```

## Notas

- En este momento `composeApp` contiene Desktop, Android e iOS bridge.
- `shared` aloja la base de dominio y MVVM.
- `server` expone la API real y es el unico proceso que accede a PostgreSQL.

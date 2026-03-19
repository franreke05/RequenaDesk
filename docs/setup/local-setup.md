# Local Setup

## Requisitos

- Android Studio con JBR disponible.
- Gradle wrapper descargado o acceso de red para descargarlo.
- SDK Android configurado para `composeApp`.

## Comandos esperados

Windows:

```powershell
.\gradlew.bat :composeApp:run
.\gradlew.bat :composeApp:assembleDebug
.\gradlew.bat :server:run
.\gradlew.bat :server:test
```

## Notas

- En este momento `composeApp` contiene Desktop, Android e iOS bridge.
- `shared` aloja la base de dominio y MVVM.
- `server` expone rutas placeholder listas para sustituirse por logica real.

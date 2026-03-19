# SupportDesk Overview

SupportDesk es un CRM de tickets para trabajo freelance con foco en `Desktop + Server`.

La base actual sigue la estructura real del proyecto Kotlin Multiplatform:

- `composeApp`: cliente KMP conectado al build.
- `shared`: modelos, contratos, use cases y viewmodels minimos.
- `server`: backend Ktor con rutas placeholder.
- `iosApp`: wrapper nativo para dejar iOS preparado.

Decisiones base del MVP:

- Una sola app desktop con roles `CLIENT` y `ADMIN`.
- Android solo como app lite para admin.
- iOS solo preparado estructuralmente.
- Sin persistencia real, auth real ni subida real de archivos en esta fase.
- MVVM simple en frontend y separacion `data / domain / presentation` por feature.

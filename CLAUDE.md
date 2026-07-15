# Instrucciones del proyecto

## Facturas

- Las facturas son documentos PDF generados bajo demanda y guardados localmente en `Escritorio/Facturas OryKai`.
- No se deben persistir facturas, sus lineas, metadatos ni URLs de descarga en PostgreSQL ni en ninguna otra base de datos.
- No crear migraciones, tablas, secuencias, repositorios de persistencia ni endpoints CRUD para facturas. La numeracion debe ser efimera y no escribir en la base de datos.
- La creacion, listado y apertura de facturas no pueden usar Ktor, HTTP, endpoints del servidor, tokens de sesion ni fallbacks de descarga. El PDF se genera directamente en el proceso JVM con los datos del formulario.
- La pantalla de creacion debe permitir seleccionar una o varias tareas del mismo cliente. Cada linea usa las horas registradas de esa tarea, redondeadas siempre hacia arriba a horas completas.
- La biblioteca de facturas debe leer exclusivamente los PDF locales y abrirlos con el visor PDF predeterminado del sistema.

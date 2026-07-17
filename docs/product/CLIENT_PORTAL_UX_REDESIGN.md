# Rediseño UX/UI del portal de cliente

## Objetivo

Hacer del portal la parte útil y comercial del CRM: el cliente entiende el
estado de su trabajo, responde cuando desbloquea algo y descubre programas
adicionales sin confundir una petición con una activación.

## Principios aplicados

- Inicio operativo, no un dashboard decorativo: prioridad, estado de servicio,
  tres indicadores breves, actividad y tareas del día.
- En escritorio (`>= 900dp` y altura suficiente), Inicio cabe en un viewport.
  Si hay un error o en móvil, pasa a disposición vertical con scroll seguro.
- `Trabajo` separa las dos colas que el cliente realmente necesita: solicitudes
  y tareas. Tablero y actividad quedan como vistas de contexto, no como ruido.
- `Programas` muestra primero las solicitudes que están en revisión y hace
  explícito que la aprobación, el precio final y el acceso los decide el
  administrador.
- `Cuenta` funciona como configuración: perfil, plan, preferencias,
  componentes, actividad mensual y acciones de sesión. No duplica Inicio.

## Sistema visual específico del portal

`app/client/components/ClientPortalDesign.kt` concentra los componentes
exclusivos del cliente:

- superficies blancas con borde neutro de 1dp, radio de 16dp y sin sombra;
- métricas compactas, con altura mínima de 88dp;
- títulos de página y de sección reutilizables;
- una alternativa a la tarjeta acentuada global para que los detalles del
  portal no tengan el borde neón destinado a administración.

Este sistema se usa en Inicio, Trabajo, Programas y Cuenta, y también en
solicitudes, tareas, ticket nuevo y servicio mediante la tarjeta de sección
específica del portal.

## Accesibilidad y comportamiento

- Los estados no dependen solo del color: todos los badges contienen texto.
- Las acciones mantienen etiquetas explícitas (`Ver solicitudes`, `Ir a
  tareas`, `Solicitar ayuda`), incluso en las vistas compactas.
- Las listas limitan el resumen a dos elementos; el detalle está en su vista
  correspondiente.
- Los textos largos se truncan solo en filas de resumen; los formularios y
  pantallas de detalle conservan el contenido completo.

## Referencias de investigación

- Salesforce prioriza la página de inicio por perfil y por las acciones de ese
  usuario: <https://help.salesforce.com/s/articleView?id=sf.basics_home_page.htm&language=en_US&type=5>
- Pipedrive describe un dashboard útil como una vista clara de la actividad y
  el siguiente paso: <https://www.pipedrive.com/en/features/sales-dashboard>
- Asana sitúa el trabajo personal en una lista priorizada y separa el detalle
  de cada tarea: <https://help.asana.com/s/article/maximize-productivity-with-my-tasks>
- HubSpot organiza el portal de cliente alrededor de tickets y de permisos de
  acceso controlados: <https://knowledge.hubspot.com/inbox/set-up-a-customer-portal?edition=starter>

## Verificación

Ejecutado localmente el 17 de julio de 2026:

```text
./gradlew.bat :shared:jvmTest :composeApp:compileKotlinJvm :server:test
```

Resultado: correcto.

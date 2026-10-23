# Despliegue V15: clientes, proyectos y tareas

Ejecuta estos comandos desde Git Bash. La migración es aditiva, pero Flyway no
la revierte: antes del despliegue confirma un backup reciente en Supabase
Dashboard > Database > Backups.

## Validación local

```bash
cd /c/Users/franr/AndroidStudioProjects/CRMFreelance
./gradlew.bat :server:test :shared:jvmTest :composeApp:compileKotlinJvm :server:installDist --no-daemon
git diff --check
```

## Despliegue del servidor

Hazlo después de integrar la rama V15 en `main`:

```bash
ssh deploy@srv1289731
cd /opt/requenadesk/RequenaDesk
git status --short
git fetch origin
git pull --ff-only origin main
/home/deploy/deploy-requenadesk.sh
```

## Verificación

```bash
git log -1 --oneline
sudo journalctl -u requenadesk -n 200 --no-pager \
  | grep -Ei 'version "15|business customers and projects|successfully applied|error'
curl -i http://127.0.0.1:8080/health/live
curl -i http://127.0.0.1:8080/health/ready
curl -i https://crm.franciscorequena.cloud/health/ready
```

Un `401` en una ruta protegida sin token es correcto. La comprobación funcional
debe hacerse iniciando sesión como usuario cliente con los programas
`BUSINESS_CUSTOMERS` y `BUSINESS_PROJECTS` aprobados.

En Supabase revisa también Settings > API y confirma que `public` no se expone
innecesariamente. V15 habilita RLS y revoca `PUBLIC`, `anon` y `authenticated`
en todas sus tablas nuevas; Ktor debe seguir siendo la única frontera de datos.

## Prueba manual mínima

1. Crear una etiqueta y un cliente comercial con una próxima acción.
2. Crear un proyecto asociado al cliente y dos tareas.
3. Colocar las tareas en días distintos, mover un pósit y reiniciar la app.
4. Iniciar, pausar, reanudar y detener el temporizador.
5. Completar una tarea y confirmar que desaparece del tablero.
6. Confirmar que una colocación de ayer vuelve a pendientes sin cambiar la
   fecha límite ni borrar la tarea.
7. Desde administración, comprobar que las métricas no muestran nombres,
   títulos, contactos ni notas.

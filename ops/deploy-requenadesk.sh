#!/usr/bin/env bash
#
# Despliegue real de RequenaDesk (Ktor) en srv1289731.
#
# Copia de referencia versionada en el repo. La copia que realmente se
# ejecuta debe vivir en /home/deploy/deploy-requenadesk.sh (mismo sitio que
# el deploy.sh de miweb), NO dentro del propio checkout de RequenaDesk, para
# que un `git reset`/pull accidentado nunca se lleve por delante el script.
#
# Reconstruido a partir del historial real de despliegues manuales:
#   git pull en /opt/requenadesk/RequenaDesk
#   -> ./gradlew :server:installDist
#   -> copiar server/build/install/server/* a /opt/requenadesk/app-dist
#   -> systemctl restart requenadesk
#
# Flyway aplica las migraciones pendientes solo, al arrancar Ktor - este
# script no toca la base de datos directamente.

set -euo pipefail

REPO_DIR="/opt/requenadesk/RequenaDesk"
DIST_DIR="/opt/requenadesk/app-dist"
BACKUP_DIR="/opt/requenadesk/app-dist.backup-$(date +%Y%m%d%H%M%S)"
SERVICE_NAME="requenadesk"
BRANCH="main"
BASE_URL="http://127.0.0.1:8080"
PUBLIC_URL="https://crm.franciscorequena.cloud"
HEALTH_TIMEOUT_SECONDS=180

FORCE=false
SKIP_PULL=false
DRY_RUN=false
SMOKE_TEST_EMAIL=""

usage() {
  cat <<'USAGE'
Uso: deploy-requenadesk.sh [opciones]

  --force                 Omite las confirmaciones interactivas (backup de BD,
                           reinicio del servicio). Usalo solo si ya verificaste
                           el backup a mano.
  --skip-pull             No hace git pull; solo recompila y redespliega lo que
                           ya haya en el checkout (util para probar el script).
  --dry-run               Muestra los pasos sin ejecutar ningun cambio real.
  --smoke-test-email EMAIL
                          Tras verificar /health/ready, hace login (pide la
                          contrasena de forma segura) y comprueba que
                          /admin/work-sessions/active responde.
  -h, --help              Muestra esta ayuda.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force) FORCE=true; shift ;;
    --skip-pull) SKIP_PULL=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    --smoke-test-email)
      SMOKE_TEST_EMAIL="${2:?--smoke-test-email requiere un valor}"
      shift 2
      ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "Argumento desconocido: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

run() {
  if $DRY_RUN; then
    echo "  [DRY RUN] $*"
  else
    "$@"
  fi
}

confirm() {
  if $FORCE || $DRY_RUN; then
    return 0
  fi
  read -r -p "$1 (escribe SI para continuar): " answer
  if [[ "$answer" != "SI" ]]; then
    echo "Cancelado."
    exit 1
  fi
}

echo "==> Paso 1/7 - Backup de la base de datos"
echo "Esta actualizacion puede incluir migraciones de esquema (Flyway las aplica"
echo "sola al arrancar Ktor). Confirma que tienes un backup reciente en Supabase"
echo "Dashboard > Database > Backups antes de continuar. Este script NO lo hace."
confirm "He confirmado que existe un backup reciente"

echo "==> Paso 2/7 - Comprobando el checkout en $REPO_DIR"
cd "$REPO_DIR"
if [[ -n "$(git status --porcelain)" ]]; then
  echo "Hay cambios locales sin commitear en $REPO_DIR:" >&2
  git status --short >&2
  echo "Resuelvelos (git stash o git commit) antes de continuar." >&2
  exit 1
fi
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
echo "Rama actual: $CURRENT_BRANCH"
if [[ "$CURRENT_BRANCH" != "$BRANCH" ]]; then
  confirm "La rama actual ('$CURRENT_BRANCH') no es '$BRANCH'. Continuar de todas formas"
fi

if $SKIP_PULL; then
  echo "==> Paso 3/7 - Omitido (--skip-pull)"
else
  echo "==> Paso 3/7 - git pull origin $BRANCH"
  run git fetch origin
  run git pull --ff-only origin "$BRANCH"
fi

echo "==> Paso 4/7 - Compilando (gradlew :server:installDist)"
run ./gradlew :server:installDist --no-daemon

echo "==> Paso 5/7 - Backup de $DIST_DIR y despliegue de la nueva version"
if [[ -d "$DIST_DIR" ]]; then
  run sudo cp -r "$DIST_DIR" "$BACKUP_DIR"
  echo "  Backup guardado en $BACKUP_DIR"
fi
# Se borra y recrea (no se fusiona) para que no queden jars viejos en lib/
# de una version anterior conviviendo con los nuevos.
run sudo rm -rf "$DIST_DIR"
run sudo mkdir -p "$DIST_DIR"
run sudo cp -r "$REPO_DIR/server/build/install/server/." "$DIST_DIR/"
run sudo chmod +x "$DIST_DIR/bin/server"

echo "==> Paso 6/7 - Reiniciando $SERVICE_NAME"
confirm "Se va a reiniciar el servicio '$SERVICE_NAME' (hay una breve interrupcion)"
run sudo systemctl restart "$SERVICE_NAME"

echo "==> Paso 7/7 - Verificando salud"
if $DRY_RUN; then
  echo "  [DRY RUN] esperaria a $BASE_URL/health/live y /health/ready"
else
  deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  healthy=false
  while [[ $SECONDS -lt $deadline ]]; do
    if curl -sf "$BASE_URL/health/live" >/dev/null 2>&1 && curl -sf "$BASE_URL/health/ready" >/dev/null 2>&1; then
      healthy=true
      break
    fi
    echo "  todavia no responde, reintentando..."
    sleep 5
  done

  if ! $healthy; then
    echo "" >&2
    echo "Ktor no respondio en $BASE_URL/health/ready tras ${HEALTH_TIMEOUT_SECONDS}s." >&2
    echo "Revisa el log: sudo journalctl -u $SERVICE_NAME -n 100 --no-pager" >&2
    echo "" >&2
    echo "Para revertir a la version anterior:" >&2
    echo "  sudo systemctl stop $SERVICE_NAME" >&2
    echo "  sudo rm -rf $DIST_DIR" >&2
    echo "  sudo mv $BACKUP_DIR $DIST_DIR" >&2
    echo "  sudo systemctl start $SERVICE_NAME" >&2
    echo "" >&2
    echo "Flyway NO revierte migraciones automaticamente: si la nueva migracion ya se aplico y" >&2
    echo "hace falta deshacerla, restaura el backup de Supabase del Paso 1." >&2
    exit 1
  fi
  echo "  live: OK, ready: OK"

  if curl -sf "$PUBLIC_URL/health/ready" >/dev/null 2>&1; then
    echo "  $PUBLIC_URL/health/ready responde correctamente (Caddy no necesita cambios)."
  else
    echo "  Aviso: $PUBLIC_URL/health/ready no respondio. Revisa Caddy si persiste." >&2
  fi
fi

if [[ -n "$SMOKE_TEST_EMAIL" ]]; then
  echo "==> Smoke test: login + /admin/work-sessions/active"
  if $DRY_RUN; then
    echo "  [DRY RUN] login como $SMOKE_TEST_EMAIL + GET /admin/work-sessions/active"
  else
    read -r -s -p "Contrasena de $SMOKE_TEST_EMAIL: " smoke_password
    echo ""
    login_response="$(curl -sf -X POST "$BASE_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$SMOKE_TEST_EMAIL\",\"password\":\"$smoke_password\"}")"
    unset smoke_password
    access_token="$(echo "$login_response" | grep -oP '"accessToken"\s*:\s*"\K[^"]+' || true)"
    if [[ -z "$access_token" ]]; then
      echo "No se pudo extraer accessToken del login. Respuesta: $login_response" >&2
      exit 1
    fi
    curl -sf "$BASE_URL/admin/work-sessions/active" -H "Authorization: Bearer $access_token" >/dev/null
    echo "  /admin/work-sessions/active respondio correctamente."
  fi
fi

echo ""
echo "Despliegue completado."
if [[ -d "$BACKUP_DIR" ]]; then
  echo "Backup anterior conservado en: $BACKUP_DIR"
fi
echo "Pendiente manual: probar desde la app de escritorio real (iniciar/pausar/reanudar/"
echo "detener un temporizador y comprobar historial y calendario)."

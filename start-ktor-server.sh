#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GRADLEW="./gradlew"

if [[ ! -f "$GRADLEW" ]]; then
  echo "No se encontro gradlew en la raiz del proyecto: $SCRIPT_DIR" >&2
  exit 1
fi

ensure_java() {
  local candidates=()
  local local_appdata_posix=""

  if [[ -n "${JAVA_HOME:-}" ]]; then
    candidates+=("$JAVA_HOME")
  fi

  candidates+=(
    "/c/Program Files/Android/Android Studio/jbr"
    "/c/Program Files/Android/Android Studio/jre"
    "/c/Program Files/JetBrains/Android Studio/jbr"
    "/c/Program Files/JetBrains/Android Studio/jre"
  )

  if [[ -n "${LOCALAPPDATA:-}" ]]; then
    if command -v cygpath >/dev/null 2>&1; then
      local_appdata_posix="$(cygpath -u "$LOCALAPPDATA")"
    else
      local_appdata_posix="$LOCALAPPDATA"
    fi

    candidates+=(
      "$local_appdata_posix/Programs/Android Studio/jbr"
      "$local_appdata_posix/Programs/Android Studio/jre"
    )

    if [[ -d "$local_appdata_posix/JetBrains/Toolbox/apps/AndroidStudio" ]] && command -v find >/dev/null 2>&1; then
      while IFS= read -r candidate; do
        candidates+=("$candidate")
      done < <(
        find "$local_appdata_posix/JetBrains/Toolbox/apps/AndroidStudio" \
          -maxdepth 4 \
          -type d \
          \( -name jbr -o -name jre \) \
          2>/dev/null
      )
    fi
  fi

  if command -v java >/dev/null 2>&1; then
    return 0
  fi

  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate/bin/java" || -x "$candidate/bin/java.exe" ]]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      return 0
    fi
  done

  echo "No se encontro Java. Define JAVA_HOME o instala Android Studio con JBR disponible." >&2
  exit 1
}

development=false
gradle_args=()

for arg in "$@"; do
  case "$arg" in
    --dev|--development)
      development=true
      ;;
    *)
      gradle_args+=("$arg")
      ;;
  esac
done

if [[ "$development" == true ]]; then
  gradle_args+=("-Pdevelopment=true")
fi

ensure_java

echo "Iniciando servidor Ktor con Gradle..."
echo "Modulo: server"
echo "URL esperada: http://localhost:8080"

bash "$GRADLEW" :server:run "${gradle_args[@]}"

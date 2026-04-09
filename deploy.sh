#!/usr/bin/env bash
set -euo pipefail

SSH_HOST="${SSH_HOST:-rpi5}"
REMOTE_DIR="${REMOTE_DIR:-~/deploy}"
APP_NAME="${APP_NAME:-commodity-trading}"

log() { printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

require_cmd mvn
require_cmd ssh
require_cmd scp
require_cmd mktemp

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

STAGING_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$STAGING_DIR" || true
}
trap cleanup EXIT

log "Building JAR locally (skip tests)…"
(cd "$ROOT_DIR" && mvn -q -DskipTests package)

JAR_PATH=""
if compgen -G "$ROOT_DIR/target/*.jar" >/dev/null; then
  # Prefer repackaged Spring Boot jar (exclude *original*)
  JAR_PATH="$(ls -t "$ROOT_DIR"/target/*.jar 2>/dev/null | grep -v 'original' | head -n 1 || true)"
fi
[[ -n "${JAR_PATH}" && -f "${JAR_PATH}" ]] || die "Could not locate built JAR under ./target/*.jar"

log "Preparing deploy artifacts…"
cp "$JAR_PATH" "$STAGING_DIR/app.jar"
cp "$ROOT_DIR/Dockerfile" "$STAGING_DIR/Dockerfile"
cp "$ROOT_DIR/docker-compose.yml" "$STAGING_DIR/docker-compose.yml"
if [[ -f "$ROOT_DIR/.env" ]]; then
  cp "$ROOT_DIR/.env" "$STAGING_DIR/.env"
fi

log "Resolving remote HOME on ${SSH_HOST}…"
REMOTE_HOME="$(ssh "${SSH_HOST}" 'printf "%s" "$HOME"')"
[[ -n "${REMOTE_HOME}" ]] || die "Could not resolve remote HOME on ${SSH_HOST}"

REMOTE_DIR_ABS="${REMOTE_DIR}"
if [[ "${REMOTE_DIR_ABS}" == "~/"* ]]; then
  REMOTE_DIR_ABS="${REMOTE_HOME}/${REMOTE_DIR_ABS#~/}"
elif [[ "${REMOTE_DIR_ABS}" != /* ]]; then
  REMOTE_DIR_ABS="${REMOTE_HOME}/${REMOTE_DIR_ABS}"
fi

log "Creating remote directory ${REMOTE_DIR_ABS} on ${SSH_HOST}…"
ssh "${SSH_HOST}" "mkdir -p \"${REMOTE_DIR_ABS}\""

log "Uploading artifacts to ${SSH_HOST}:${REMOTE_DIR_ABS}/ …"
scp "$STAGING_DIR/app.jar" "$STAGING_DIR/Dockerfile" "$STAGING_DIR/docker-compose.yml" "${SSH_HOST}:${REMOTE_DIR_ABS}/"
if [[ -f "$STAGING_DIR/.env" ]]; then
  scp "$STAGING_DIR/.env" "${SSH_HOST}:${REMOTE_DIR_ABS}/"
fi

log "Deploying on ${SSH_HOST} (docker compose down → up -d --build)…"
ssh "${SSH_HOST}" "REMOTE_DIR_ABS=\"${REMOTE_DIR_ABS}\" APP_NAME=\"${APP_NAME}\" bash -se" <<'REMOTE'
set -euo pipefail

REMOTE_DIR_ABS="${REMOTE_DIR_ABS:?}"
APP_NAME="${APP_NAME:-commodity-trading}"

cd "$REMOTE_DIR_ABS"

backup_if_exists() {
  local f="$1"
  if [[ -f "$f" ]]; then
    cp -f "$f" "${f}.bak"
  fi
}

restore_backup() {
  local f="$1"
  if [[ -f "${f}.bak" ]]; then
    mv -f "${f}.bak" "$f"
  fi
}

rollback() {
  echo "Rolling back…" >&2
  set +e
  restore_backup "app.jar"
  restore_backup "Dockerfile"
  restore_backup "docker-compose.yml"
  restore_backup ".env"
  docker compose -f docker-compose.yml down >/dev/null 2>&1 || true
  docker compose -f docker-compose.yml up -d --build || true
  set -e
}

trap rollback ERR

backup_if_exists "app.jar"
backup_if_exists "Dockerfile"
backup_if_exists "docker-compose.yml"
backup_if_exists ".env"

pwd
ls -la

docker compose -f docker-compose.yml down || true
docker compose -f docker-compose.yml up -d --build
docker compose -f docker-compose.yml ps
REMOTE

log "Deployment complete. Remote status above."
log "Cleaning up local staging artifacts…"
rm -rf "$STAGING_DIR"

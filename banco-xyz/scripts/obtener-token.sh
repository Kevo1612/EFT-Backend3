#!/usr/bin/env bash
# Obtiene un access token de Keycloak.
# Uso: ./scripts/obtener-token.sh <cliente-oauth> <usuario> <password>
#   ./scripts/obtener-token.sh web-app cliente1 cliente123
#   ./scripts/obtener-token.sh movil-app cliente1 cliente123
#   ./scripts/obtener-token.sh cajero-app cliente1 cliente123
#   ./scripts/obtener-token.sh web-app admin admin123
# Ejemplo: export TOKEN=$(./scripts/obtener-token.sh web-app cliente1 cliente123)
set -euo pipefail
CLIENT=${1:-web-app}
USER=${2:-cliente1}
PASS=${3:-cliente123}
KEYCLOAK=${KEYCLOAK_URL:-http://localhost:8180}
curl -s -X POST "$KEYCLOAK/realms/banco-xyz/protocol/openid-connect/token" \
  -d grant_type=password -d client_id="$CLIENT" -d username="$USER" -d password="$PASS" \
  | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("access_token") or d)'

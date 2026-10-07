#!/usr/bin/env bash
# =====================================================================
# Recorrido de punta a punta (util para el video de presentacion).
# Requisitos: docker compose up -d (todos los servicios healthy), curl y python3.
# Uso: ./scripts/demo.sh
# =====================================================================
set -uo pipefail
GW=${GATEWAY_URL:-http://localhost:8080}
KC=${KEYCLOAK_URL:-http://localhost:8180}
CURL="curl -s"
[[ "$GW" == https* ]] && CURL="curl -sk"

json() { python3 -m json.tool 2>/dev/null || cat; }
token() {
  curl -s -X POST "$KC/realms/banco-xyz/protocol/openid-connect/token" \
    -d grant_type=password -d client_id="$1" -d username="$2" -d password="$3" \
    | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])'
}
titulo() { echo; echo "=================================================================="; echo "  $1"; echo "=================================================================="; }

titulo "1. Tokens por canal (Keycloak)"
WEB=$(token web-app cliente1 cliente123)
MOVIL=$(token movil-app cliente1 cliente123)
CAJERO=$(token cajero-app cliente1 cliente123)
ADMIN=$(token web-app admin admin123)
echo "tokens obtenidos: web, movil, cajero, admin"

titulo "2. BFF Web - dashboard completo (datos agregados en paralelo)"
$CURL "$GW/web/dashboard" -H "Authorization: Bearer $WEB" | json

titulo "3. BFF Movil - resumen liviano (cuentas enmascaradas)"
$CURL "$GW/movil/resumen" -H "Authorization: Bearer $MOVIL" | json

titulo "4. Seguridad por canal: token movil contra BFF web -> 401"
$CURL -o /dev/null -w "HTTP %{http_code}\n" "$GW/web/dashboard" -H "Authorization: Bearer $MOVIL"

titulo "5. Cajero sin terminal autorizado -> 403"
$CURL -o /dev/null -w "HTTP %{http_code}\n" "$GW/cajero/cuentas/1/saldo" -H "Authorization: Bearer $CAJERO"

titulo "6. Cajero - consulta de saldo (terminal ATM-001)"
$CURL "$GW/cajero/cuentas/1/saldo" -H "Authorization: Bearer $CAJERO" -H "X-Cajero-Id: ATM-001" | json

titulo "7. Transferencia web cuenta 1 -> cuenta 3 (saga + evento Kafka)"
KEY=$(python3 -c 'import uuid; print(uuid.uuid4())')
$CURL -X POST "$GW/web/transferencias" -H "Authorization: Bearer $WEB" -H "Content-Type: application/json" \
  -H "Idempotency-Key: $KEY" -d '{"cuentaOrigenId":1,"cuentaDestinoId":3,"monto":25000,"descripcion":"Pago arriendo"}' | json

titulo "8. Mismo Idempotency-Key -> devuelve la MISMA operacion (no cobra dos veces)"
$CURL -X POST "$GW/web/transferencias" -H "Authorization: Bearer $WEB" -H "Content-Type: application/json" \
  -H "Idempotency-Key: $KEY" -d '{"cuentaOrigenId":1,"cuentaDestinoId":3,"monto":25000,"descripcion":"Pago arriendo"}' | json

titulo "9. Transferencia con saldo insuficiente -> 422 RECHAZADO"
$CURL -X POST "$GW/web/transferencias" -H "Authorization: Bearer $WEB" -H "Content-Type: application/json" \
  -d '{"cuentaOrigenId":2,"cuentaDestinoId":3,"monto":99999999}' -w "\nHTTP %{http_code}\n"

titulo "10. Deteccion de fraude: 3 retiros seguidos en cajero -> alerta CRITICA -> cuenta bloqueada"
for i in 1 2 3; do
  $CURL -X POST "$GW/cajero/retiros" -H "Authorization: Bearer $CAJERO" -H "X-Cajero-Id: ATM-001" \
    -H "Content-Type: application/json" -H "Idempotency-Key: demo-retiro-$i-$(date +%s)" \
    -d '{"cuentaId":2,"monto":10000}' | json
done
sleep 3
echo "Estado de la cuenta 2 (consultado como admin):"
$CURL "$GW/api/cuentas/2" -H "Authorization: Bearer $ADMIN" | json
echo "Notificaciones del cliente 1 (generadas por el consumidor Kafka de ms-clientes):"
$CURL "$GW/api/clientes/1/notificaciones" -H "Authorization: Bearer $ADMIN" | json

titulo "11. Desbloqueo por administrador"
$CURL -X PATCH "$GW/api/cuentas/2/desbloquear" -H "Authorization: Bearer $ADMIN" | json

titulo "12. Batch: lanzar los 3 jobs sobre semana_3"
for JOB in reporteTransaccionesJob calculoInteresesJob estadosCuentaJob; do
  $CURL -X POST "$GW/api/batch/jobs/$JOB?semana=semana_3&periodo=2024-12" -H "Authorization: Bearer $ADMIN" | json
done
echo "Esperando 20 s a que terminen..."; sleep 20
for JOB in reporteTransaccionesJob calculoInteresesJob estadosCuentaJob; do
  echo "--- $JOB (ultima ejecucion)"
  $CURL "$GW/api/batch/jobs/$JOB/ejecuciones?limite=1" -H "Authorization: Bearer $ADMIN" | json
done
echo "--- Totales de control semana_3"
$CURL "$GW/api/batch/reportes/control?semana=semana_3" -H "Authorization: Bearer $ADMIN" | json

titulo "Fin. Revisar tambien: Eureka :8761 | Kafka UI :8090 | Zipkin :9411 | Prometheus :9090"

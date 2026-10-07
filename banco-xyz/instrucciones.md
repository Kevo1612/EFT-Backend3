# Instrucciones para ejecutar y probar cada componente

Este documento explica, paso a paso, cómo levantar el sistema completo del **Banco XYZ** y verificar cada componente: batch, BFF, microservicios, seguridad, resiliencia, Kafka, escalado y monitoreo.

---

## 0. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| Docker Desktop / Docker Engine + Compose v2 | 24+ | Levantar todo el sistema |
| RAM asignada a Docker | **8 GB mínimo** | Son 18 contenedores (12 JVM) |
| JDK | 17 | Solo para compilar o ejecutar sin Docker |
| Maven | 3.9+ | Solo para compilar o ejecutar sin Docker |
| `curl` y `python3` | — | Pruebas y script de demostración |

Puertos que deben estar libres: `8080, 8180, 8761, 8888, 8090, 9411, 9090, 5432, 29092`.

---

## 1. Compilar y ejecutar los tests

```bash
git clone <url-del-repo> banco-xyz
cd banco-xyz
mvn clean package            # compila los 11 módulos y ejecuta los tests unitarios
```

Los tests de `batch-service` validan las reglas migradas del legacy con los datos de `semana_1` y `semana_2`: formatos de fecha, anomalías, cálculo de intereses y normalización de signos.

```bash
mvn -pl batch-service test   # solo los tests del batch
```

> Para ejecutar con Docker no hace falta tener Maven instalado: la imagen compila el código (build multi-etapa).

---

## 2. Levantar todo con Docker Compose

```bash
docker compose up -d --build
```

La primera vez tarda varios minutos porque descarga las dependencias Maven y las imágenes. Para seguir el estado:

```bash
docker compose ps                    # todos deben quedar "healthy" (aprox. 2-3 min)
docker compose logs -f ms-pagos      # logs de un servicio
```

Orden de arranque (lo controla `depends_on` + `healthcheck`): PostgreSQL, Kafka y Keycloak → Config Server → Eureka → microservicios, BFF, batch y gateway.

### Consolas disponibles

| Componente | URL | Credenciales |
|---|---|---|
| API Gateway (entrada única) | http://localhost:8080 | JWT |
| Eureka (servicios registrados) | http://localhost:8761 | — |
| Config Server | http://localhost:8888/ms-pagos/default | — |
| Keycloak | http://localhost:8180 | `admin` / `admin` |
| Kafka UI | http://localhost:8090 | — |
| Zipkin (trazas) | http://localhost:9411 | — |
| Prometheus (métricas) | http://localhost:9090 | — |

### Verificación rápida (script automático)

```bash
./scripts/demo.sh
```

Recorre en orden: tokens por canal, dashboard web, resumen móvil, rechazo de un token de otro canal, rechazo de un cajero no autorizado, saldo en cajero, transferencia, idempotencia, saldo insuficiente, detección de fraude con bloqueo automático, desbloqueo y los 3 jobs batch. Las secciones siguientes explican cada prueba por separado.

---

## 3. Obtener tokens (Keycloak)

Cada canal tiene su propio cliente OAuth2. Para pruebas está habilitado el *password grant*:

```bash
export GW=http://localhost:8080
export WEB=$(./scripts/obtener-token.sh web-app cliente1 cliente123)
export MOVIL=$(./scripts/obtener-token.sh movil-app cliente1 cliente123)
export CAJERO=$(./scripts/obtener-token.sh cajero-app cliente1 cliente123)
export ADMIN=$(./scripts/obtener-token.sh web-app admin admin123)
```

Equivalente con curl:

```bash
curl -s -X POST http://localhost:8180/realms/banco-xyz/protocol/openid-connect/token \
  -d grant_type=password -d client_id=web-app -d username=cliente1 -d password=cliente123
```

> Los tokens del cajero duran **2 minutos**, los de web 5 y los de móvil 15. Si recibes `401`, vuelve a pedir el token.

---

## 4. Componentes de infraestructura Spring Cloud

### 4.1 Config Server

```bash
curl -s http://localhost:8888/ms-pagos,resiliencia/default | python3 -m json.tool | head -40
```

Debe mostrar las propiedades de `ms-pagos.yml`, `resiliencia.yml` y `application.yml` (carpeta `config-server/src/main/resources/config-repo`).

### 4.2 Eureka

Abre http://localhost:8761. Deben aparecer `API-GATEWAY`, `MS-CLIENTES`, `MS-CUENTAS (2)`, `MS-PAGOS (2)`, `BFF-WEB`, `BFF-MOVIL`, `BFF-CAJERO` y `BATCH-SERVICE`.

### 4.3 Gateway: enrutamiento y seguridad perimetral

```bash
curl -i $GW/web/dashboard                                         # 401: sin token
curl -i $GW/api/clientes -H "Authorization: Bearer $WEB"          # 403: /api/** es solo para ADMIN
curl -s "$GW/api/clientes?page=0&size=5" -H "Authorization: Bearer $ADMIN" | python3 -m json.tool
curl -s $GW/actuator/gateway/routes | python3 -m json.tool        # rutas configuradas
```

---

## 5. Parte 1: procesos batch (Spring Batch)

Los jobs se lanzan por API (rol ADMIN) y se ejecutan de forma asíncrona.

### 5.1 Lanzar los jobs

```bash
# Job 1: Reporte de Transacciones Diarias
curl -s -X POST "$GW/api/batch/jobs/reporteTransaccionesJob?semana=semana_3" -H "Authorization: Bearer $ADMIN"

# Job 2: Cálculo de Intereses Mensuales (periodo = mes a calcular)
curl -s -X POST "$GW/api/batch/jobs/calculoInteresesJob?semana=semana_3&periodo=2024-12" -H "Authorization: Bearer $ADMIN"

# Job 3: Estados de Cuenta Anuales (step particionado)
curl -s -X POST "$GW/api/batch/jobs/estadosCuentaJob?semana=semana_3" -H "Authorization: Bearer $ADMIN"
```

`semana` puede ser `semana_1`, `semana_2` o `semana_3` (las carpetas del repositorio legacy).

### 5.2 Ver el resultado de la ejecución

```bash
curl -s "$GW/api/batch/jobs/reporteTransaccionesJob/ejecuciones?limite=1" -H "Authorization: Bearer $ADMIN" | python3 -m json.tool
```

Por cada step se muestran `leidos`, `filtrados`, `escritos`, `omitidos`, `commits` y `rollbacks`. El step de carga de `semana_3` termina con `exitCode: "COMPLETED WITH SKIPS"`. En `estadosCuentaJob` aparecen los steps `consolidarEstadosWorkerStep:particion0..3`, que se ejecutaron en paralelo.

### 5.3 Ver los datos generados

```bash
H="Authorization: Bearer $ADMIN"
curl -s "$GW/api/batch/reportes/control?semana=semana_3" -H "$H" | python3 -m json.tool              # totales de control
curl -s "$GW/api/batch/reportes/transacciones/resumen?semana=semana_3" -H "$H" | python3 -m json.tool
curl -s "$GW/api/batch/reportes/transacciones/anomalias?semana=semana_1" -H "$H" | python3 -m json.tool
curl -s "$GW/api/batch/reportes/intereses?semana=semana_1&periodo=2024-12" -H "$H" | python3 -m json.tool
curl -s "$GW/api/batch/reportes/estados-cuenta?semana=semana_1" -H "$H" | python3 -m json.tool
curl -s "$GW/api/batch/reportes/rechazados?job=calculoInteresesJob&semana=semana_3" -H "$H" | python3 -m json.tool
```

Reportes CSV generados (equivalentes a los listados del legacy):

```bash
docker cp banco-batch:/app/output ./output && ls output
# reporte_transacciones_semana_3.csv  intereses_semana_3_2024-12.csv  estados_cuenta_semana_3.csv
```

**Resultados esperados para `semana_1`** (validados también por los tests unitarios):

| Job | Resultado |
|---|---|
| Transacciones | 10 cargadas; anómalas: id 3 (`MONTO_NEGATIVO`), id 4 (`MONTO_CERO`), id 8 (`DUPLICADO` de id 6) |
| Intereses | 8 cuentas; 101 → $25,00 (abono), 102 → $120,00 (cargo), 105 → $52,50 (cargo); total $677,50; las cuentas 101 y 106 quedan marcadas `POSIBLE_DUPLICADO_TITULAR` |
| Estados | 8 cuentas; 101 → saldo neto $500 (depósito 1.000 − retiro 500); 104 → −$100 |

**Resultados esperados para `semana_3`:** 951 transacciones cargadas (573 anómalas, 49 omitidas por fecha `2024-13-01`), 50 cuentas con interés (660 rechazadas + 290 duplicadas) y 901 movimientos anuales en 20 cuentas (99 omitidos). Como más del 30 % de los registros de intereses se rechaza, se publica una alerta `CALIDAD_DATOS` en Kafka.

### 5.4 Probar la idempotencia

Lanza dos veces el mismo job y semana: `control` debe entregar exactamente los mismos totales (la limpieza inicial y el *upsert* evitan duplicados).

### 5.5 Probar el fallo crítico y la reejecución automática

Lanza un job sobre una semana que no existe (el archivo de entrada falta, que es un fallo crítico):

```bash
curl -s -X POST "$GW/api/batch/jobs/reporteTransaccionesJob?semana=semana_9" -H "Authorization: Bearer $ADMIN"
docker compose logs -f batch-service
```

En el log se ve la política completa:

1. `cargaTransaccionesStep` falla → flujo `on("FAILED").fail()` → el job termina `FAILED`; el step de limpieza no se repite.
2. `Job reporteTransaccionesJob FALLO en intento 1/3` → `Reejecutando ... en 10000 ms (reanuda desde el step fallido)`.
3. Intentos 2 y 3 (espera de 10 s y luego 20 s). Como el archivo sigue faltando, queda `FAILED` para revisión.
4. En `GET /api/batch/jobs/reporteTransaccionesJob/ejecuciones` aparecen 3 ejecuciones de la **misma instancia**. Un reinicio manual (`POST /api/batch/ejecuciones/<ejecucionId>/reiniciar`) ahora es rechazado en el log porque el step superó `startLimit(3)`, la protección contra reintentos infinitos. En un fallo transitorio (más abajo) el reinicio manual sí reanuda el job.

**Fallos transitorios de base de datos:** si la conexión a PostgreSQL se corta durante un chunk (por ejemplo, con `docker compose restart postgres` mientras corre un job), el step reintenta el chunk hasta 3 veces con backoff exponencial (0,5 s → 1 s → 2 s) antes de darlo por fallido. Si aun así falla, la reejecución automática reanuda el job desde ese step cuando la base vuelve.

### 5.6 Programación automática (opcional)

Con `BATCH_SCHEDULER=true` en el servicio `batch-service` del `docker-compose.yml`, los jobs corren solos: transacciones todos los días a las 02:00, intereses el día 1 de cada mes a las 03:00 y estados el 1 de enero a las 04:00 (hora de Chile).

---

## 6. Parte 2: Backend for Frontend

### 6.1 BFF Web (`/web/**`, solo tokens `web-app`)

```bash
curl -s $GW/web/dashboard -H "Authorization: Bearer $WEB" | python3 -m json.tool       # vista completa
curl -s $GW/web/cuentas/1 -H "Authorization: Bearer $WEB" | python3 -m json.tool       # detalle + cartola
curl -s -X POST $GW/web/transferencias -H "Authorization: Bearer $WEB" -H "Content-Type: application/json" \
     -H "Idempotency-Key: $(uuidgen)" -d '{"cuentaOrigenId":1,"cuentaDestinoId":3,"monto":15000,"descripcion":"Prueba"}'
curl -s -X POST $GW/web/pagos-servicios -H "Authorization: Bearer $WEB" -H "Content-Type: application/json" \
     -d '{"cuentaOrigenId":1,"monto":23990,"servicio":"Electricidad","numeroCliente":"123456"}'
curl -s -X POST $GW/web/cuentas -H "Authorization: Bearer $WEB" -H "Content-Type: application/json" -d '{"tipo":"AHORRO"}'
curl -s "$GW/web/admin/clientes" -H "Authorization: Bearer $ADMIN"                     # backoffice (ADMIN)
```

### 6.2 BFF Móvil (`/movil/**`, solo tokens `movil-app`)

```bash
curl -s $GW/movil/resumen -H "Authorization: Bearer $MOVIL" | python3 -m json.tool
curl -s $GW/movil/cuentas/1/movimientos -H "Authorization: Bearer $MOVIL"
curl -s -X POST $GW/movil/transferencias -H "Authorization: Bearer $MOVIL" -H "Content-Type: application/json" \
     -d '{"origen":1,"destino":3,"monto":5000,"glosa":"cafe"}'
```

Optimizaciones a observar:

```bash
# Respuesta comprimida (gzip) y ETag
curl -s -D - -o /dev/null $GW/movil/resumen -H "Authorization: Bearer $MOVIL" -H "Accept-Encoding: gzip"
# Repetir enviando el ETag recibido -> 304 Not Modified (sin cuerpo)
curl -s -o /dev/null -w "%{http_code}\n" $GW/movil/resumen -H "Authorization: Bearer $MOVIL" -H 'If-None-Match: "<etag>"'
```

Compara el tamaño de `/movil/resumen` con `/web/dashboard`: la respuesta móvil es mucho más pequeña.

### 6.3 BFF Cajeros (`/cajero/**`, solo tokens `cajero-app` + terminal registrado)

```bash
H1="Authorization: Bearer $CAJERO"; H2="X-Cajero-Id: ATM-001"
curl -s $GW/cajero/cuentas/1/saldo -H "$H1" -H "$H2"
curl -s -X POST $GW/cajero/retiros -H "$H1" -H "$H2" -H "Content-Type: application/json" \
     -H "Idempotency-Key: retiro-0001" -d '{"cuentaId":1,"monto":20000}'
curl -s -X POST $GW/cajero/depositos -H "$H1" -H "$H2" -H "Content-Type: application/json" \
     -H "Idempotency-Key: dep-0001" -d '{"cuentaId":1,"monto":50000}'
```

Controles del canal (todos deben fallar):

```bash
curl -s -o /dev/null -w "%{http_code}\n" $GW/cajero/cuentas/1/saldo -H "$H1"                         # 403 sin terminal
curl -s -o /dev/null -w "%{http_code}\n" $GW/cajero/cuentas/1/saldo -H "$H1" -H "X-Cajero-Id: ATM-999" # 403 terminal no registrado
curl -s -X POST $GW/cajero/retiros -H "$H1" -H "$H2" -H "Content-Type: application/json" -d '{"cuentaId":1,"monto":20000}'          # 400 falta Idempotency-Key
curl -s -X POST $GW/cajero/retiros -H "$H1" -H "$H2" -H "Content-Type: application/json" -H "Idempotency-Key: x1" -d '{"cuentaId":1,"monto":12345}'  # 400 no es múltiplo de 5.000
curl -s -o /dev/null -w "%{http_code}\n" $GW/cajero/cuentas/3/saldo -H "$H1" -H "$H2"                 # 403 cuenta de otro cliente
```

Repetir un retiro con el **mismo** `Idempotency-Key` devuelve la misma referencia y no descuenta dos veces. Las operaciones quedan auditadas en `docker compose logs bff-cajero | grep AUDITORIA`.

### 6.4 Autenticación por canal

```bash
curl -s -o /dev/null -w "%{http_code}\n" $GW/web/dashboard  -H "Authorization: Bearer $MOVIL"   # 401
curl -s -o /dev/null -w "%{http_code}\n" $GW/movil/resumen  -H "Authorization: Bearer $WEB"     # 401
curl -s -o /dev/null -w "%{http_code}\n" $GW/cajero/cuentas/1/saldo -H "Authorization: Bearer $WEB" -H "X-Cajero-Id: ATM-001"  # 401
```

### 6.5 HTTPS

```bash
./scripts/generar-certificados.sh          # crea certs/banco-xyz.p12 (autofirmado)
SSL_ENABLED=true docker compose up -d api-gateway
curl -sk https://localhost:8080/actuator/health
GATEWAY_URL=https://localhost:8080 ./scripts/demo.sh
```

---

## 7. Parte 3: microservicios, resiliencia y Kafka

### 7.1 Microservicios (acceso directo como ADMIN)

```bash
H="Authorization: Bearer $ADMIN"
curl -s $GW/api/clientes/1 -H "$H"
curl -s -X POST $GW/api/clientes -H "$H" -H "Content-Type: application/json" \
  -d '{"rut":"15555555-5","username":"cliente3","nombres":"Pedro","apellidos":"Rojas","email":"pedro@correo.cl","segmento":"PYME"}'
curl -s $GW/api/cuentas/cliente/1 -H "$H"
curl -s -X PATCH $GW/api/cuentas/2/bloquear -H "$H"
curl -s -X PATCH $GW/api/cuentas/2/desbloquear -H "$H"
curl -s -X POST $GW/api/pagos/depositos -H "$H" -H "Content-Type: application/json" -d '{"cuentaDestinoId":4,"monto":1000}'
curl -s $GW/api/pagos/cuenta/1 -H "$H"
```

Solo `ms-pagos` puede mover saldos. Incluso un ADMIN recibe 403 si intenta hacerlo directamente:

```bash
curl -s -X POST $GW/api/cuentas/1/movimientos -H "$H" -H "Content-Type: application/json" \
  -d '{"referencia":"x","tipo":"CREDITO","monto":1000000}'      # 403 (requiere rol SERVICIO)
```

La documentación OpenAPI de cada servicio está disponible dentro de la red Docker en `/swagger-ui.html`, o en `http://localhost:<puerto>/swagger-ui.html` al ejecutarlos localmente (sección 9).

### 7.2 Circuit breaker, retry y fallback

**a) Pagos diferidos cuando `ms-cuentas` cae**

```bash
docker compose stop ms-cuentas
curl -s -X POST $GW/web/transferencias -H "Authorization: Bearer $WEB" -H "Content-Type: application/json" \
  -d '{"cuentaOrigenId":1,"cuentaDestinoId":3,"monto":1000}'
# -> estado "PENDIENTE_REINTENTO" (HTTP 202): el pago no se pierde
docker compose start ms-cuentas
# en ~30-60 s el reproceso lo completa:
curl -s $GW/api/pagos/<referencia> -H "Authorization: Bearer $ADMIN"     # estado COMPLETADO
```

**b) Degradación en la web (dashboard parcial)**

```bash
docker compose stop ms-pagos
curl -s $GW/web/dashboard -H "Authorization: Bearer $WEB" | python3 -m json.tool
# -> responde con cuentas y saldo, "ultimasOperaciones": [] y
#    "advertencias": ["El historial de operaciones no esta disponible temporalmente"]
docker compose start ms-pagos
```

**c) Caché en el canal móvil**

```bash
curl -s $GW/movil/resumen -H "Authorization: Bearer $MOVIL" > /dev/null   # se guarda en caché
docker compose stop ms-cuentas
curl -s $GW/movil/resumen -H "Authorization: Bearer $MOVIL"               # "cache": true
docker compose start ms-cuentas
```

**d) Estado de los circuitos**

```bash
docker compose exec bff-web curl -s localhost:8091/actuator/circuitbreakers | python3 -m json.tool
docker compose exec bff-web curl -s localhost:8091/actuator/health | python3 -m json.tool
```

**e) Compensación (SAGA):** si el abono al destino es rechazado (por ejemplo, la cuenta destino quedó `CERRADA` entre la validación y el abono), el pago queda `REVERTIDO` y el monto vuelve al origen (movimiento `<ref>-R` en la cartola).

### 7.3 Kafka: eventos en tiempo real

1. Abre **Kafka UI** (http://localhost:8090) → *Topics*: `banco.transacciones.completadas`, `banco.alertas.seguridad`, `banco.cuentas.eventos` y `banco.batch.resultados` (3 particiones cada uno).
2. Haz una transferencia (sección 6.1) y revisa el mensaje en `banco.transacciones.completadas`.
3. El consumidor de `ms-clientes` genera la notificación:
   ```bash
   curl -s $GW/api/clientes/1/notificaciones -H "Authorization: Bearer $ADMIN" | python3 -m json.tool
   ```
4. **Detección de fraude** (productor y consumidor en acción): 3 retiros en cajero en menos de 10 minutos:
   ```bash
   for i in 1 2 3; do curl -s -X POST $GW/cajero/retiros -H "$H1" -H "$H2" -H "Content-Type: application/json" \
     -H "Idempotency-Key: f-$i-$RANDOM" -d '{"cuentaId":2,"monto":10000}'; echo; done
   curl -s $GW/api/cuentas/2 -H "Authorization: Bearer $ADMIN"     # "estado": "BLOQUEADA"
   ```
   `ms-pagos` publica una alerta `CRITICA` → `ms-cuentas` bloquea la cuenta y `ms-clientes` registra la notificación. Para desbloquear: `PATCH /api/cuentas/2/desbloquear`.
5. Una transferencia de $5.000.000 o más genera una alerta `ALTA` (`MONTO_ELEVADO`).

### 7.4 Escalado horizontal y balanceo de carga

```bash
docker compose up -d --scale ms-pagos=3 --scale bff-web=2
```

- En Eureka aparecen 3 instancias de `MS-PAGOS` y 2 de `BFF-WEB`.
- Repite varias peticiones y revisa `docker compose logs ms-pagos`: las peticiones se reparten entre las réplicas (round-robin).
- Prometheus (http://localhost:9090 → *Status → Targets*) descubre automáticamente las nuevas réplicas vía Eureka.

### 7.5 Monitoreo

- **Zipkin** (http://localhost:9411): busca por `serviceName=api-gateway` y abre la traza de `/web/dashboard`; se ve el *fan-out* en paralelo hacia ms-clientes, ms-cuentas y ms-pagos.
- **Prometheus**: consultas de ejemplo `http_server_requests_seconds_count`, `resilience4j_circuitbreaker_state`, `spring_batch_job_seconds_count`.
- **Logs correlacionados**: `docker compose logs | grep <traceId>` muestra la misma petición en todos los servicios.

---

## 8. Detener y limpiar

```bash
docker compose down          # detiene (conserva los datos)
docker compose down -v       # detiene y borra los volúmenes (BD y reportes)
```

---

## 9. Ejecución local sin Docker (desarrollo)

1. Levanta solo la infraestructura:
   ```bash
   docker compose up -d postgres kafka keycloak zipkin
   ```
2. En terminales separadas y en este orden (los valores por defecto ya apuntan a `localhost`):
   ```bash
   mvn -pl config-server spring-boot:run
   mvn -pl eureka-server spring-boot:run
   mvn install -DskipTests      # instala common-lib en el repositorio local (solo la primera vez)
   mvn -pl ms-clientes spring-boot:run
   mvn -pl ms-cuentas spring-boot:run
   mvn -pl ms-pagos spring-boot:run
   mvn -pl bff-web spring-boot:run
   mvn -pl bff-movil spring-boot:run
   mvn -pl bff-cajero spring-boot:run
   mvn -pl batch-service spring-boot:run
   mvn -pl api-gateway spring-boot:run
   ```
3. Swagger de cada servicio: `http://localhost:8081/swagger-ui.html` (cuentas), `8082` (pagos), `8083` (clientes), `8091` (web), `8092` (móvil).

---

## 10. Solución de problemas

| Síntoma | Causa / solución |
|---|---|
| Un servicio se reinicia en bucle al inicio | Esperaba al Config Server o a Keycloak; se estabiliza solo. Revisa con `docker compose ps` |
| `401` en todas las llamadas | Token expirado (cajero: 2 min). Pide uno nuevo |
| `401` en un BFF con un token válido | Usaste el token de otro canal (`azp` distinto). Es el comportamiento esperado |
| `503 El servicio no esta disponible` | Circuit breaker abierto o réplicas aún registrándose en Eureka; espera unos 20 s |
| Contenedores terminan con *OOMKilled* | Aumenta la memoria de Docker a 8 GB o más |
| Puerto ocupado | Cambia el puerto publicado en `docker-compose.yml` |

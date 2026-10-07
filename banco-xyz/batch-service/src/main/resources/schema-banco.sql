-- Tablas de resultados de los procesos batch (las tablas BATCH_* de Spring Batch las crea Spring Boot).
-- Todas las claves primarias incluyen "semana" para permitir reejecuciones idempotentes (upsert).

CREATE TABLE IF NOT EXISTS transacciones_diarias (
    semana             VARCHAR(20)   NOT NULL,
    linea              INT           NOT NULL,
    id_origen          BIGINT        NOT NULL,
    fecha              DATE          NOT NULL,
    monto              NUMERIC(18,2),
    tipo               VARCHAR(20)   NOT NULL,
    anomalia           BOOLEAN       NOT NULL,
    motivo             VARCHAR(200),
    fecha_normalizada  BOOLEAN       NOT NULL,
    procesado_en       TIMESTAMP     NOT NULL DEFAULT now(),
    PRIMARY KEY (semana, linea)
);

CREATE TABLE IF NOT EXISTS resumen_transacciones_diarias (
    semana              VARCHAR(20)   NOT NULL,
    fecha               DATE          NOT NULL,
    tipo                VARCHAR(20)   NOT NULL,
    cantidad            INT           NOT NULL,
    monto_valido        NUMERIC(18,2) NOT NULL,
    cantidad_anomalias  INT           NOT NULL,
    PRIMARY KEY (semana, fecha, tipo)
);

CREATE TABLE IF NOT EXISTS intereses_calculados (
    semana        VARCHAR(20)   NOT NULL,
    periodo       VARCHAR(7)    NOT NULL,
    linea         INT           NOT NULL,
    cuenta_id     BIGINT        NOT NULL,
    nombre        VARCHAR(100),
    tipo          VARCHAR(20)   NOT NULL,
    edad          INT           NOT NULL,
    saldo         NUMERIC(18,2) NOT NULL,
    tasa          NUMERIC(8,5)  NOT NULL,
    interes       NUMERIC(18,2) NOT NULL,
    saldo_final   NUMERIC(18,2) NOT NULL,
    naturaleza    VARCHAR(10)   NOT NULL,
    observacion   VARCHAR(200),
    procesado_en  TIMESTAMP     NOT NULL DEFAULT now(),
    PRIMARY KEY (semana, periodo, linea)
);

CREATE TABLE IF NOT EXISTS movimientos_anuales (
    semana         VARCHAR(20)   NOT NULL,
    linea          INT           NOT NULL,
    cuenta_id      BIGINT        NOT NULL,
    fecha          DATE          NOT NULL,
    anio           INT           NOT NULL,
    transaccion    VARCHAR(20)   NOT NULL,
    monto          NUMERIC(18,2) NOT NULL,
    monto_firmado  NUMERIC(18,2) NOT NULL,
    descripcion    VARCHAR(200)  NOT NULL,
    observaciones  VARCHAR(200),
    procesado_en   TIMESTAMP     NOT NULL DEFAULT now(),
    PRIMARY KEY (semana, linea)
);
CREATE INDEX IF NOT EXISTS idx_mov_anuales_cuenta ON movimientos_anuales (semana, cuenta_id);

CREATE TABLE IF NOT EXISTS estado_cuenta_anual (
    semana                  VARCHAR(20)   NOT NULL,
    cuenta_id               BIGINT        NOT NULL,
    anio                    INT           NOT NULL,
    total_depositos         NUMERIC(18,2) NOT NULL,
    total_retiros           NUMERIC(18,2) NOT NULL,
    total_compras           NUMERIC(18,2) NOT NULL,
    total_pagos             NUMERIC(18,2) NOT NULL,
    saldo_neto              NUMERIC(18,2) NOT NULL,
    cantidad_movimientos    INT           NOT NULL,
    movimientos_observados  INT           NOT NULL,
    generado_en             TIMESTAMP     NOT NULL DEFAULT now(),
    PRIMARY KEY (semana, cuenta_id, anio)
);

CREATE TABLE IF NOT EXISTS registros_rechazados (
    id         BIGSERIAL     PRIMARY KEY,
    job        VARCHAR(80)   NOT NULL,
    semana     VARCHAR(20),
    archivo    VARCHAR(100),
    linea      INT,
    contenido  VARCHAR(500),
    motivo     VARCHAR(300),
    fecha      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_rechazados_job ON registros_rechazados (job, semana);

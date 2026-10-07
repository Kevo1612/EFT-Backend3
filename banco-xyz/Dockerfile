# syntax=docker/dockerfile:1.6
# =====================================================================
# Dockerfile UNICO para todos los modulos (build multi-etapa).
# Uso: docker build --build-arg MODULE=ms-pagos -t banco-xyz/ms-pagos .
# =====================================================================

# ---------- Etapa 1: compilacion con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /src
COPY . .
# -pl <modulo> -am : compila solo el modulo pedido y sus dependencias (common-lib)
# cache de ~/.m2 compartida entre builds -> las dependencias se descargan una sola vez
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -pl ${MODULE} -am -DskipTests package

# ---------- Etapa 2: imagen de ejecucion (solo JRE, sin codigo fuente ni Maven)
FROM eclipse-temurin:17-jre
ARG MODULE
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 app \
    && mkdir -p /app/output /app/certs && chown -R app /app
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
# el contenedor no corre como root
USER app
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

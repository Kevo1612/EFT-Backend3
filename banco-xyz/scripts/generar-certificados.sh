#!/usr/bin/env bash
# Genera un certificado autofirmado (PKCS12) para habilitar HTTPS en el API Gateway en desarrollo.
# En produccion (AWS) el certificado lo emite ACM y TLS termina en el Application Load Balancer.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p certs
rm -f certs/banco-xyz.p12
keytool -genkeypair \
  -alias banco-xyz \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore certs/banco-xyz.p12 \
  -storepass "${SSL_KEYSTORE_PASSWORD:-changeit}" \
  -validity 365 \
  -dname "CN=localhost, OU=TI, O=Banco XYZ, L=Santiago, C=CL" \
  -ext "SAN=dns:localhost,ip:127.0.0.1"
chmod 644 certs/banco-xyz.p12
echo "Certificado creado en certs/banco-xyz.p12"
echo "Levante el gateway con HTTPS:  SSL_ENABLED=true docker compose up -d api-gateway"

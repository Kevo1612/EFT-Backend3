#!/usr/bin/env bash
# Construye las imagenes de todos los modulos y las publica en Amazon ECR.
# Requisitos: AWS CLI v2 configurado (aws configure) y Docker.
# Uso: AWS_REGION=us-east-1 ./scripts/push-ecr.sh [tag]
set -euo pipefail
cd "$(dirname "$0")/.."

REGION=${AWS_REGION:-us-east-1}
TAG=${1:-1.0.0}
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
MODULOS=(config-server eureka-server api-gateway ms-clientes ms-cuentas ms-pagos bff-web bff-movil bff-cajero batch-service)

aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"

for M in "${MODULOS[@]}"; do
  REPO="banco-xyz/$M"
  aws ecr describe-repositories --repository-names "$REPO" --region "$REGION" >/dev/null 2>&1 \
    || aws ecr create-repository --repository-name "$REPO" --region "$REGION" \
         --image-scanning-configuration scanOnPush=true >/dev/null
  echo ">> $M"
  docker build --platform linux/amd64 --build-arg MODULE="$M" -t "$REGISTRY/$REPO:$TAG" .
  docker push "$REGISTRY/$REPO:$TAG"
done
echo "Imagenes publicadas en $REGISTRY/banco-xyz/*:$TAG"

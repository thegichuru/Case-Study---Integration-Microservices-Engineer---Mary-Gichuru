#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="case-study"
IMAGE_TAG="${1:-latest}"
IMAGE_NAME="case-study:${IMAGE_TAG}"

cd "$(dirname "$0")/.."

echo "[1/4] Building Spring Boot jar"
./gradlew clean bootJar --no-daemon

echo "[2/4] Building Docker image ${IMAGE_NAME}"
docker build -t "${IMAGE_NAME}" .

echo "[3/4] Applying Kubernetes manifests"
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml

if [[ -f "k8s/secret.yaml" ]]; then
  kubectl apply -f k8s/secret.yaml
else
  echo "Missing k8s/secret.yaml. Copy k8s/secret.example.yaml and set real credentials."
  exit 1
fi

kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

echo "[4/4] Updating deployment image"
kubectl -n "${NAMESPACE}" set image deployment/case-study case-study="${IMAGE_NAME}"
kubectl -n "${NAMESPACE}" rollout status deployment/case-study

echo "Deployment completed."



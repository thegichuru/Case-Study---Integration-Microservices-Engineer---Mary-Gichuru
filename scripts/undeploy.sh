#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

kubectl delete -f k8s/hpa.yaml --ignore-not-found
kubectl delete -f k8s/service.yaml --ignore-not-found
kubectl delete -f k8s/deployment.yaml --ignore-not-found
kubectl delete -f k8s/configmap.yaml --ignore-not-found
if [[ -f "k8s/secret.yaml" ]]; then
  kubectl delete -f k8s/secret.yaml --ignore-not-found
else
  kubectl delete -f k8s/secret.example.yaml --ignore-not-found
fi
kubectl delete -f k8s/namespace.yaml --ignore-not-found

echo "Resources removed."



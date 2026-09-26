#!/usr/bin/env bash
set -euo pipefail

CONTEXT="kind-banksolution"
PORT="${1:-8443}"

ADMIN_PASSWORD="$(kubectl --context "$CONTEXT" -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 --decode)"
echo "ArgoCD: http://localhost:$PORT  user: admin  password: $ADMIN_PASSWORD"

kubectl --context "$CONTEXT" -n argocd port-forward svc/argocd-server "$PORT:80"

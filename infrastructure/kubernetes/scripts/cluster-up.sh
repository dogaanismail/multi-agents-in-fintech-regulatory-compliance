#!/usr/bin/env bash
set -euo pipefail

KUBERNETES_DIRECTORY="$(cd "$(dirname "$0")/.." && pwd)"
CLUSTER_NAME="banksolution"

if ! kind get clusters | grep -qx "$CLUSTER_NAME"; then
    kind create cluster --config "$KUBERNETES_DIRECTORY/kind/cluster.yaml"
fi

kubectl cluster-info --context "kind-$CLUSTER_NAME"

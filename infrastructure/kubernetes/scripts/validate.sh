#!/usr/bin/env bash
set -euo pipefail

KUBERNETES_DIRECTORY="$(cd "$(dirname "$0")/.." && pwd)"
RENDER_DIRECTORY="$(mktemp -d)"
trap 'rm -rf "$RENDER_DIRECTORY"' EXIT

"$KUBERNETES_DIRECTORY/scripts/render-all.sh" "$RENDER_DIRECTORY"

for chart_directory in "$KUBERNETES_DIRECTORY"/charts/service-types/* "$KUBERNETES_DIRECTORY"/charts/data/* \
    "$KUBERNETES_DIRECTORY"/charts/platform/* "$KUBERNETES_DIRECTORY"/charts/gitops/*; do
    helm lint "$chart_directory" --quiet
done

kubeconform \
    -strict \
    -summary \
    -output text \
    -schema-location default \
    -schema-location 'https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json' \
    "$RENDER_DIRECTORY"

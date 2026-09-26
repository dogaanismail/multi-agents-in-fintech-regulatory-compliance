#!/usr/bin/env bash
set -euo pipefail

KUBERNETES_DIRECTORY="$(cd "$(dirname "$0")/.." && pwd)"

helmfile --file "$KUBERNETES_DIRECTORY/helmfile.yaml.gotmpl" --environment "${ENVIRONMENT:-local}" sync "$@"

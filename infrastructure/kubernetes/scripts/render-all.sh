#!/usr/bin/env bash
set -euo pipefail

KUBERNETES_DIRECTORY="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIRECTORY="${1:?usage: render-all.sh <output-directory>}"
ENVIRONMENT="${ENVIRONMENT:-local}"
APPS_VALUES="$KUBERNETES_DIRECTORY/charts/gitops/bank-solution-apps/values.yaml"

cd "$KUBERNETES_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"

while read -r repository_url; do
    helm repo add "$(echo "$repository_url" | sed -E 's#https?://##; s#[^a-zA-Z0-9]+#-#g')" "$repository_url" --force-update >/dev/null
done < <(grep -hoE 'repository: https?://[^ ]+' charts/*/*/Chart.yaml | awk '{print $2}' | sort -u)

for chart_yaml in charts/*/*/Chart.yaml; do
    chart_directory="$(dirname "$chart_yaml")"
    if grep -q '^dependencies:' "$chart_yaml"; then
        helm dependency build "$chart_directory" >/dev/null
    fi
done

render() {
    local release="$1" namespace="$2" chart="$3"
    shift 3
    local value_arguments=()
    for value_file in "$@"; do
        [[ -f "$value_file" ]] && value_arguments+=(--values "$value_file")
    done
    echo "rendering $namespace/$release ($chart)"
    helm template "$release" "$chart" --namespace "$namespace" ${value_arguments[@]+"${value_arguments[@]}"} \
        > "$OUTPUT_DIRECTORY/$namespace--$release.yaml"
}

for service_file in services/*/*.yaml; do
    namespace="$(basename "$(dirname "$service_file")")"
    release="$(basename "$service_file" .yaml)"
    service_type="$(awk '/^serviceType:/ {print $2}' "$service_file")"
    [[ -n "$service_type" ]] || { echo "$service_file: missing serviceType" >&2; exit 1; }
    [[ -d "charts/service-types/$service_type" ]] || { echo "$service_file: unknown serviceType '$service_type'" >&2; exit 1; }
    render "$release" "$namespace" "charts/service-types/$service_type" \
        "$service_file" "environments/$ENVIRONMENT/services.yaml"
done

while read -r release namespace chart; do
    render "$release" "$namespace" "$chart" \
        "data/$namespace/$release.yaml" "environments/$ENVIRONMENT/$release.yaml"
done < <(python3 -c '
import sys, yaml
for store in yaml.safe_load(open(sys.argv[1]))["stores"]:
    print(store["name"], store["namespace"], store["chart"])
' "$APPS_VALUES")

render bank-solution-apps argocd charts/gitops/bank-solution-apps

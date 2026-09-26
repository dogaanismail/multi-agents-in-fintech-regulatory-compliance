#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
BACKEND_DIRECTORY="$REPOSITORY_ROOT/bank-solution-backend"
CLUSTER_NAME="banksolution"
IMAGE_TAG="local"

JAVA_IMAGES=(
    "configuration-svc-api|configuration-service/configuration-svc"
    "configuration-svc-db-migration-app|configuration-service/configuration-svc-db-migration"
    "risk-engine-svc-api|risk-engine-service/risk-engine-svc"
    "risk-svc-db-migration-app|risk-engine-service/risk-engine-svc-db-migration"
    "account-svc-api|account-service/account-svc"
    "account-svc-db-migration-app|account-service/account-svc-db-migration"
    "customer-svc-api|customer-service/customer-svc"
    "customer-svc-db-migration-app|customer-service/customer-svc-db-migration"
    "customer-profile-svc-api|customer-profile-service/customer-profile-svc"
    "customer-profile-svc-db-migration-app|customer-profile-service/customer-profile-svc-db-migration"
    "payment-history-svc-api|payment-history-service/payment-history-svc"
    "payment-history-svc-db-migration-app|payment-history-service/payment-history-svc-db-migration"
    "payment-svc-api|payment-service/payment-svc"
    "payment-svc-db-migration-app|payment-service/payment-svc-db-migration"
    "payment-engine-svc-api|payment-engine-service/payment-engine-svc"
    "network-topology-svc|network-topology-service/network-topology-svc"
    "ledger-svc|ledger-service/ledger-svc"
    "backoffice-gateway-svc|backoffice-gateway/backoffice-gateway-svc"
)

DOCKERFILE_IMAGES=(
    "backoffice-ui|bank-solution-backoffice"
    "transaction-pattern-agent|ai-services/agents/transaction_pattern_agent"
    "customer-risk-agent|ai-services/agents/customer_risk_agent"
    "network-analysis-agent|ai-services/agents/network_analysis_agent"
)

matches_filter() {
    local image_name="$1"
    shift
    [[ $# -eq 0 ]] && return 0
    for filter in "$@"; do
        [[ "$image_name" == *"$filter"* ]] && return 0
    done
    return 1
}

selected_images=()
for image in "${JAVA_IMAGES[@]}"; do
    matches_filter "${image%%|*}" "$@" && selected_images+=("$image")
done

selected_dockerfile_images=()
for image in "${DOCKERFILE_IMAGES[@]}"; do
    matches_filter "${image%%|*}" "$@" && selected_dockerfile_images+=("$image")
done

if [[ ${#selected_images[@]} -eq 0 && ${#selected_dockerfile_images[@]} -eq 0 ]]; then
    echo "No image matches: $*" >&2
    exit 1
fi

gradle_task_for() {
    echo ":${1//\//:}:bootJar"
}

gradle_tasks=()
for image in ${selected_images[@]+"${selected_images[@]}"}; do
    gradle_tasks+=("$(gradle_task_for "${image#*|}")")
done
if [[ ${#gradle_tasks[@]} -gt 0 ]]; then
    (cd "$BACKEND_DIRECTORY" && ./gradlew "${gradle_tasks[@]}")
fi

for image in ${selected_images[@]+"${selected_images[@]}"}; do
    image_name="${image%%|*}"
    module_directory="$BACKEND_DIRECTORY/${image#*|}"
    docker build --tag "$image_name:$IMAGE_TAG" "$module_directory"
    kind load docker-image "$image_name:$IMAGE_TAG" --name "$CLUSTER_NAME"
done

for image in ${selected_dockerfile_images[@]+"${selected_dockerfile_images[@]}"}; do
    image_name="${image%%|*}"
    docker build --tag "$image_name:$IMAGE_TAG" "$REPOSITORY_ROOT/${image#*|}"
    kind load docker-image "$image_name:$IMAGE_TAG" --name "$CLUSTER_NAME"
done

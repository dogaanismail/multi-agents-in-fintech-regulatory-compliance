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
)

selected_images=()
for image in "${JAVA_IMAGES[@]}"; do
    if [[ $# -eq 0 ]]; then
        selected_images+=("$image")
        continue
    fi
    for filter in "$@"; do
        if [[ "${image%%|*}" == *"$filter"* ]]; then
            selected_images+=("$image")
            break
        fi
    done
done

if [[ ${#selected_images[@]} -eq 0 ]]; then
    echo "No image matches: $*" >&2
    exit 1
fi

gradle_task_for() {
    echo ":${1//\//:}:bootJar"
}

gradle_tasks=()
for image in "${selected_images[@]}"; do
    gradle_tasks+=("$(gradle_task_for "${image#*|}")")
done
(cd "$BACKEND_DIRECTORY" && ./gradlew "${gradle_tasks[@]}")

for image in "${selected_images[@]}"; do
    image_name="${image%%|*}"
    module_directory="$BACKEND_DIRECTORY/${image#*|}"
    docker build --tag "$image_name:$IMAGE_TAG" "$module_directory"
    kind load docker-image "$image_name:$IMAGE_TAG" --name "$CLUSTER_NAME"
done

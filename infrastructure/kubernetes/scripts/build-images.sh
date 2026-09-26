#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
BACKEND_DIRECTORY="$REPOSITORY_ROOT/bank-solution-backend"
CLUSTER_NAME="banksolution"
IMAGE_TAG="local"

JAVA_IMAGES=(
    "configuration-svc-api|configuration-service/configuration-svc"
    "configuration-svc-db-migration-app|configuration-service/configuration-svc-db-migration"
)

gradle_task_for() {
    echo ":${1//\//:}:bootJar"
}

gradle_tasks=()
for image in "${JAVA_IMAGES[@]}"; do
    gradle_tasks+=("$(gradle_task_for "${image#*|}")")
done
(cd "$BACKEND_DIRECTORY" && ./gradlew "${gradle_tasks[@]}")

for image in "${JAVA_IMAGES[@]}"; do
    image_name="${image%%|*}"
    module_directory="$BACKEND_DIRECTORY/${image#*|}"
    docker build --tag "$image_name:$IMAGE_TAG" "$module_directory"
    kind load docker-image "$image_name:$IMAGE_TAG" --name "$CLUSTER_NAME"
done

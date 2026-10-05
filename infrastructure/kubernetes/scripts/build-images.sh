#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
BACKEND_DIRECTORY="$REPOSITORY_ROOT/bank-solution-backend"
IMAGE_CATALOG="$REPOSITORY_ROOT/infrastructure/container-images.txt"
CLUSTER_NAME="banksolution"
IMAGE_TAG="local"

matches_filter() {
    local image_name="$1"
    shift
    [[ $# -eq 0 ]] && return 0
    for filter in "$@"; do
        [[ "$image_name" == *"$filter"* ]] && return 0
    done
    return 1
}

gradle_boot_jar_task() {
    local module_path="${1#bank-solution-backend/}"
    echo ":${module_path//\//:}:bootJar"
}

selected_images=()
while read -r image_name build_type build_context; do
    matches_filter "$image_name" "$@" && selected_images+=("$image_name $build_type $build_context")
done < "$IMAGE_CATALOG"

if [[ ${#selected_images[@]} -eq 0 ]]; then
    echo "No image matches: $*" >&2
    exit 1
fi

gradle_tasks=()
for image in "${selected_images[@]}"; do
    read -r _ build_type build_context <<<"$image"
    [[ "$build_type" == "gradle" ]] && gradle_tasks+=("$(gradle_boot_jar_task "$build_context")")
done
if [[ ${#gradle_tasks[@]} -gt 0 ]]; then
    (cd "$BACKEND_DIRECTORY" && ./gradlew "${gradle_tasks[@]}")
fi

for image in "${selected_images[@]}"; do
    read -r image_name _ build_context <<<"$image"
    docker build --tag "$image_name:$IMAGE_TAG" "$REPOSITORY_ROOT/$build_context"
    kind load docker-image "$image_name:$IMAGE_TAG" --name "$CLUSTER_NAME"
done

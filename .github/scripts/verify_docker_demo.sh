#!/usr/bin/env bash

set -euo pipefail

image_tag="${1:?Usage: verify_docker_demo.sh <image-tag>}"
development_compose_file="${2:-docker-compose.yml}"
demo_compose_file="${3:-compose.demo.yaml}"

project_name="spring-data-jpa-school-app-ci-${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"

cleanup() {
  echo "Cleaning up the Docker demo environment..."

  APP_IMAGE="$image_tag" \
    docker compose \
      --project-name "$project_name" \
      -f "$demo_compose_file" \
      down \
      --volumes \
      --remove-orphans \
    || true
}

trap cleanup EXIT

echo "Verifying Docker image: $image_tag"

if ! docker image inspect "$image_tag" > /dev/null; then
  echo "Docker image was not found locally: $image_tag"
  exit 1
fi

image_id="$(
  docker image inspect "$image_tag" \
    --format '{{.Id}}'
)"

image_size="$(
  docker image inspect "$image_tag" \
    --format '{{.Size}}'
)"

image_architecture="$(
  docker image inspect "$image_tag" \
    --format '{{.Architecture}}'
)"

echo "Checking Java runtime..."

java_version="$(
  docker run \
    --rm \
    --pull never \
    --entrypoint java \
    "$image_tag" \
    -version \
    2>&1
)"

printf '%s\n' "$java_version"

if ! grep -Eq 'version "21([."]|$)' <<< "$java_version"; then
  echo "Expected Java 21 in the Docker image."
  exit 1
fi

echo "Validating development Docker Compose configuration..."

POSTGRES_PASSWORD="ci-placeholder-password" \
  docker compose \
    -f "$development_compose_file" \
    config \
    > /dev/null

echo "Validating demo Docker Compose configuration..."

APP_IMAGE="$image_tag" \
  docker compose \
    --project-name "$project_name" \
    -f "$demo_compose_file" \
    config \
    > /dev/null

echo "Starting the demo environment..."

printf 'q\n' \
  | APP_IMAGE="$image_tag" \
      timeout 90s docker compose \
        --project-name "$project_name" \
        -f "$demo_compose_file" \
        run \
        --rm \
        -T \
        app

echo "Docker demo completed successfully."

summary=$(cat <<EOF
## Docker image and demo

| Property | Value |
|---|---|
| Image tag | \`$image_tag\` |
| Image ID | \`$image_id\` |
| Size | $image_size bytes |
| Architecture | \`$image_architecture\` |
| Java 21 runtime | Verified |
| Development Compose | Valid |
| Demo Compose | Valid |
| PostgreSQL integration | Verified |
| Application startup | Verified |
| Docker Hub push | No |
EOF
)

printf '%s\n' "$summary"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  printf '%s\n' "$summary" >> "$GITHUB_STEP_SUMMARY"
fi

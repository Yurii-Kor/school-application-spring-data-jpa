#!/usr/bin/env bash

set -Eeuo pipefail

run_tests=false
reset_database=false
postgres_password_override=""

usage() {
  cat <<'HELP'
Usage: ./run.sh [options]

Options:
  --run-tests
      Run Maven tests before starting the application.

  --reset-database
      Remove existing containers and the PostgreSQL volume before startup.

  --postgres-password <password>
      Override the local PostgreSQL password.

  -h, --help
      Show this help message.
HELP
}

fail() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

while (( $# > 0 )); do
  case "$1" in
    --run-tests)
      run_tests=true
      shift
      ;;

    --reset-database)
      reset_database=true
      shift
      ;;

    --postgres-password)
      if (( $# < 2 )); then
        fail "The --postgres-password option requires a value."
      fi

      postgres_password_override="$2"
      shift 2
      ;;

    -h|--help)
      usage
      exit 0
      ;;

    *)
      fail "Unknown option: $1. Use --help to see the supported options."
      ;;
  esac
done

if [[ ! -x "./mvnw" ]]; then
  fail "Executable Maven Wrapper was not found: ./mvnw"
fi

if ! command -v docker > /dev/null 2>&1; then
  fail "Docker CLI was not found. Install or start Docker and try again."
fi

echo "Checking Docker Compose..."

if ! docker compose version > /dev/null 2>&1; then
  fail "Docker Compose is not available."
fi

if [[ -n "$postgres_password_override" ]]; then
  export POSTGRES_PASSWORD="$postgres_password_override"
elif [[ -z "${POSTGRES_PASSWORD:-}" && ! -f ".env" ]]; then
  export POSTGRES_PASSWORD="local-dev-password"
fi

if [[ "$reset_database" == true ]]; then
  echo "Removing existing containers and PostgreSQL data..."

  docker compose down \
    --volumes \
    --remove-orphans
fi

maven_arguments=(
  --batch-mode
  --no-transfer-progress
  clean
  package
)

if [[ "$run_tests" == false ]]; then
  maven_arguments+=("-DskipTests")
fi

echo "Building the Spring Boot application..."

./mvnw "${maven_arguments[@]}"

echo "Validating Docker Compose configuration..."

docker compose config > /dev/null

echo "Building the local image and starting the application..."

docker compose run \
  --rm \
  --build \
  app

echo
echo "Application exited successfully."
echo "PostgreSQL remains available with its local data."
echo "Use 'docker compose down' to stop the environment."
echo "Use './run.sh --reset-database' for a clean database."

#!/usr/bin/env bash

set -euo pipefail

target_directory="${1:-target}"
expected_start_class="${2:-ua.foxminded.schoolapplication.SchoolApplicationConsole}"
expected_application_class="${expected_start_class//.//}.class"
expected_application_class="BOOT-INF/classes/${expected_application_class}"

temporary_directory=""
jar_contents_file=""

cleanup() {
  if [[ -n "$temporary_directory" ]]; then
    rm -rf "$temporary_directory"
  fi

  if [[ -n "$jar_contents_file" ]]; then
    rm -f "$jar_contents_file"
  fi
}

trap cleanup EXIT

if [[ ! -d "$target_directory" ]]; then
  echo "Target directory does not exist: $target_directory"
  exit 1
fi

mapfile -t jar_files < <(
  find "$target_directory" \
    -maxdepth 1 \
    -type f \
    -name "*.jar" \
    ! -name "*.jar.original" \
    -print \
    | sort
)

if (( ${#jar_files[@]} != 1 )); then
  echo "Expected exactly one executable JAR in: $target_directory"
  echo "Found: ${#jar_files[@]}"

  if (( ${#jar_files[@]} > 0 )); then
    printf ' - %s\n' "${jar_files[@]}"
  fi

  exit 1
fi

jar_path="${jar_files[0]}"

if [[ ! -s "$jar_path" ]]; then
  echo "The Spring Boot JAR is missing or empty: $jar_path"
  exit 1
fi

jar_contents_file="$(mktemp)"
jar tf "$jar_path" > "$jar_contents_file"

if ! grep -Fxq \
    "$expected_application_class" \
    "$jar_contents_file"; then
  echo "The expected application class was not found in the JAR:"
  echo "$expected_application_class"
  exit 1
fi

temporary_directory="$(mktemp -d)"
absolute_jar_path="$(realpath "$jar_path")"

(
  cd "$temporary_directory"
  jar xf "$absolute_jar_path" META-INF/MANIFEST.MF
)

manifest_file="$temporary_directory/META-INF/MANIFEST.MF"

if [[ ! -f "$manifest_file" ]]; then
  echo "The JAR does not contain META-INF/MANIFEST.MF."
  exit 1
fi

manifest="$(
  tr -d '\r' < "$manifest_file" \
    | sed ':a;N;$!ba;s/\n //g'
)"

expected_main_class="org.springframework.boot.loader.launch.JarLauncher"

if ! printf '%s\n' "$manifest" \
    | grep -Fxq "Main-Class: $expected_main_class"; then
  echo "The expected Spring Boot launcher is missing:"
  echo "Main-Class: $expected_main_class"
  exit 1
fi

if ! printf '%s\n' "$manifest" \
    | grep -Fxq "Start-Class: $expected_start_class"; then
  echo "The expected application entry point is missing:"
  echo "Start-Class: $expected_start_class"
  exit 1
fi

jar_name="$(basename "$jar_path")"
jar_size="$(du -h "$jar_path" | cut -f1)"
jar_sha256="$(sha256sum "$jar_path" | cut -d' ' -f1)"

summary=$(cat <<EOF
## Spring Boot JAR

| Property | Value |
|---|---|
| File | \`$jar_name\` |
| Size | $jar_size |
| SHA-256 | \`$jar_sha256\` |
| Main class | \`$expected_main_class\` |
| Start class | \`$expected_start_class\` |
| Executable archive | Verified |
EOF
)

printf '%s\n' "$summary"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  printf '%s\n' "$summary" >> "$GITHUB_STEP_SUMMARY"
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "path=$jar_path" >> "$GITHUB_OUTPUT"
  echo "name=$jar_name" >> "$GITHUB_OUTPUT"
  echo "sha256=$jar_sha256" >> "$GITHUB_OUTPUT"
fi

echo "Spring Boot JAR verified successfully."

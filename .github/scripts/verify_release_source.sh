#!/usr/bin/env bash

set -euo pipefail

if (( $# < 3 || $# > 4 )); then
  echo "Usage: verify_release_source.sh <release-tag> <release-version> <release-sha> [pom-file]"
  exit 1
fi

release_tag="$1"
release_version="$2"
expected_release_sha="$3"
pom_file="${4:-pom.xml}"

fail() {
  echo "::error::$1"
  exit 1
}

read_project_version() {
  python3 - "$pom_file" <<'PY'
import sys
import xml.etree.ElementTree as ET

pom_path = sys.argv[1]
root = ET.parse(pom_path).getroot()
namespace = {"m": "http://maven.apache.org/POM/4.0.0"}

version = root.find("m:version", namespace)

if version is None or not version.text or not version.text.strip():
    raise SystemExit("The project version was not found in pom.xml.")

print(version.text.strip())
PY
}

if [[ ! -f "$pom_file" ]]; then
  fail "Maven project file was not found: $pom_file"
fi

if [[ ! "$release_version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  fail "Release version must use MAJOR.MINOR.PATCH without a suffix: $release_version"
fi

if [[ ! "$expected_release_sha" =~ ^[0-9a-f]{40}$ ]]; then
  fail "Expected release SHA is invalid: $expected_release_sha"
fi

if [[ "$release_tag" != "v$release_version" ]]; then
  fail "Release tag $release_tag does not match release version $release_version."
fi

if ! git show-ref \
    --verify \
    --quiet \
    "refs/tags/$release_tag"; then
  fail "Release tag is not available in the checkout: $release_tag"
fi

current_sha="$(git rev-parse HEAD)"
tag_commit_sha="$(git rev-parse "$release_tag^{commit}")"
project_version="$(read_project_version)"

if [[ "$current_sha" != "$expected_release_sha" ]]; then
  fail "Checked out commit $current_sha does not match expected release commit $expected_release_sha."
fi

if [[ "$tag_commit_sha" != "$expected_release_sha" ]]; then
  fail "Release tag $release_tag points to $tag_commit_sha instead of $expected_release_sha."
fi

if [[ "$project_version" != "$release_version" ]]; then
  fail "pom.xml version $project_version does not match release version $release_version."
fi

summary=$(cat <<EOF
## Release source verification

| Property | Value |
|---|---|
| Release version | \`$release_version\` |
| Release tag | \`$release_tag\` |
| Checked out commit | \`$current_sha\` |
| Tag commit | \`$tag_commit_sha\` |
| Project version | \`$project_version\` |
| Source verification | Passed |
EOF
)

printf '%s\n' "$summary"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  printf '%s\n' "$summary" >> "$GITHUB_STEP_SUMMARY"
fi

echo "Release source verified successfully."

#!/usr/bin/env bash

set -euo pipefail

pom_file="${POM_FILE:-pom.xml}"
release_branch="${RELEASE_BRANCH:-main}"
requested_version="${REQUESTED_VERSION:-}"
event_name="${GITHUB_EVENT_NAME:-}"
repository_ref="${GITHUB_REF:-}"
release_sha="$(git rev-parse HEAD)"

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

find_remote_tag_commit() {
  local tag_name="$1"
  local tag_object_sha
  local peeled_commit_sha

  tag_object_sha="$(
    git ls-remote \
      --tags \
      origin \
      "refs/tags/$tag_name" \
      | awk 'NR == 1 { print $1 }'
  )"

  if [[ -z "$tag_object_sha" ]]; then
    return 1
  fi

  peeled_commit_sha="$(
    git ls-remote \
      --tags \
      origin \
      "refs/tags/$tag_name^{}" \
      | awk 'NR == 1 { print $1 }'
  )"

  printf '%s\n' "${peeled_commit_sha:-$tag_object_sha}"
}

write_output() {
  local name="$1"
  local value="$2"

  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    printf '%s=%s\n' "$name" "$value" >> "$GITHUB_OUTPUT"
  fi
}

if [[ ! -f "$pom_file" ]]; then
  fail "Maven project file was not found: $pom_file"
fi

project_version="$(read_project_version)"

if [[ ! "$project_version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  fail "The Maven project version must use MAJOR.MINOR.PATCH without a suffix: $project_version"
fi

case "$event_name" in
  pull_request)
    mode="validate"
    release_version="$project_version"
    ;;

  workflow_dispatch)
    mode="create"

    if [[ "$repository_ref" != "refs/heads/$release_branch" ]]; then
      fail "Release tags can only be created from the $release_branch branch. Current ref: $repository_ref"
    fi

    if [[ -z "$requested_version" ]]; then
      fail "A release version is required for a manual run."
    fi

    if [[ ! "$requested_version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
      fail "The requested version must use MAJOR.MINOR.PATCH without a suffix: $requested_version"
    fi

    if [[ "$requested_version" != "$project_version" ]]; then
      fail "Requested version $requested_version does not match pom.xml version $project_version."
    fi

    release_version="$requested_version"
    ;;

  *)
    fail "Unsupported workflow event: ${event_name:-not set}"
    ;;
esac

release_tag="v$release_version"
tag_action="available"
tag_created="false"
remote_tag_commit=""

if remote_tag_commit="$(find_remote_tag_commit "$release_tag")"; then
  if [[ "$mode" == "create" && "$remote_tag_commit" != "$release_sha" ]]; then
    fail "Tag $release_tag already points to $remote_tag_commit instead of current main commit $release_sha."
  fi

  if [[ "$mode" == "create" ]]; then
    tag_action="reused"
    echo "Tag $release_tag already points to the current commit and will be reused."
  else
    tag_action="exists"
    echo "Validation notice: tag $release_tag already exists at $remote_tag_commit."
  fi
elif [[ "$mode" == "create" ]]; then
  git config user.name "github-actions[bot]"
  git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

  git tag \
    --annotate "$release_tag" \
    --message "Release $release_tag" \
    "$release_sha"

  git push origin "refs/tags/$release_tag"

  verified_tag_commit="$(find_remote_tag_commit "$release_tag")"

  if [[ "$verified_tag_commit" != "$release_sha" ]]; then
    fail "Tag verification failed after push: expected $release_sha, found $verified_tag_commit."
  fi

  tag_action="created"
  tag_created="true"
  remote_tag_commit="$verified_tag_commit"

  echo "Created release tag $release_tag at $release_sha."
else
  echo "Release tag $release_tag is available."
fi

write_output "release_version" "$release_version"
write_output "release_tag" "$release_tag"
write_output "release_sha" "$release_sha"
write_output "tag_action" "$tag_action"
write_output "tag_created" "$tag_created"

requested_version_display="${requested_version:-not provided}"
remote_tag_display="${remote_tag_commit:-not created}"

summary=$(cat <<EOF
## Release tag preparation

| Property | Value |
|---|---|
| Mode | \`$mode\` |
| Event | \`${event_name:-not set}\` |
| Project version | \`$project_version\` |
| Requested version | \`$requested_version_display\` |
| Release tag | \`$release_tag\` |
| Checked commit | \`$release_sha\` |
| Tag action | \`$tag_action\` |
| Remote tag commit | \`$remote_tag_display\` |
EOF
)

printf '%s\n' "$summary"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  printf '%s\n' "$summary" >> "$GITHUB_STEP_SUMMARY"
fi

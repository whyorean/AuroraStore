#!/usr/bin/env bash
set -euo pipefail

tool_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
default_source="$(git -C "$tool_dir/.." rev-parse --show-toplevel)"
source_path="${1:-$default_source}"
source_root="$(git -C "$source_path" rev-parse --show-toplevel)"

if [[ $# -ge 2 ]]; then
    base_commit="$2"
else
    base_commit="$(tr -d '[:space:]' < "$tool_dir/BASE_COMMIT")"
fi

if ! git -C "$source_root" cat-file -e "${base_commit}^{commit}" 2>/dev/null; then
    echo "Baseline commit $base_commit is unavailable in $source_root." >&2
    exit 1
fi

source_head="$(git -C "$source_root" rev-parse HEAD)"
if ! git -C "$source_root" merge-base --is-ancestor "$base_commit" "$source_head"; then
    echo "Baseline $base_commit is not an ancestor of $source_head." >&2
    echo "Port the patch onto a descendant of the selected baseline first." >&2
    exit 1
fi

temporary_index="$(mktemp "${TMPDIR:-/tmp}/aurora-ui-index.XXXXXX")"
temporary_status="$(mktemp "${TMPDIR:-/tmp}/aurora-ui-status.XXXXXX")"
temporary_patch="$tool_dir/aurora-expressive-ui.patch.tmp.$$"
temporary_manifest="$tool_dir/MANIFEST.md.tmp.$$"
temporary_base="$tool_dir/BASE_COMMIT.tmp.$$"
cleanup() {
    rm -f "$temporary_index" "$temporary_status" "$temporary_patch" \
        "$temporary_manifest" "$temporary_base"
}
trap cleanup EXIT

GIT_INDEX_FILE="$temporary_index" git -C "$source_root" read-tree "$base_commit"
GIT_INDEX_FILE="$temporary_index" git -C "$source_root" add --all -- \
    . ':(exclude)patching' ':(exclude)patching/**' \
    ':(exclude)showcase' ':(exclude)showcase/**'
GIT_INDEX_FILE="$temporary_index" git -C "$source_root" diff \
    --cached --name-status --no-renames -z "$base_commit" -- \
    . ':(exclude)patching' ':(exclude)patching/**' \
    ':(exclude)showcase' ':(exclude)showcase/**' > "$temporary_status"
GIT_INDEX_FILE="$temporary_index" git -C "$source_root" diff \
    --cached --binary --full-index "$base_commit" -- \
    . ':(exclude)patching' ':(exclude)patching/**' \
    ':(exclude)showcase' ':(exclude)showcase/**' > "$temporary_patch"

if [[ ! -s "$temporary_patch" ]]; then
    echo "No source changes found relative to $base_commit." >&2
    exit 1
fi

added=0
modified=0
deleted=0
while IFS= read -r -d '' status && IFS= read -r -d '' path; do
    case "$status" in
        A) added=$((added + 1)) ;;
        D) deleted=$((deleted + 1)) ;;
        *) modified=$((modified + 1)) ;;
    esac
done < "$temporary_status"

exported_at="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
patch_sha256="$(shasum -a 256 "$temporary_patch" | awk '{print $1}')"
{
    cat <<EOF
# Generated Patch Manifest

- Generated: $exported_at
- Baseline commit: $base_commit
- Source HEAD: $source_head
- Files: $((added + modified + deleted)) total ($modified modified, $added added, $deleted deleted)
- Patch: aurora-expressive-ui.patch
- Patch SHA-256: $patch_sha256

| Status | Path |
| --- | --- |
EOF
    while IFS= read -r -d '' status && IFS= read -r -d '' path; do
        printf '| %s | `%s` |\n' "$status" "$path"
    done < "$temporary_status"
} > "$temporary_manifest"

printf '%s\n' "$base_commit" > "$temporary_base"
mv "$temporary_patch" "$tool_dir/aurora-expressive-ui.patch"
mv "$temporary_manifest" "$tool_dir/MANIFEST.md"
mv "$temporary_base" "$tool_dir/BASE_COMMIT"

echo "Exported $((added + modified + deleted)) files against $base_commit."
echo "Patch: $tool_dir/aurora-expressive-ui.patch"
echo "Manifest: $tool_dir/MANIFEST.md"

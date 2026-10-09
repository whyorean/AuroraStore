#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 /path/to/clean/AuroraStore-checkout" >&2
    exit 2
fi

tool_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
target_root="$(git -C "$1" rev-parse --show-toplevel)"
base_commit="$(tr -d '[:space:]' < "$tool_dir/BASE_COMMIT")"
patch_file="$tool_dir/aurora-expressive-ui.patch"

if [[ ! -s "$patch_file" ]]; then
    echo "Patch file is missing or empty: $patch_file" >&2
    exit 1
fi

target_head="$(git -C "$target_root" rev-parse HEAD)"
if ! git -C "$target_root" cat-file -e "${base_commit}^{commit}" 2>/dev/null || \
   ! git -C "$target_root" merge-base --is-ancestor "$base_commit" "$target_head"; then
    echo "Target HEAD $target_head is not a descendant of the recorded baseline." >&2
    echo "Use a full checkout containing $base_commit, or port the patch to this base." >&2
    exit 1
fi

if git -C "$target_root" apply --reverse --check "$patch_file" >/dev/null 2>&1; then
    echo "Patch is already present in $target_root."
    exit 0
fi

if [[ -n "$(git -C "$target_root" status --porcelain --untracked-files=all)" ]]; then
    echo "Target checkout is not clean: $target_root" >&2
    echo "Commit, stash, or remove its local changes before applying the patch." >&2
    exit 1
fi

if ! git -C "$target_root" apply --3way --check "$patch_file"; then
    echo "Patch does not apply cleanly to $target_root." >&2
    echo "Resolve the affected UI files deliberately, then rebuild and inspect the screens." >&2
    exit 1
fi

git -C "$target_root" apply --3way --index "$patch_file"
echo "Patch applied and staged in $target_root."
echo "Review with git diff --cached, then build with ./gradlew :app:assembleDebug."

#!/usr/bin/env bash
# Rebuild pr/upstream from add-sorting: product commits only, squashed into one
# upstream-style commit. Run after landing a feature so the PR branch can never
# silently go stale. Self-asserts that the PR tree matches the source for every
# product path; a dirty diff at the end means the classifier misfired.
set -euo pipefail
SRC=add-sorting
BASE=84ef74d   # upstream master this repo forks from

# Run the suite up front: the rebuild checks out the BASE commit, where the
# hooksPath-relative hook file is absent from the tree — git silently skips
# hooks there, so the gate must be explicit, not hook-run.
ANDROID_HOME=/root/android-sdk sh ./gradlew testDebugUnitTest --no-daemon -q

is_tooling() {
    git show --name-only --format= "$1" \
        | grep -Eq '(^app/src/test/|^scripts/|^docs/|^AGENTS\.md$|^CODING_STANDARDS\.md$|^GLOSSARY\.md$|^\.gitignore$|^app/build\.gradle$)'
}

git stash list >/dev/null   # fail fast if the worktree plumbing is unhappy
git checkout -q --detach "$BASE"

MSG=$(mktemp)
for c in $(git rev-list --reverse "$BASE".."$SRC"); do
    is_tooling "$c" && continue
    git cherry-pick -n "$c"
    git log -1 --format='* %s' "$c" >> "$MSG"
done
if [ ! -s "$MSG" ]; then echo "pr-cut: no product commits found in $BASE..$SRC" >&2; rm -f "$MSG"; exit 1; fi

git commit -q --no-verify -F "$MSG"
git branch -f pr/upstream HEAD
git checkout -q "$SRC"
rm -f "$MSG"

if ! git diff --quiet pr/upstream "$SRC" -- app/src/main README.md; then
    echo "pr-cut: PR tree does not match $SRC on product paths — stopping" >&2
    git diff --stat pr/upstream "$SRC" -- app/src/main README.md >&2
    exit 1
fi
echo "pr/upstream rebuilt: $(git log -1 --format='%h %s' pr/upstream)"

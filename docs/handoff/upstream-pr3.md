# Upstream PR#3 — follow-up handoff

State doc for continuing this fork's work after a gap. Session transcript and
commits carry the history; this file only says where things stand and what to
do next.

## Snapshot

| item | value |
| --- | --- |
| Upstream PR | [pccr10001/mifare-uid-changer#3](https://github.com/pccr10001/mifare-uid-changer/pull/3) OPEN, 2026-10-15. Author repo is quiet (~8 stars, last push 2025-02) — expect slow or no response; do not chase. |
| `add-sorting` | local tip `b1abc02`, synced to fork (product 4 + tooling 8 commits) |
| `pr/upstream` | `e77bdf6` — pure-product squash, PR head, 4 files 96+/6− |
| upstream base | `origin/master` = `84ef74d` |
| suite | 14 Robolectric tests green (`testDebugUnitTest`, verdict per build-test.md) |

History lives in commits, not here: `git log origin/master..add-sorting --oneline`
(product commits) and the `local tooling:` ones.

## Next-session branches

1. `gh pr view 3 --repo pccr10001/mifare-uid-changer --comments`
2. No response → do nothing, wait.
3. Review arrived → change on `add-sorting`, rebuild with `bash scripts/pr-cut.sh`,
   `git push --force <fork-url> pr/upstream`. Keep PR body in sync if claims change.
4. Merged → fork `master` to upstream, archive this doc (mark it done here).

## Backlog (deliberately outside PR#3 — minimal-change principle)

Status vocabulary per `docs/agents/triage-labels.md`.

| status | item | note |
| --- | --- | --- |
| ready-for-agent | rename `searchQuery` → `categoryFilter` | misleading name; PR-external |
| needs-info | category-first sort grouping | GLOSSARY says prefix grouping, impl is plain alphabetical — ask author in PR#3 |
| ready-for-agent | drop dead `position` param in `showDeleteTagUidDialog` | |
| ready-for-agent | add `return` after second `finish()` guard in `setupNfc` | same shape rule as CODING_STANDARDS |
| needs-info | filter survives rotation? | device-local UX decision |
| needs-info | `UidItem` equals/hashCode | current identity-delete is the documented spec; touch only if defensive copies appear |

## Environment traps (verified the hard way)

- rebase surgery rules: `build-test.md` Git plumbing section.
- `bash scripts/*.sh`, `sh ./gradlew` only.
- GH_TOKEN is a fine-grained PAT: push own fork OK; cannot fork/PR other repos —
  web click or classic token.
- Claims must match net squashed diff (a zero-net "fix" line got flagged by review).
- External-AI review of raw diff snippets misjudged `editTarget`/`saveTagUid` —
  answer from full files + green tests `editSameUid_renamesInPlace`,
  `editNewUid_duplicatesKeepingOriginal`.

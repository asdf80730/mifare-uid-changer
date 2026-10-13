# Coding Standards

Read during review of this repo's diffs (upstream: minimal-change PRs to `pccr10001/mifareuidchanger`).

- **Display is a derived view**: every mutation of `settings.getList()` must be followed by `saveList()` + `refreshDisplay()`; adapters never index `settings.getList()` directly — delete by item identity, not adapter position (filtering makes positions wrong).
- **Category = name convention**, the `Category` entry in `GLOSSARY.md`: `+` before the first name, `getCategories()` as single source. A name without `+` belongs to no category; nothing may create categories by other parsing.
- **Backward-compatible persistence**: `Settings`/`UidItem` JSON stays loadable by 1.3.2 (nullable new fields only; device-local state like filters/history stays out of the JSON and out of backups).
- **PR shape**: the PR ships as ONE squashed upstream-style commit built by `scripts/pr-cut.sh` — never hand-reorder or hand-squash; per-feature commits live only on `add-sorting`. No test framework, no CI config in the diff; the Robolectric suite and `scripts/` are local tooling kept out of upstream.
- **Finish-guard shape**: any lifecycle guard calling `finish()` must return or else immediately — code after the guard runs while the activity is still alive. `setupNfc`'s tripwire covers its own site only; new guards need the same shape.
- **View truth source**: behaviour assertions read from the view layer, not from a field the same test mutates — count items through the RecyclerView adapter (`displayedCount`), never by reflecting `displayList` (the lost `setAdapter` line hid in the app for three suite-green rounds that way).

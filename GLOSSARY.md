# Mifare UID Writer

An Android app that manages a saved list of 4-byte UIDs and writes them to block 0 of magic Mifare Classic gen 2 cards.

## Language

**UID**:
The 4-byte hex identifier read from an NFC tag.
_Avoid_: id (when meaning the displayed value)

**UID Item**:
One entry in the app's list: a saved UID and its display name. What users colloquially call 「卡片」.
_Avoid_: Card, Tag, Entry

**Magic Card**:
The physical Mifare Classic gen 2 card that receives the UID write to block 0.
_Avoid_: Card (in write contexts), Tag (in write contexts)

**Tag**:
A physical contactless tag scanned to read its UID.
_Avoid_: Card (in scan contexts)

**Category**:
A logical grouping of UID Items expressed in the name via a `+` delimiter (`Home+Front Door`). Sorting and filtering group items by that prefix; names without `+` belong to no category. The delimiter is deliberately rare so casual names never create categories. Not a stored field.
_Avoid_: Label, Group, Tag

**Save**:
The commit rule for the UID Item list, keyed by UID: an already-saved UID is renamed in place, an unseen UID joins as a new item.
_Avoid_: Update, Upsert (in user-facing text)

**Edit**:
Modifying a UID Item's name or UID from the long-press menu. A name-only edit renames the item in place; a UID change saves as a new item with the original kept — from the user's view it doubles as copy.

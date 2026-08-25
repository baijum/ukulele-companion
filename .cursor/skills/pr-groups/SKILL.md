---
name: pr-groups
description: Group open GitHub issues into cohesive sets, each landable as a single PR, with a merge order and a parallelism verdict. Use when the user asks which issues can be fixed in one PR, how to batch issues into PRs, how to plan the work for a release, or how to split a set of issues across sessions. Accepts a priority/area label, a milestone title, or a comma-separated list of issue numbers.
argument-hint: "[label:<name> | milestone-title | NNN,NNN,...]"
---

# PR Groups

Arguments: `$ARGUMENTS`

Turn a set of open issues into **groups, each of which one PR can close**, plus
the order to land them in.

This is the opposite of maximizing parallelism. Two issues in the same function
belong in one PR even though that serializes them — splitting them means two
reviews of the same code and a merge conflict between your own branches.

This is a two-platform repo (Android/Kotlin + iOS/Swift) over a shared KMP
module. That changes the grouping in one specific way, threaded through every
step below: **a fix in `shared/src/commonMain/` lands once and fixes both
platforms**, while a bug that lives on only one platform — very common here,
where issues are usually filed as Android-vs-iOS parity divergences — touches
only that platform's files. Always know which of the three surfaces an issue
sits on (shared / android / ios) before grouping it.

## 1. Scope the set

Interpret the argument:

| Argument shape | Query |
|---|---|
| empty | `gh issue list --state open --limit 500 --json number,title,labels,assignees` |
| `label:<name>` | add `--label "<name>"` (repeat per comma-separated label; they AND). This repo's practical filters are the priority labels `P0` / `P1` / `P2` and the area labels `audio` / `ui` / `music-content` / `accessibility` / `testing` / `ci-cd`. |
| a milestone title | add `--milestone "<title>"` — but milestones are rarely set here; expect an empty result and fall back to asking whether the user meant a label. |
| comma-separated numbers | fetch exactly those with `gh issue view` |

If a filter returns nothing, say so and stop. Never silently widen to the whole
tracker. If the returned count equals the limit, raise the limit and rerun.

Drop from consideration, and say which you dropped and why: epics and tracking
issues, issues already assigned, issues with an open linked PR, and anything
labelled `blocked` / `wontfix` / `duplicate` / `invalid`.

## 2. Read the bodies

Bodies in this tracker are unusually good: they name the exact `.kt` and
`.swift` files with line numbers, and they usually state the Android-vs-iOS
divergence outright ("Android guards this; iOS does not"). Every rule below
depends on knowing which code an issue touches, so fetch bodies:

```bash
gh issue list --state open --limit 500 --label "<name>" --json number,title,labels,body
```

For a large set, write the bodies to a scratch file and read that rather than
paging them through several tool calls.

## 3. Verify each issue still reproduces

**Do this before grouping, not after.** An issue can have been fixed by a PR
that closed its siblings and never got closed itself — scheduling it wastes a
slot in the plan and, worse, makes the whole plan look untrustworthy when
someone discovers it.

Match the repro cost to the surface:

- **Shared-module logic** (`shared/src/commonMain/`) is cheap to check — write
  or run a `shared/src/commonTest/` case, or `./gradlew :shared:testDebugUnitTest`.
  Worth doing for the whole set.
- **Android UI / repository behaviour** needs an emulator; see the
  `android-bug-reproduce` skill. Do it only when the grouping hinges on it.
- **iOS behaviour** needs the simulator. Same rule — only if it decides a group.

Report anything that no longer reproduces as **verify-and-close**, with the
observed output next to the issue's own "Expected" block, and leave it out of
the groups. Do not close it yourself unless the user asks.

Also check whether a merged PR already claims the area: if several sibling
issues were closed together (git log / `gh pr list --state merged`), ask whether
this one was simply missed.

## 4. Ground the file claims in the source

An issue's diagnosis is a hypothesis. Before grouping on a claim, confirm the
named sites still exist — bodies cite line numbers that drift as the file moves:

```bash
grep -n '<symbol>' <path from the body>     # do the named sites exist?
find . -name '<File>.kt' -o -name '<File>.swift' -not -path '*/build/*'
```

What you are looking for is cheap and specific:

- Which surface is this — `shared/src/commonMain/`, `app/src/main/` (Android),
  or `iosApp/` (iOS)? A file with no prefix in the body is often the shared one;
  resolve it with `find`.
- Do the two issues you want to pair really sit in the same function, or just
  the same file?
- Has the surrounding code moved since the issue was filed?

A grouping built on a stale line number falls apart on contact.

## 5. Group by cohesion

Strongest signal first — pair on the highest one that applies:

1. **Same shared-module function or file** (`shared/src/commonMain/`). Highest
   leverage in this repo: one diff, one review, and it fixes Android and iOS at
   once. Prefer landing a bug here over patching each platform separately.
2. **Same platform file, or adjacent arms of one `when` / `switch`.** One
   reviewer context, one file-level test update. (Two Kotlin fixes in one
   `Repository.kt`, or two Swift fixes in one `View.swift`.)
3. **Same parity divergence for one feature.** The commonest shape here: a
   behaviour that differs between the Android and iOS mirror of one feature
   (e.g. a `*Repository` that exists on both platforms). If the fix is
   "make iOS match Android" for the *same feature area*, the two edits — the
   `.kt` and the `.swift` — belong in one PR so the platforms land in parity.
   Also groups two *different* issues when they share one root cause across the
   mirror ("iOS `clearAll`/`importData` destroys data Android already guards").
   State the shared cause in one sentence; if you cannot, it is not a group.
4. **Same verification harness.** Fixes proven by one shared-test run, one
   emulator session, or one simulator session batch well even when otherwise
   unrelated — you pay the harness cost once.
5. **Same zero-risk class.** Pure `music-content` data fixes (chords, scales,
   progressions) or pure string additions touching no audio/engine code batch
   broadly, because the review risk of adding one more is near zero. Say they
   are trivially splittable.

Do **not** group:

- A fix needing a design decision (or asking "is this intended?") with one that
  doesn't. The design discussion will hold the whole PR hostage.
- An **audio hot-path** change (`AudioCaptureEngine`, `PitchDetector`,
  `NeuralPitchSupervisor`, the tuner/monitor buffer loop) with unrelated local
  fixes — it needs its own careful review for per-frame allocations and frame
  dropping.
- An **accessibility-behaviour** change with unrelated logic. Accessibility is
  treated as seriously as functionality here and needs its own TalkBack /
  VoiceOver pass; don't bury it in a mixed PR. Grouping *by* "same screen needs
  one a11y re-test" is fine — that is signal #4.
- Issues whose only link is a shared label or milestone.

Keep a group to what one reviewer can hold at once. Beyond roughly four issues,
split unless they are the zero-risk class. Remember a two-platform group is
already two files' worth of review before you add issues.

## 6. Check what the grouped edits would blow

A group is a plan to make several edits to the same place, so check the
constraints that only bite in aggregate:

- **The 16-locale string table.** If two or more issues in a group each add a
  user-facing string, they all funnel through the same `strings.xml` +
  `Localizable.xcstrings` across 16 locales (see the `add-string` /
  `add-translations` skills). That is both a batching win (do the translation
  pass once) and a merge-conflict magnet if split across branches — note it.
- **The ktlint baseline.** If a group touches many Kotlin files, the PR will
  move `ktlint-baseline.xml`. Never wholesale-regenerate it — that silently
  drops entries for untouched files. Let the ratchet handle only the changed
  files.
- **A broken required check.** A skipped matrix job reports an unexpanded name
  and can strand every PR; if a group would leave a required check red, that is
  a step-7 ordering problem, not a step-6 note.
- **Any single-entry table or generated list** several issues in the group each
  need to extend.

## 7. Order the groups

Three categories land first, and all three were load-bearing in practice:

- **Shared before platform.** A `shared/src/commonMain/` fix that both platforms
  depend on lands before any platform-specific work that assumes it.
- **Instrument before subject.** If one issue is a broken detector, test, or
  assertion for the bug class the others are in, it goes first — otherwise the
  other fixes cannot be verified and a green run proves nothing.
- **Signal before work.** If one issue makes CI produce false reds (an open
  `ci-failure`, a broken required matrix check), it goes first — otherwise every
  later PR's CI is untrustworthy and someone burns time disproving a failure
  they did not cause.

Then: groups touching disjoint files run **in parallel**. Groups needing a
design decision go last, on their own.

Apply any dependency stated in a body ("depends on", "blocked by", "after #NNN
lands") as a hard edge; a satisfied edge (target already closed) is not an edge.

## 8. Name the long pole

Say explicitly which group gates the release, and whether the remaining groups
are shippable without it. If they are, say so in one sentence — it is the most
actionable thing in the whole plan, because it converts a blocked release into
a scoping decision the user can make. A P0 group (data loss / silently wrong /
core-user-base-blocking) is the usual long pole.

## Output format

For each group: a letter, the issue numbers, a **platform tag**
(`shared` / `android` / `ios` / `both`), the files, and one sentence on why
they are one PR. Then the order, then the caveats.

```text
A — #NNN + #NNN · [shared] <shared cause in a few words>   (land first: shared-before-platform)
    files: shared/src/commonMain/.../Foo.kt:41
    <one sentence: why one PR>

B — #NNN · [ios] <what it is>
    files: iosApp/UkuleleCompanion/Repositories/Bar.swift:64
```

Follow with the order as a short diagram:

```text
A  →  B, C, D  (parallel, disjoint files)  →  E  (design-first)
```

Close with: anything that no longer reproduces (from step 3), any repo
constraint a group would blow (step 6 — the 16-locale table, the ktlint
baseline), and the long-pole sentence (step 8).

If the user wants to launch concurrent sessions rather than review a plan, also
emit the group leaders as bare paste-able lines, one wave per line, containing
nothing else:

```text
Wave 1: NNN, NNN
Wave 2: NNN, NNN, NNN
```

Waves come from the order in step 7 — every group in a wave touches files no
other group in that wave touches, so their PRs can merge in any order.

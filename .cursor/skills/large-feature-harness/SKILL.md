---
name: large-feature-harness
description: Planner-Generator-Evaluator harness for large, multi-screen features or major refactors. Separates planning, implementation, and evaluation into distinct phases with isolated context, preventing self-grading bias and context drift. Use when the user asks to build a large feature, do a major refactor, or implement something spanning multiple screens/files.
allowed-tools: Read, Grep, Glob, Bash(./gradlew *), Bash(xcodebuild *), Bash(scripts/preflight.sh*), Write, Edit, MultiEdit
---

# Large Feature Harness (Planner-Generator-Evaluator)

A three-phase workflow for tasks too large for a single agent pass. Based on
Anthropic's harness design pattern — separates planning, generation, and
evaluation so each phase has focused context and the evaluator cannot be
influenced by the generator's reasoning.

## When to Use

- Feature spanning 3+ screens or ViewModels
- Refactors touching 10+ files
- New feature areas requiring both Android and iOS implementation
- Multi-hour autonomous coding sessions

For single-file changes or small bug fixes, skip this and work directly.

## Phase 1: Plan

Produce a structured specification before writing any code.

1. **Gather context.** Read the relevant existing code, AGENTS.md, and any
   applicable `.cursor/rules/*.mdc` files. Use the architecture map
   (`docs/architecture-map.md`) to understand navigation and ViewModel mappings.

2. **Write a plan document.** Create `docs/plans/<feature-name>.md` with:

   ```markdown
   # Feature: <name>

   ## Goal
   One paragraph describing the user-visible outcome.

   ## Screens / Components
   - [ ] <ScreenName> — backed by <ViewModel>, what it does
   - [ ] ...

   ## Shared Domain Changes
   - [ ] <class/function> in shared/src/commonMain/...

   ## Data Model Changes
   - [ ] <enum/data class> changes

   ## Acceptance Criteria
   - [ ] Criterion 1 (testable)
   - [ ] Criterion 2 (testable)
   - [ ] ...

   ## Sprint Order
   1. Sprint 1: <scope> — what "done" looks like
   2. Sprint 2: <scope> — what "done" looks like
   3. ...
   ```

3. **Review with user.** Present the plan and get confirmation before proceeding.

## Phase 2: Generate (Sprint Loop)

Implement one sprint at a time. Each sprint produces a buildable, testable
increment.

For each sprint:

1. **Read the plan** to understand the current sprint scope.
2. **Implement** the code changes for this sprint only.
3. **Run automated checks** after implementation:
   ```bash
   ./gradlew assembleDebug        # Android builds
   ./gradlew testDebugUnitTest    # Unit tests pass
   ./gradlew :shared:jvmTest      # Shared tests pass
   ```
4. **Update the plan** — check off completed items, note any deviations.
5. **Commit** the sprint with message: `Add: <feature> — sprint N (<scope>)`.

Do NOT self-evaluate quality or completeness — that is the evaluator's job.

## Phase 3: Evaluate

After all sprints are complete (or after each sprint for critical features),
run evaluation separately. The evaluator should NOT read the generator's
reasoning or chat history — only the code diff and the acceptance criteria.

### Automated Evaluation

Run the full quality gate suite:

```bash
scripts/preflight.sh                # ktlint + shared tests + unit tests + lint
```

If iOS code was touched:

```bash
xcodebuild -project iosApp/UkuleleCompanion.xcodeproj \
  -scheme UkuleleCompanion \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  build
```

### Accessibility Evaluation

Launch the accessibility-reviewer subagent against the diff:

```
Review the diff for TalkBack/VoiceOver regressions against
.cursor/rules/compose-accessibility.mdc and
.cursor/rules/swiftui-accessibility.mdc.
```

### Rubric-Based Evaluation

Grade against the acceptance criteria from the plan. For each criterion,
assign PASS / FAIL with a one-line justification. If any criterion fails,
re-invoke the generator with the specific failure — NOT the full evaluator
transcript, just the finding:

```
Sprint N evaluation: FAIL on criterion "<X>".
Finding: <specific issue and location>.
Fix and re-run the automated checks.
```

### Evaluation Checklist

- [ ] All automated gates pass (preflight + iOS build if applicable)
- [ ] Every acceptance criterion from the plan is met
- [ ] No accessibility regressions (reviewer subagent confirms)
- [ ] Both High-G and Low-G tuning work (if chord/note logic changed)
- [ ] Left-handed mode not broken (if fretboard UI changed)
- [ ] Animations respect reduce motion
- [ ] No per-frame allocations in audio hot path

## Key Principles

1. **Separate evaluation from generation.** The agent must not grade its own
   work — use a separate evaluation pass or subagent.
2. **Define "done" before starting.** The plan's acceptance criteria are the
   contract. Sprint scopes are fixed before implementation begins.
3. **Structured handoffs.** The plan document is the persistent artifact that
   survives context resets between phases.
4. **Fail fast with specifics.** Evaluation failures produce actionable
   findings (file, line, what's wrong), not vague "needs improvement."

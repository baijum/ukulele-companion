# Test Plan: Practice & Scoring

## Scope
- `shared/.../domain/PracticeRoutineGenerator.kt`
- `shared/.../domain/PlayAlongScorer.kt`
- `shared/.../data/DailyChallengeGenerator.kt`

## Current Coverage
None.

## Files to Create
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/PracticeRoutineGeneratorTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/domain/PlayAlongScorerTest.kt`
- `shared/src/commonTest/kotlin/com/baijum/ukufretboard/data/DailyChallengeGeneratorTest.kt`

## PracticeRoutineGenerator Test Cases

### `generate(durationMinutes, skillLevel, focusAreas)`
- 15-minute routine → total step durations sum to ~15 minutes
- 30-minute routine → longer/more steps
- Beginner level → simpler exercises
- Advanced level → includes complex exercises
- Focus on specific area → routine weighted toward that area
- Empty focus areas → balanced routine
- All SkillLevel values produce valid routines
- All FocusArea values are handled
- Each step has non-empty title and description
- Step types are valid StepType enum values

## PlayAlongScorer Test Cases

### `recordBeat()` / `getScore()`
- Perfect timing → high accuracy score
- All beats on time → streak increases
- Missed beat → breaks streak
- Score grades: all correct → "A", none correct → "F"
- Mixed performance → intermediate grade (B, C, D)
- Empty recording → zero score
- Streak tracking: consecutive hits increment, miss resets to 0
- Best streak tracked across session

### Scoring Boundaries
- 90%+ → A
- 80-89% → B
- 70-79% → C (verify actual thresholds from code)
- Accuracy = hits / total beats

## DailyChallengeGenerator Test Cases

### `generateForDate(year, dayOfYear)`
- Returns exactly 3 challenges
- Same date always returns same challenges (deterministic)
- Different dates return different challenges
- Each challenge has valid ChallengeType
- Each challenge has non-empty title and description
- targetCount > 0 for each challenge
- navTarget is non-empty (valid navigation destination)

### `today()`
- Returns 3 challenges (delegates to generateForDate)

### Date Determinism
- generateForDate(2026, 1) == generateForDate(2026, 1) (idempotent)
- generateForDate(2026, 1) != generateForDate(2026, 2) (varies by day)
- generateForDate(2025, 100) != generateForDate(2026, 100) (varies by year)

### Challenge Type Coverage
- Over many dates, all ChallengeType values appear (run for 30 consecutive days)

## Priority
Medium — PlayAlongScorer has stateful logic (streak tracking, grading) that benefits from thorough testing. DailyChallengeGenerator's determinism is important for user experience.

## Estimated Test Count
~30-35 test cases

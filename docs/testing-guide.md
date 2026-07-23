# Testing Guide for AI Agents

Which test patterns to use for which component type, how to run them, and
what a good test looks like. Supplements `.cursor/rules/testing-conventions.mdc`.

## Decision Tree: What Test to Write

```
Is the logic in shared/src/commonMain/?
├── Yes: Write in shared/src/commonTest/ using JUnit 4
│   ├── Has mathematical invariants? → Kotest property test (*PropertyTest.kt)
│   └── Specific behavior? → Standard JUnit test (*Test.kt)
│
Is it an Android ViewModel?
├── Yes: Write in app/src/test/ using JUnit 4
│   └── Test StateFlow emissions with Turbine or collectAsState assertions
│
Is it Android domain logic (not in shared)?
├── Yes: Write in app/src/test/ using JUnit 4
│   └── Consider extracting to shared first (see /extract-to-shared)
│
Is it a UI accessibility concern?
├── Yes: Write in app/src/androidTest/ using Compose UI Testing
│   └── Assert semantics nodes, content descriptions, heading traits
│
Is it audio processing (fuzz testing)?
├── Yes: Write in app/src/test/fuzz/ using Kotest property tests
│   └── Generate random audio buffers and verify no crashes/NaN
│
Is it iOS-specific?
└── Yes: Write in iosApp/UkuleleCompanionTests/ using XCTest
```

## Test Locations and Frameworks

| Layer | Directory | Framework | Assertions | Runner |
|-------|-----------|-----------|------------|--------|
| Shared domain/data (51 files) | `shared/src/commonTest/` | `kotlin.test` + Kotest Property | `kotlin.test.*` | `./gradlew :shared:jvmTest` |
| Android ViewModel (5 files) | `app/src/test/.../viewmodel/` | JUnit 4, some Robolectric | `org.junit.Assert.*` | `./gradlew testDebugUnitTest` |
| Android domain (3 files) | `app/src/test/.../domain/` | JUnit 4, Robolectric | `org.junit.Assert.*` | `./gradlew testDebugUnitTest` |
| Android data/repo (4 files) | `app/src/test/.../data/` | JUnit 4, Robolectric | `org.junit.Assert.*` | `./gradlew testDebugUnitTest` |
| Audio fuzz (6 files) | `app/src/test/.../fuzz/` | Jazzer `@FuzzTest` (JUnit 5) | Must not throw | `./gradlew testDebugUnitTest` |
| Android UI/a11y (3 files) | `app/src/androidTest/` | Compose UI Testing (JUnit 4) | Compose matchers | `./gradlew connectedAndroidTest` |
| iOS (17 files) | `iosApp/UkuleleCompanionTests/` | XCTest | `XCTAssert*` | Xcode test runner |

**Note:** Shared tests use `kotlin.test` (multiplatform), not JUnit directly.
Android unit tests use JUnit 4 + JUnit Platform (for Jazzer). No Mockito/mockk
anywhere — tests use real objects or injectable fakes (e.g. `timeProvider` lambda).
No shared test-helper module — helpers are file-local.

## Patterns by Component Type

### Shared Domain Logic (Property Tests)

For functions with mathematical invariants (transpose, pitch detection, chord
detection), write property tests that generate thousands of random inputs.
Use `kotlin.test.Test` (not `org.junit.Test`) in shared tests.

```kotlin
// shared/src/commonTest/.../domain/TransposePropertyTest.kt
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TransposePropertyTest {
    @Test
    fun transposeThenInverseIsIdentity() = runBlocking {
        checkAll(Arb.int(0..11), Arb.int(-48..48)) { pitchClass, semitones ->
            val transposed = Transpose.transposePitchClass(pitchClass, semitones)
            assertEquals(pitchClass, Transpose.transposePitchClass(transposed, -semitones))
        }
    }
}
```

Key invariants to test:
- **Identity:** `f(x, 0) == x`
- **Round-trip:** `inverse(f(x)) == x`
- **Associativity:** `f(f(x, a), b) == f(x, a + b)`
- **Robustness:** no crash on arbitrary input

### Shared Data Models (Unit Tests)

For data classes, enums, parsers — test serialization round-trips, edge cases,
and known values.

```kotlin
// shared/src/commonTest/.../data/ChordParserTest.kt
@Test
fun `parse C major`() {
    val result = ChordParser.parse("C")
    assertEquals("C", result.root)
    assertEquals(ChordFormula.MAJOR, result.formula)
}
```

### Android ViewModels

Test StateFlow emissions in response to actions. Mock or fake the repository.

```kotlin
// app/src/test/.../viewmodel/FretboardViewModelTest.kt
@Test
fun `selecting root updates state`() {
    val vm = FretboardViewModel()
    vm.selectRoot(0) // C
    assertEquals(0, vm.uiState.value.selectedRootPitchClass)
}
```

### Audio Fuzz Tests (Jazzer)

The project uses Jazzer `@FuzzTest` (JUnit 5) for audio pipeline fuzzing.
These generate arbitrary inputs and verify no crashes or exceptions.

```kotlin
// app/src/test/.../fuzz/ChordNameParserFuzz.kt
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest

class ChordNameParserFuzz {
    @FuzzTest
    fun fuzzParse(data: FuzzedDataProvider) {
        ChordNameParser.parse(data.consumeRemainingAsString()) // must not throw
    }
}
```

Six fuzz test files exist: `FFTProcessorFuzz`, `PitchDetectorFuzz`,
`TunerNoteMapperFuzz`, `AudioResamplerFuzz`, `ChordDetectorFuzz`,
`ChordNameParserFuzz`.

### Instrumented Accessibility Tests

Assert that TalkBack-relevant semantics are present on UI nodes.

```kotlin
// app/src/androidTest/.../AccessibilityTest.kt
@Test
fun tunerHasLiveRegion() {
    composeTestRule.setContent { TunerTab(...) }
    composeTestRule
        .onNodeWithText("detected note")
        .assertExists()
        .assert(hasLiveRegion())
}
```

## What to Always Test

| Change Type | Required Tests |
|-------------|---------------|
| New shared domain function | Property test for invariants + unit tests for known values |
| New/modified ViewModel | StateFlow emission tests |
| Chord/note logic change | Both High-G and Low-G tuning |
| Fretboard UI change | Left-handed mode |
| Audio pipeline change | Fuzz test with random buffers |
| New UI screen | Accessibility instrumented test |
| Parser/formatter | Round-trip property test |

## Running Tests

```bash
# Quick: just shared + unit
./gradlew :shared:jvmTest && ./gradlew testDebugUnitTest

# Full preflight (includes lint)
scripts/preflight.sh

# Instrumented (requires running emulator)
./gradlew connectedAndroidTest

# Single test class
./gradlew :shared:jvmTest --tests "*PitchDetectorTest"
./gradlew testDebugUnitTest --tests "*FretboardViewModelTest"

# iOS
xcodebuild test -project iosApp/UkuleleCompanion.xcodeproj \
  -scheme UkuleleCompanion \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro'
```

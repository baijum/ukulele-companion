package com.baijum.ukufretboard.audio

import com.baijum.ukufretboard.data.BeatType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [MetronomeEngine.startMetronome].
 *
 * Regression coverage for #582: accent/mute edits made while the metronome is
 * running must take effect without restarting the bar. The engine takes the
 * accent pattern as a provider (`() -> List<BeatType>`) and re-reads it on each
 * beat, so mutating the source list mid-run is reflected on the next beat.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeEngineTest {
    @Test
    fun muteEditWhileRunningTakesEffectOnNextMeasureWithoutRestart() =
        runTest {
            val engine = MetronomeEngine()

            // Mutable source the provider reads from — mirrors the ViewModel's StateFlow.
            var pattern =
                listOf(BeatType.NORMAL, BeatType.NORMAL, BeatType.NORMAL, BeatType.NORMAL)

            // Record the type of each main beat (subBeat == 0) in order.
            val mainBeatTypes = mutableListOf<BeatType>()

            engine.startMetronome(
                scope = this,
                bpm = 60, // subdivision 1 => 1000 ms per tick
                beatsPerMeasure = 4,
                subdivision = 1,
                accentPattern = { pattern },
                onTick = { _, subBeat, type ->
                    if (subBeat == 0) mainBeatTypes.add(type)
                },
            )

            // Play the first full measure (4 beats at 1000 ms each).
            advanceTimeBy(4_000)
            runCurrent()

            // First measure is untouched: every beat NORMAL.
            assertEquals(
                listOf(BeatType.NORMAL, BeatType.NORMAL, BeatType.NORMAL, BeatType.NORMAL),
                mainBeatTypes.take(4),
            )

            // Mute beat index 2 while the engine keeps running (no restart).
            pattern = pattern.toMutableList().also { it[2] = BeatType.MUTE }

            // Play the next measure.
            advanceTimeBy(4_000)
            runCurrent()
            engine.stop()

            // The running tick loop picked up the edit: beat index 2 is now muted.
            val secondMeasure = mainBeatTypes.drop(4).take(4)
            assertEquals(
                "mid-run mute must be reflected in the pattern the tick loop reads",
                listOf(BeatType.NORMAL, BeatType.NORMAL, BeatType.MUTE, BeatType.NORMAL),
                secondMeasure,
            )
        }
}

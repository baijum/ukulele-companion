package com.baijum.ukufretboard.audio

import com.baijum.ukufretboard.data.BeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A coroutine-based metronome engine that supports both chord-progression playback
 * and standalone metronome mode with subdivisions and accent patterns.
 *
 * This uses `delay()` for timing, which provides sufficient accuracy
 * for a practice metronome (within ~10ms precision).
 */
class MetronomeEngine {

    private var job: Job? = null

    /** Whether the metronome is currently playing. */
    val isPlaying: Boolean get() = job?.isActive == true

    /**
     * Starts the metronome.
     *
     * @param scope The coroutine scope to launch in.
     * @param bpm Beats per minute (60–240).
     * @param beatsPerChord Number of beats before advancing to the next chord.
     * @param chordCount Total number of chords in the progression.
     * @param loop Whether to loop back to the first chord after the last.
     * @param onBeat Callback fired on each beat with (chordIndex, beatInChord).
     *   chordIndex is 0-based; beatInChord is 1-based.
     * @param onComplete Callback fired when playback finishes (only if not looping).
     */
    fun start(
        scope: CoroutineScope,
        bpm: Int,
        beatsPerChord: Int,
        chordCount: Int,
        loop: Boolean,
        onBeat: (chordIndex: Int, beatInChord: Int) -> Unit,
        onComplete: () -> Unit = {},
    ) {
        stop() // Cancel any existing playback

        val beatDurationMs = (60_000L / bpm.coerceIn(30, 300))

        job = scope.launch {
            var chordIndex = 0
            var beat = 1

            // Fire the first beat immediately
            onBeat(chordIndex, beat)

            while (isActive) {
                delay(beatDurationMs)
                if (!isActive) break

                beat++
                if (beat > beatsPerChord) {
                    beat = 1
                    chordIndex++
                    if (chordIndex >= chordCount) {
                        if (loop) {
                            chordIndex = 0
                        } else {
                            onComplete()
                            return@launch
                        }
                    }
                }
                onBeat(chordIndex, beat)
            }
        }
    }

    /**
     * Starts a standalone metronome with subdivision and accent pattern support.
     *
     * @param scope The coroutine scope to launch in.
     * @param bpm Beats per minute (40–300).
     * @param beatsPerMeasure Number of beats in each measure.
     * @param subdivision Number of ticks per beat (1 = quarter, 2 = eighth, 3 = triplet, 4 = sixteenth).
     * @param accentPattern Provider of the [BeatType] for each beat in the measure. Read once per
     *   main beat so accent/mute edits made while running take effect on the next beat without
     *   restarting the bar. The returned list is expected to have [beatsPerMeasure] entries.
     * @param onTick Callback fired on each tick with (beat 0-based, subBeat 0-based, type).
     *   On the main beat (subBeat == 0), type comes from [accentPattern].
     *   On subdivision ticks (subBeat > 0), type is always [BeatType.NORMAL].
     * @param onMeasure Callback fired at the start of each new measure with the measure count (1-based).
     */
    fun startMetronome(
        scope: CoroutineScope,
        bpm: Int,
        beatsPerMeasure: Int,
        subdivision: Int,
        accentPattern: () -> List<BeatType>,
        onTick: (beat: Int, subBeat: Int, type: BeatType) -> Unit,
        onMeasure: (measureCount: Int) -> Unit = {},
    ) {
        stop()

        val tickDurationMs = 60_000L / (bpm.coerceIn(30, 300).toLong() * subdivision.coerceIn(1, 4))

        job = scope.launch {
            var measureCount = 1
            onMeasure(measureCount)

            while (isActive) {
                for (beat in 0 until beatsPerMeasure) {
                    // Re-read the pattern each main beat so accent/mute edits made while
                    // running take effect on the next beat (see issue #582).
                    val currentPattern = accentPattern()
                    for (sub in 0 until subdivision) {
                        if (!isActive) return@launch
                        val type = if (sub == 0) {
                            currentPattern.getOrElse(beat) { BeatType.NORMAL }
                        } else {
                            BeatType.NORMAL
                        }
                        onTick(beat, sub, type)
                        delay(tickDurationMs)
                    }
                }
                measureCount++
                onMeasure(measureCount)
            }
        }
    }

    /**
     * Stops the metronome.
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}

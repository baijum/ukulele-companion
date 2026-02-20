package com.baijum.ukufretboard.audio

import com.baijum.ukufretboard.data.FingerpickingPattern
import com.baijum.ukufretboard.data.StrumDirection
import com.baijum.ukufretboard.data.StrumPattern
import com.baijum.ukufretboard.data.UkuleleTuning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays strumming and fingerpicking patterns as audio using [ToneGenerator].
 *
 * Only one pattern plays at a time — calling any play method cancels the
 * previous playback. The caller provides the [CoroutineScope]; cancelling
 * that scope also stops playback.
 *
 * Open strings of the supplied [UkuleleTuning] are used as the demo chord,
 * so the sound matches whatever tuning the user has selected.
 */
class PatternPlayer {

    private var job: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentIndex = MutableStateFlow(-1)

    /** Index of the beat/step currently sounding, or -1 when idle. */
    val currentIndex: StateFlow<Int> = _currentIndex

    /** Volume boost for accented beats relative to normal beats. */
    private val emphasisVolume = 1.0f
    private val normalVolume = 0.7f

    /** Strum delay kept short so the chord sounds tight. */
    private val strumDelayMs = 30

    /** Short note duration for chuck/muted hits. */
    private val chuckDurationMs = 40

    /**
     * Plays a [StrumPattern] in a loop at the given [bpm].
     *
     * DOWN and UP beats trigger a chord strum on all open strings.
     * CHUCK triggers a very short strum (muted sound). MISS and PAUSE
     * are silent rests that still occupy the beat's time slot.
     */
    fun playStrum(
        scope: CoroutineScope,
        pattern: StrumPattern,
        bpm: Int,
        tuning: UkuleleTuning,
    ) {
        stop()
        val beatMs = 60_000L / bpm.coerceIn(30, 300)
        val openStrings = openStringNotes(tuning)
        val reversedStrings = openStrings.reversed()

        job = scope.launch {
            _isPlaying.value = true
            try {
                while (isActive) {
                    pattern.beats.forEachIndexed { index, beat ->
                        if (!isActive) return@launch
                        _currentIndex.value = index
                        val vol = if (beat.emphasis) emphasisVolume else normalVolume

                        when (beat.direction) {
                            StrumDirection.DOWN -> {
                                ToneGenerator.fireChord(openStrings, strumDelayMs, vol)
                            }
                            StrumDirection.UP -> {
                                ToneGenerator.fireChord(reversedStrings, strumDelayMs, vol)
                            }
                            StrumDirection.CHUCK -> {
                                ToneGenerator.fireChord(openStrings, strumDelayMs, vol * 0.5f)
                                delay(chuckDurationMs.toLong())
                            }
                            StrumDirection.MISS, StrumDirection.PAUSE -> {
                                // silent rest
                            }
                        }
                        delay(beatMs)
                    }
                }
            } finally {
                _isPlaying.value = false
                _currentIndex.value = -1
            }
        }
    }

    /**
     * Plays a [FingerpickingPattern] in a loop at the given [bpm].
     *
     * Each step plucks a single open string with [ToneGenerator.fireNote].
     */
    fun playFingerpick(
        scope: CoroutineScope,
        pattern: FingerpickingPattern,
        bpm: Int,
        tuning: UkuleleTuning,
    ) {
        stop()
        val stepMs = 60_000L / bpm.coerceIn(30, 300)

        job = scope.launch {
            _isPlaying.value = true
            try {
                while (isActive) {
                    pattern.steps.forEachIndexed { index, step ->
                        if (!isActive) return@launch
                        _currentIndex.value = index
                        val vol = if (step.emphasis) emphasisVolume else normalVolume
                        val pc = tuning.pitchClasses[step.stringIndex]
                        val oct = tuning.octaves[step.stringIndex]
                        ToneGenerator.fireNote(pc, oct, vol)
                        delay(stepMs)
                    }
                }
            } finally {
                _isPlaying.value = false
                _currentIndex.value = -1
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _isPlaying.value = false
        _currentIndex.value = -1
    }

    private fun openStringNotes(tuning: UkuleleTuning): List<Pair<Int, Int>> =
        tuning.pitchClasses.zip(tuning.octaves)
}

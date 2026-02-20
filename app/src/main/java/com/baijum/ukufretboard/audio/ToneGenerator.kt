package com.baijum.ukufretboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.baijum.ukufretboard.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.pow

/**
 * Plays ukulele notes using real sampled audio from OGG files.
 *
 * Contains one recorded sample per chromatic pitch class (C through B).
 * Notes in different octaves are produced by adjusting the playback rate
 * (e.g., 2× for one octave up, 0.5× for one octave down).
 *
 * Samples sourced from Freesound.org — "Ukelele single notes, close-mic" pack
 * by stomachache (CC BY 3.0). See ATTRIBUTION.md for details.
 *
 * Uses Android's [SoundPool] for low-latency, polyphonic playback. Up to 8
 * simultaneous streams are supported, allowing full chord playback with overlap.
 *
 * Thread-safe: uses a [Mutex] to ensure only one chord/strum plays at a time
 * (individual notes within a chord are polyphonic).
 */
object ToneGenerator {

    /**
     * The octave of the recorded samples.
     * Standard ukulele range starts at C4, so the samples represent octave 4.
     */
    private const val SAMPLE_OCTAVE = 4

    /** Default duration of a single note in milliseconds. */
    private const val DEFAULT_NOTE_DURATION_MS = 600

    /** Default time between successive string plucks in a strum, in milliseconds. */
    private const val DEFAULT_STRUM_DELAY_MS = 50

    /** Maximum simultaneous audio streams (4 strings + headroom for overlap). */
    private const val MAX_STREAMS = 8

    /** SoundPool instance for low-latency playback. */
    private var soundPool: SoundPool? = null

    /** Loaded sample IDs indexed by pitch class (0 = C, 1 = C#, ... 11 = B). */
    private val sampleIds = IntArray(12)

    /** Whether the SoundPool has been initialized with samples. */
    private var initialized = false

    /** Mutex to prevent overlapping chord/strum sequences. */
    private val playbackMutex = Mutex()

    /**
     * Mapping from pitch class to the corresponding raw resource file.
     *
     * Each entry maps a pitch class (0–11) to an OGG file in res/raw/
     * containing a single recorded ukulele note.
     */
    private val SAMPLE_RESOURCES = mapOf(
        0 to R.raw.uke_c,
        1 to R.raw.uke_csharp,
        2 to R.raw.uke_d,
        3 to R.raw.uke_dsharp,
        4 to R.raw.uke_e,
        5 to R.raw.uke_f,
        6 to R.raw.uke_fsharp,
        7 to R.raw.uke_g,
        8 to R.raw.uke_gsharp,
        9 to R.raw.uke_a,
        10 to R.raw.uke_asharp,
        11 to R.raw.uke_b,
    )

    /**
     * Initializes the SoundPool and loads all 12 chromatic samples.
     *
     * Must be called once with a valid [Context] before any playback methods.
     * Safe to call multiple times — subsequent calls are no-ops.
     *
     * @param context An Android context (typically the application context).
     */
    fun init(context: Context) {
        if (initialized) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attrs)
            .build()

        SAMPLE_RESOURCES.forEach { (pitchClass, resId) ->
            sampleIds[pitchClass] = soundPool!!.load(context, resId, 1)
        }

        initialized = true
    }

    /**
     * Releases the SoundPool and all loaded samples.
     *
     * Call this when the audio engine is no longer needed (e.g., on app destroy).
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        initialized = false
    }

    /**
     * Calculates the frequency in Hz for a given pitch class and octave.
     *
     * Uses the equal-temperament formula:
     *     frequency = 440 × 2^((midiNote - 69) / 12)
     *
     * Retained for any code that needs frequency values (e.g., display purposes).
     *
     * @param pitchClass The pitch class (0 = C, 1 = C#, ... 11 = B).
     * @param octave The octave number (e.g., 4 for the octave containing A440).
     * @return The frequency in Hz.
     */
    fun frequencyOf(pitchClass: Int, octave: Int): Double {
        val midiNote = (octave + 1) * 12 + pitchClass
        return 440.0 * 2.0.pow((midiNote - 69).toDouble() / 12.0)
    }

    /**
     * Plays a single sampled note at the given pitch class and octave.
     *
     * Selects the sample matching the pitch class and adjusts the playback rate
     * to shift it to the target octave. Acquires [playbackMutex] to prevent
     * overlapping sequences.
     *
     * @param pitchClass The pitch class (0–11).
     * @param octave The octave (typically 3–5 for ukulele).
     * @param durationMs Duration to sustain playback before allowing the next sound.
     */
    suspend fun playNote(
        pitchClass: Int,
        octave: Int,
        durationMs: Int = DEFAULT_NOTE_DURATION_MS,
        volume: Float = 1f,
    ) {
        val sp = soundPool ?: return
        val sampleId = sampleIds[pitchClass % 12]
        if (sampleId == 0) return

        val rate = playbackRate(octave)
        val vol = volume.coerceIn(0f, 1f)

        playbackMutex.withLock {
            withContext(Dispatchers.Default) {
                sp.play(sampleId, vol, vol, 1, 0, rate)
                delay(durationMs.toLong())
            }
        }
    }

    /**
     * Plays a chord as a strum — notes are triggered with a slight delay between
     * successive strings, simulating a real strum.
     *
     * Each note is played via SoundPool (polyphonic — all notes overlap naturally).
     * The strum delay creates the characteristic "raked" sound.
     *
     * @param notes A list of (pitchClass, octave) pairs, one per string, in strum order.
     * @param noteDurationMs Duration to hold after the last note before releasing the mutex.
     * @param strumDelayMs Delay between successive string plucks in milliseconds.
     */
    suspend fun playChord(
        notes: List<Pair<Int, Int>>,
        noteDurationMs: Int = DEFAULT_NOTE_DURATION_MS,
        strumDelayMs: Int = DEFAULT_STRUM_DELAY_MS,
        volume: Float = 1f,
    ) {
        if (notes.isEmpty()) return
        val sp = soundPool ?: return
        val vol = volume.coerceIn(0f, 1f)

        playbackMutex.withLock {
            withContext(Dispatchers.Default) {
                notes.forEachIndexed { index, (pitchClass, octave) ->
                    val sampleId = sampleIds[pitchClass % 12]
                    if (sampleId != 0) {
                        val rate = playbackRate(octave)
                        sp.play(sampleId, vol, vol, 1, 0, rate)
                    }
                    if (index < notes.size - 1) {
                        delay(strumDelayMs.toLong())
                    }
                }
                // Allow the final note to ring before releasing the mutex
                delay(noteDurationMs.toLong())
            }
        }
    }

    /**
     * Triggers a single note without acquiring the playback mutex or waiting.
     *
     * Intended for callers (e.g. [PatternPlayer]) that manage their own
     * timing and need to fire notes in rapid succession.
     *
     * @param pitchClass The pitch class (0–11).
     * @param octave The octave (typically 3–5 for ukulele).
     * @param volume Playback volume 0..1.
     */
    fun fireNote(pitchClass: Int, octave: Int, volume: Float = 1f) {
        val sp = soundPool ?: return
        val sampleId = sampleIds[pitchClass % 12]
        if (sampleId == 0) return
        val rate = playbackRate(octave)
        val vol = volume.coerceIn(0f, 1f)
        sp.play(sampleId, vol, vol, 1, 0, rate)
    }

    /**
     * Triggers a chord strum without acquiring the playback mutex or waiting.
     *
     * Fires all notes with the given inter-string delay on [Dispatchers.Default].
     * Intended for callers that manage their own beat timing externally.
     *
     * @param notes A list of (pitchClass, octave) pairs in strum order.
     * @param strumDelayMs Delay between successive string plucks.
     * @param volume Playback volume 0..1.
     */
    suspend fun fireChord(
        notes: List<Pair<Int, Int>>,
        strumDelayMs: Int = DEFAULT_STRUM_DELAY_MS,
        volume: Float = 1f,
    ) {
        if (notes.isEmpty()) return
        val sp = soundPool ?: return
        val vol = volume.coerceIn(0f, 1f)

        withContext(Dispatchers.Default) {
            notes.forEachIndexed { index, (pitchClass, octave) ->
                val sampleId = sampleIds[pitchClass % 12]
                if (sampleId != 0) {
                    val rate = playbackRate(octave)
                    sp.play(sampleId, vol, vol, 1, 0, rate)
                }
                if (index < notes.size - 1) {
                    delay(strumDelayMs.toLong())
                }
            }
        }
    }

    /**
     * Computes the SoundPool playback rate for a target octave.
     *
     * The rate doubles per octave up and halves per octave down, relative to
     * [SAMPLE_OCTAVE]. SoundPool supports rates from 0.5 to 2.0, giving us
     * a usable range of octave 3 through octave 5.
     *
     * @param octave The target octave.
     * @return The playback rate, clamped to [0.5, 2.0].
     */
    private fun playbackRate(octave: Int): Float {
        val octaveDiff = octave - SAMPLE_OCTAVE
        return 2.0.pow(octaveDiff.toDouble()).toFloat().coerceIn(0.5f, 2.0f)
    }
}

package com.baijum.ukufretboard.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Captures audio from the device microphone and emits normalised sample buffers.
 *
 * Wraps Android's [AudioRecord] API, reading PCM 16-bit mono audio at 44.1 kHz
 * in a coroutine loop. Incoming samples are accumulated in a circular ring
 * buffer. Once a full [FRAME_SIZE] window has been filled, an analysis frame is
 * emitted via [onBuffer][start] every [HOP_SIZE] new samples, producing 75%
 * overlap between consecutive frames (~43 updates/second).
 *
 * Lifecycle mirrors [ToneGenerator]: call [start] to begin capture and [stop]
 * to release resources. The RECORD_AUDIO permission must be granted before
 * calling [start]; if not, the call is a no-op.
 */
object AudioCaptureEngine {

    /** Recording sample rate in Hz. */
    const val SAMPLE_RATE = 44100

    /**
     * Number of samples per analysis frame.
     *
     * 4096 samples at 44.1 kHz ≈ 93 ms — long enough to resolve the lowest
     * ukulele fundamental (D3 ≈ 147 Hz requires ≥ 2 periods ≈ 14 ms) while
     * keeping latency comfortable.
     */
    const val FRAME_SIZE = 4096

    /**
     * Number of new samples between consecutive analysis frames (hop size).
     *
     * With [FRAME_SIZE] = 4096, a hop of 1024 produces 75 % overlap and an
     * analysis update every ~23 ms — roughly 43 updates per second —
     * providing smooth visual feedback for the tuner needle.
     */
    const val HOP_SIZE = 1024

    /** Factor to convert a signed 16-bit PCM sample to the -1.0..1.0 range. */
    private const val SHORT_TO_FLOAT = 1.0f / Short.MAX_VALUE

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    /** Whether capture is currently active. */
    val isCapturing: Boolean get() = captureJob?.isActive == true

    /**
     * Begins capturing audio from the microphone.
     *
     * Samples are read in [HOP_SIZE] chunks and accumulated in a circular
     * ring buffer. Once the buffer contains at least [FRAME_SIZE] samples, an
     * overlapping analysis frame is linearised and passed to [onBuffer] on a
     * background thread every [HOP_SIZE] new samples.
     *
     * @param scope    Coroutine scope that owns the capture lifetime.
     * @param onBuffer Callback receiving a [FloatArray] of [FRAME_SIZE]
     *   normalised samples. The array is reused across calls; consumers must
     *   not hold a reference beyond the callback invocation.
     */
    fun start(scope: CoroutineScope, onBuffer: (FloatArray) -> Unit) {
        if (isCapturing) return

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBuf, FRAME_SIZE * 2) // at least 2 frames

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (_: SecurityException) {
            // Permission not granted — caller should check before starting.
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }

        audioRecord = recorder
        recorder.startRecording()

        captureJob = scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    // Read from the recorder in hop-sized chunks.
                    val shortBuf = ShortArray(HOP_SIZE)

                    // Circular ring buffer that always holds the most recent
                    // FRAME_SIZE samples.
                    val ringBuffer = FloatArray(FRAME_SIZE)
                    var writePos = 0

                    // Reusable linear buffer passed to the callback.
                    val analysisBuf = FloatArray(FRAME_SIZE)

                    // Counts samples accumulated since the last emission (or
                    // since capture started).  An analysis frame is emitted
                    // once `filled` reaches FRAME_SIZE, then every HOP_SIZE
                    // new samples thereafter.
                    var filled = 0

                    while (isActive) {
                        val read = try {
                            recorder.read(shortBuf, 0, HOP_SIZE)
                        } catch (_: IllegalStateException) {
                            // Recorder was stopped/released — exit gracefully.
                            break
                        }
                        if (read <= 0) continue

                        // Accumulate into the ring buffer.
                        for (i in 0 until read) {
                            ringBuffer[writePos] = shortBuf[i] * SHORT_TO_FLOAT
                            writePos = (writePos + 1) % FRAME_SIZE
                        }
                        filled += read

                        // Emit once we have a full analysis window, then
                        // every HOP_SIZE new samples thereafter.
                        if (filled >= FRAME_SIZE) {
                            // Linearise: writePos points one past the newest
                            // sample, so (writePos + 0) % FRAME_SIZE is the
                            // oldest sample in the window.
                            for (i in 0 until FRAME_SIZE) {
                                analysisBuf[i] =
                                    ringBuffer[(writePos + i) % FRAME_SIZE]
                            }
                            onBuffer(analysisBuf)
                            filled -= HOP_SIZE
                        }
                    }
                }
            } finally {
                recorder.release()
            }
        }
    }

    /**
     * Stops capturing and releases the [AudioRecord] resource.
     *
     * The [AudioRecord] is stopped here to unblock any pending [AudioRecord.read]
     * call in the capture coroutine. The actual [AudioRecord.release] is deferred
     * to the coroutine's `finally` block so it runs only **after** the capture
     * loop has exited, avoiding a race where `release()` frees native resources
     * while `read()` is still using them.
     */
    fun stop() {
        // Stop recording first to unblock any pending read() call in the
        // capture coroutine.  Without this, cancel() alone cannot interrupt
        // the blocking AudioRecord.read().
        audioRecord?.let { rec ->
            try {
                if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    rec.stop()
                }
            } catch (_: IllegalStateException) {
                // Already stopped — ignore.
            }
        }

        // Cancel the capture coroutine.  The finally block inside start()
        // will call recorder.release() once the coroutine finishes.
        captureJob?.cancel()
        captureJob = null
        audioRecord = null
    }
}

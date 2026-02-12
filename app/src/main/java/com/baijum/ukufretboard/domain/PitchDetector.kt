package com.baijum.ukufretboard.domain

import kotlin.math.abs

/**
 * Result of a successful pitch detection.
 *
 * @property frequencyHz Detected fundamental frequency in Hertz.
 * @property confidence  Confidence in the detection (0.0 = perfect periodicity,
 *   values below [PitchDetector.DEFAULT_THRESHOLD] are considered reliable).
 */
data class PitchResult(
    val frequencyHz: Double,
    val confidence: Double,
)

/**
 * Monophonic pitch detector using the YIN algorithm.
 *
 * The YIN algorithm (de Cheveigné & Kawahara, 2002) estimates the fundamental
 * frequency of a periodic signal. It works well for monophonic instruments
 * with complex harmonic content such as nylon-string ukuleles.
 *
 * Algorithm steps:
 * 1. Compute the difference function (auto-correlation variant).
 * 2. Compute the cumulative mean normalised difference function (CMND).
 * 3. Apply an absolute threshold to find the first dip below [threshold].
 * 4. Reject low-confidence frames (attack transients / noise).
 * 5. Best Local Estimate — refine the lag in the raw difference function.
 * 6. Parabolic interpolation on the raw difference function.
 * 7. Convert the lag to a frequency: f = sampleRate / lag.
 *
 * This is a pure-Kotlin implementation with no external dependencies.
 */
object PitchDetector {

    /**
     * Default CMND threshold.
     *
     * Lower values require a cleaner periodic signal. 0.15 works well for
     * close-mic'd ukulele recordings; increase to 0.20 for noisier environments.
     */
    const val DEFAULT_THRESHOLD = 0.15

    /** Minimum detectable frequency in Hz (~D2, well below Baritone D3). */
    private const val MIN_FREQUENCY = 65.0

    /** Maximum detectable frequency in Hz (above the highest ukulele fret). */
    private const val MAX_FREQUENCY = 1100.0

    /** Minimum RMS amplitude to consider a buffer as containing a signal. */
    private const val SILENCE_THRESHOLD = 0.01f

    /**
     * Maximum CMNDF dip value considered reliable.
     *
     * During clean sustain, `cmnd[bestTau]` is typically 0.01–0.10.
     * During attack transients it jumps to 0.3–0.8. Frames where the
     * best dip exceeds this value are rejected as aperiodic.
     */
    private const val CONFIDENCE_REJECT_THRESHOLD = 0.30

    /**
     * Neighbourhood radius (in samples) for the Best Local Estimate
     * refinement (YIN paper, Step 5). The raw difference function is
     * searched within ±[BEST_LOCAL_RADIUS] of the CMND-selected lag
     * to find the true least-squares minimum.
     */
    private const val BEST_LOCAL_RADIUS = 2

    /**
     * Maximum allowed pitch jump between consecutive frames, in semitones.
     *
     * When a [previousFrequency] is supplied to [detect], the lag search
     * is first constrained to a window of ±[MAX_SEMITONE_JUMP] semitones
     * around the previous pitch. If no valid dip is found (e.g. the user
     * switched strings), the search falls back to the full range.
     */
    private const val MAX_SEMITONE_JUMP = 2.0

    /**
     * Frequency ratio corresponding to [MAX_SEMITONE_JUMP].
     * `2^(2/12) ≈ 1.1225` — the detected frequency may deviate by up to
     * ~12.2 % from the previous frame before a full-range search is used.
     */
    private val CONTINUITY_FACTOR = Math.pow(2.0, MAX_SEMITONE_JUMP / 12.0)

    /**
     * Detects the fundamental frequency of the audio in [samples].
     *
     * @param samples           Normalised audio samples (−1.0 .. 1.0).
     * @param sampleRate        Sample rate in Hz (e.g. 44100).
     * @param threshold         CMND threshold (default [DEFAULT_THRESHOLD]).
     * @param previousFrequency Frequency detected in the previous frame, or
     *   `null` if unavailable (first frame, silence gap, etc.). When provided,
     *   the lag search is first constrained to ±[MAX_SEMITONE_JUMP] semitones
     *   around this frequency to prevent wild pitch jumps. If no valid dip is
     *   found within the constrained range, the search falls back to the full
     *   frequency range.
     * @return A [PitchResult] if a reliable pitch is found, or `null` if the
     *   buffer is silent or no periodic signal is detected.
     */
    fun detect(
        samples: FloatArray,
        sampleRate: Int,
        threshold: Double = DEFAULT_THRESHOLD,
        previousFrequency: Double? = null,
    ): PitchResult? {
        // --- Silence gate ---------------------------------------------------
        if (rms(samples) < SILENCE_THRESHOLD) return null

        val halfLen = samples.size / 2
        if (halfLen < 2) return null

        // Lag limits derived from the frequency range we care about.
        val minLag = (sampleRate / MAX_FREQUENCY).toInt().coerceAtLeast(2)
        val maxLag = (sampleRate / MIN_FREQUENCY).toInt().coerceAtMost(halfLen)
        if (minLag >= maxLag) return null

        // --- Step 1: Difference function (Fast YIN via FFT) -----------------
        // Decompose the SDF as d(tau) = E0 + E(tau) - 2*r(tau) where:
        //   E0     = sum x[j]^2  for j = 0..W-1        (reference energy)
        //   E(tau) = sum x[j]^2  for j = tau..tau+W-1   (shifted energy)
        //   r(tau) = sum x[j]*x[j+tau] for j = 0..W-1   (cross-correlation)
        // E0 is constant; E(tau) uses a prefix-sum; r(tau) uses FFT.

        val diff = computeDifferenceFunctionFFT(samples, halfLen, maxLag)

        // --- Step 2: Cumulative mean normalised difference (CMND) -----------
        val cmnd = FloatArray(maxLag + 1)
        cmnd[0] = 1.0f
        var runningSum = 0.0f
        for (tau in 1..maxLag) {
            runningSum += diff[tau]
            cmnd[tau] = if (runningSum > 0) diff[tau] * tau / runningSum else 1.0f
        }

        // --- Step 3: Absolute threshold (with continuity constraint) ---------
        // If a previous frequency is available, first search a narrow window
        // (±MAX_SEMITONE_JUMP semitones) to prevent wild pitch jumps during
        // sustained notes.  Fall back to the full range if the constrained
        // search finds nothing (e.g. the user switched strings).
        var bestTau = -1

        if (previousFrequency != null && previousFrequency > 0.0) {
            val cMinFreq = previousFrequency / CONTINUITY_FACTOR
            val cMaxFreq = previousFrequency * CONTINUITY_FACTOR
            // Frequency ↔ lag is inverted: higher freq → lower lag.
            val cMinLag = (sampleRate / cMaxFreq).toInt().coerceAtLeast(minLag)
            val cMaxLag = (sampleRate / cMinFreq).toInt().coerceAtMost(maxLag)
            if (cMinLag < cMaxLag) {
                bestTau = findFirstDipBelow(cmnd, cMinLag, cMaxLag, threshold)
            }
        }

        // Full-range fallback.
        if (bestTau < 0) {
            bestTau = findFirstDipBelow(cmnd, minLag, maxLag, threshold)
        }

        if (bestTau < 0) return null

        // --- Step 4: Confidence gate ----------------------------------------
        // Reject frames where the CMND dip is not deep enough.  During
        // attack transients the signal is aperiodic and the dip is shallow,
        // so this prevents "flashing" a wrong note on pluck.
        if (cmnd[bestTau] > CONFIDENCE_REJECT_THRESHOLD) return null

        // --- Step 5: Best Local Estimate (YIN paper, Step 5) ----------------
        // The CMND normalisation can slightly shift the minimum location.
        // Search the vicinity in the raw difference function for the true
        // least-squares minimum before interpolating.
        var localBestTau = bestTau
        var localBestVal = diff[bestTau]
        val searchStart = (bestTau - BEST_LOCAL_RADIUS).coerceAtLeast(minLag)
        val searchEnd = (bestTau + BEST_LOCAL_RADIUS).coerceAtMost(maxLag)
        for (candidate in searchStart..searchEnd) {
            if (diff[candidate] < localBestVal) {
                localBestVal = diff[candidate]
                localBestTau = candidate
            }
        }

        // --- Step 6: Parabolic interpolation on raw SDF ---------------------
        val refinedTau = parabolicInterpolation(diff, localBestTau, maxLag)

        // --- Step 7: Convert lag → frequency --------------------------------
        val frequency = sampleRate.toDouble() / refinedTau
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) return null

        return PitchResult(
            frequencyHz = frequency,
            // Confidence still comes from the CMND — it provides a
            // normalised 0-to-1 periodicity metric for downstream gating.
            confidence = cmnd[bestTau].toDouble(),
        )
    }

    // --- Helpers ------------------------------------------------------------

    /**
     * Root-mean-square amplitude of the buffer.
     *
     * Exposed so that callers (e.g. onset detection in [TunerViewModel][
     * com.baijum.ukufretboard.viewmodel.TunerViewModel]) can compute RMS
     * without duplicating the logic.
     */
    fun rms(samples: FloatArray): Float {
        var sumSq = 0.0f
        for (s in samples) {
            sumSq += s * s
        }
        return kotlin.math.sqrt(sumSq / samples.size)
    }

    /**
     * Refines an integer lag estimate using parabolic (quadratic) interpolation
     * around the minimum of a discrete function (e.g. the raw difference
     * function or the CMND).
     *
     * @return The refined lag as a [Double].
     */
    private fun parabolicInterpolation(
        values: FloatArray,
        tau: Int,
        maxLag: Int,
    ): Double {
        if (tau < 1 || tau >= maxLag) return tau.toDouble()

        val s0 = values[tau - 1].toDouble()
        val s1 = values[tau].toDouble()
        val s2 = values[tau + 1].toDouble()

        val denominator = 2.0 * (2.0 * s1 - s0 - s2)
        return if (abs(denominator) > 1e-12) {
            tau + (s0 - s2) / denominator
        } else {
            tau.toDouble()
        }
    }

    /**
     * Searches the CMND for the first dip below [threshold] in [minLag]..[maxLag].
     *
     * When a dip is found, walks forward while the CMND continues to decrease
     * to locate the true local minimum. Returns the lag of that minimum, or
     * −1 if no dip falls below the threshold.
     */
    private fun findFirstDipBelow(
        cmnd: FloatArray,
        minLag: Int,
        maxLag: Int,
        threshold: Double,
    ): Int {
        for (tau in minLag..maxLag) {
            if (cmnd[tau] < threshold) {
                var best = tau
                while (best + 1 <= maxLag && cmnd[best + 1] < cmnd[best]) {
                    best++
                }
                return best
            }
        }
        return -1
    }

    // --- Fast YIN helpers ---------------------------------------------------

    /**
     * Computes the squared difference function using FFT-based
     * cross-correlation (O(N log N) instead of O(N * maxLag)).
     *
     * Uses the decomposition:
     *   d(tau) = E0 + E(tau) - 2 * r(tau)
     * where E0 and E(tau) are windowed energy terms computed via a
     * prefix-sum of squares, and r(tau) is the cross-correlation of
     * the reference window with the full signal, computed via:
     *   r = IFFT( conj(FFT(ref)) * FFT(sig) )
     *
     * @param samples Audio samples of length N.
     * @param halfLen Window length W = N/2.
     * @param maxLag  Maximum lag to compute.
     * @return A [FloatArray] of size maxLag+1 containing d(0)..d(maxLag).
     */
    private fun computeDifferenceFunctionFFT(
        samples: FloatArray,
        halfLen: Int,
        maxLag: Int,
    ): FloatArray {
        val n = samples.size

        // --- Prefix sum of squares for energy terms -------------------------
        val prefixSq = DoubleArray(n + 1)
        for (i in 0 until n) {
            prefixSq[i + 1] = prefixSq[i] + samples[i].toDouble() * samples[i]
        }
        val energy0 = prefixSq[halfLen] // E_x(0) = sum x[j]^2, j=0..W-1

        // --- FFT-based cross-correlation ------------------------------------
        val fftSize = nextPow2(n * 2) // zero-pad to avoid circular aliasing

        // Reference window: x[0..W-1], zero-padded
        val refReal = FloatArray(fftSize)
        for (i in 0 until halfLen) refReal[i] = samples[i]
        val refImag = FloatArray(fftSize)

        // Full signal: x[0..N-1], zero-padded
        val sigReal = FloatArray(fftSize)
        for (i in 0 until n) sigReal[i] = samples[i]
        val sigImag = FloatArray(fftSize)

        FFTProcessor.fft(refReal, refImag)
        FFTProcessor.fft(sigReal, sigImag)

        // Cross-spectrum: conj(Ref) * Sig
        // Reuse refReal/refImag to store the result (saves an allocation).
        for (i in 0 until fftSize) {
            val ar = refReal[i]; val ai = refImag[i]
            val br = sigReal[i]; val bi = sigImag[i]
            refReal[i] = ar * br + ai * bi   // real part of conj(A)*B
            refImag[i] = ai * br - ar * bi   // imag part of conj(A)*B
        }

        FFTProcessor.ifft(refReal, refImag)
        // refReal[tau] now contains r(tau) = sum_{j=0}^{W-1} x[j]*x[j+tau]

        // --- Assemble SDF ---------------------------------------------------
        val diff = FloatArray(maxLag + 1)
        for (tau in 1..maxLag) {
            val energyTau = prefixSq[tau + halfLen] - prefixSq[tau]
            diff[tau] = (energy0 + energyTau - 2.0 * refReal[tau].toDouble())
                .coerceAtLeast(0.0) // clamp tiny negative values from FP rounding
                .toFloat()
        }
        return diff
    }

    /**
     * Returns the smallest power of two >= [n].
     */
    private fun nextPow2(n: Int): Int {
        if (n <= 1) return 1
        val highBit = Integer.highestOneBit(n)
        return if (highBit == n) n else highBit shl 1
    }
}

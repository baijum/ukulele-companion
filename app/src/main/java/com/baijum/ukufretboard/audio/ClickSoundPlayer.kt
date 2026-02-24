package com.baijum.ukufretboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.baijum.ukufretboard.R

/**
 * Plays metronome click sounds via [SoundPool] for low-latency playback.
 *
 * Two click sounds are loaded: a higher-pitched click for accented beats
 * and a lower-pitched click for normal beats and subdivisions.
 */
class ClickSoundPlayer(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val highClickId: Int = soundPool.load(context, R.raw.click_high, 1)
    private val lowClickId: Int = soundPool.load(context, R.raw.click_low, 1)

    fun playAccent(volume: Float = 1.0f) {
        soundPool.play(highClickId, volume, volume, 1, 0, 1.0f)
    }

    fun playNormal(volume: Float = 0.8f) {
        soundPool.play(lowClickId, volume, volume, 1, 0, 1.0f)
    }

    fun playSubdivision(volume: Float = 0.5f) {
        soundPool.play(lowClickId, volume, volume, 0, 0, 1.3f)
    }

    fun release() {
        soundPool.release()
    }
}

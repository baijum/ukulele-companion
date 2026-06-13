package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baijum.ukufretboard.data.BeatType
import com.baijum.ukufretboard.audio.ClickSoundPlayer
import com.baijum.ukufretboard.audio.MetronomeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the standalone metronome screen.
 *
 * Manages BPM, time signature, subdivision, accent pattern, and playback state.
 * Plays audible click sounds on each beat via [ClickSoundPlayer].
 */
class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = MetronomeEngine()
    private val clickPlayer = ClickSoundPlayer(application)

    private val _bpm = MutableStateFlow(100)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _beatsPerMeasure = MutableStateFlow(4)
    val beatsPerMeasure: StateFlow<Int> = _beatsPerMeasure.asStateFlow()

    private val _subdivision = MutableStateFlow(1)
    val subdivision: StateFlow<Int> = _subdivision.asStateFlow()

    private val _accentPattern = MutableStateFlow(defaultAccentPattern(4))
    val accentPattern: StateFlow<List<BeatType>> = _accentPattern.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBeat = MutableStateFlow(-1)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    private val _currentSubBeat = MutableStateFlow(0)
    val currentSubBeat: StateFlow<Int> = _currentSubBeat.asStateFlow()

    private val _measureCount = MutableStateFlow(0)
    val measureCount: StateFlow<Int> = _measureCount.asStateFlow()

    private val _isCompound = MutableStateFlow(false)
    val isCompound: StateFlow<Boolean> = _isCompound.asStateFlow()

    private val _timeSignatureLabel = MutableStateFlow("4/4")
    val timeSignatureLabel: StateFlow<String> = _timeSignatureLabel.asStateFlow()

    fun setBpm(value: Int) {
        _bpm.value = value.coerceIn(30, 300)
        if (_isPlaying.value) restartEngine()
    }

    fun setTimeSignature(label: String) {
        _timeSignatureLabel.value = label
        when (label) {
            "6/8" -> {
                _isCompound.value = true
                _beatsPerMeasure.value = 2
                _subdivision.value = 3
                _accentPattern.value = defaultAccentPattern(2)
            }
            "12/8" -> {
                _isCompound.value = true
                _beatsPerMeasure.value = 4
                _subdivision.value = 3
                _accentPattern.value = defaultAccentPattern(4)
            }
            else -> {
                _isCompound.value = false
                val beats = label.substringBefore("/").toIntOrNull()?.coerceIn(2, 7) ?: 4
                _beatsPerMeasure.value = beats
                _subdivision.value = 1
                _accentPattern.value = defaultAccentPattern(beats)
            }
        }
        stop()
    }

    fun setBeatsPerMeasure(value: Int) {
        _beatsPerMeasure.value = value.coerceIn(2, 7)
        _accentPattern.value = defaultAccentPattern(value)
        _isCompound.value = false
        _timeSignatureLabel.value = "$value/4"
        stop()
    }

    fun setSubdivision(value: Int) {
        if (_isCompound.value) return
        _subdivision.value = value.coerceIn(1, 4)
        stop()
    }

    fun toggleBeatType(beatIndex: Int) {
        val pattern = _accentPattern.value.toMutableList()
        if (beatIndex in pattern.indices) {
            pattern[beatIndex] = when (pattern[beatIndex]) {
                BeatType.ACCENT -> BeatType.NORMAL
                BeatType.NORMAL -> BeatType.MUTE
                BeatType.MUTE -> BeatType.ACCENT
            }
            _accentPattern.value = pattern
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) stop() else start()
    }

    private fun start() {
        _isPlaying.value = true
        _measureCount.value = 0
        _currentBeat.value = -1
        startEngine()
    }

    fun stop() {
        engine.stop()
        _isPlaying.value = false
        _currentBeat.value = -1
        _currentSubBeat.value = 0
    }

    private fun startEngine() {
        engine.startMetronome(
            scope = viewModelScope,
            bpm = _bpm.value,
            beatsPerMeasure = _beatsPerMeasure.value,
            subdivision = _subdivision.value,
            accentPattern = _accentPattern.value,
            onTick = { beat, subBeat, type ->
                _currentBeat.value = beat
                _currentSubBeat.value = subBeat
                when {
                    type == BeatType.MUTE -> {}
                    subBeat > 0 -> clickPlayer.playSubdivision()
                    type == BeatType.ACCENT -> clickPlayer.playAccent()
                    else -> clickPlayer.playNormal()
                }
            },
            onMeasure = { count ->
                _measureCount.value = count
            },
        )
    }

    private fun restartEngine() {
        engine.stop()
        startEngine()
    }

    override fun onCleared() {
        engine.stop()
        clickPlayer.release()
        super.onCleared()
    }

    companion object {
        private fun defaultAccentPattern(beatsPerMeasure: Int): List<BeatType> =
            List(beatsPerMeasure) { if (it == 0) BeatType.ACCENT else BeatType.NORMAL }
    }
}

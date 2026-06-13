package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.domain.NoteInfo
import com.baijum.ukufretboard.domain.StringMatch

enum class TuningStatus {
    SILENT,
    FLAT,
    SHARP,
    IN_TUNE,
    CLOSE,
}

enum class NeuralRuntimeStatus {
    LOADING,
    ACTIVE,
    FALLBACK,
}

data class TunerUiState(
    val isListening: Boolean = false,
    val detectedNote: String? = null,
    val centsDeviation: Double = 0.0,
    val displayCentsDeviation: Double = 0.0,
    val confidence: Double = 1.0,
    val tuningStatus: TuningStatus = TuningStatus.SILENT,
    val targetString: StringMatch? = null,
    val stringProgress: List<Boolean> = listOf(false, false, false, false),
    val noteInfo: NoteInfo? = null,
    val isNeuralAvailable: Boolean = false,
    val isNeuralActive: Boolean = false,
    val neuralRuntimeStatus: NeuralRuntimeStatus = NeuralRuntimeStatus.LOADING,
    val autoAdvanceTarget: Int = -1,
    val lastSettledNote: String? = null,
    val lastSettledCents: Double = 0.0,
)

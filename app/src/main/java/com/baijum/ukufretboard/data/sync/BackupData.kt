package com.baijum.ukufretboard.data.sync

import kotlinx.serialization.Serializable

/**
 * Top-level container for the backup JSON file.
 *
 * Version 2 expands coverage to include all user data: favorites (with folders),
 * chord sheets, custom progressions, custom strum/fingerpicking patterns,
 * learning progress, and settings.
 *
 * @property version Schema version for forward compatibility.
 * @property exportedAt Timestamp (epoch millis) when this backup was created.
 * @property favorites All saved favorite voicings.
 * @property favoriteFolders All favorite folder definitions.
 * @property chordSheets All saved chord sheets.
 * @property customProgressions All user-created chord progressions.
 * @property customStrumPatterns All user-created strumming patterns.
 * @property customFingerpickingPatterns All user-created fingerpicking patterns.
 * @property learningProgress Learning activity statistics and streaks.
 * @property settings Application settings snapshot.
 */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val favorites: List<BackupFavorite> = emptyList(),
    val favoriteFolders: List<BackupFavoriteFolder> = emptyList(),
    val chordSheets: List<BackupChordSheet> = emptyList(),
    val customProgressions: List<BackupProgression> = emptyList(),
    val customStrumPatterns: List<BackupStrumPattern> = emptyList(),
    val customFingerpickingPatterns: List<BackupFingerpickingPattern> = emptyList(),
    val learningProgress: BackupLearningProgress = BackupLearningProgress(),
    val settings: BackupSettings = BackupSettings(),
    val knownChords: List<String> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

// =============================================================================
// Favorites
// =============================================================================

/**
 * Serializable representation of a favorite voicing for backup.
 */
@Serializable
data class BackupFavorite(
    val rootPitchClass: Int,
    val chordSymbol: String,
    val frets: List<Int>,
    val addedAt: Long,
    val folderId: String? = null,
    val folderIds: List<String> = emptyList(),
)

/**
 * Serializable representation of a favorite folder for backup.
 */
@Serializable
data class BackupFavoriteFolder(
    val id: String,
    val name: String,
    val createdAt: Long,
    val voicingOrder: List<String> = emptyList(),
)

// =============================================================================
// Chord Sheets
// =============================================================================

/**
 * Serializable representation of a chord sheet for backup.
 */
@Serializable
data class BackupChordSheet(
    val id: String,
    val title: String,
    val artist: String = "",
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)

// =============================================================================
// Custom Progressions
// =============================================================================

/**
 * Serializable representation of a custom chord progression for backup.
 */
@Serializable
data class BackupProgression(
    val id: String,
    val name: String,
    val description: String = "",
    val scaleType: String,
    val degrees: List<BackupChordDegree>,
    val createdAt: Long,
)

/**
 * Serializable representation of a chord degree within a progression.
 */
@Serializable
data class BackupChordDegree(
    val interval: Int,
    val quality: String,
    val numeral: String,
)

// =============================================================================
// Custom Strum Patterns
// =============================================================================

/**
 * Serializable representation of a custom strumming pattern for backup.
 */
@Serializable
data class BackupStrumPattern(
    val id: String,
    val name: String,
    val beats: List<BackupStrumBeat>,
    val createdAt: Long,
)

/**
 * Serializable representation of a single strum beat.
 */
@Serializable
data class BackupStrumBeat(
    val direction: String,
    val emphasis: Boolean,
)

// =============================================================================
// Custom Fingerpicking Patterns
// =============================================================================

/**
 * Serializable representation of a custom fingerpicking pattern for backup.
 */
@Serializable
data class BackupFingerpickingPattern(
    val id: String,
    val name: String,
    val steps: List<BackupFingerpickStep>,
    val createdAt: Long,
)

/**
 * Serializable representation of a single fingerpick step.
 */
@Serializable
data class BackupFingerpickStep(
    val finger: String,
    val stringIndex: Int,
    val emphasis: Boolean,
)

// =============================================================================
// Learning Progress
// =============================================================================

/**
 * Serializable snapshot of all learning activity progress.
 *
 * Captures the raw SharedPreferences key-value pairs so that all stats
 * (lessons completed, quiz scores, interval/chord ear training, streaks)
 * are preserved without needing individual fields for every stat type.
 */
@Serializable
data class BackupLearningProgress(
    val entries: Map<String, String> = emptyMap(),
)

// =============================================================================
// Settings
// =============================================================================

/**
 * Serializable representation of app settings for backup.
 */
@Serializable
data class BackupSettings(
    val soundEnabled: Boolean = true,
    val volume: Float = 0.7f,
    val noteDurationMs: Int = 600,
    val strumDelayMs: Int = 50,
    val strumDown: Boolean = true,
    val playOnTap: Boolean = false,
    val themeMode: String = "SYSTEM",
    val showExplorerTips: Boolean = true,
    val tuning: String = "HIGH_G",
    val leftHanded: Boolean = false,
    val lastFret: Int = 12,
    val showNoteNames: Boolean = true,
)

package com.baijum.ukufretboard.data.sync

import android.content.Context
import com.baijum.ukufretboard.data.ChordSheetRepository
import com.baijum.ukufretboard.data.CustomFingerpickingPatternRepository
import com.baijum.ukufretboard.data.CustomProgressionRepository
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.FavoriteFolder
import com.baijum.ukufretboard.data.FavoriteVoicing
import com.baijum.ukufretboard.data.FavoritesRepository
import com.baijum.ukufretboard.data.LearningProgressRepository
import com.baijum.ukufretboard.data.AppSettings
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.DisplaySettings
import com.baijum.ukufretboard.data.FretboardSettings

import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.viewmodel.SettingsViewModel
import kotlinx.serialization.json.Json

/**
 * Manages exporting all user data to a JSON string and importing it back.
 *
 * Collects data from all repositories and the settings view model,
 * serializes to a [BackupData] JSON document, and deserializes/imports back.
 */
class BackupRestoreManager(
    private val context: Context,
    private val settingsViewModel: SettingsViewModel,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Lazy-init repositories (same pattern used throughout the app)
    private val favoritesRepo by lazy { FavoritesRepository(context) }
    private val chordSheetRepo by lazy { ChordSheetRepository(context) }
    private val progressionRepo by lazy { CustomProgressionRepository(context) }
    private val strumPatternRepo by lazy { CustomStrumPatternRepository(context) }
    private val fingerpickingRepo by lazy { CustomFingerpickingPatternRepository(context) }
    private val learningProgressRepo by lazy { LearningProgressRepository(context) }
    /**
     * Collects all user data from all repositories and serializes to JSON.
     *
     * @return The complete backup as a JSON string.
     */
    fun exportBackup(): String {
        val backup = BackupData(
            version = BackupData.CURRENT_VERSION,
            exportedAt = System.currentTimeMillis(),
            favorites = favoritesRepo.getAll().map { fav ->
                BackupFavorite(
                    rootPitchClass = fav.rootPitchClass,
                    chordSymbol = fav.chordSymbol,
                    frets = fav.frets,
                    addedAt = fav.addedAt,
                    folderIds = fav.folderIds,
                )
            },
            favoriteFolders = favoritesRepo.getAllFolders().map { folder ->
                BackupFavoriteFolder(
                    id = folder.id,
                    name = folder.name,
                    createdAt = folder.createdAt,
                    voicingOrder = folder.voicingOrder,
                )
            },
            chordSheets = chordSheetRepo.getAll().map { sheet ->
                BackupChordSheet(
                    id = sheet.id,
                    title = sheet.title,
                    artist = sheet.artist,
                    content = sheet.content,
                    createdAt = sheet.createdAt,
                    updatedAt = sheet.updatedAt,
                )
            },
            customProgressions = progressionRepo.getAll().map { cp ->
                BackupProgression(
                    id = cp.id,
                    name = cp.progression.name,
                    description = cp.progression.description,
                    scaleType = cp.progression.scaleType.name,
                    degrees = cp.progression.degrees.map { d ->
                        BackupChordDegree(
                            interval = d.interval,
                            quality = d.quality,
                            numeral = d.numeral,
                        )
                    },
                    createdAt = cp.createdAt,
                )
            },
            customStrumPatterns = strumPatternRepo.getAll().map { csp ->
                BackupStrumPattern(
                    id = csp.id,
                    name = csp.pattern.name,
                    beats = csp.pattern.beats.map { b ->
                        BackupStrumBeat(
                            direction = b.direction.name,
                            emphasis = b.emphasis,
                        )
                    },
                    createdAt = csp.createdAt,
                )
            },
            customFingerpickingPatterns = fingerpickingRepo.getAll().map { cfp ->
                BackupFingerpickingPattern(
                    id = cfp.id,
                    name = cfp.pattern.name,
                    steps = cfp.pattern.steps.map { s ->
                        BackupFingerpickStep(
                            finger = s.finger.name,
                            stringIndex = s.stringIndex,
                            emphasis = s.emphasis,
                        )
                    },
                    createdAt = cfp.createdAt,
                )
            },
            learningProgress = BackupLearningProgress(
                entries = learningProgressRepo.exportAll(),
            ),
            settings = settingsViewModel.exportSettings().let { s ->
                BackupSettings(
                    soundEnabled = s.sound.enabled,
                    volume = s.sound.volume,
                    noteDurationMs = s.sound.noteDurationMs,
                    strumDelayMs = s.sound.strumDelayMs,
                    strumDown = s.sound.strumDown,
                    playOnTap = s.sound.playOnTap,
                    themeMode = s.display.themeMode.name,
                    tuning = s.tuning.tuning.name,
                    leftHanded = s.fretboard.leftHanded,
                    lastFret = s.fretboard.lastFret,
                    showNoteNames = s.fretboard.showNoteNames,
                )
            },
        )

        return json.encodeToString(BackupData.serializer(), backup)
    }

    /**
     * Deserializes a JSON backup string and imports all data into local storage.
     *
     * Merge strategy:
     * - Favorites, folders, chord sheets, progressions, patterns: union merge
     *   (existing data is preserved, new data is added).
     * - Learning progress: merged (entries are combined).
     * - Settings: replaced with backup values.
     *
     * @param jsonContent The JSON string from a backup file.
     */
    fun importBackup(jsonContent: String) {
        val backup = json.decodeFromString(BackupData.serializer(), jsonContent)

        // --- Favorites ---
        favoritesRepo.importAll(backup.favorites.map { f ->
            // Backward compat: if folderIds is empty but old folderId is present, migrate
            val ids = f.folderIds.ifEmpty {
                listOfNotNull(f.folderId)
            }
            FavoriteVoicing(
                rootPitchClass = f.rootPitchClass,
                chordSymbol = f.chordSymbol,
                frets = f.frets,
                addedAt = f.addedAt,
                folderIds = ids,
            )
        })

        // --- Favorite folders ---
        favoritesRepo.importFolders(backup.favoriteFolders.map { f ->
            FavoriteFolder(
                id = f.id,
                name = f.name,
                createdAt = f.createdAt,
                voicingOrder = f.voicingOrder,
            )
        })

        // --- Chord sheets ---
        chordSheetRepo.importAll(backup.chordSheets.map { s ->
            com.baijum.ukufretboard.data.ChordSheet(
                id = s.id,
                title = s.title,
                artist = s.artist,
                content = s.content,
                createdAt = s.createdAt,
                updatedAt = s.updatedAt,
            )
        })

        // --- Custom progressions ---
        importProgressions(backup.customProgressions)

        // --- Custom strum patterns ---
        importStrumPatterns(backup.customStrumPatterns)

        // --- Custom fingerpicking patterns ---
        importFingerpickingPatterns(backup.customFingerpickingPatterns)

        // --- Learning progress ---
        learningProgressRepo.importAll(backup.learningProgress.entries)

        // --- Settings (replace) ---
        val bs = backup.settings
        settingsViewModel.replaceAll(
            AppSettings(
                sound = SoundSettings(
                    enabled = bs.soundEnabled,
                    volume = bs.volume,
                    noteDurationMs = bs.noteDurationMs,
                    strumDelayMs = bs.strumDelayMs,
                    strumDown = bs.strumDown,
                    playOnTap = bs.playOnTap,
                ),
                display = DisplaySettings(
                    themeMode = try {
                        ThemeMode.valueOf(bs.themeMode)
                    } catch (_: Exception) {
                        ThemeMode.SYSTEM
                    },
                ),
                tuning = TuningSettings(
                    tuning = try {
                        UkuleleTuning.valueOf(bs.tuning)
                    } catch (_: Exception) {
                        UkuleleTuning.HIGH_G
                    },
                ),
                fretboard = FretboardSettings(
                    leftHanded = bs.leftHanded,
                    lastFret = bs.lastFret,
                    showNoteNames = bs.showNoteNames,
                ),
            )
        )
    }

    // --- Helpers for importing domain objects that need reconstruction ---

    private fun importProgressions(items: List<BackupProgression>) {
        val domainItems = items.mapNotNull { bp ->
            try {
                com.baijum.ukufretboard.data.CustomProgression(
                    id = bp.id,
                    progression = com.baijum.ukufretboard.data.Progression(
                        name = bp.name,
                        description = bp.description,
                        degrees = bp.degrees.map { d ->
                            com.baijum.ukufretboard.data.ChordDegree(
                                interval = d.interval,
                                quality = d.quality,
                                numeral = d.numeral,
                            )
                        },
                        scaleType = com.baijum.ukufretboard.data.ScaleType.valueOf(bp.scaleType),
                    ),
                    createdAt = bp.createdAt,
                )
            } catch (_: Exception) {
                null
            }
        }
        progressionRepo.importAll(domainItems)
    }

    private fun importStrumPatterns(items: List<BackupStrumPattern>) {
        val domainItems = items.mapNotNull { bsp ->
            try {
                val beats = bsp.beats.map { b ->
                    com.baijum.ukufretboard.data.StrumBeat(
                        direction = com.baijum.ukufretboard.data.StrumDirection.valueOf(b.direction),
                        emphasis = b.emphasis,
                    )
                }
                val notation = beats.joinToString(" ") { b ->
                    if (b.emphasis) b.direction.symbol.uppercase()
                    else b.direction.symbol
                }
                com.baijum.ukufretboard.data.CustomStrumPattern(
                    id = bsp.id,
                    pattern = com.baijum.ukufretboard.data.StrumPattern(
                        name = bsp.name,
                        description = "Custom pattern",
                        difficulty = com.baijum.ukufretboard.data.Difficulty.BEGINNER,
                        beats = beats,
                        notation = notation,
                        suggestedBpm = 80..120,
                    ),
                    createdAt = bsp.createdAt,
                )
            } catch (_: Exception) {
                null
            }
        }
        strumPatternRepo.importAll(domainItems)
    }

    private fun importFingerpickingPatterns(items: List<BackupFingerpickingPattern>) {
        val domainItems = items.mapNotNull { bfp ->
            try {
                val steps = bfp.steps.map { s ->
                    com.baijum.ukufretboard.data.FingerpickStep(
                        finger = com.baijum.ukufretboard.data.Finger.valueOf(s.finger),
                        stringIndex = s.stringIndex,
                        emphasis = s.emphasis,
                    )
                }
                val notation = steps.joinToString(" ") { s ->
                    val stringName = com.baijum.ukufretboard.data.FingerpickingPatterns.STRING_NAMES[s.stringIndex]
                    "${s.finger.label}($stringName)"
                }
                com.baijum.ukufretboard.data.CustomFingerpickingPattern(
                    id = bfp.id,
                    pattern = com.baijum.ukufretboard.data.FingerpickingPattern(
                        name = bfp.name,
                        description = "Custom pattern",
                        difficulty = com.baijum.ukufretboard.data.Difficulty.BEGINNER,
                        steps = steps,
                        notation = notation,
                        suggestedBpm = 60..100,
                    ),
                    createdAt = bfp.createdAt,
                )
            } catch (_: Exception) {
                null
            }
        }
        fingerpickingRepo.importAll(domainItems)
    }
}

package com.baijum.ukufretboard.data.sync

import android.content.Context
import com.baijum.ukufretboard.data.AchievementRepository
import com.baijum.ukufretboard.data.ChordSheetRepository
import com.baijum.ukufretboard.data.CustomFingerpickingPatternRepository
import com.baijum.ukufretboard.data.CustomProgressionRepository
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.FavoriteFolder
import com.baijum.ukufretboard.data.FavoriteVoicing
import com.baijum.ukufretboard.data.FavoritesRepository
import com.baijum.ukufretboard.data.LearningProgressRepository
import com.baijum.ukufretboard.data.Melody
import com.baijum.ukufretboard.data.MelodyNote
import com.baijum.ukufretboard.data.MelodyRepository
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.PracticeTimerRepository
import com.baijum.ukufretboard.data.Setlist
import com.baijum.ukufretboard.data.SetlistRepository
import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.viewmodel.SettingsViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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
    private val melodyRepo by lazy { MelodyRepository(context) }
    private val setlistRepo by lazy { SetlistRepository(context) }
    private val achievementRepo by lazy { AchievementRepository(context) }
    private val practiceTimerRepo by lazy { PracticeTimerRepository(context) }
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
                    subtitle = sheet.subtitle,
                    artist = sheet.artist,
                    content = sheet.content,
                    key = sheet.key,
                    capo = sheet.capo,
                    strumPatternName = sheet.strumPatternName,
                    labels = sheet.labels,
                    createdAt = sheet.createdAt,
                    updatedAt = sheet.updatedAt,
                    viewCount = sheet.viewCount,
                    lastViewedAt = sheet.lastViewedAt,
                    totalViewTimeMs = sheet.totalViewTimeMs,
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
                    timeSignature = csp.pattern.timeSignature,
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
                    timeSignature = cfp.pattern.timeSignature,
                )
            },
            melodies = melodyRepo.getAll().map { melody ->
                BackupMelody(
                    id = melody.id,
                    name = melody.name,
                    notes = melody.notes.map { n ->
                        BackupMelodyNote(
                            pitchClass = n.pitchClass,
                            octave = n.octave,
                            duration = n.duration.name,
                            stringIndex = n.stringIndex,
                            fret = n.fret,
                        )
                    },
                    bpm = melody.bpm,
                    createdAt = melody.createdAt,
                )
            },
            learningProgress = BackupLearningProgress(
                entries = learningProgressRepo.exportAll(),
            ),
            setlists = setlistRepo.getAll().map { sl ->
                BackupSetlist(
                    id = sl.id,
                    name = sl.name,
                    songIds = sl.songIds,
                    createdAt = sl.createdAt,
                    updatedAt = sl.updatedAt,
                )
            },
            achievements = achievementRepo.exportAll(),
            practiceTimer = practiceTimerRepo.exportAll().let { pt ->
                BackupPracticeTimer(
                    totalMinutes = pt.totalMinutes,
                    totalSessions = pt.totalSessions,
                    longestSession = pt.longestSession,
                    lastSessionTime = pt.lastSessionTime,
                    dailyGoal = pt.dailyGoal,
                    dailyMinutes = pt.dailyMinutes,
                )
            },
            settings = settingsViewModel.exportSettings().let { s ->
                BackupSettings(
                    soundEnabled = s.sound.enabled,
                    volume = s.sound.volume,
                    noteDurationMs = s.sound.noteDurationMs,
                    strumDelayMs = s.sound.strumDelayMs,
                    strumDown = s.sound.strumDown,
                    playOnTap = s.sound.playOnTap,
                    themeMode = s.display.themeMode.name,
                    showExplorerTips = s.display.showExplorerTips,
                    showLearnSection = s.display.showLearnSection,
                    showReferenceSection = s.display.showReferenceSection,
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
     * - Favorites, folders, chord sheets, progressions, patterns, setlists: union merge
     *   (existing data is preserved, new data is added).
     * - Learning progress, achievements: merged (entries are combined).
     * - Practice timer: max-merge (cumulative stats keep the higher value).
     * - Settings: backup fields are merged into the current settings so that
     *   settings not covered by the backup (scale practice, tuner, noise gate,
     *   onboarding, etc.) are preserved at their current values.
     *
     * @param jsonContent The JSON string from a backup file.
     */
    fun importBackup(jsonContent: String) {
        val normalized = normalizeIosFormat(jsonContent)
        val backup = json.decodeFromString(BackupData.serializer(), normalized)

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
                subtitle = s.subtitle,
                artist = s.artist,
                content = s.content,
                key = s.key,
                capo = s.capo,
                strumPatternName = s.strumPatternName,
                labels = s.labels,
                createdAt = s.createdAt,
                updatedAt = s.updatedAt,
                viewCount = s.viewCount,
                lastViewedAt = s.lastViewedAt,
                totalViewTimeMs = s.totalViewTimeMs,
            )
        })

        // --- Custom progressions ---
        importProgressions(backup.customProgressions)

        // --- Custom strum patterns ---
        importStrumPatterns(backup.customStrumPatterns)

        // --- Custom fingerpicking patterns ---
        importFingerpickingPatterns(backup.customFingerpickingPatterns)

        // --- Melodies ---
        importMelodies(backup.melodies)

        // --- Learning progress ---
        learningProgressRepo.importAll(backup.learningProgress.entries)

        // --- Setlists ---
        setlistRepo.importAll(backup.setlists.map { sl ->
            Setlist(
                id = sl.id,
                name = sl.name,
                songIds = sl.songIds,
                createdAt = sl.createdAt,
                updatedAt = sl.updatedAt,
            )
        })

        // --- Achievements ---
        achievementRepo.importAll(backup.achievements)

        // --- Practice timer ---
        practiceTimerRepo.importAll(
            com.baijum.ukufretboard.data.PracticeTimerExport(
                totalMinutes = backup.practiceTimer.totalMinutes,
                totalSessions = backup.practiceTimer.totalSessions,
                longestSession = backup.practiceTimer.longestSession,
                lastSessionTime = backup.practiceTimer.lastSessionTime,
                dailyGoal = backup.practiceTimer.dailyGoal,
                dailyMinutes = backup.practiceTimer.dailyMinutes,
            )
        )

        // --- Settings (merge into current -- backup only covers a subset of
        //     AppSettings, so we must preserve fields that were never exported) ---
        val current = settingsViewModel.exportSettings()
        val bs = backup.settings
        settingsViewModel.replaceAll(
            current.copy(
                sound = current.sound.copy(
                    enabled = bs.soundEnabled,
                    volume = bs.volume,
                    noteDurationMs = bs.noteDurationMs,
                    strumDelayMs = bs.strumDelayMs,
                    strumDown = bs.strumDown,
                    playOnTap = bs.playOnTap,
                ),
                display = current.display.copy(
                    themeMode = try {
                        ThemeMode.valueOf(bs.themeMode)
                    } catch (_: Exception) {
                        current.display.themeMode
                    },
                    showExplorerTips = bs.showExplorerTips,
                    showLearnSection = bs.showLearnSection,
                    showReferenceSection = bs.showReferenceSection,
                ),
                tuning = current.tuning.copy(
                    tuning = try {
                        UkuleleTuning.valueOf(bs.tuning)
                    } catch (_: Exception) {
                        current.tuning.tuning
                    },
                ),
                fretboard = current.fretboard.copy(
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
                        timeSignature = bsp.timeSignature,
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
                        timeSignature = bfp.timeSignature,
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

    private fun importMelodies(items: List<BackupMelody>) {
        val domainItems = items.mapNotNull { bm ->
            try {
                Melody(
                    id = bm.id,
                    name = bm.name,
                    notes = bm.notes.map { n ->
                        MelodyNote(
                            pitchClass = n.pitchClass,
                            octave = n.octave,
                            duration = NoteDuration.valueOf(n.duration),
                            stringIndex = n.stringIndex,
                            fret = n.fret,
                        )
                    },
                    bpm = bm.bpm,
                    createdAt = bm.createdAt,
                )
            } catch (_: Exception) {
                null
            }
        }
        melodyRepo.importAll(domainItems)
    }

    // =========================================================================
    // iOS backup format normalization
    // =========================================================================

    /**
     * Detects old iOS-format backup JSON and normalizes it to the KMP
     * [BackupData] schema so that [importBackup] can decode it unchanged.
     *
     * iOS-format is identified by the presence of `"timestamp"` (epoch seconds)
     * and absence of `"exportedAt"`. If the JSON is already KMP format it is
     * returned as-is.
     */
    private fun normalizeIosFormat(jsonContent: String): String {
        val root = try {
            json.parseToJsonElement(jsonContent).jsonObject
        } catch (_: Exception) {
            return jsonContent
        }

        val hasTimestamp = "timestamp" in root
        val hasExportedAt = "exportedAt" in root
        if (!hasTimestamp || hasExportedAt) return jsonContent

        val out = buildJsonObject {
            put("version", root["version"] ?: JsonPrimitive(3))

            val ts = root["timestamp"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            put("exportedAt", JsonPrimitive((ts * 1000).toLong()))

            copyArray(root, "favorites")?.let { put("favorites", it) }
            (copyArray(root, "favoriteFolders") ?: copyArray(root, "folders"))
                ?.let { put("favoriteFolders", it) }
            copyArray(root, "chord_sheets")?.let { put("chordSheets", it) }
            normalizeIosProgressions(root)?.let { put("customProgressions", it) }
            copyArray(root, "custom_strum_patterns")
                ?.let { put("customStrumPatterns", it) }
            copyArray(root, "custom_fingerpicking_patterns")
                ?.let { put("customFingerpickingPatterns", it) }
            copyArray(root, "setlists")?.let { put("setlists", it) }
            normalizeIosMelodies(root)?.let { put("melodies", it) }
            normalizeIosLearnProgress(root)?.let { (lp, achievements) ->
                put("learningProgress", lp)
                put("achievements", achievements)
            }
            normalizeIosPracticeTimer(root)?.let { put("practiceTimer", it) }
            normalizeIosSettings(root)?.let { put("settings", it) }

            put("knownChords", root["knownChords"] ?: buildJsonArray {})
        }

        return json.encodeToString(JsonObject.serializer(), out)
    }

    private fun copyArray(src: JsonObject, srcKey: String): JsonArray? =
        src[srcKey] as? JsonArray

    private fun normalizeIosProgressions(root: JsonObject): JsonArray? {
        val arr = root["custom_progressions"] as? JsonArray ?: return null
        return buildJsonArray {
            for (elem in arr) {
                val obj = elem.jsonObject
                val intervals = obj["degreeIntervals"]?.jsonArray
                val qualities = obj["degreeQualities"]?.jsonArray
                val numerals = obj["degreeNumerals"]?.jsonArray
                if (intervals != null && qualities != null && numerals != null) {
                    val count = minOf(intervals.size, qualities.size, numerals.size)
                    val degrees = buildJsonArray {
                        for (i in 0 until count) {
                            add(buildJsonObject {
                                put("interval", intervals[i])
                                put("quality", qualities[i])
                                put("numeral", numerals[i])
                            })
                        }
                    }
                    add(buildJsonObject {
                        put("id", obj["id"] ?: JsonPrimitive(""))
                        put("name", obj["name"] ?: JsonPrimitive(""))
                        put("description", obj["description"] ?: JsonPrimitive(""))
                        put("scaleType", obj["scaleType"] ?: JsonPrimitive("MAJOR"))
                        put("degrees", degrees)
                        put("createdAt", obj["createdAt"]
                            ?: JsonPrimitive(System.currentTimeMillis()))
                    })
                }
            }
        }
    }

    private val iosDurationToKMP = mapOf(
        "\uD834\uDD5D" to "WHOLE",     // 𝅝
        "\uD834\uDD5E" to "HALF",      // 𝅗𝅥
        "\u2669" to "QUARTER",          // ♩
        "\u266A" to "EIGHTH",           // ♪
        "\uD834\uDD63" to "SIXTEENTH", // 𝅘𝅥𝅯
    )

    private fun normalizeIosMelodies(root: JsonObject): JsonArray? {
        val arr = root["melodies"] as? JsonArray ?: return null
        return buildJsonArray {
            for (elem in arr) {
                val melody = elem.jsonObject
                val notes = melody["notes"]?.jsonArray ?: continue
                val convertedNotes = buildJsonArray {
                    for (noteElem in notes) {
                        val note = noteElem.jsonObject
                        val dur = note["duration"]?.jsonPrimitive?.contentOrNull ?: "QUARTER"
                        val kmpDur = iosDurationToKMP[dur] ?: dur
                        add(buildJsonObject {
                            note["pitchClass"]?.let { put("pitchClass", it) }
                            put("octave", note["octave"] ?: JsonPrimitive(4))
                            put("duration", JsonPrimitive(kmpDur))
                        })
                    }
                }
                add(buildJsonObject {
                    put("id", melody["id"] ?: JsonPrimitive(""))
                    put("name", melody["name"] ?: JsonPrimitive(""))
                    put("notes", convertedNotes)
                    put("bpm", melody["bpm"] ?: JsonPrimitive(120))
                    put("createdAt", melody["createdAt"]
                        ?: JsonPrimitive(System.currentTimeMillis()))
                })
            }
        }
    }

    private fun normalizeIosLearnProgress(
        root: JsonObject,
    ): Pair<JsonObject, JsonObject>? {
        val lp = root["learn_progress"]?.jsonObject ?: return null
        val entries = buildJsonObject {
            for ((key, value) in lp) {
                if (key == "unlocked_achievements") continue
                put(key, JsonPrimitive(value.jsonPrimitive.contentOrNull ?: ""))
            }
        }
        val achievements = buildJsonObject {
            val unlocked = lp["unlocked_achievements"]?.jsonArray
            if (unlocked != null) {
                for (id in unlocked) {
                    put(id.jsonPrimitive.content, JsonPrimitive(0L))
                }
            }
        }
        return buildJsonObject { put("entries", entries) } to achievements
    }

    private fun normalizeIosPracticeTimer(root: JsonObject): JsonElement? =
        root["practice_timer"]

    private val iosSettingsToKMP = mapOf(
        "sound_enabled" to "soundEnabled",
        "volume" to "volume",
        "note_duration_ms" to "noteDurationMs",
        "strum_delay_ms" to "strumDelayMs",
        "strum_down" to "strumDown",
        "play_on_tap" to "playOnTap",
        "show_tips" to "showExplorerTips",
        "show_learn_tab" to "showLearnSection",
        "show_reference_tab" to "showReferenceSection",
        "left_handed" to "leftHanded",
        "show_note_names" to "showNoteNames",
        "last_fret" to "lastFret",
    )

    private val iosThemeToKMP = mapOf(
        "Light" to "LIGHT",
        "Dark" to "DARK",
        "System" to "SYSTEM",
        "High Contrast" to "HIGH_CONTRAST",
    )

    private val iosTuningToKMP = mapOf(
        "High-G (Standard)" to "HIGH_G",
        "Low-G" to "LOW_G",
        "Baritone (DGBE)" to "BARITONE",
        "D-Tuning (ADF#B)" to "D_TUNING",
        "Slack Key (GCEG)" to "SLACK_KEY",
        "Open A (AC#EA)" to "OPEN_A",
        "Low A (GCEa)" to "LOW_A",
        "Half-Step Down" to "HALF_STEP_DOWN",
    )

    private fun normalizeIosSettings(root: JsonObject): JsonObject? {
        val settings = root["settings"]?.jsonObject ?: return null
        return buildJsonObject {
            for ((key, value) in settings) {
                when (key) {
                    "theme_mode" -> {
                        val label = value.jsonPrimitive.contentOrNull ?: "System"
                        put("themeMode", JsonPrimitive(iosThemeToKMP[label] ?: "SYSTEM"))
                    }
                    "selected_tuning" -> {
                        val label = value.jsonPrimitive.contentOrNull ?: "High-G (Standard)"
                        put("tuning", JsonPrimitive(iosTuningToKMP[label] ?: "HIGH_G"))
                    }
                    else -> {
                        val kmpKey = iosSettingsToKMP[key]
                        if (kmpKey != null) {
                            put(kmpKey, value)
                        }
                    }
                }
            }
        }
    }
}

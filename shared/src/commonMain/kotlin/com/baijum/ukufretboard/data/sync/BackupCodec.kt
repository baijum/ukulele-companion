package com.baijum.ukufretboard.data.sync

import com.baijum.ukufretboard.platform.currentTimeMillis
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
 * Shared codec for encoding/decoding [BackupData] JSON.
 *
 * All backup serialization goes through this object so that the format stays
 * byte-compatible between Android and iOS. The [decode] function normalizes
 * legacy iOS-format backups and fixes timestamp units before deserialization.
 *
 * Timestamp convention: all timestamps are **epoch milliseconds**.
 */
object BackupCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** Serializes [data] to a pretty-printed JSON string. */
    fun encode(data: BackupData): String =
        json.encodeToString(BackupData.serializer(), data)

    /**
     * Deserializes a backup JSON string, normalizing legacy formats first.
     *
     * Handles:
     * - Old iOS format (identified by `"timestamp"` field in epoch seconds)
     * - KMP-format backups with fractional or seconds-unit timestamps
     */
    fun decode(jsonString: String): BackupData {
        val afterLegacy = normalizeIosFormat(jsonString)
        val normalized = normalizeKmpTimestamps(afterLegacy)
        return json.decodeFromString(BackupData.serializer(), normalized)
    }

    // =========================================================================
    // Legacy iOS backup format normalization
    // =========================================================================

    /**
     * Detects old iOS-format backup JSON and normalizes it to the KMP
     * [BackupData] schema.
     *
     * iOS-format is identified by the presence of `"timestamp"` (epoch seconds)
     * and absence of `"exportedAt"`. If the JSON is already KMP format it is
     * returned as-is.
     */
    internal fun normalizeIosFormat(jsonContent: String): String {
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

            normalizeIosFavorites(root, "favorites", "addedAt")
                ?.let { put("favorites", it) }
            normalizeIosFavorites(root, "folders", "createdAt")
                ?.let { arr ->
                    put("favoriteFolders", arr)
                } ?: normalizeIosFavorites(root, "favoriteFolders", "createdAt")
                    ?.let { put("favoriteFolders", it) }
            copyArray(root, "chord_sheets")?.let { put("chordSheets", it) }
            normalizeIosProgressions(root)?.let { put("customProgressions", it) }
            copyArray(root, "custom_strum_patterns")
                ?.let { put("customStrumPatterns", it) }
            normalizeIosFingerpicking(root)
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

    // =========================================================================
    // KMP-format timestamp normalization
    // =========================================================================

    /**
     * Normalizes timestamp fields in KMP-format backups that may contain
     * fractional Doubles (from iOS JSONSerialization) or epoch-seconds values.
     *
     * Truncates fractional parts so kotlinx.serialization can decode to Long,
     * and converts seconds to milliseconds using a magnitude heuristic:
     * values below 100 billion are assumed to be seconds.
     */
    internal fun normalizeKmpTimestamps(jsonContent: String): String {
        val root = try {
            json.parseToJsonElement(jsonContent).jsonObject
        } catch (_: Exception) {
            return jsonContent
        }

        if ("exportedAt" !in root) return jsonContent

        val arrayFields = mapOf(
            "setlists" to arrayOf("createdAt", "updatedAt"),
            "customStrumPatterns" to arrayOf("createdAt"),
            "customFingerpickingPatterns" to arrayOf("createdAt"),
        )
        val objectFields = mapOf(
            "practiceTimer" to arrayOf("lastSessionTime"),
        )

        var modified = false
        val out = buildJsonObject {
            for ((key, value) in root) {
                val arrTsFields = arrayFields[key]
                val objTsFields = objectFields[key]
                when {
                    arrTsFields != null -> {
                        val arr = value as? JsonArray
                        if (arr != null) {
                            put(key, normalizeTimestampArray(arr, *arrTsFields))
                            modified = true
                        } else {
                            put(key, value)
                        }
                    }
                    objTsFields != null -> {
                        val obj = value as? JsonObject
                        if (obj != null) {
                            put(key, normalizeTimestampObject(obj, *objTsFields))
                            modified = true
                        } else {
                            put(key, value)
                        }
                    }
                    else -> put(key, value)
                }
            }
        }

        return if (modified) json.encodeToString(JsonObject.serializer(), out)
        else jsonContent
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private fun normalizeTimestampArray(
        arr: JsonArray,
        vararg tsFields: String,
    ): JsonArray = buildJsonArray {
        for (elem in arr) {
            val obj = elem as? JsonObject
            if (obj == null) { add(elem); continue }
            add(normalizeTimestampObject(obj, *tsFields))
        }
    }

    private fun normalizeTimestampObject(
        obj: JsonObject,
        vararg tsFields: String,
    ): JsonObject = buildJsonObject {
        for ((k, v) in obj) {
            if (k in tsFields) {
                val ts = (v as? JsonPrimitive)?.doubleOrNull
                if (ts != null) {
                    val millis = if (ts < 100_000_000_000) {
                        (ts * 1000).toLong()
                    } else {
                        ts.toLong()
                    }
                    put(k, JsonPrimitive(millis))
                } else {
                    put(k, v)
                }
            } else {
                put(k, v)
            }
        }
    }

    private fun copyArray(src: JsonObject, srcKey: String): JsonArray? =
        src[srcKey] as? JsonArray

    private fun normalizeIosFavorites(
        root: JsonObject,
        key: String,
        tsField: String,
    ): JsonArray? {
        val arr = root[key] as? JsonArray ?: return null
        return buildJsonArray {
            for (elem in arr) {
                val obj = elem.jsonObject
                val ts = obj[tsField]?.jsonPrimitive?.doubleOrNull
                if (ts != null && ts < 1_000_000_000_000) {
                    val updated = buildJsonObject {
                        for ((k, v) in obj) {
                            if (k == tsField) {
                                put(k, JsonPrimitive((ts * 1000).toLong()))
                            } else {
                                put(k, v)
                            }
                        }
                    }
                    add(updated)
                } else {
                    add(elem)
                }
            }
        }
    }

    private val iosFingerToKMP = mapOf(
        "T" to "THUMB", "I" to "INDEX", "M" to "MIDDLE", "R" to "RING", "P" to "PINKY",
    )

    private fun normalizeIosFingerpicking(root: JsonObject): JsonArray? {
        val arr = root["custom_fingerpicking_patterns"] as? JsonArray ?: return null
        return buildJsonArray {
            for (elem in arr) {
                val pattern = elem.jsonObject
                val steps = pattern["steps"]?.jsonArray
                if (steps != null) {
                    val converted = buildJsonArray {
                        for (stepElem in steps) {
                            val step = stepElem.jsonObject
                            val finger = step["finger"]?.jsonPrimitive?.contentOrNull
                            val kmpFinger = finger?.let { iosFingerToKMP[it] ?: it } ?: finger
                            add(buildJsonObject {
                                for ((k, v) in step) {
                                    if (k == "finger" && kmpFinger != null) {
                                        put(k, JsonPrimitive(kmpFinger))
                                    } else {
                                        put(k, v)
                                    }
                                }
                            })
                        }
                    }
                    add(buildJsonObject {
                        for ((k, v) in pattern) {
                            if (k == "steps") put(k, converted)
                            else put(k, v)
                        }
                    })
                } else {
                    add(elem)
                }
            }
        }
    }

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
                            ?: JsonPrimitive(currentTimeMillis()))
                    })
                }
            }
        }
    }

    private val iosDurationToKMP = mapOf(
        "\uD834\uDD5D" to "WHOLE",
        "\uD834\uDD5E" to "HALF",
        "\u2669" to "QUARTER",
        "\u266A" to "EIGHTH",
        "\uD834\uDD63" to "SIXTEENTH",
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
                        ?: JsonPrimitive(currentTimeMillis()))
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

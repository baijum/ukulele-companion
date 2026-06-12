package com.baijum.ukufretboard.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupCodecTest {

    @Test
    fun roundTripEncodeDecodePreservesAllFields() {
        val original = BackupData(
            exportedAt = 1717430400000L,
            favorites = listOf(
                BackupFavorite(
                    rootPitchClass = 0,
                    chordSymbol = "",
                    frets = listOf(0, 0, 0, 3),
                    addedAt = 1717430400000L,
                    folderIds = listOf("f1"),
                ),
            ),
            favoriteFolders = listOf(
                BackupFavoriteFolder(
                    id = "f1",
                    name = "Jazz",
                    createdAt = 1717430400000L,
                    voicingOrder = listOf("v1", "v2"),
                ),
            ),
            chordSheets = listOf(
                BackupChordSheet(
                    id = "cs-1",
                    title = "Test Song",
                    content = "[C]Hello [G]World",
                    createdAt = 1717430400000L,
                    updatedAt = 1717430400000L,
                    viewCount = 5,
                    lastViewedAt = 1717430500000L,
                    totalViewTimeMs = 60000L,
                ),
            ),
            customProgressions = listOf(
                BackupProgression(
                    id = "cp-1",
                    name = "I-IV-V",
                    description = "Basic",
                    scaleType = "MAJOR",
                    degrees = listOf(
                        BackupChordDegree(0, "major", "I"),
                        BackupChordDegree(5, "major", "IV"),
                        BackupChordDegree(7, "major", "V"),
                    ),
                    createdAt = 1717430400000L,
                ),
            ),
            customStrumPatterns = listOf(
                BackupStrumPattern(
                    id = "sp-1",
                    name = "Basic Down",
                    beats = listOf(
                        BackupStrumBeat("DOWN", false),
                        BackupStrumBeat("DOWN", true),
                    ),
                    createdAt = 1717430400000L,
                    timeSignature = "4/4",
                ),
            ),
            customFingerpickingPatterns = listOf(
                BackupFingerpickingPattern(
                    id = "fp-1",
                    name = "Travis",
                    steps = listOf(
                        BackupFingerpickStep("THUMB", 3, false),
                        BackupFingerpickStep("INDEX", 1, false),
                    ),
                    createdAt = 1717430400000L,
                ),
            ),
            setlists = listOf(
                BackupSetlist(
                    id = "sl-1",
                    name = "Gig Set",
                    songIds = listOf("cs-1"),
                    createdAt = 1717430400000L,
                    updatedAt = 1717430400000L,
                ),
            ),
            melodies = listOf(
                BackupMelody(
                    id = "m-1",
                    name = "Test Melody",
                    notes = listOf(
                        BackupMelodyNote(pitchClass = 0, octave = 4, duration = "QUARTER"),
                        BackupMelodyNote(pitchClass = null, octave = 4, duration = "HALF"),
                    ),
                    bpm = 120,
                    createdAt = 1717430400000L,
                ),
            ),
            learningProgress = BackupLearningProgress(
                entries = mapOf("streak_days" to "5", "quiz_total_ALL" to "42"),
            ),
            settings = BackupSettings(
                soundEnabled = true,
                volume = 0.8f,
                themeMode = "DARK",
                tuning = "LOW_G",
                leftHanded = true,
                lastFret = 15,
            ),
            achievements = mapOf("first_chord" to 1717430400000L),
            practiceTimer = BackupPracticeTimer(
                totalMinutes = 60,
                totalSessions = 10,
                longestSession = 20,
                lastSessionTime = 1717430400000L,
                dailyGoal = 15,
                dailyMinutes = mapOf("2026-06-01" to 25),
            ),
            knownChords = listOf("C", "Am"),
        )

        val encoded = BackupCodec.encode(original)
        val decoded = BackupCodec.decode(encoded)

        assertEquals(original.version, decoded.version)
        assertEquals(original.exportedAt, decoded.exportedAt)
        assertEquals(original.favorites, decoded.favorites)
        assertEquals(original.favoriteFolders, decoded.favoriteFolders)
        assertEquals(original.chordSheets, decoded.chordSheets)
        assertEquals(original.customProgressions, decoded.customProgressions)
        assertEquals(original.customStrumPatterns, decoded.customStrumPatterns)
        assertEquals(original.customFingerpickingPatterns, decoded.customFingerpickingPatterns)
        assertEquals(original.setlists, decoded.setlists)
        assertEquals(original.melodies, decoded.melodies)
        assertEquals(original.learningProgress, decoded.learningProgress)
        assertEquals(original.settings, decoded.settings)
        assertEquals(original.achievements, decoded.achievements)
        assertEquals(original.practiceTimer, decoded.practiceTimer)
        assertEquals(original.knownChords, decoded.knownChords)
    }

    @Test
    fun decodeVersion3FixtureFromAndroid() {
        val fixture = """
        {
            "version": 3,
            "exportedAt": 1717430400000,
            "favorites": [
                {
                    "rootPitchClass": 7,
                    "chordSymbol": "7",
                    "frets": [0, 2, 1, 2],
                    "addedAt": 1717430400000,
                    "folderIds": ["folder-jazz"]
                }
            ],
            "favoriteFolders": [
                {
                    "id": "folder-jazz",
                    "name": "Jazz Voicings",
                    "createdAt": 1717430400000,
                    "voicingOrder": []
                }
            ],
            "chordSheets": [],
            "customProgressions": [],
            "customStrumPatterns": [],
            "customFingerpickingPatterns": [],
            "setlists": [],
            "melodies": [],
            "learningProgress": { "entries": {} },
            "settings": {
                "soundEnabled": true,
                "volume": 0.7,
                "noteDurationMs": 600,
                "strumDelayMs": 50,
                "strumDown": true,
                "playOnTap": false,
                "themeMode": "SYSTEM",
                "showExplorerTips": true,
                "showLearnSection": true,
                "showReferenceSection": true,
                "tuning": "HIGH_G",
                "leftHanded": false,
                "lastFret": 12,
                "showNoteNames": true
            },
            "achievements": {},
            "practiceTimer": {
                "totalMinutes": 0,
                "totalSessions": 0,
                "longestSession": 0,
                "lastSessionTime": 0,
                "dailyGoal": 15,
                "dailyMinutes": {}
            },
            "knownChords": ["C", "Am", "F"]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(fixture)
        assertEquals(3, decoded.version)
        assertEquals(1717430400000L, decoded.exportedAt)
        assertEquals(1, decoded.favorites.size)
        assertEquals(7, decoded.favorites[0].rootPitchClass)
        assertEquals("7", decoded.favorites[0].chordSymbol)
        assertEquals(listOf(0, 2, 1, 2), decoded.favorites[0].frets)
        assertEquals(listOf("folder-jazz"), decoded.favorites[0].folderIds)
        assertEquals("Jazz Voicings", decoded.favoriteFolders[0].name)
        assertEquals(listOf("C", "Am", "F"), decoded.knownChords)
    }

    @Test
    fun decodeLegacyIosFormatWithTimestamp() {
        val legacyIos = """
        {
            "version": 3,
            "timestamp": 1717430400.0,
            "favorites": [
                {
                    "rootPitchClass": 0,
                    "chordSymbol": "",
                    "frets": [0, 0, 0, 3],
                    "addedAt": 1717430400.0,
                    "folderIds": []
                }
            ],
            "folders": [
                {
                    "id": "f1",
                    "name": "Pop",
                    "createdAt": 1717430400.0
                }
            ],
            "settings": {
                "theme_mode": "Dark",
                "selected_tuning": "Low-G",
                "sound_enabled": true,
                "volume": 0.5,
                "left_handed": true
            }
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacyIos)
        assertEquals(3, decoded.version)
        assertEquals(1717430400000L, decoded.exportedAt)
        assertEquals(1, decoded.favorites.size)
        assertEquals(1717430400000L, decoded.favorites[0].addedAt)
        assertEquals(1, decoded.favoriteFolders.size)
        assertEquals("Pop", decoded.favoriteFolders[0].name)
        assertEquals(1717430400000L, decoded.favoriteFolders[0].createdAt)
        assertEquals("DARK", decoded.settings.themeMode)
        assertEquals("LOW_G", decoded.settings.tuning)
        assertTrue(decoded.settings.leftHanded)
    }

    @Test
    fun timestampsInSecondsAreConvertedToMilliseconds() {
        val jsonWithSeconds = """
        {
            "version": 3,
            "exportedAt": 1717430400000,
            "favorites": [],
            "setlists": [
                {
                    "id": "sl-1",
                    "name": "Test",
                    "songIds": [],
                    "createdAt": 1717430400.0,
                    "updatedAt": 1717430400.0
                }
            ],
            "practiceTimer": {
                "totalMinutes": 0,
                "totalSessions": 0,
                "longestSession": 0,
                "lastSessionTime": 1717430400.0,
                "dailyGoal": 15,
                "dailyMinutes": {}
            }
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(jsonWithSeconds)
        assertEquals(1717430400000L, decoded.setlists[0].createdAt)
        assertEquals(1717430400000L, decoded.setlists[0].updatedAt)
        assertEquals(1717430400000L, decoded.practiceTimer.lastSessionTime)
    }

    @Test
    fun timestampsAlreadyInMillisArePreserved() {
        val jsonWithMillis = """
        {
            "version": 3,
            "exportedAt": 1717430400000,
            "favorites": [],
            "setlists": [
                {
                    "id": "sl-1",
                    "name": "Test",
                    "songIds": [],
                    "createdAt": 1717430400000,
                    "updatedAt": 1717430400000
                }
            ],
            "practiceTimer": {
                "totalMinutes": 0,
                "totalSessions": 0,
                "longestSession": 0,
                "lastSessionTime": 1717430400000,
                "dailyGoal": 15,
                "dailyMinutes": {}
            }
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(jsonWithMillis)
        assertEquals(1717430400000L, decoded.setlists[0].createdAt)
        assertEquals(1717430400000L, decoded.practiceTimer.lastSessionTime)
    }

    @Test
    fun decodeLegacyIosProgressions() {
        val legacyIos = """
        {
            "version": 3,
            "timestamp": 1717430400.0,
            "custom_progressions": [
                {
                    "id": "cp-1",
                    "name": "I-IV-V",
                    "description": "Basic",
                    "scaleType": "MAJOR",
                    "degreeIntervals": [0, 5, 7],
                    "degreeQualities": ["major", "major", "major"],
                    "degreeNumerals": ["I", "IV", "V"],
                    "createdAt": 1717430400000
                }
            ]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacyIos)
        assertEquals(1, decoded.customProgressions.size)
        assertEquals("I-IV-V", decoded.customProgressions[0].name)
        assertEquals(3, decoded.customProgressions[0].degrees.size)
        assertEquals(0, decoded.customProgressions[0].degrees[0].interval)
        assertEquals("major", decoded.customProgressions[0].degrees[0].quality)
        assertEquals("I", decoded.customProgressions[0].degrees[0].numeral)
    }

    @Test
    fun decodeLegacyIosFingerpickingWithShortFingerNames() {
        val legacyIos = """
        {
            "version": 3,
            "timestamp": 1717430400.0,
            "custom_fingerpicking_patterns": [
                {
                    "id": "fp-1",
                    "name": "Travis",
                    "steps": [
                        {"finger": "T", "stringIndex": 3, "emphasis": false},
                        {"finger": "I", "stringIndex": 1, "emphasis": false},
                        {"finger": "M", "stringIndex": 0, "emphasis": true}
                    ],
                    "createdAt": 1717430400000,
                    "timeSignature": "4/4"
                }
            ]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacyIos)
        assertEquals(1, decoded.customFingerpickingPatterns.size)
        assertEquals("THUMB", decoded.customFingerpickingPatterns[0].steps[0].finger)
        assertEquals("INDEX", decoded.customFingerpickingPatterns[0].steps[1].finger)
        assertEquals("MIDDLE", decoded.customFingerpickingPatterns[0].steps[2].finger)
    }

    @Test
    fun decodeLegacyIosMelodiesWithUnicodeNoteDurations() {
        val legacyIos = """
        {
            "version": 3,
            "timestamp": 1717430400.0,
            "melodies": [
                {
                    "id": "m-1",
                    "name": "Scale",
                    "notes": [
                        {"pitchClass": 0, "octave": 4, "duration": "\u2669"},
                        {"pitchClass": 4, "octave": 4, "duration": "\u266A"},
                        {"octave": 4, "duration": "\uD834\uDD5E"}
                    ],
                    "bpm": 100,
                    "createdAt": 1717430400000
                }
            ]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacyIos)
        assertEquals(1, decoded.melodies.size)
        assertEquals("QUARTER", decoded.melodies[0].notes[0].duration)
        assertEquals("EIGHTH", decoded.melodies[0].notes[1].duration)
        assertEquals("HALF", decoded.melodies[0].notes[2].duration)
        assertEquals(null, decoded.melodies[0].notes[2].pitchClass)
    }

    @Test
    fun decodeLegacyIosLearnProgressWithAchievements() {
        val legacyIos = """
        {
            "version": 3,
            "timestamp": 1717430400.0,
            "learn_progress": {
                "streak_days": "5",
                "quiz_total_ALL": "42",
                "unlocked_achievements": ["first_chord", "ten_songs"]
            }
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacyIos)
        assertEquals("5", decoded.learningProgress.entries["streak_days"])
        assertEquals("42", decoded.learningProgress.entries["quiz_total_ALL"])
        assertEquals(2, decoded.achievements.size)
        assertTrue("first_chord" in decoded.achievements)
        assertTrue("ten_songs" in decoded.achievements)
    }

    @Test
    fun normalizeTimestampsForChordSheets() {
        val jsonWithFractional = """
        {
            "version": 3,
            "exportedAt": 1717430400000,
            "chordSheets": [
                {
                    "id": "cs-1",
                    "title": "Song",
                    "content": "",
                    "createdAt": 1718180000123.456,
                    "updatedAt": 1718180000123.789,
                    "lastViewedAt": 1718180000123.0,
                    "viewCount": 0,
                    "totalViewTimeMs": 0
                }
            ]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(jsonWithFractional)
        assertEquals(1718180000123L, decoded.chordSheets[0].createdAt)
        assertEquals(1718180000123L, decoded.chordSheets[0].updatedAt)
        assertEquals(1718180000123L, decoded.chordSheets[0].lastViewedAt)
    }

    @Test
    fun normalizeTimestampsForMelodiesAndProgressions() {
        val jsonWithSeconds = """
        {
            "version": 3,
            "exportedAt": 1717430400000,
            "melodies": [
                {
                    "id": "m-1",
                    "name": "Scale",
                    "notes": [],
                    "bpm": 120,
                    "createdAt": 1718180000
                }
            ],
            "customProgressions": [
                {
                    "id": "cp-1",
                    "name": "I-IV-V",
                    "description": "",
                    "scaleType": "MAJOR",
                    "degrees": [],
                    "createdAt": 1718180000
                }
            ]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(jsonWithSeconds)
        assertEquals(1718180000000L, decoded.melodies[0].createdAt)
        assertEquals(1718180000000L, decoded.customProgressions[0].createdAt)
    }

    @Test
    fun normalizeTimestampsForFavoritesAndFolders() {
        val jsonWithFractional = """
        {
            "version": 3,
            "exportedAt": 1717430400000,
            "favorites": [
                {
                    "rootPitchClass": 0,
                    "chordSymbol": "",
                    "frets": [0, 0, 0, 3],
                    "addedAt": 1718180000123.456
                }
            ],
            "favoriteFolders": [
                {
                    "id": "f1",
                    "name": "Jazz",
                    "createdAt": 1718180000
                }
            ]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(jsonWithFractional)
        assertEquals(1718180000123L, decoded.favorites[0].addedAt)
        assertEquals(1718180000000L, decoded.favoriteFolders[0].createdAt)
    }

    @Test
    fun encodeProducesPrettyPrintedJson() {
        val data = BackupData(exportedAt = 12345L)
        val encoded = BackupCodec.encode(data)
        assertTrue(encoded.contains("\n"))
        assertTrue(encoded.contains("  "))
    }

    @Test
    fun decodeIgnoresUnknownKeys() {
        val jsonWithExtras = """
        {
            "version": 3,
            "exportedAt": 12345,
            "unknownField": "should be ignored",
            "anotherUnknown": [1, 2, 3]
        }
        """.trimIndent()

        val decoded = BackupCodec.decode(jsonWithExtras)
        assertEquals(3, decoded.version)
        assertEquals(12345L, decoded.exportedAt)
    }

    @Test
    fun emptyBackupRoundTrips() {
        val data = BackupData(exportedAt = 1000L)
        val encoded = BackupCodec.encode(data)
        val decoded = BackupCodec.decode(encoded)
        assertEquals(data.version, decoded.version)
        assertEquals(data.exportedAt, decoded.exportedAt)
        assertTrue(decoded.favorites.isEmpty())
        assertTrue(decoded.chordSheets.isEmpty())
        assertTrue(decoded.knownChords.isEmpty())
    }
}

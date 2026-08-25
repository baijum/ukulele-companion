package com.baijum.ukufretboard.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.ScaleType
import com.baijum.ukufretboard.domain.ChordEarTrainer
import com.baijum.ukufretboard.domain.IntervalTrainer
import com.baijum.ukufretboard.domain.QuizGenerator

/** Reusable [Saver] for [List]<[String]> (e.g. sheet labels) so it survives activity recreation. */
val StringListSaver: Saver<List<String>, Any> =
    listSaver(
        save = { ArrayList(it) },
        restore = { it.toList() },
    )

/** Reusable [Saver] for any non-nullable enum backed by its [Enum.name]. */
inline fun <reified E : Enum<E>> enumSaver(): Saver<E, String> =
    Saver(
        save = { it.name },
        restore = { enumValueOf<E>(it) },
    )

/** Reusable [Saver] for a nullable enum backed by its [Enum.name] (empty string for null). */
inline fun <reified E : Enum<E>> nullableEnumSaver(): Saver<E?, String> =
    Saver(
        save = { it?.name ?: "" },
        restore = { name -> if (name.isEmpty()) null else enumValueOf<E>(name) },
    )

/** Saver for a nullable [IntervalTrainer.IntervalQuestion]. */
val IntervalQuestionSaver: Saver<IntervalTrainer.IntervalQuestion?, Any> =
    listSaver(
        save = { q ->
            if (q == null) {
                emptyList()
            } else {
                listOf(
                    q.note1PitchClass,
                    q.note2PitchClass,
                    q.note1Name,
                    q.note2Name,
                    q.intervalSemitones,
                    q.correctAnswer,
                    ArrayList(q.options),
                    q.correctIndex,
                    q.note1Fret,
                    q.note2Fret,
                    q.note1Octave,
                    q.note2Octave,
                    q.direction.name,
                )
            }
        },
        restore = { list ->
            if (list.isEmpty()) {
                null
            } else {
                IntervalTrainer.IntervalQuestion(
                    note1PitchClass = list[0] as Int,
                    note2PitchClass = list[1] as Int,
                    note1Name = list[2] as String,
                    note2Name = list[3] as String,
                    intervalSemitones = list[4] as Int,
                    correctAnswer = list[5] as String,
                    options = @Suppress("UNCHECKED_CAST") (list[6] as List<String>),
                    correctIndex = list[7] as Int,
                    note1Fret = list[8] as Int,
                    note2Fret = list[9] as Int,
                    note1Octave = list[10] as Int,
                    note2Octave = list[11] as Int,
                    direction = IntervalTrainer.IntervalDirection.valueOf(list[12] as String),
                )
            }
        },
    )

/** Saver for a nullable [QuizGenerator.QuizQuestion]. */
val QuizQuestionSaver: Saver<QuizGenerator.QuizQuestion?, Any> =
    listSaver(
        save = { q ->
            if (q == null) {
                emptyList()
            } else {
                listOf(
                    q.category.name,
                    q.question,
                    ArrayList(q.options),
                    q.correctIndex,
                    q.explanation,
                )
            }
        },
        restore = { list ->
            if (list.isEmpty()) {
                null
            } else {
                QuizGenerator.QuizQuestion(
                    category = QuizGenerator.QuizCategory.valueOf(list[0] as String),
                    question = list[1] as String,
                    options = @Suppress("UNCHECKED_CAST") (list[2] as List<String>),
                    correctIndex = list[3] as Int,
                    explanation = list[4] as String,
                )
            }
        },
    )

/** Saver for a nullable [ChordEarTrainer.ChordEarQuestion]. */
val ChordEarQuestionSaver: Saver<ChordEarTrainer.ChordEarQuestion?, Any> =
    listSaver(
        save = { q ->
            if (q == null) {
                emptyList()
            } else {
                listOf(
                    q.rootPitchClass,
                    q.rootName,
                    q.quality,
                    q.symbol,
                    q.notes.joinToString("|") { "${it.first},${it.second}" },
                    q.correctAnswer,
                    ArrayList(q.options),
                    q.correctIndex,
                )
            }
        },
        restore = { list ->
            if (list.isEmpty()) {
                null
            } else {
                ChordEarTrainer.ChordEarQuestion(
                    rootPitchClass = list[0] as Int,
                    rootName = list[1] as String,
                    quality = list[2] as String,
                    symbol = list[3] as String,
                    notes =
                        (list[4] as String).split("|").map { s ->
                            val parts = s.split(",")
                            parts[0].toInt() to parts[1].toInt()
                        },
                    correctAnswer = list[5] as String,
                    options = @Suppress("UNCHECKED_CAST") (list[6] as List<String>),
                    correctIndex = list[7] as Int,
                )
            }
        },
    )

/** Saver for a nullable [Progression] using string-serialized degrees. */
val ProgressionSaver: Saver<Progression?, Any> =
    listSaver(
        save = { p ->
            if (p == null) {
                emptyList()
            } else {
                listOf(
                    p.name,
                    p.description,
                    p.scaleType.name,
                    p.degrees.joinToString("|") { "${it.interval};${it.quality};${it.numeral}" },
                )
            }
        },
        restore = { list ->
            if (list.isEmpty()) {
                null
            } else {
                val degreesStr = list[3]
                Progression(
                    name = list[0],
                    description = list[1],
                    scaleType = ScaleType.valueOf(list[2]),
                    degrees =
                        degreesStr.split("|").map { s ->
                            val parts = s.split(";", limit = 3)
                            ChordDegree(
                                interval = parts[0].toInt(),
                                quality = parts[1],
                                numeral = parts[2],
                            )
                        },
                )
            }
        },
    )

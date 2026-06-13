package com.baijum.ukufretboard.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordCategory
import com.baijum.ukufretboard.data.DailyChallengeGenerator
import com.baijum.ukufretboard.data.Difficulty
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.ScaleCategory
import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.domain.AchievementCategory
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.QuizGenerator
import com.baijum.ukufretboard.domain.PracticeRoutineGenerator
import com.baijum.ukufretboard.data.FretPosition
import com.baijum.ukufretboard.data.PlayDirection
import com.baijum.ukufretboard.data.PracticeMode

@Composable
fun ThemeMode.localizedLabel(): String = stringResource(
    when (this) {
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.HIGH_CONTRAST -> R.string.theme_high_contrast
    },
)

@Composable
fun UkuleleTuning.localizedLabel(): String = stringResource(
    when (this) {
        UkuleleTuning.HIGH_G -> R.string.tuning_high_g
        UkuleleTuning.LOW_G -> R.string.tuning_low_g
        UkuleleTuning.BARITONE -> R.string.tuning_baritone
        UkuleleTuning.D_TUNING -> R.string.tuning_d_tuning
        UkuleleTuning.SLACK_KEY -> R.string.tuning_slack_key
        UkuleleTuning.OPEN_A -> R.string.tuning_open_a
        UkuleleTuning.LOW_A -> R.string.tuning_low_a
        UkuleleTuning.HALF_STEP_DOWN -> R.string.tuning_half_step_down
    },
)

@Composable
fun ChordCategory.localizedLabel(): String = stringResource(
    when (this) {
        ChordCategory.TRIAD -> R.string.chord_category_triad
        ChordCategory.SEVENTH -> R.string.chord_category_seventh
        ChordCategory.SUSPENDED -> R.string.chord_category_suspended
        ChordCategory.EXTENDED -> R.string.chord_category_extended
    },
)

@Composable
fun Difficulty.localizedLabel(): String = stringResource(
    when (this) {
        Difficulty.BEGINNER -> R.string.difficulty_beginner
        Difficulty.INTERMEDIATE -> R.string.difficulty_intermediate
        Difficulty.ADVANCED -> R.string.difficulty_advanced
    },
)

@Composable
fun ScaleCategory.localizedLabel(): String = stringResource(
    when (this) {
        ScaleCategory.BASIC -> R.string.scale_category_basic
        ScaleCategory.BLUES -> R.string.scale_category_blues
        ScaleCategory.MODES -> R.string.scale_category_modes
        ScaleCategory.JAZZ -> R.string.scale_category_jazz
        ScaleCategory.HINDUSTANI -> R.string.scale_category_hindustani
        ScaleCategory.CARNATIC -> R.string.scale_category_carnatic
        ScaleCategory.WORLD -> R.string.scale_category_world
        ScaleCategory.SYMMETRIC -> R.string.scale_category_symmetric
    },
)

@Composable
fun NoteDuration.localizedLabel(): String = stringResource(
    when (this) {
        NoteDuration.WHOLE -> R.string.duration_whole
        NoteDuration.HALF -> R.string.duration_half
        NoteDuration.QUARTER -> R.string.duration_quarter
        NoteDuration.EIGHTH -> R.string.duration_eighth
        NoteDuration.SIXTEENTH -> R.string.duration_sixteenth
    },
)

@Composable
fun ChordInfo.Inversion.localizedLabel(): String = stringResource(
    when (this) {
        ChordInfo.Inversion.ROOT -> R.string.inversion_root
        ChordInfo.Inversion.FIRST -> R.string.inversion_first
        ChordInfo.Inversion.SECOND -> R.string.inversion_second
        ChordInfo.Inversion.THIRD -> R.string.inversion_third
    },
)

@Composable
fun PracticeMode.localizedLabel(): String = stringResource(
    when (this) {
        PracticeMode.PLAY_ALONG -> R.string.practice_mode_play_along
        PracticeMode.QUIZ -> R.string.practice_mode_quiz
        PracticeMode.EAR_TRAINING -> R.string.practice_mode_ear_training
    },
)

@Composable
fun PlayDirection.localizedLabel(): String = stringResource(
    when (this) {
        PlayDirection.ASCENDING -> R.string.play_direction_ascending
        PlayDirection.DESCENDING -> R.string.play_direction_descending
        PlayDirection.BOTH -> R.string.play_direction_both
    },
)

@Composable
fun FretPosition.localizedLabel(): String = stringResource(
    when (this) {
        FretPosition.ALL -> R.string.fret_position_all
        FretPosition.OPEN -> R.string.fret_position_open
        FretPosition.MID -> R.string.fret_position_mid
        FretPosition.HIGH -> R.string.fret_position_high
    },
)

@Composable
fun PracticeRoutineGenerator.StepType.localizedLabel(): String = stringResource(
    when (this) {
        PracticeRoutineGenerator.StepType.WARM_UP -> R.string.step_type_warm_up
        PracticeRoutineGenerator.StepType.CHORD_DRILL -> R.string.step_type_chord_drill
        PracticeRoutineGenerator.StepType.SCALE_PRACTICE -> R.string.step_type_scale_practice
        PracticeRoutineGenerator.StepType.EAR_TRAINING -> R.string.step_type_ear_training
        PracticeRoutineGenerator.StepType.THEORY_QUIZ -> R.string.step_type_theory_quiz
        PracticeRoutineGenerator.StepType.SONG_PRACTICE -> R.string.step_type_song_practice
        PracticeRoutineGenerator.StepType.FREE_PLAY -> R.string.step_type_free_play
    },
)

@Composable
fun PracticeRoutineGenerator.FocusArea.localizedLabel(): String = stringResource(
    when (this) {
        PracticeRoutineGenerator.FocusArea.CHORDS -> R.string.focus_area_chords
        PracticeRoutineGenerator.FocusArea.SCALES -> R.string.focus_area_scales
        PracticeRoutineGenerator.FocusArea.EAR -> R.string.focus_area_ear
        PracticeRoutineGenerator.FocusArea.THEORY -> R.string.focus_area_theory
        PracticeRoutineGenerator.FocusArea.SONGS -> R.string.focus_area_songs
    },
)

@Composable
fun PracticeRoutineGenerator.SkillLevel.localizedLabel(): String = stringResource(
    when (this) {
        PracticeRoutineGenerator.SkillLevel.BEGINNER -> R.string.skill_level_beginner
        PracticeRoutineGenerator.SkillLevel.INTERMEDIATE -> R.string.skill_level_intermediate
        PracticeRoutineGenerator.SkillLevel.ADVANCED -> R.string.skill_level_advanced
    },
)

@Composable
fun DailyChallengeGenerator.ChallengeType.localizedLabel(): String = stringResource(
    when (this) {
        DailyChallengeGenerator.ChallengeType.LEARN_CHORD -> R.string.challenge_type_learn_chord
        DailyChallengeGenerator.ChallengeType.THEORY_QUIZ -> R.string.challenge_type_theory_quiz
        DailyChallengeGenerator.ChallengeType.PRACTICE_SONG -> R.string.challenge_type_practice_song
        DailyChallengeGenerator.ChallengeType.SCALE_PRACTICE -> R.string.challenge_type_scale_practice
        DailyChallengeGenerator.ChallengeType.CHORD_TRANSITION -> R.string.challenge_type_chord_transition
        DailyChallengeGenerator.ChallengeType.EAR_TRAINING -> R.string.challenge_type_ear_training
    },
)

@Composable
fun AchievementCategory.localizedLabel(): String = stringResource(
    when (this) {
        AchievementCategory.PRACTICE -> R.string.achievement_category_practice
        AchievementCategory.LEARNING -> R.string.achievement_category_learning
        AchievementCategory.EAR -> R.string.achievement_category_ear
        AchievementCategory.CHORDS -> R.string.achievement_category_chords
        AchievementCategory.SONGS -> R.string.achievement_category_songs
    },
)

@Composable
fun QuizGenerator.QuizCategory.localizedLabel(): String = stringResource(
    when (this) {
        QuizGenerator.QuizCategory.INTERVALS -> R.string.quiz_category_intervals
        QuizGenerator.QuizCategory.CHORDS -> R.string.quiz_category_chords
        QuizGenerator.QuizCategory.KEYS -> R.string.quiz_category_keys
        QuizGenerator.QuizCategory.SCALES -> R.string.quiz_category_scales
        QuizGenerator.QuizCategory.PROGRESSIONS -> R.string.quiz_category_progressions
    },
)

/**
 * Returns the localized title for an achievement, looked up by its unique ID.
 */
@Composable
fun achievementTitle(id: String): String = stringResource(
    when (id) {
        "streak_3" -> R.string.achievement_streak_3_title
        "streak_7" -> R.string.achievement_streak_7_title
        "streak_30" -> R.string.achievement_streak_30_title
        "first_lesson" -> R.string.achievement_first_lesson_title
        "half_lessons" -> R.string.achievement_half_lessons_title
        "all_lessons" -> R.string.achievement_all_lessons_title
        "quiz_10" -> R.string.achievement_quiz_10_title
        "quiz_50" -> R.string.achievement_quiz_50_title
        "quiz_100" -> R.string.achievement_quiz_100_title
        "perfect_streak_5" -> R.string.achievement_perfect_streak_5_title
        "perfect_streak_10" -> R.string.achievement_perfect_streak_10_title
        "ear_first" -> R.string.achievement_ear_first_title
        "ear_50" -> R.string.achievement_ear_50_title
        "ear_accuracy_80" -> R.string.achievement_ear_accuracy_80_title
        "first_song" -> R.string.achievement_first_song_title
        "songs_5" -> R.string.achievement_songs_5_title
        "songs_10" -> R.string.achievement_songs_10_title
        "fav_5" -> R.string.achievement_fav_5_title
        "fav_25" -> R.string.achievement_fav_25_title
        "scale_first" -> R.string.achievement_scale_first_title
        "scale_25" -> R.string.achievement_scale_25_title
        else -> R.string.app_name
    },
)

/**
 * Returns the localized description for an achievement, looked up by its unique ID.
 */
@Composable
fun achievementDescription(id: String): String = stringResource(
    when (id) {
        "streak_3" -> R.string.achievement_streak_3_desc
        "streak_7" -> R.string.achievement_streak_7_desc
        "streak_30" -> R.string.achievement_streak_30_desc
        "first_lesson" -> R.string.achievement_first_lesson_desc
        "half_lessons" -> R.string.achievement_half_lessons_desc
        "all_lessons" -> R.string.achievement_all_lessons_desc
        "quiz_10" -> R.string.achievement_quiz_10_desc
        "quiz_50" -> R.string.achievement_quiz_50_desc
        "quiz_100" -> R.string.achievement_quiz_100_desc
        "perfect_streak_5" -> R.string.achievement_perfect_streak_5_desc
        "perfect_streak_10" -> R.string.achievement_perfect_streak_10_desc
        "ear_first" -> R.string.achievement_ear_first_desc
        "ear_50" -> R.string.achievement_ear_50_desc
        "ear_accuracy_80" -> R.string.achievement_ear_accuracy_80_desc
        "first_song" -> R.string.achievement_first_song_desc
        "songs_5" -> R.string.achievement_songs_5_desc
        "songs_10" -> R.string.achievement_songs_10_desc
        "fav_5" -> R.string.achievement_fav_5_desc
        "fav_25" -> R.string.achievement_fav_25_desc
        "scale_first" -> R.string.achievement_scale_first_desc
        "scale_25" -> R.string.achievement_scale_25_desc
        else -> R.string.app_name
    },
)

/**
 * Returns the Material icon for an achievement, looked up by its unique ID.
 */
fun achievementIcon(id: String): ImageVector = when (id) {
    "streak_3", "streak_7", "streak_30" -> Icons.Filled.Star
    "first_lesson", "half_lessons", "all_lessons" -> Icons.Filled.Info
    "quiz_10", "quiz_50", "quiz_100" -> Icons.Filled.Search
    "perfect_streak_5", "perfect_streak_10" -> Icons.Filled.Star
    "ear_first", "ear_50", "ear_accuracy_80" -> Icons.Filled.PlayArrow
    "first_song", "songs_5", "songs_10" -> Icons.Filled.Create
    "fav_5", "fav_25" -> Icons.Filled.Favorite
    "scale_first", "scale_25" -> Icons.Filled.Home
    else -> Icons.Filled.Star
}

/**
 * Returns the localized theory module name.
 */
@Composable
fun theoryModuleName(module: String): String = stringResource(
    when (module) {
        "Notes & Pitch" -> R.string.theory_module_notes_pitch
        "Intervals" -> R.string.theory_module_intervals
        "Scales" -> R.string.theory_module_scales
        "Chords" -> R.string.theory_module_chords
        "Keys & Signatures" -> R.string.theory_module_keys_signatures
        "Progressions & Harmony" -> R.string.theory_module_progressions_harmony
        "Rhythm" -> R.string.theory_module_rhythm
        else -> R.string.app_name
    },
)

/**
 * Returns the localized theory lesson title.
 */
@Composable
fun theoryLessonTitle(id: String): String = stringResource(
    when (id) {
        "notes_12" -> R.string.theory_title_notes_12
        "notes_sharp_flat" -> R.string.theory_title_notes_sharp_flat
        "intervals_intro" -> R.string.theory_title_intervals_intro
        "intervals_table" -> R.string.theory_title_intervals_table
        "scales_major" -> R.string.theory_title_scales_major
        "scales_minor" -> R.string.theory_title_scales_minor
        "scales_modes" -> R.string.theory_title_scales_modes
        "chords_triads" -> R.string.theory_title_chords_triads
        "chords_seventh" -> R.string.theory_title_chords_seventh
        "chords_inversions" -> R.string.theory_title_chords_inversions
        "keys_intro" -> R.string.theory_title_keys_intro
        "keys_circle" -> R.string.theory_title_keys_circle
        "harmony_diatonic" -> R.string.theory_title_harmony_diatonic
        "harmony_functions" -> R.string.theory_title_harmony_functions
        "rhythm_time_sig" -> R.string.theory_title_rhythm_time_sig
        "rhythm_note_values" -> R.string.theory_title_rhythm_note_values
        "rhythm_constant_motion" -> R.string.theory_title_rhythm_constant_motion
        "rhythm_straight_swing" -> R.string.theory_title_rhythm_straight_swing
        "rhythm_percussive" -> R.string.theory_title_rhythm_percussive
        "rhythm_world" -> R.string.theory_title_rhythm_world
        else -> R.string.app_name
    },
)

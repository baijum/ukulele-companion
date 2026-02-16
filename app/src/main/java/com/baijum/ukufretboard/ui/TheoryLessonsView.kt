package com.baijum.ukufretboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import androidx.compose.material.icons.filled.Check
import com.baijum.ukufretboard.data.TheoryLesson
import com.baijum.ukufretboard.data.TheoryLessons
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel

/**
 * Theory Lessons Hub with structured curriculum.
 *
 * Shows lessons organized by module. Tapping a lesson opens its content
 * with an explanation, key points, and a mini quiz.
 * Tracks lesson completion and quiz results via [LearningProgressViewModel].
 */
@Composable
fun TheoryLessonsView(
    progressViewModel: LearningProgressViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var selectedLesson by remember { mutableStateOf<TheoryLesson?>(null) }

    if (selectedLesson != null) {
        LessonDetailView(
            lesson = selectedLesson!!,
            onBack = {
                progressViewModel?.markLessonCompleted(selectedLesson!!.id)
                selectedLesson = null
            },
            onQuizPassed = { lessonId ->
                progressViewModel?.markLessonQuizPassed(lessonId)
            },
        )
    } else {
        LessonListView(
            onLessonSelected = { selectedLesson = it },
            isLessonCompleted = { progressViewModel?.isLessonCompleted(it) ?: false },
            isQuizPassed = { progressViewModel?.isLessonQuizPassed(it) ?: false },
            completedCount = progressViewModel?.state?.collectAsState()?.value?.completedLessons ?: 0,
            totalCount = TheoryLessons.ALL.size,
            modifier = modifier,
        )
    }
}

@Composable
private fun LessonListView(
    onLessonSelected: (TheoryLesson) -> Unit,
    isLessonCompleted: (String) -> Boolean,
    isQuizPassed: (String) -> Boolean,
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val lessonsByModule = remember { TheoryLessons.byModule() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.theory_lessons_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.theory_lessons_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Overall progress bar
        if (completedCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.theory_lessons_progress),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$completedCount / " + stringResource(R.string.theory_lessons_count, totalCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = {
                    if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TheoryLessons.MODULES.forEachIndexed { moduleIndex, moduleName ->
            val lessons = lessonsByModule[moduleName] ?: emptyList()
            val moduleCompleted = lessons.count { isLessonCompleted(it.id) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${moduleIndex + 1}. $moduleName",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (moduleCompleted > 0) {
                            Text(
                                text = "$moduleCompleted/${lessons.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    lessons.forEachIndexed { index, lesson ->
                        val completed = isLessonCompleted(lesson.id)
                        val quizPassed = isQuizPassed(lesson.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLessonSelected(lesson) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (completed) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.cd_completed),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (quizPassed) {
                                Text(
                                    text = "\u2713",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            Text(
                                text = "\u203A",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index < lessons.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonDetailView(
    lesson: TheoryLesson,
    onBack: () -> Unit,
    onQuizPassed: (String) -> Unit = {},
) {
    var selectedQuizAnswer by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Back button and title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Column {
                Text(
                    text = lesson.module,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        lesson.content.split("\n\n").forEach { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // Key points
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.theory_lessons_key_points),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(8.dp))
                lesson.keyPoints.forEach { point ->
                    Text(
                        text = "\u2022 $point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mini quiz
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.theory_lessons_quick_check),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lesson.quizQuestion,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(12.dp))

                lesson.quizOptions.forEachIndexed { index, option ->
                    val isSelected = selectedQuizAnswer == index
                    val isCorrect = index == lesson.quizCorrectIndex
                    val hasAnswered = selectedQuizAnswer != null

                    val containerColor = when {
                        hasAnswered && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                        hasAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    OutlinedButton(
                        onClick = {
                            if (selectedQuizAnswer == null) {
                                selectedQuizAnswer = index
                                if (index == lesson.quizCorrectIndex) {
                                    onQuizPassed(lesson.id)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor),
                        enabled = selectedQuizAnswer == null,
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                        )
                    }
                }

                if (selectedQuizAnswer != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val isCorrect = selectedQuizAnswer == lesson.quizCorrectIndex
                    Text(
                        text = if (isCorrect) stringResource(R.string.label_correct_answer) else stringResource(R.string.theory_lessons_not_quite),
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = lesson.quizExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next lesson button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.theory_lessons_back))
        }
    }
}

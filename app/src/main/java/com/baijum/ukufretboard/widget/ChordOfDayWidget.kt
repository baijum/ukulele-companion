package com.baijum.ukufretboard.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.baijum.ukufretboard.MainActivity
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordOfDay
import com.baijum.ukufretboard.data.Notes

/**
 * Glance app widget that displays the chord of the day.
 *
 * Shows the chord name, finger positions, and notes.
 * Tapping the widget opens the app.
 */
class ChordOfDayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val chord = ChordOfDay.chordForDate()
        val rootName = Notes.pitchClassToName(chord.rootPitchClass)
        val chordName = "$rootName${chord.symbol}"
        val fingerPositions = chord.frets.joinToString("  ")

        // Compute note names for each string
        val openStrings = listOf(7, 0, 4, 9) // G, C, E, A
        val noteNames = chord.frets.mapIndexed { i, fret ->
            val pc = (openStrings[i] + fret) % 12
            Notes.pitchClassToName(pc)
        }.joinToString("  ")

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getString(R.string.widget_title),
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                        ),
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = chordName,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        ),
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = fingerPositions,
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                        ),
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = noteNames,
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Receiver for the Chord of the Day widget.
 */
class ChordOfDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChordOfDayWidget()
}

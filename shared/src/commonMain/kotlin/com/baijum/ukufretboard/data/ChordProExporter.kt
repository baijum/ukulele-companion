package com.baijum.ukufretboard.data

/**
 * Exports a [ChordSheet] to ChordPro format.
 *
 * Generates a valid ChordPro file with `{title}` and `{artist}` directives
 * followed by the song content. Chord markers `[Am]` are already in ChordPro
 * syntax and pass through unchanged.
 */
object ChordProExporter {

    /**
     * Converts a [ChordSheet] to a ChordPro-formatted string.
     *
     * @param sheet The chord sheet to export.
     * @return The ChordPro-formatted text.
     */
    private val SECTION_LABEL = Regex("""\[(Chorus|Verse|Bridge)]""")

    fun export(sheet: ChordSheet): String = buildString {
        appendLine("{title: ${sheet.title}}")
        if (sheet.artist.isNotEmpty()) {
            appendLine("{artist: ${sheet.artist}}")
        }
        if (sheet.key.isNotEmpty()) {
            appendLine("{key: ${sheet.key}}")
        }
        if (sheet.capo > 0) {
            appendLine("{capo: ${sheet.capo}}")
        }
        appendLine()

        sheet.content.lines().forEach { line ->
            val match = SECTION_LABEL.matchEntire(line.trim())
            if (match != null) {
                val section = match.groupValues[1].lowercase()
                appendLine("{start_of_$section}")
            } else {
                appendLine(line)
            }
        }
    }

    /**
     * Returns a suggested filename for the exported ChordPro file.
     *
     * @param sheet The chord sheet being exported.
     * @return A sanitised filename with `.cho` extension.
     */
    fun suggestedFilename(sheet: ChordSheet): String {
        val base = sheet.title.ifEmpty { "song" }
            .replace(Regex("[^a-zA-Z0-9 _-]"), "")
            .replace(" ", "_")
            .take(50)
        return "$base.cho"
    }
}

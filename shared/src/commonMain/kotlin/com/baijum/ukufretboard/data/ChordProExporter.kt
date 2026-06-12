package com.baijum.ukufretboard.data

/**
 * Exports a [ChordSheet] to ChordPro format.
 *
 * Generates a valid ChordPro file with metadata directives followed by the song
 * content. Chord markers `[Am]` are already in ChordPro syntax and pass through
 * unchanged. Section labels like `[Chorus]` are converted back to
 * `{start_of_chorus}` / `{end_of_chorus}` directive pairs.
 */
object ChordProExporter {

    /**
     * Matches internal section labels: `[Chorus]`, `[Verse 2]`, `[Bridge]`, etc.
     * Captures the section type and optional custom label.
     */
    private val SECTION_LABEL =
        Regex("""\[(Chorus|Verse|Bridge|Intro|Outro|Interlude|Tab)(?:\s+(.*))?\]""")

    /** Maps a captured section type to its ChordPro directive base name. */
    private fun sectionDirective(type: String): String = type.lowercase()

    /** Strips `{`, `}`, and newlines from a metadata value to prevent directive injection. */
    private fun sanitizeMeta(value: String): String =
        value.replace("{", "").replace("}", "").replace("\n", " ").replace("\r", "")

    /**
     * Converts a [ChordSheet] to a ChordPro-formatted string.
     *
     * @param sheet The chord sheet to export.
     * @return The ChordPro-formatted text.
     */
    fun export(sheet: ChordSheet): String = buildString {
        appendLine("{title: ${sanitizeMeta(sheet.title)}}")
        if (sheet.subtitle.isNotEmpty()) {
            appendLine("{subtitle: ${sanitizeMeta(sheet.subtitle)}}")
        }
        if (sheet.artist.isNotEmpty()) {
            appendLine("{artist: ${sanitizeMeta(sheet.artist)}}")
        }
        if (sheet.key.isNotEmpty()) {
            appendLine("{key: ${sanitizeMeta(sheet.key)}}")
        }
        if (sheet.capo > 0) {
            appendLine("{capo: ${sheet.capo}}")
        }
        appendLine()

        var openSection: String? = null
        val lines = sheet.content.lines()

        lines.forEach { line ->
            val match = SECTION_LABEL.matchEntire(line.trim())
            if (match != null) {
                if (openSection != null) {
                    appendLine("{end_of_$openSection}")
                }
                val type = sectionDirective(match.groupValues[1])
                val customSuffix = match.groupValues[2].trim()
                openSection = type
                if (customSuffix.isNotEmpty()) {
                    appendLine("{start_of_$type: ${match.groupValues[1]} $customSuffix}")
                } else {
                    appendLine("{start_of_$type}")
                }
            } else {
                val trimmed = line.trimStart()
                if (trimmed.startsWith("#")) {
                    appendLine("{comment: $line}")
                } else if (trimmed.startsWith("{")) {
                    appendLine("\\$line")
                } else {
                    appendLine(line)
                }
            }
        }

        if (openSection != null) {
            appendLine("{end_of_$openSection}")
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

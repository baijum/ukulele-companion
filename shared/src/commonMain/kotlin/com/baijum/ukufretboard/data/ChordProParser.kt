package com.baijum.ukufretboard.data

/**
 * Parses ChordPro-formatted text into a [ChordSheet].
 *
 * ChordPro format uses directives like `{title: ...}` and `{artist: ...}`
 * for metadata, and inline `[ChordName]` markers for chords — the same
 * format already used by our [ChordSheet.content].
 *
 * Supported directives:
 * - `{title: ...}` / `{t: ...}` — song title
 * - `{subtitle: ...}` / `{st: ...}` / `{artist: ...}` — artist/subtitle
 * - `{comment: ...}` / `{c: ...}` — inserted as text lines
 * - `{start_of_chorus}` / `{soc}` — inserts "[Chorus]"
 * - `{end_of_chorus}` / `{eoc}` — inserts blank line
 * - `{start_of_verse}` / `{sov}` — inserts "[Verse]"
 * - `{end_of_verse}` / `{eov}` — inserts blank line
 * - `{start_of_bridge}` / `{sob}` — inserts "[Bridge]"
 * - `{end_of_bridge}` / `{eob}` — inserts blank line
 * - `{key: ...}` — noted but not yet used
 * - `{capo: ...}` — noted but not yet used
 *
 * Unsupported directives are silently skipped.
 */
object ChordProParser {

    /** Regex matching ChordPro directives: `{name}` or `{name: value}`. */
    private val DIRECTIVE = Regex("""\{(\w+)(?::\s*(.*?))?\}""")

    /**
     * Parses a ChordPro-formatted string into a [ChordSheet].
     *
     * Chord markers `[Am]` pass through unchanged since they use the
     * same syntax as our internal format.
     *
     * @param input The raw ChordPro text content.
     * @param defaultTitle Fallback title if none is found in directives.
     * @return A new [ChordSheet] with parsed title, artist, and content.
     */
    fun parse(input: String, defaultTitle: String = "Imported Song"): ChordSheet {
        var title = ""
        var artist = ""
        var key = ""
        var capo = 0
        val contentLines = mutableListOf<String>()

        input.lines().forEach { line ->
            val trimmed = line.trim()

            // Check for directive lines
            val match = DIRECTIVE.find(trimmed)
            if (match != null && trimmed.startsWith("{")) {
                val directive = match.groupValues[1].lowercase()
                val value = match.groupValues[2].trim()

                when (directive) {
                    "title", "t" -> title = value
                    "subtitle", "st", "artist" -> artist = value
                    "comment", "c", "ci" -> {
                        if (value.isNotEmpty()) contentLines.add(value)
                    }
                    "start_of_chorus", "soc" -> contentLines.add("[Chorus]")
                    "end_of_chorus", "eoc" -> contentLines.add("")
                    "start_of_verse", "sov" -> contentLines.add("[Verse]")
                    "end_of_verse", "eov" -> contentLines.add("")
                    "start_of_bridge", "sob" -> contentLines.add("[Bridge]")
                    "end_of_bridge", "eob" -> contentLines.add("")
                    "key" -> key = value
                    "capo" -> capo = value.toIntOrNull() ?: 0
                    else -> {
                        // Unknown directive — skip
                    }
                }
            } else {
                // Regular content line — chord markers [Am] pass through
                contentLines.add(line)
            }
        }

        return ChordSheet(
            title = title.ifEmpty { defaultTitle },
            artist = artist,
            content = contentLines.joinToString("\n").trim(),
            key = key,
            capo = capo,
        )
    }

    /**
     * Checks whether a filename has a recognised ChordPro extension.
     *
     * @param filename The file name or path to check.
     * @return `true` if the extension suggests ChordPro format.
     */
    fun isChordProFile(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".cho") ||
            lower.endsWith(".chordpro") ||
            lower.endsWith(".chopro") ||
            lower.endsWith(".crd") ||
            lower.endsWith(".pro")
    }
}

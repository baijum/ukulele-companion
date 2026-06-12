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
 * - `{subtitle: ...}` / `{st: ...}` — subtitle (separate from artist)
 * - `{artist: ...}` — artist name
 * - `{comment: ...}` / `{c: ...}` / `{ci: ...}` — inserted as text lines
 * - `{start_of_chorus}` / `{soc}` — inserts "[Chorus]" (or custom label)
 * - `{end_of_chorus}` / `{eoc}` — inserts blank line
 * - `{start_of_verse}` / `{sov}` — inserts "[Verse]" (or custom label)
 * - `{end_of_verse}` / `{eov}` — inserts blank line
 * - `{start_of_bridge}` / `{sob}` — inserts "[Bridge]" (or custom label)
 * - `{end_of_bridge}` / `{eob}` — inserts blank line
 * - `{start_of_intro}` — inserts "[Intro]" (or custom label)
 * - `{end_of_intro}` — inserts blank line
 * - `{start_of_outro}` — inserts "[Outro]" (or custom label)
 * - `{end_of_outro}` — inserts blank line
 * - `{start_of_interlude}` — inserts "[Interlude]" (or custom label)
 * - `{end_of_interlude}` — inserts blank line
 * - `{start_of_tab}` / `{sot}` — inserts "[Tab]", content preserved as-is
 * - `{end_of_tab}` / `{eot}` — inserts blank line
 * - `{chorus}` — inserts "[Chorus]" (recall directive)
 * - `{key: ...}` — stored in [ChordSheet.key]
 * - `{capo: ...}` — stored in [ChordSheet.capo]
 * - `{tempo: ...}` — inserted as content comment line
 * - `{time: ...}` — inserted as content comment line
 * - `{define: ...}` — intentionally skipped (custom voicings not supported)
 *
 * Lines starting with `#` are treated as file-level comments and skipped.
 * Unsupported directives are silently skipped.
 */
object ChordProParser {

    /** Regex matching ChordPro directives: `{name}` or `{name: value}`. */
    private val DIRECTIVE = Regex("""\{(\w+)(?::\s*(.*?))?\}""")

    /** Maps section start directives to their default label names. */
    private val SECTION_STARTS = mapOf(
        "start_of_chorus" to "Chorus", "soc" to "Chorus",
        "start_of_verse" to "Verse", "sov" to "Verse",
        "start_of_bridge" to "Bridge", "sob" to "Bridge",
        "start_of_intro" to "Intro",
        "start_of_outro" to "Outro",
        "start_of_interlude" to "Interlude",
        "start_of_tab" to "Tab", "sot" to "Tab",
    )

    /** Section end directives. */
    private val SECTION_ENDS = setOf(
        "end_of_chorus", "eoc",
        "end_of_verse", "eov",
        "end_of_bridge", "eob",
        "end_of_intro",
        "end_of_outro",
        "end_of_interlude",
        "end_of_tab", "eot",
    )

    /**
     * Parses a ChordPro-formatted string into a [ChordSheet].
     *
     * Chord markers `[Am]` pass through unchanged since they use the
     * same syntax as our internal format.
     *
     * @param input The raw ChordPro text content.
     * @param defaultTitle Fallback title if none is found in directives.
     * @return A new [ChordSheet] with parsed title, artist, subtitle, and content.
     */
    fun parse(input: String, defaultTitle: String = "Imported Song"): ChordSheet {
        var title = ""
        var artist = ""
        var subtitle = ""
        var key = ""
        var capo = 0
        val metaLines = mutableListOf<String>()
        val contentLines = mutableListOf<String>()

        input.lines().forEach { line ->
            val trimmed = line.trim()

            if (trimmed.startsWith("#")) return@forEach

            if (trimmed.startsWith("\\{")) {
                contentLines.add(line.replaceFirst("\\", ""))
                return@forEach
            }

            val match = DIRECTIVE.find(trimmed)
            if (match != null && trimmed.startsWith("{")) {
                val directive = match.groupValues[1].lowercase()
                val value = match.groupValues[2].trim()

                when {
                    directive == "title" || directive == "t" -> title = value
                    directive == "artist" -> artist = value
                    directive == "subtitle" || directive == "st" -> subtitle = value
                    directive == "comment" || directive == "c" || directive == "ci" -> {
                        if (value.isNotEmpty()) contentLines.add(value)
                    }
                    directive in SECTION_STARTS -> {
                        val defaultLabel = SECTION_STARTS[directive]!!
                        val label = value.ifEmpty { defaultLabel }
                        contentLines.add("[$label]")
                    }
                    directive in SECTION_ENDS -> contentLines.add("")
                    directive == "chorus" -> contentLines.add("[Chorus]")
                    directive == "key" -> key = value
                    directive == "capo" -> capo = value.toIntOrNull() ?: 0
                    directive == "tempo" -> {
                        if (value.isNotEmpty()) metaLines.add("Tempo: $value BPM")
                    }
                    directive == "time" -> {
                        if (value.isNotEmpty()) metaLines.add("Time: $value")
                    }
                    directive == "define" -> { /* Custom voicings not supported */ }
                    else -> { /* Unknown directive — skip */ }
                }
            } else {
                contentLines.add(line)
            }
        }

        val allLines = if (metaLines.isNotEmpty()) {
            metaLines + "" + contentLines
        } else {
            contentLines
        }

        return ChordSheet(
            title = title.ifEmpty { defaultTitle },
            artist = artist,
            subtitle = subtitle,
            content = allLines.joinToString("\n").trim(),
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

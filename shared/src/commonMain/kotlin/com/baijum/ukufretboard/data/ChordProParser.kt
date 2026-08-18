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
 *
 * Callers should prefer [looksLikeChordPro] over [isChordProFile] when deciding
 * whether to route imported text through this parser: ChordPro files are often
 * distributed with a `.txt` extension, and Storage Access Framework content URIs
 * frequently carry no usable extension at all.
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

    /** Space-separated metadata and layout directive names, split into [RECOGNISED_DIRECTIVES]. */
    private const val METADATA_DIRECTIVES =
        "title t subtitle st artist composer lyricist album year copyright sorttitle " +
            "key capo tempo time duration comment c ci comment_italic comment_box cb " +
            "chorus define chord meta new_song ns musicpath transpose zoom columns col " +
            "column_break colb new_page np textfont textsize chordfont chordsize titles"

    /**
     * Directive names that mark text as ChordPro even when this parser does not
     * act on them (e.g. `album`, `composer`). Used only by [looksLikeChordPro];
     * keeping it broader than the handled set makes format detection reliable
     * for files whose only directives are bibliographic metadata.
     */
    private val RECOGNISED_DIRECTIVES: Set<String> =
        SECTION_STARTS.keys + SECTION_ENDS + METADATA_DIRECTIVES.split(' ').toSet()

    /** Matches a line consisting solely of a directive: `{name}` or `{name: value}`. */
    private val WHOLE_LINE_DIRECTIVE = Regex("""^\{(\w+)(?::.*)?\}$""")

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

    /**
     * Checks whether text content is ChordPro by looking for directives.
     *
     * A file counts as ChordPro when at least one line consists solely of a
     * recognised directive, e.g. `{title: Heart of Gold}` or `{start_of_chorus}`.
     * Requiring a known directive name keeps lyrics that merely contain braces
     * from being misdetected.
     *
     * This is more reliable than [isChordProFile]: extensions are absent from
     * Storage Access Framework content URIs and are frequently `.txt` in the
     * wild.
     *
     * @param content The raw text content to inspect.
     * @return `true` if the content contains at least one ChordPro directive.
     */
    fun looksLikeChordPro(content: String): Boolean =
        content.lineSequence().any { line ->
            val name = WHOLE_LINE_DIRECTIVE.find(line.trim())?.groupValues?.get(1)
            name != null && name.lowercase() in RECOGNISED_DIRECTIVES
        }
}

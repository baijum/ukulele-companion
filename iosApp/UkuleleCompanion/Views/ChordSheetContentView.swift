import SwiftUI
import shared

/// Renders parsed ChordPro content: section headings, lyrics, and their chords either
/// on a row above or inline in `[C]` form, with every chord name tappable.
///
/// Shared by `SongViewerView` and `PerformanceModeView` so both show the same formatted
/// sheet. Fullscreen used to print each line verbatim, so the reader saw raw `[Em]`
/// markers and lost chord colouring, headings and tap targets (issue #520).
struct ChordSheetContentView: View {
    let content: String
    let font: Font
    /// "inline" keeps the `[C]` brackets in the lyric line; anything else puts the
    /// chords on their own row above. Mirrors the Settings value verbatim.
    let chordDisplayStyle: String
    let chordColor: Color
    let onChordTap: (String) -> Void

    var body: some View {
        let lines = content.components(separatedBy: "\n")
        VStack(alignment: .leading, spacing: 2) {
            ForEach(0..<lines.count, id: \.self) { i in
                lineView(lines[i])
                    .id("line_\(i)")
            }
        }
    }

    @ViewBuilder
    private func lineView(_ line: String) -> some View {
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        // A lone bracketed word is a section marker ("[Chorus]") unless it names a
        // chord, in which case it is a chord-only line and gets parsed as usual.
        if trimmed.hasPrefix("["), trimmed.hasSuffix("]"), !trimmed.contains("\n"),
           ChordNameParser.shared.parse(input: String(trimmed.dropFirst().dropLast())) == nil {
            Text(String(trimmed.dropFirst().dropLast()))
                .font(font)
                .bold()
                .foregroundColor(.secondary)
                .padding(.top, 8)
                .padding(.bottom, 2)
                .accessibilityAddTraits(.isHeader)
        } else {
            let segments = ChordParser.shared.parseLine(line: line)
                .asArray(of: ChordParser.TextSegment.self)
            parsedLineView(segments: segments)
        }
    }

    @ViewBuilder
    private func parsedLineView(segments: [ChordParser.TextSegment]) -> some View {
        let hasChords = segments.contains(where: { $0 is ChordParser.TextSegmentChord })
        if hasChords && chordDisplayStyle == "inline" {
            HStack(spacing: 0) {
                ForEach(0..<segments.count, id: \.self) { i in
                    let segment = segments[i]
                    if let chord = segment as? ChordParser.TextSegmentChord {
                        chordText("[\(chord.name)]", name: chord.name)
                    } else if let plain = segment as? ChordParser.TextSegmentPlainText {
                        Text(plain.text)
                            .font(font)
                    }
                }
            }
        } else if hasChords {
            let result = Self.buildChordsAboveLyrics(segments: segments)
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    ForEach(0..<result.chordElements.count, id: \.self) { i in
                        let element = result.chordElements[i]
                        if element.isChord {
                            chordText(element.text, name: element.text)
                        } else {
                            Text(element.text)
                                .font(font)
                                .accessibilityHidden(true)
                        }
                    }
                }

                if !result.lyrics.trimmingCharacters(in: .whitespaces).isEmpty {
                    Text(result.lyrics)
                        .font(font)
                }
            }
        } else {
            let text = segments.compactMap { ($0 as? ChordParser.TextSegmentPlainText)?.text }.joined()
            Text(text.isEmpty ? " " : text)
                .font(font)
        }
    }

    private func chordText(_ label: String, name: String) -> some View {
        Text(label)
            .font(font)
            .foregroundColor(chordColor)
            .bold()
            .onTapGesture { onChordTap(name) }
            .accessibilityLabel(name)
            .accessibilityAddTraits(.isButton)
            .accessibilityHint("Show chord diagram")
    }

    static func buildChordsAboveLyrics(segments: [ChordParser.TextSegment])
        -> (chordElements: [(text: String, isChord: Bool)], lyrics: String, chordNames: [String])
    {
        var lyrics = ""
        var chordEntries: [(position: Int, name: String)] = []

        for segment in segments {
            if let chord = segment as? ChordParser.TextSegmentChord {
                chordEntries.append((position: lyrics.count, name: chord.name))
            } else if let plain = segment as? ChordParser.TextSegmentPlainText {
                lyrics += plain.text
            }
        }

        var chordElements: [(text: String, isChord: Bool)] = []
        var pos = 0
        for entry in chordEntries {
            let gap = entry.position - pos
            if gap > 0 {
                chordElements.append((String(repeating: " ", count: gap), false))
                pos = entry.position
            } else if pos > 0 {
                chordElements.append((" ", false))
                pos += 1
            }
            chordElements.append((entry.name, true))
            pos += entry.name.count
        }

        return (chordElements, lyrics, chordEntries.map { $0.name })
    }
}

/// The sheet shown after tapping a chord name in a song: its diagram and a play action.
///
/// Hoisted out of `SongViewerView` so fullscreen can show it too — the tap target only
/// became reachable there once fullscreen started rendering parsed chords (issue #520).
struct ChordDetailPopover: View {
    let chord: String
    let tonePlayer: TonePlayer

    var body: some View {
        if let parsed = ChordNameParser.shared.parse(input: chord) {
            // Follows the selected tuning (#576) and Allow Muted Strings (#593).
            let tuning = FretboardPreferences.tuning.asUkuleleStrings
            let voicings = VoicingGenerator.shared.generate(
                rootPitchClass: Int32(parsed.rootPitchClass),
                formula: parsed.formula,
                tuning: tuning,
                allowMutedStrings: FretboardPreferences.allowMuted
            ).asArray(of: ChordVoicing.self)

            VStack(spacing: 8) {
                Text(chord)
                    .font(.headline)
                if let voicing = voicings.first {
                    ChordDiagramView(voicing: voicing, chordName: chord)
                    Button {
                        play(voicing: voicing)
                    } label: {
                        Label("Play", systemImage: "play.fill")
                            .font(.caption)
                    }
                    .buttonStyle(.bordered)
                    .accessibilityLabel("Play \(chord)")
                } else {
                    Text("No voicing found")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding()
        } else {
            VStack(spacing: 8) {
                Text(chord)
                    .font(.headline)
                Text("Chord not recognized")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding()
        }
    }

    private func play(voicing: ChordVoicing) {
        let fretList = voicing.fretInts
        let openPitchClasses = FretboardPreferences.tuning.pitchClassInts
        let pitchClasses = (0..<fretList.count).compactMap { i -> Int32? in
            let fret = fretList[i]
            guard fret >= 0 else { return nil }
            return (openPitchClasses[i] + Int32(fret)) % 12
        }
        tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
    }
}

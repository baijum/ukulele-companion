import SwiftUI
import shared

struct ChordResultView: View {
    let result: DetectionResultWrapper
    var onPlayChord: (() -> Void)?
    var onShareChord: (() -> Void)?
    var frets: [Int32] = []
    var tuning: [shared.UkuleleString] = []
    var showTips: Bool = true
    var onSuggestedChord: ((String) -> Void)?
    var onShowInLibrary: ((String, String) -> Void)?
    var onAlternateTapped: ((String) -> Void)?

    @State private var tipIndex: Int = Int.random(in: 0..<ExplorerTips.shared.ALL.count)
    private let suggestedChords = ["C", "Am", "F", "G7", "Em", "D"]

    var body: some View {
        VStack(spacing: 8) {
            switch result {
            case .noSelection:
                Label("Tap the fretboard to explore", systemImage: "hand.tap")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                if onSuggestedChord != nil {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Try These")
                            .font(.caption.bold())
                            .foregroundStyle(.secondary)
                            .accessibilityAddTraits(.isHeader)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(suggestedChords, id: \.self) { chord in
                                    Button { onSuggestedChord?(chord) } label: {
                                        Text(chord)
                                            .font(.subheadline.bold())
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 8)
                                            .background(Color.accentColor.opacity(0.15))
                                            .foregroundStyle(Color.accentColor)
                                            .clipShape(Capsule())
                                    }
                                    .buttonStyle(.plain)
                                    .accessibilityLabel("Try \(chord)")
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 8)
                }

                if showTips {
                    tipCard
                }

            case .singleNote(let name, _):
                VStack(spacing: 4) {
                    Text(name)
                        .font(.title.bold())
                    Text("Single note")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .accessibilityCombined(label: "Single note: \(name)")

            case .interval(let notes):
                VStack(spacing: 4) {
                    HStack(spacing: 12) {
                        ForEach(0..<notes.count, id: \.self) { i in
                            Text(notes[i].name)
                                .font(.title2.bold())
                        }
                    }
                    Text("Interval")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .accessibilityCombined(label: "Interval: " + notes.map { $0.name }.joined(separator: " and "))

            case .chordFound(let name, let quality, _, let rootPc, let notes, let alternates, let formula):
                VStack(spacing: 4) {
                    if let onShowInLibrary {
                        Button { onShowInLibrary(name, formula?.symbol ?? "") } label: {
                            Text(name)
                                .font(.largeTitle.bold())
                                .underline()
                        }
                        .buttonStyle(.plain)
                        .accessibilityHint("Double-tap to show in chord library")
                    } else {
                        Text(name)
                            .font(.largeTitle.bold())
                    }
                    Text(quality)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .accessibilityCombined(
                    label: [name, quality, alternates.isEmpty ? nil : "also known as \(alternates.joined(separator: ", "))"]
                        .compactMap { $0 }.joined(separator: ", ")
                )

                if !alternates.isEmpty {
                    HStack(spacing: 6) {
                        Text("Also:")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        ForEach(alternates, id: \.self) { alt in
                            Button { onAlternateTapped?(alt) } label: {
                                Text(alt)
                                    .font(.caption.bold())
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 3)
                                    .background(.quaternary)
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("\(alt), tap to view in library")
                        }
                    }
                }

                if !notes.isEmpty {
                    Text(notes.map { $0.name }.joined(separator: " – "))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("Notes: " + notes.map { $0.name }.joined(separator: ", "))
                }

                if !frets.isEmpty {
                    Text(frets.map { String($0) }.joined(separator: " – "))
                        .font(.subheadline.monospaced())
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("Finger positions: " + frets.map { String($0) }.joined(separator: ", "))
                }

                HStack(spacing: 12) {
                    if let onPlay = onPlayChord {
                        Button(action: onPlay) {
                            Label("Play", systemImage: "play.fill")
                                .font(.subheadline)
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.small)
                    }

                    if let onShare = onShareChord {
                        Button(action: onShare) {
                            Label("Share", systemImage: "square.and.arrow.up")
                                .font(.subheadline)
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                    }
                }
                .padding(.top, 4)

                chordDetailSection

            case .noMatch(let notes):
                VStack(spacing: 4) {
                    Text("No chord match")
                        .font(.headline)
                        .foregroundStyle(.secondary)
                    HStack(spacing: 6) {
                        ForEach(0..<notes.count, id: \.self) { i in
                            Text(notes[i].name)
                                .font(.caption.bold())
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(.quaternary)
                                .clipShape(Capsule())
                        }
                    }
                }
                .accessibilityCombined(label: "No chord match. Notes: " + notes.map { $0.name }.joined(separator: ", "))
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
    }

    // MARK: - Did You Know Tip Card

    private var tipCard: some View {
        let tips = ExplorerTips.shared.ALL as [String]
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "lightbulb.fill")
                    .foregroundStyle(.yellow)
                Text("Did You Know?")
                    .font(.subheadline.bold())
                Spacer()
                Button {
                    tipIndex = (tipIndex + 1) % tips.count
                } label: {
                    Label("Next tip", systemImage: "arrow.right.circle")
                        .font(.caption)
                }
                .buttonStyle(.plain)
                .foregroundStyle(Color.accentColor)
            }
            Text(tips[tipIndex % tips.count])
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 8)
        .accessibilityCombined(label: "Did you know: \(tips[tipIndex % tips.count])")
    }

    // MARK: - Chord Detail Section

    @ViewBuilder
    private var chordDetailSection: some View {
        if case .chordFound(let name, _, _, let rootPitchClass, let notes, _, let formula) = result,
           let formula = formula, frets.count == 4 {
            let detail = ChordDetail(
                name: name, rootPitchClass: rootPitchClass,
                notes: notes, formula: formula,
                frets: frets, tuning: tuning
            )
            Divider()
                .padding(.top, 8)

            VStack(alignment: .leading, spacing: 10) {
                if let aliasText = detail.aliasText {
                    ChordInfoRow(label: "Also written as", value: aliasText)
                }
                ChordInfoRow(label: "Intervals", value: detail.intervalBreakdown)
                ChordInfoRow(label: "Formula", value: detail.formulaStr)
                ChordInfoRow(label: "Fingering", value: detail.fingeringStr)
                ChordInfoRow(label: "Difficulty", value: detail.difficultyStr)
                ChordInfoRow(label: "Inversion", value: detail.inversionText)
            }
            .padding(.top, 4)
        }
    }
}

// MARK: - Chord Detail Computation

private struct ChordDetail {
    let aliasText: String?
    let intervalBreakdown: String
    let formulaStr: String
    let fingeringStr: String
    let difficultyStr: String
    let inversionText: String

    init(name: String, rootPitchClass: Int32,
         notes: [(pitchClass: Int32, name: String)],
         formula: ChordFormula,
         frets: [Int32], tuning: [shared.UkuleleString]) {

        let aliases = formula.aliases as [String]
        if !aliases.isEmpty {
            let symbol = formula.symbol
            let rootName = name.hasSuffix(symbol) && !symbol.isEmpty
                ? String(name.dropLast(symbol.count))
                : name
            aliasText = aliases.map { rootName + $0 }.joined(separator: ", ")
        } else {
            aliasText = nil
        }

        let kotlinNotes = notes.map { Note(pitchClass: $0.pitchClass, name: $0.name) }
        intervalBreakdown = ChordInfo.shared.buildIntervalBreakdown(
            root: kotlinNotes.first!, notes: kotlinNotes
        )

        formulaStr = ChordInfo.shared.buildFormulaString(formula: formula)

        let kotlinFrets = frets.map { KotlinInt(int: $0) }
        let fingering = ChordInfo.shared.suggestFingering(frets: kotlinFrets)
        fingeringStr = ChordInfo.shared.formatFingering(fingering: fingering)

        difficultyStr = ChordInfo.shared.rateDifficulty(frets: kotlinFrets)

        let inversion = ChordInfo.shared.determineInversion(
            frets: kotlinFrets, rootPitchClass: rootPitchClass,
            formula: formula, tuning: tuning
        )
        let bassPc = ChordInfo.shared.bassPitchClass(frets: kotlinFrets, tuning: tuning)
        let bassNote = Notes.shared.pitchClassToName(pitchClass: bassPc)
        if inversion == ChordInfo.Inversion.root {
            inversionText = "Root position (bass: \(bassNote))"
        } else {
            inversionText = "\(inversion.label) (bass: \(bassNote))"
        }
    }
}

// MARK: - Chord Info Row

private struct ChordInfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 100, alignment: .trailing)
            Text(value)
                .font(.caption)
            Spacer()
        }
        .accessibilityCombined(label: "\(label): \(value)")
    }
}

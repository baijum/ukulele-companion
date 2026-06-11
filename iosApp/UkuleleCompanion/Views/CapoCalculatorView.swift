import SwiftUI
import shared

struct CapoCalculatorView: View {
    enum Mode {
        case singleChord(rootPitchClass: Int32, formula: ChordFormula)
        case progression(progression: Progression, keyRoot: Int32)
    }

    let mode: Mode
    private let leftHanded: Bool

    private let tuning: [shared.UkuleleString] = UkuleleTuning.highG.asUkuleleStrings

    init(mode: Mode) {
        self.mode = mode
        let defaults = UserDefaults(suiteName: "app_settings") ?? .standard
        self.leftHanded = defaults.bool(forKey: "left_handed")
    }

    var body: some View {
        Group {
            switch mode {
            case .singleChord(let root, let formula):
                singleChordView(rootPitchClass: root, formula: formula)
            case .progression(let progression, let keyRoot):
                progressionView(progression: progression, keyRoot: keyRoot)
            }
        }
        .navigationTitle("Capo Calculator")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Single Chord Mode

    private func singleChordView(rootPitchClass: Int32, formula: ChordFormula) -> some View {
        let results = CapoCalculator.shared.forSingleChord(
            rootPitchClass: rootPitchClass,
            formula: formula,
            tuning: tuning
        ).asArray(of: CapoCalculator.SingleChordResult.self)

        return ScrollView {
            VStack(spacing: 12) {
                if results.isEmpty {
                    Text("No capo positions available.")
                        .foregroundStyle(.secondary)
                        .padding(.top, 32)
                } else {
                    let soundingName = Notes.shared.pitchClassToName(pitchClass: rootPitchClass) + formula.symbol
                    Text("Sounding: \(soundingName)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    Text("Place a capo to find easier chord shapes.")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                        .padding(.bottom, 4)

                    let bestScore = results.first?.score ?? 0

                    ForEach(0..<results.count, id: \.self) { i in
                        singleResultCard(
                            result: results[i],
                            isRecommended: i == 0,
                            maxScore: bestScore
                        )
                    }
                }
            }
            .padding()
        }
    }

    // MARK: - Progression Mode

    private func progressionView(progression: Progression, keyRoot: Int32) -> some View {
        let results = CapoCalculator.shared.forProgression(
            progression: progression,
            keyRoot: keyRoot,
            tuning: tuning
        ).asArray(of: CapoCalculator.ProgressionResult.self)

        return ScrollView {
            VStack(spacing: 12) {
                if results.isEmpty {
                    Text("No capo positions available for this progression.")
                        .foregroundStyle(.secondary)
                        .padding(.top, 32)
                } else {
                    Text("Find the easiest capo position for all chords.")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                        .padding(.bottom, 4)

                    let bestTotal = results.first?.totalScore ?? 0

                    ForEach(0..<results.count, id: \.self) { i in
                        progressionResultCard(
                            result: results[i],
                            isRecommended: i == 0,
                            maxScore: bestTotal
                        )
                    }
                }
            }
            .padding()
        }
    }

    // MARK: - Single Result Card

    private func singleResultCard(
        result: CapoCalculator.SingleChordResult,
        isRecommended: Bool,
        maxScore: Int32
    ) -> some View {
        HStack(alignment: .top, spacing: 8) {
            // Capo fret
            VStack {
                Text(result.capoFret == 0 ? "No Capo" : "Fret \(result.capoFret)")
                    .font(.subheadline.bold())
                    .foregroundStyle(isRecommended ? Color.accentColor : .primary)
            }
            .frame(width: 56)

            // Shape info
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("Play \(result.shapeName) shape")
                        .font(.subheadline.bold())
                    if isRecommended {
                        Text("Recommended")
                            .font(.caption2.bold())
                            .foregroundStyle(Color.accentColor)
                    }
                }
                let frets = result.bestVoicing.fretInts
                Text("Frets: \(frets.map(String.init).joined(separator: " "))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                let ease = maxScore > 0 ? Int(result.score * 100 / maxScore) : 0
                Text("Ease: \(ease)%")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Mini diagram
            ChordDiagramView(voicing: result.bestVoicing)
                .scaleEffect(0.8)
                .frame(width: 100)
        }
        .padding()
        .background(isRecommended ? Color.accentColor.opacity(0.1) : Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .accessibilityCombined(
            label: "\(result.capoFret == 0 ? "No capo" : "Capo fret \(result.capoFret)"), play \(result.shapeName) shape\(isRecommended ? ", recommended" : "")"
        )
    }

    // MARK: - Progression Result Card

    private func progressionResultCard(
        result: CapoCalculator.ProgressionResult,
        isRecommended: Bool,
        maxScore: Int32
    ) -> some View {
        let chordResults = result.chordResults.asArray(of: CapoCalculator.SingleChordResult.self)

        return VStack(alignment: .leading, spacing: 8) {
            // Header row
            HStack {
                Text(result.capoFret == 0 ? "No Capo" : "Fret \(result.capoFret)")
                    .font(.subheadline.bold())
                    .foregroundStyle(isRecommended ? Color.accentColor : .primary)
                    .frame(width: 56, alignment: .leading)

                VStack(alignment: .leading) {
                    Text("Shapes: \(chordResults.map(\.shapeName).joined(separator: " - "))")
                        .font(.caption.bold())
                    if isRecommended {
                        Text("Recommended")
                            .font(.caption2.bold())
                            .foregroundStyle(Color.accentColor)
                    }
                    let ease = maxScore > 0 ? Int(result.totalScore * 100 / maxScore) : 0
                    Text("Ease: \(ease)%")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            // Mini diagrams row
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<chordResults.count, id: \.self) { j in
                        VStack(spacing: 2) {
                            Text(chordResults[j].shapeName)
                                .font(.caption2.bold())
                            ChordDiagramView(voicing: chordResults[j].bestVoicing)
                                .scaleEffect(0.7)
                                .frame(width: 90, height: 110)
                        }
                    }
                }
            }
        }
        .padding()
        .background(isRecommended ? Color.accentColor.opacity(0.1) : Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

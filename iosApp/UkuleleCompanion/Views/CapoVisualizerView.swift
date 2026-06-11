import SwiftUI
import shared

struct CapoVisualizerView: View {
    let voicing: ChordVoicing
    let rootPitchClass: Int32
    let formula: ChordFormula

    @State private var capoPosition: Float = 0

    private let leftHanded: Bool
    private let tuning: [shared.UkuleleString]

    init(voicing: ChordVoicing, rootPitchClass: Int32, formula: ChordFormula) {
        self.voicing = voicing
        self.rootPitchClass = rootPitchClass
        self.formula = formula
        let defaults = UserDefaults(suiteName: "app_settings") ?? .standard
        self.leftHanded = defaults.bool(forKey: "left_handed")
        self.tuning = UkuleleTuning.highG.asUkuleleStrings
    }

    private var capoFret: Int { Int(capoPosition) }
    private var soundingRoot: Int32 { (rootPitchClass + Int32(capoFret)) % 12 }
    private var originalName: String { Notes.shared.pitchClassToName(pitchClass: rootPitchClass) + formula.symbol }
    private var soundingName: String { Notes.shared.pitchClassToName(pitchClass: soundingRoot) + formula.symbol }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Current chord info
                Text("Shape: \(originalName)")
                    .font(.subheadline)

                if capoFret > 0 {
                    Text("With capo at fret \(capoFret): sounds as \(soundingName)")
                        .font(.subheadline.bold())
                        .foregroundStyle(Color.accentColor)
                } else {
                    Text("No capo - sounds as \(originalName)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // Capo position slider
                VStack(alignment: .leading, spacing: 4) {
                    Text("Capo Position")
                        .font(.caption.bold())
                        .foregroundStyle(.secondary)
                    HStack {
                        Text("0")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Slider(value: $capoPosition, in: 0...11, step: 1)
                            .accessibilityValue(capoFret == 0 ? "No capo" : "Fret \(capoFret)")
                        Text("11")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Text(capoFret == 0 ? "No Capo" : "Fret \(capoFret)")
                        .font(.title2.bold())
                        .frame(maxWidth: .infinity, alignment: .center)
                }

                // Side-by-side diagrams
                HStack(alignment: .top) {
                    // Original shape
                    VStack(spacing: 4) {
                        Text(originalName)
                            .font(.caption.bold())
                        Text("Shape")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        ChordDiagramView(voicing: voicing, chordName: nil)
                    }
                    .frame(maxWidth: .infinity)

                    // With capo
                    VStack(spacing: 4) {
                        Text(capoFret > 0 ? soundingName : originalName)
                            .font(.caption.bold())
                            .foregroundStyle(capoFret > 0 ? Color.accentColor : Color.primary)
                        Text(capoFret > 0 ? "Sounding" : "Same")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        ChordDiagramView(voicing: voicing, chordName: nil)
                    }
                    .frame(maxWidth: .infinity)
                }

                // Sounding notes
                if capoFret > 0 {
                    let frets = voicing.fretInts
                    let noteNames = frets.enumerated().map { (i, fret) -> String in
                        if fret < 0 { return "x" }
                        let pc = (tuning[i].openPitchClass + Int32(fret) + Int32(capoFret)) % 12
                        return Notes.shared.pitchClassToName(pitchClass: pc)
                    }
                    Text("Sounding notes: \(noteNames.joined(separator: " "))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // Educational tip
                if capoFret > 0 {
                    Text("A capo at fret \(capoFret) raises every string by \(capoFret) semitone\(capoFret == 1 ? "" : "s"). Playing a \(originalName) shape now sounds as \(soundingName).")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
            .padding()
        }
        .navigationTitle("Capo Visualizer")
        .navigationBarTitleDisplayMode(.inline)
    }
}

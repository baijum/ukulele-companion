import SwiftUI
import shared

struct ScaleChordsView: View {
    @State private var selectedRoot: Int32 = 0
    @State private var selectedScale: Scale?

    private let noteNames: [String] = (0..<12).map { Notes.shared.pitchClassToName(pitchClass: Int32($0)) }
    private let scales: [Scale] = Scales.shared.ALL as! [Scale]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Root picker
                Text("Root Note")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(0..<12, id: \.self) { index in
                            Button(noteNames[index]) {
                                selectedRoot = Int32(index)
                            }
                            .buttonStyle(.bordered)
                            .tint(selectedRoot == Int32(index) ? .accentColor : .secondary)
                            .accessibilityAddTraits(selectedRoot == Int32(index) ? .isSelected : [])
                        }
                    }
                }

                // Scale picker
                Text("Scale")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(scales, id: \.name) { scale in
                            Button(scale.name) {
                                selectedScale = scale
                            }
                            .buttonStyle(.bordered)
                            .tint(selectedScale?.name == scale.name ? .accentColor : .secondary)
                            .accessibilityAddTraits(selectedScale?.name == scale.name ? .isSelected : [])
                        }
                    }
                }

                // Triads list
                if let scale = selectedScale {
                    let chords = ScaleChordBuilder.shared.buildTriads(rootPitchClass: selectedRoot, scale: scale) as! [ScaleChord]
                    if chords.isEmpty {
                        Text("This scale has too few notes to build triads.")
                            .foregroundStyle(.secondary)
                            .padding(.top, 8)
                    } else {
                        Text("Diatonic Triads")
                            .font(.headline)
                            .accessibilityAddTraits(.isHeader)
                        ForEach(chords, id: \.degree) { chord in
                            let notes = chord.notes as! [String]
                            HStack(spacing: 16) {
                                Text(chord.numeral)
                                    .font(.title3.bold())
                                    .frame(width: 50, alignment: .leading)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(chord.rootName + chord.symbol)
                                        .font(.body.bold())
                                    Text(chord.quality)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }

                                Spacer()

                                Text(notes.joined(separator: " - "))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 6)
                            .padding(.horizontal, 12)
                            .background(Color(.secondarySystemGroupedBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .accessibilityCombined(label: "\(chord.numeral), \(chord.rootName)\(chord.symbol), \(chord.quality), notes: \(notes.joined(separator: ", "))")
                        }
                    }
                } else {
                    Text("Select a root note and scale to see diatonic triads.")
                        .foregroundStyle(.secondary)
                        .padding(.top, 8)
                }
            }
            .padding()
        }
        .navigationTitle("Scale Chords")
        .onAppear {
            if selectedScale == nil {
                selectedScale = scales.first
            }
        }
    }
}

import SwiftUI
import shared

struct FretboardNoteMapView: View {
    @EnvironmentObject var settings: SettingsViewModel
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var highlightPitchClass: Int32 = -1
    @State private var isLowG = false

    private let fretCount = 12
    private var cellSize: CGFloat { sizeClass == .regular ? 52 : 36 }
    @StateObject private var tonePlayer = TonePlayer()

    private var effectiveTuning: UkuleleTuning {
        if isLowG { return .lowG }
        return UkuleleTuning.entries.first { $0.label == settings.selectedTuning } ?? .highG
    }

    private var canToggleHighLow: Bool {
        let base = UkuleleTuning.entries.first { $0.label == settings.selectedTuning } ?? .highG
        return base == .highG || base == .lowG
    }

    var body: some View {
        let tuning = effectiveTuning

        VStack(spacing: 16) {
            if canToggleHighLow {
                HStack(spacing: 8) {
                    tuningChip(label: "High-G", selected: !isLowG) { isLowG = false }
                    tuningChip(label: "Low-G", selected: isLowG) { isLowG = true }
                }
                .padding(.horizontal)
            }

            // Note highlight filter
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    filterChip(label: "All", pitchClass: -1)
                    ForEach(0..<Int32(12), id: \.self) { pc in
                        let name = Notes.shared.pitchClassToName(pitchClass: pc)
                        filterChip(label: name, pitchClass: pc)
                    }
                }
                .padding(.horizontal)
            }

            // Fretboard grid
            ScrollView(.horizontal, showsIndicators: false) {
                VStack(spacing: 0) {
                    HStack(spacing: 0) {
                        Text("")
                            .frame(width: 32)
                        ForEach(0...fretCount, id: \.self) { fret in
                            Text(fret == 0 ? "Open" : "\(fret)")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .frame(width: cellSize)
                        }
                    }
                    .accessibilityHidden(true)

                    let pitchClasses = tuning.pitchClassInts
                    let stringNames = tuning.stringNameArray
                    let octaves = tuning.octaveInts

                    ForEach(Array((0..<4).reversed()), id: \.self) { stringIndex in
                        HStack(spacing: 0) {
                            Text(stringNames[stringIndex])
                                .font(.caption.bold())
                                .frame(width: 32)
                                .accessibilityHidden(true)

                            ForEach(0...fretCount, id: \.self) { fret in
                                let openPC = pitchClasses[stringIndex]
                                let note = NoteKt.calculateNote(
                                    openStringPitchClass: openPC,
                                    fret: Int32(fret)
                                )
                                // Octave for this cell, matching Android's
                                // baseOctave + (openPc + fret) / 12 (see #588).
                                let octave = octaves[stringIndex] + (openPC + Int32(fret)) / 12
                                let isHighlighted = highlightPitchClass < 0 ||
                                    note.pitchClass == highlightPitchClass
                                let isOpenString = fret == 0

                                Text(note.name)
                                    .font(.caption2.bold())
                                    .frame(width: cellSize, height: cellSize)
                                    .background(
                                        isHighlighted
                                            ? Color.accentColor.opacity(isOpenString ? 0.3 : 0.15)
                                            : Color(.systemGray6)
                                    )
                                    .foregroundStyle(isHighlighted ? .primary : .quaternary)
                                    .border(Color(.systemGray4), width: 0.5)
                                    .onTapGesture {
                                        tonePlayer.playNote(pitchClass: note.pitchClass, octave: octave)
                                    }
                                    .accessibilityLabel("\(note.name), string \(stringIndex + 1), fret \(fret)")
                                    .accessibilityValue(isHighlighted ? "highlighted" : "dimmed")
                                    .accessibilityHint("Double tap to play")
                            }
                        }
                    }
                }
                .padding(.horizontal)
            }

            tipsCard

            Spacer()
        }
        .padding(.top)
        .navigationTitle("Fretboard Note Map")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var tipsCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Tips")
                .font(.subheadline.bold())
                .accessibilityAddTraits(.isHeader)
            VStack(alignment: .leading, spacing: 4) {
                tipRow("Tap any note to hear it")
                tipRow("Use the highlight filter to find all positions of a note")
                tipRow("Fret 12 is the same notes as fret 0 (one octave higher)")
                tipRow("Switch to Low-G to see how the G string changes")
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.accentColor.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }

    private func tipRow(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 6) {
            Text("\u{2022}")
                .font(.caption)
            Text(text)
                .font(.caption)
        }
        .foregroundStyle(.secondary)
    }

    @ViewBuilder
    private func tuningChip(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.caption.bold())
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? Color.accentColor : Color(.systemGray5))
                .foregroundStyle(selected ? .white : .primary)
                .clipShape(Capsule())
        }
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    @ViewBuilder
    private func filterChip(label: String, pitchClass: Int32) -> some View {
        Button {
            highlightPitchClass = pitchClass
        } label: {
            Text(label)
                .font(.caption.bold())
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(
                    highlightPitchClass == pitchClass
                        ? Color.accentColor
                        : Color(.systemGray5)
                )
                .foregroundStyle(
                    highlightPitchClass == pitchClass ? .white : .primary
                )
                .clipShape(Capsule())
        }
        .accessibilityAddTraits(highlightPitchClass == pitchClass ? .isSelected : [])
    }
}

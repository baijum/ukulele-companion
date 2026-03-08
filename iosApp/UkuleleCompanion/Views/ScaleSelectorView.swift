import SwiftUI
import shared

/// Scale selector panel for the Explorer: root, scale type, position, and chords-in-scale.
struct ScaleSelectorView: View {
    let state: ScaleOverlayState
    let tuningPitchClasses: [Int32]
    let onRootChanged: (Int32) -> Void
    let onScaleChanged: (Scale) -> Void
    let onToggle: () -> Void
    let onPositionChanged: (ClosedRange<Int>?) -> Void
    let onChordTapped: ([Int32]) -> Void

    private let noteNames: [String] = (0..<12).map {
        Notes.shared.pitchClassToName(pitchClass: Int32($0))
    }
    private let scales: [Scale] = Scales.shared.ALL as! [Scale]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Enable/disable toggle
            HStack {
                Text("Scale Overlay")
                    .font(.subheadline.bold())
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                Toggle("", isOn: Binding(
                    get: { state.enabled },
                    set: { _ in onToggle() }
                ))
                .labelsHidden()
                .accessibilityLabel("Scale overlay")
            }

            if state.enabled {
                rootSelector
                scaleSelector

                if state.scale != nil {
                    positionSelector
                    chordsInScaleSection
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Root Selector

    private var rootSelector: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Root")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(0..<12, id: \.self) { pc in
                        let isSelected = state.root == Int32(pc)
                        Button(noteNames[pc]) {
                            onRootChanged(Int32(pc))
                        }
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(isSelected ? Color.accentColor : Color.secondary.opacity(0.15),
                                    in: Capsule())
                        .foregroundStyle(isSelected ? .white : .primary)
                    }
                }
            }
        }
    }

    // MARK: - Scale Selector

    private var scaleSelector: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Scale")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(scales, id: \.name) { scale in
                        let isSelected = state.scale?.name == scale.name
                        Button(scale.name) {
                            onScaleChanged(scale)
                        }
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(isSelected ? Color.accentColor : Color.secondary.opacity(0.15),
                                    in: Capsule())
                        .foregroundStyle(isSelected ? .white : .primary)
                    }
                }
            }
        }
    }

    // MARK: - Position Selector

    private var positionSelector: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Position")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            let positions = generatePositions()

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    Button("All") {
                        onPositionChanged(nil)
                    }
                    .font(.caption)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(state.positionFretRange == nil ? Color.accentColor : Color.secondary.opacity(0.15),
                                in: Capsule())
                    .foregroundStyle(state.positionFretRange == nil ? .white : .primary)

                    ForEach(0..<positions.count, id: \.self) { idx in
                        let pos = positions[idx]
                        let lower = pos.fretRange.start.intValue
                        let upper = pos.fretRange.endInclusive.intValue
                        let range = lower...upper
                        let isSelected = state.positionFretRange == range
                        Button(pos.name) {
                            onPositionChanged(range)
                        }
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(isSelected ? Color.accentColor : Color.secondary.opacity(0.15),
                                    in: Capsule())
                        .foregroundStyle(isSelected ? .white : .primary)
                    }
                }
            }
        }
    }

    // MARK: - Chords in Scale

    private var chordsInScaleSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Chords in Scale")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            let chords = buildChords()

            if chords.isEmpty {
                Text("Not enough notes to build triads")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(chords, id: \.degree) { chord in
                            VStack(spacing: 2) {
                                Text(chord.numeral)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                                Text(chord.rootName + chord.symbol)
                                    .font(.caption.bold())
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 6)
                            .background(Color.secondary.opacity(0.1), in: RoundedRectangle(cornerRadius: 8))
                            .accessibilityLabel("\(chord.rootName) \(chord.quality), degree \(chord.degree)")
                        }
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private func generatePositions() -> [ScalePosition] {
        guard let scale = state.scale else { return [] }
        let intervals = (scale.intervals as! [NSNumber]).map { $0.intValue }
        let tuning = tuningPitchClasses.map { Int($0) }
        let result = ScalePositions.shared.generate(
            root: Int32(state.root),
            intervals: intervals.map { KotlinInt(int: Int32($0)) },
            tuningPitchClasses: tuning.map { KotlinInt(int: Int32($0)) }
        )
        return result as! [ScalePosition]
    }

    private func buildChords() -> [ScaleChord] {
        guard let scale = state.scale else { return [] }
        let result = ScaleChordBuilder.shared.buildTriads(
            rootPitchClass: state.root,
            scale: scale
        )
        return result as! [ScaleChord]
    }
}

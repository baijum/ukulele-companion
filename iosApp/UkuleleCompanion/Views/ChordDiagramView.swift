import SwiftUI
import shared

/// Traditional vertical chord diagram (strings left-to-right, frets top-to-bottom).
struct ChordDiagramView: View {
    let voicing: ChordVoicing
    var chordName: String?
    var leftHanded: Bool = false
    var bassStringIndex: Int? = nil
    var commonToneIndices: Set<Int>? = nil
    var capoFret: Int? = nil
    var inversionLabel: String? = nil
    var isFavorite: Bool = false
    var onFavoriteClick: (() -> Void)? = nil

    @Environment(\.horizontalSizeClass) private var sizeClass

    private let stringCount = 4
    private let fretCount = 5
    private var stringSpacing: CGFloat { sizeClass == .regular ? 32 : 24 }
    private var fretSpacing: CGFloat { sizeClass == .regular ? 30 : 22 }
    private var dotRadius: CGFloat { sizeClass == .regular ? 10 : 8 }
    private let nutHeight: CGFloat = 4

    private var diagramWidth: CGFloat { CGFloat(stringCount - 1) * stringSpacing }
    private var diagramHeight: CGFloat { CGFloat(fretCount) * fretSpacing }

    private var frets: [Int] {
        let raw = voicing.fretInts
        return leftHanded ? raw.reversed() : raw
    }

    private var startFret: Int {
        let nonOpen = frets.filter { $0 > 0 }
        guard let minFret = nonOpen.min() else { return 0 }
        return minFret <= fretCount ? 0 : minFret - 1
    }

    private var showNut: Bool { startFret == 0 }

    private var fretDescription: String {
        let descriptions = frets.map { fret -> String in
            if fret == 0 { return "open" }
            if fret < 0 { return "muted" }
            return "\(fret)"
        }
        return "Strings: " + descriptions.joined(separator: ", ")
    }

    var body: some View {
        VStack(spacing: 4) {
            if let name = chordName {
                Text(name)
                    .font(.caption.bold())
            }

            // Open/muted string indicators above nut
            openMutedRow
                .accessibilityHidden(true)

            ZStack(alignment: .topLeading) {
                // Grid lines
                Canvas { context, size in
                    drawGrid(context: context, size: size)
                }
                .frame(width: diagramWidth + 16, height: diagramHeight + 8)
                .accessibilityHidden(true)

                // Position label
                if !showNut {
                    Text("\(startFret + 1)fr")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                        .offset(x: diagramWidth + 12, y: 2)
                }

                // Capo bar
                if let capo = capoFret, capo > 0 {
                    let relCapo = capo - startFret
                    let capoVisible = showNut ? (relCapo >= 1 && relCapo <= fretCount) : (relCapo >= 0 && relCapo < fretCount)
                    if capoVisible {
                        let capoY = 4 + (CGFloat(relCapo) - 0.5) * fretSpacing
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color.brown)
                            .frame(width: diagramWidth + 8, height: 6)
                            .position(x: 8 + diagramWidth / 2, y: capoY)
                            .accessibilityHidden(true)
                    }
                }

                // Fret dots
                ForEach(0..<stringCount, id: \.self) { s in
                    let fret = frets[s]
                    if fret > 0 {
                        let displayFret = fret - startFret
                        if displayFret > 0 && displayFret <= fretCount {
                            let originalIndex = leftHanded ? (stringCount - 1 - s) : s
                            Circle()
                                .fill(dotColor(for: originalIndex))
                                .frame(width: dotRadius * 2, height: dotRadius * 2)
                                .position(
                                    x: 8 + CGFloat(s) * stringSpacing,
                                    y: 4 + (CGFloat(displayFret) - 0.5) * fretSpacing
                                )
                                .accessibilityHidden(true)
                        }
                    }
                }
            }

            if let label = inversionLabel {
                Text(label)
                    .font(.caption2)
                    .fontWeight(.medium)
                    .foregroundStyle(.tint)
            }

            if let onFav = onFavoriteClick {
                Button(action: onFav) {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .font(.caption)
                        .foregroundStyle(isFavorite ? .red : .secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isFavorite ? "Remove from favorites" : "Add to favorites")
            }
        }
        .padding(8)
        .accessibilityCombined(label: chordName ?? "Chord diagram", value: fretDescription)
    }

    private var openMutedRow: some View {
        HStack(spacing: stringSpacing - 12) {
            ForEach(0..<stringCount, id: \.self) { s in
                let fret = frets[s]
                if fret == 0 {
                    Circle()
                        .strokeBorder(Color.primary, lineWidth: 1.5)
                        .frame(width: 12, height: 12)
                } else if fret < 0 {
                    Text("X")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.secondary)
                        .frame(width: 12, height: 12)
                } else {
                    Color.clear.frame(width: 12, height: 12)
                }
            }
        }
    }

    private func dotColor(for stringIndex: Int) -> Color {
        if stringIndex == bassStringIndex { return .orange }
        if let common = commonToneIndices, common.contains(stringIndex) { return .cyan }
        return .primary
    }

    private func drawGrid(context: GraphicsContext, size: CGSize) {
        let xOffset: CGFloat = 8
        let yOffset: CGFloat = 4

        // Nut or top fret line
        if showNut {
            var nutPath = Path()
            nutPath.move(to: CGPoint(x: xOffset, y: yOffset))
            nutPath.addLine(to: CGPoint(x: xOffset + diagramWidth, y: yOffset))
            context.stroke(nutPath, with: .color(.primary), lineWidth: nutHeight)
        }

        // Horizontal fret lines
        for f in 0...fretCount {
            let y = yOffset + CGFloat(f) * fretSpacing
            var path = Path()
            path.move(to: CGPoint(x: xOffset, y: y))
            path.addLine(to: CGPoint(x: xOffset + diagramWidth, y: y))
            context.stroke(path, with: .color(.secondary.opacity(0.5)), lineWidth: 1)
        }

        // Vertical string lines
        for s in 0..<stringCount {
            let x = xOffset + CGFloat(s) * stringSpacing
            var path = Path()
            path.move(to: CGPoint(x: x, y: yOffset))
            path.addLine(to: CGPoint(x: x, y: yOffset + diagramHeight))
            context.stroke(path, with: .color(.secondary.opacity(0.6)), lineWidth: 1)
        }
    }
}

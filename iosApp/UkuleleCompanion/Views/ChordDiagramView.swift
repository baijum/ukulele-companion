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
    private let minFretRows = 5
    private var stringSpacing: CGFloat { sizeClass == .regular ? 32 : 24 }
    private var fretSpacing: CGFloat { sizeClass == .regular ? 30 : 22 }
    private var dotRadius: CGFloat { sizeClass == .regular ? 10 : 8 }
    private let nutHeight: CGFloat = 4
    private let openArea: CGFloat = 18

    private var diagramWidth: CGFloat { CGFloat(stringCount - 1) * stringSpacing }
    private var diagramHeight: CGFloat { CGFloat(fretRowCount) * fretSpacing }
    private var gridTop: CGFloat { openArea + (showNut ? nutHeight : 0) }

    private var frets: [Int] {
        let raw = voicing.fretInts
        return leftHanded ? raw.reversed() : raw
    }

    private var startFret: Int {
        let maxFret = Int(voicing.maxFret)
        let minFret = Int(voicing.minFret)
        if maxFret == 0 || maxFret <= minFretRows { return 0 }
        return max(1, minFret)
    }

    private var fretRowCount: Int {
        let maxFret = Int(voicing.maxFret)
        if maxFret == 0 || maxFret <= minFretRows { return minFretRows }
        return max(minFretRows, maxFret - startFret + 1)
    }

    private var showNut: Bool { startFret == 0 }

    private var stringNames: [String] {
        leftHanded ? ["A", "E", "C", "G"] : ["G", "C", "E", "A"]
    }

    private var perStringDescriptions: [String] {
        frets.enumerated().map { (index, fret) in
            let name = stringNames[index]
            let role: String
            let originalIndex = leftHanded ? (stringCount - 1 - index) : index
            if originalIndex == bassStringIndex {
                role = ", bass note"
            } else if let common = commonToneIndices, common.contains(originalIndex) {
                role = ", common tone"
            } else {
                role = ""
            }
            if fret == 0 { return "\(name) string open\(role)" }
            if fret < 0 { return "\(name) string muted\(role)" }
            return "\(name) string fret \(fret)\(role)"
        }
    }

    private var fretDescription: String {
        perStringDescriptions.joined(separator: ", ")
    }

    var body: some View {
        VStack(spacing: 4) {
            if let name = chordName {
                Text(name)
                    .font(.caption.bold())
            }

            ZStack(alignment: .topLeading) {
                Canvas { context, size in
                    drawDiagram(context: context, size: size)
                }
                .frame(width: diagramWidth + 16, height: gridTop + diagramHeight + 4)
                .accessibilityHidden(true)

                // Position label
                if !showNut {
                    Text("\(startFret)fr")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                        .offset(x: diagramWidth + 12, y: gridTop + 2)
                }

                // Capo bar
                if let capo = capoFret, capo > 0 {
                    let relCapo = capo - startFret
                    let capoVisible = showNut ? (relCapo >= 1 && relCapo <= fretRowCount) : (relCapo >= 0 && relCapo < fretRowCount)
                    if capoVisible {
                        let capoY = showNut
                            ? gridTop + (CGFloat(relCapo) - 0.5) * fretSpacing
                            : gridTop + (CGFloat(relCapo) + 0.5) * fretSpacing
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
                        let relFret = fret - startFret
                        let visible = showNut
                            ? (relFret >= 1 && relFret <= fretRowCount)
                            : (relFret >= 0 && relFret < fretRowCount)
                        if visible {
                            let originalIndex = leftHanded ? (stringCount - 1 - s) : s
                            let y = showNut
                                ? gridTop + (CGFloat(relFret) - 0.5) * fretSpacing
                                : gridTop + (CGFloat(relFret) + 0.5) * fretSpacing
                            Circle()
                                .fill(dotColor(for: originalIndex))
                                .frame(width: dotRadius * 2, height: dotRadius * 2)
                                .position(
                                    x: 8 + CGFloat(s) * stringSpacing,
                                    y: y
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
        .accessibilityCombined(
            label: chordName ?? "Chord diagram",
            value: fretDescription,
            hint: "Long press to share as image"
        )
        .accessibilityCustomContent(
            AccessibilityCustomContentKey("String 1", id: "s1"),
            perStringDescriptions.count > 0 ? perStringDescriptions[0] : ""
        )
        .accessibilityCustomContent(
            AccessibilityCustomContentKey("String 2", id: "s2"),
            perStringDescriptions.count > 1 ? perStringDescriptions[1] : ""
        )
        .accessibilityCustomContent(
            AccessibilityCustomContentKey("String 3", id: "s3"),
            perStringDescriptions.count > 2 ? perStringDescriptions[2] : ""
        )
        .accessibilityCustomContent(
            AccessibilityCustomContentKey("String 4", id: "s4"),
            perStringDescriptions.count > 3 ? perStringDescriptions[3] : ""
        )
    }

    private func dotColor(for stringIndex: Int) -> Color {
        if stringIndex == bassStringIndex { return .orange }
        if let common = commonToneIndices, common.contains(stringIndex) { return .cyan }
        return .primary
    }

    private func drawDiagram(context: GraphicsContext, size: CGSize) {
        let xOffset: CGFloat = 8
        let gt = gridTop

        // Open/muted string indicators
        for s in 0..<stringCount {
            let fret = frets[s]
            let x = xOffset + CGFloat(s) * stringSpacing

            if fret == 0 {
                let circle = Path(ellipseIn: CGRect(
                    x: x - 6, y: openArea / 2 - 6,
                    width: 12, height: 12
                ))
                context.stroke(circle, with: .color(.primary), lineWidth: 1.5)
            } else if fret < 0 {
                let centerY = openArea / 2
                let arm: CGFloat = 4
                var line1 = Path()
                line1.move(to: CGPoint(x: x - arm, y: centerY - arm))
                line1.addLine(to: CGPoint(x: x + arm, y: centerY + arm))
                context.stroke(line1, with: .color(.secondary), lineWidth: 1.5)
                var line2 = Path()
                line2.move(to: CGPoint(x: x - arm, y: centerY + arm))
                line2.addLine(to: CGPoint(x: x + arm, y: centerY - arm))
                context.stroke(line2, with: .color(.secondary), lineWidth: 1.5)
            }
        }

        // Nut
        if showNut {
            var nutPath = Path()
            nutPath.move(to: CGPoint(x: xOffset, y: gt))
            nutPath.addLine(to: CGPoint(x: xOffset + diagramWidth, y: gt))
            context.stroke(nutPath, with: .color(.primary), lineWidth: nutHeight)
        }

        // Horizontal fret lines
        for f in 0...fretRowCount {
            let y = gt + CGFloat(f) * fretSpacing
            var path = Path()
            path.move(to: CGPoint(x: xOffset, y: y))
            path.addLine(to: CGPoint(x: xOffset + diagramWidth, y: y))
            context.stroke(path, with: .color(.secondary.opacity(0.5)), lineWidth: 1)
        }

        // Vertical string lines
        for s in 0..<stringCount {
            let x = xOffset + CGFloat(s) * stringSpacing
            var path = Path()
            path.move(to: CGPoint(x: x, y: gt))
            path.addLine(to: CGPoint(x: x, y: gt + diagramHeight))
            context.stroke(path, with: .color(.secondary.opacity(0.6)), lineWidth: 1)
        }
    }
}

import SwiftUI
import shared

struct FretboardView: View {
    @ObservedObject var viewModel: FretboardViewModel
    var leftHanded: Bool = false
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var cellSize: CGFloat { sizeClass == .regular ? 64 : 48 }
    private let nutWidth: CGFloat = 4
    private let stringLineWidth: CGFloat = 1.5
    private let fretLineWidth: CGFloat = 1
    private let markerFrets: Set<Int> = [5, 7, 10]
    private let doubleMarkerFret = 12

    private var fretOrder: [Int] {
        let range = Array(0..<viewModel.fretCount)
        return leftHanded ? range.reversed() : range
    }

    var body: some View {
        VStack(spacing: 0) {
            fretNumbersRow
            HStack(spacing: 0) {
                stringLabelsColumn
                ScrollView(.horizontal, showsIndicators: false) {
                    fretboardGrid
                }
            }
            fretMarkersRow
        }
    }

    // MARK: - Fret Numbers

    private var fretNumbersRow: some View {
        HStack(spacing: 0) {
            Color.clear.frame(width: 32, height: 20)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(fretOrder, id: \.self) { fret in
                        fretNumberLabel(fret: fret)
                    }
                }
            }
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private func fretNumberLabel(fret: Int) -> some View {
        let isCapo = viewModel.capoFret > 0 && fret == viewModel.capoFret
        let isBlocked = viewModel.capoFret > 0 && fret >= 1 && fret < viewModel.capoFret

        if fret == 0 {
            Text("")
                .font(.caption2)
                .frame(width: cellSize, height: 20)
        } else if isCapo {
            Text("\(fret)")
                .font(.caption2.bold())
                .foregroundStyle(.white)
                .padding(.horizontal, 4)
                .background(Color.orange, in: RoundedRectangle(cornerRadius: 4))
                .frame(width: cellSize, height: 20)
        } else {
            Text("\(fret)")
                .font(.caption2)
                .foregroundStyle(isBlocked ? .quaternary : .secondary)
                .frame(width: cellSize, height: 20)
        }
    }

    // MARK: - String Labels

    private var stringLabelsColumn: some View {
        VStack(spacing: 0) {
            ForEach(Array((0..<FretboardViewModel.stringCount).reversed()), id: \.self) { stringIndex in
                Text(viewModel.tuning[stringIndex].name)
                    .font(.caption.bold())
                    .frame(width: 32, height: cellSize)
            }
        }
        .accessibilityHidden(true)
    }

    // MARK: - Fretboard Grid

    private var fretboardGrid: some View {
        ZStack(alignment: .topLeading) {
            Canvas { context, size in
                drawFretboard(context: context, size: size)
            }
            .frame(
                width: CGFloat(viewModel.fretCount) * cellSize,
                height: CGFloat(FretboardViewModel.stringCount) * cellSize
            )
            .accessibilityHidden(true)

            VStack(spacing: 0) {
                ForEach(Array((0..<FretboardViewModel.stringCount).reversed()), id: \.self) { stringIndex in
                    HStack(spacing: 0) {
                        ForEach(fretOrder, id: \.self) { fret in
                            let noteInfo = viewModel.getNoteAt(stringIndex: stringIndex, fret: fret)
                            let isBlockedByCapo = viewModel.capoFret > 0 && fret >= 1 && fret <= viewModel.capoFret
                            let isCapoFret = viewModel.capoFret > 0 && fret == viewModel.capoFret
                            let inPositionRange = viewModel.scaleOverlay.positionFretRange.map {
                                $0.contains(fret)
                            } ?? true
                            let inScale = viewModel.scaleOverlay.enabled
                                && viewModel.scaleOverlay.scaleNotes.contains(noteInfo.pitchClass)
                                && inPositionRange
                            let isScaleRoot = viewModel.scaleOverlay.enabled
                                && noteInfo.pitchClass == viewModel.scaleOverlay.root

                            FretCellView(
                                stringIndex: stringIndex,
                                fret: fret,
                                isSelected: viewModel.selections[stringIndex] == fret,
                                showNoteName: viewModel.showNoteNames,
                                noteName: noteInfo.name,
                                stringName: viewModel.tuning[stringIndex].name,
                                cellSize: cellSize,
                                isBlockedByCapo: isBlockedByCapo,
                                isCapoFret: isCapoFret,
                                isInScale: inScale,
                                isScaleRoot: isScaleRoot,
                                onTap: { viewModel.toggleFret(stringIndex: stringIndex, fret: fret) }
                            )
                        }
                    }
                }
            }
        }
    }

    // MARK: - Fret Markers

    private var fretMarkersRow: some View {
        HStack(spacing: 0) {
            Color.clear.frame(width: 32, height: 16)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(fretOrder, id: \.self) { fret in
                        Group {
                            if fret == doubleMarkerFret {
                                HStack(spacing: 2) {
                                    Circle().fill(.tertiary).frame(width: 5, height: 5)
                                    Circle().fill(.tertiary).frame(width: 5, height: 5)
                                }
                            } else if markerFrets.contains(fret) {
                                Circle().fill(.tertiary).frame(width: 6, height: 6)
                            } else {
                                Color.clear
                            }
                        }
                        .frame(width: cellSize, height: 16)
                    }
                }
            }
        }
        .accessibilityHidden(true)
    }

    // MARK: - Canvas Drawing

    private func drawFretboard(context: GraphicsContext, size: CGSize) {
        let strings = FretboardViewModel.stringCount
        let frets = viewModel.fretCount

        for s in 0..<strings {
            let y = CGFloat(s) * cellSize + cellSize / 2
            var path = Path()
            path.move(to: CGPoint(x: 0, y: y))
            path.addLine(to: CGPoint(x: size.width, y: y))
            context.stroke(path, with: .color(.secondary.opacity(0.4)), lineWidth: stringLineWidth)
        }

        for f in 0..<frets {
            let displayIndex = leftHanded ? (frets - 1 - f) : f
            let x = CGFloat(displayIndex + 1) * cellSize
            let lineWidth: CGFloat = f == 0 ? nutWidth : fretLineWidth
            var path = Path()
            path.move(to: CGPoint(x: x, y: 0))
            path.addLine(to: CGPoint(x: x, y: size.height))
            context.stroke(
                path,
                with: .color(f == 0 ? .primary.opacity(0.6) : .secondary.opacity(0.3)),
                lineWidth: lineWidth
            )
        }

        // Capo bar — gold bar across each capo fret column
        if viewModel.capoFret > 0 {
            let capoDisplayIndex = leftHanded
                ? fretOrder.firstIndex(of: viewModel.capoFret)!
                : viewModel.capoFret
            let capoX = CGFloat(capoDisplayIndex) * cellSize + cellSize / 2
            let barHeight: CGFloat = 6
            for s in 0..<strings {
                let y = CGFloat(s) * cellSize + cellSize / 2 - barHeight / 2
                let rect = CGRect(x: capoX - cellSize / 2, y: y, width: cellSize, height: barHeight)
                context.fill(
                    Path(roundedRect: rect, cornerRadius: barHeight / 2),
                    with: .color(.orange.opacity(0.7))
                )
            }

            // Dim blocked frets
            for f in 1..<viewModel.capoFret {
                let displayIdx = leftHanded ? fretOrder.firstIndex(of: f)! : f
                let x = CGFloat(displayIdx) * cellSize
                let rect = CGRect(x: x, y: 0, width: cellSize, height: size.height)
                context.fill(Path(rect), with: .color(.primary.opacity(0.05)))
            }
        }
    }
}

// MARK: - Fret Cell

struct FretCellView: View {
    let stringIndex: Int
    let fret: Int
    let isSelected: Bool
    let showNoteName: Bool
    let noteName: String
    let stringName: String
    let cellSize: CGFloat
    var isBlockedByCapo: Bool = false
    var isCapoFret: Bool = false
    var isInScale: Bool = false
    var isScaleRoot: Bool = false
    let onTap: () -> Void

    private let scaleDotSize: CGFloat = 24
    private let scaleColor = Color.blue.opacity(0.6)
    private let scaleRootColor = Color.green.opacity(0.8)

    var body: some View {
        Button(action: onTap) {
            ZStack {
                if isBlockedByCapo && !isSelected {
                    Color.clear
                } else if isSelected {
                    Circle()
                        .fill(Color.accentColor)
                        .frame(width: 32, height: 32)
                    Text(noteName)
                        .font(.caption2.bold())
                        .foregroundStyle(.white)
                } else if fret == 0 {
                    Circle()
                        .strokeBorder(Color.accentColor, lineWidth: 2)
                        .frame(width: 28, height: 28)
                    if showNoteName {
                        Text(noteName)
                            .font(.caption2)
                            .foregroundStyle(Color.accentColor)
                    }
                } else if isInScale {
                    Circle()
                        .fill(isScaleRoot ? scaleRootColor : scaleColor)
                        .frame(width: scaleDotSize, height: scaleDotSize)
                    if showNoteName {
                        Text(noteName)
                            .font(.system(size: 9).bold())
                            .foregroundStyle(.white.opacity(0.9))
                    }
                } else if showNoteName {
                    Text(noteName)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
            .frame(width: cellSize, height: cellSize)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(isBlockedByCapo)
        .accessibilityLabel(accessibilityText)
        .accessibilityHint(isBlockedByCapo ? "Blocked by capo" : "Double tap to select or deselect")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private var accessibilityText: String {
        var text = "\(stringName) string, fret \(fret), \(noteName)"
        if isBlockedByCapo { text += ", blocked by capo" }
        if isInScale { text += isScaleRoot ? ", scale root" : ", in scale" }
        return text
    }
}

// MARK: - Capo Selector

struct CapoSelectorView: View {
    let capoFret: Int
    let lastFret: Int
    let onCapoChange: (Int) -> Void

    var body: some View {
        HStack {
            Text("Capo")
                .font(.subheadline.bold())
                .accessibilityAddTraits(.isHeader)

            Spacer()

            Button(action: { onCapoChange(max(0, capoFret - 1)) }) {
                Image(systemName: "minus.circle")
            }
            .disabled(capoFret <= 0)
            .accessibilityLabel("Decrease capo")

            Text(capoFret == 0 ? "No capo" : "Fret \(capoFret)")
                .font(.subheadline)
                .frame(minWidth: 70)
                .accessibilityValue(capoFret == 0 ? "No capo" : "Fret \(capoFret)")

            Button(action: { onCapoChange(min(lastFret, capoFret + 1)) }) {
                Image(systemName: "plus.circle")
            }
            .disabled(capoFret >= lastFret)
            .accessibilityLabel("Increase capo")
        }
        .padding(.vertical, 4)
    }
}

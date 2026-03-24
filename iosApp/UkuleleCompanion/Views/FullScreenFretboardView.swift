import SwiftUI
import shared

struct FullScreenFretboardView: View {
    @ObservedObject var fretboardVM: FretboardViewModel
    var leftHanded: Bool = false
    @Environment(\.dismiss) private var dismiss
    @State private var showOverlay = true

    private let autoHideDelay: TimeInterval = 4.0
    private var fretCount: Int { FretboardViewModel.fretCount }
    private let stringNames = ["G", "C", "E", "A"]

    private var fretOrder: [Int] {
        let range = Array(0..<fretCount)
        return leftHanded ? range.reversed() : range
    }

    var body: some View {
        GeometryReader { geo in
            let labelWidth: CGFloat = 28
            let cellW = (geo.size.width - labelWidth) / CGFloat(fretCount)
            let cellH = geo.size.height / 5

            ZStack {
                Color.black.ignoresSafeArea()

                VStack(spacing: 0) {
                    fretNumberRow(cellW: cellW, labelWidth: labelWidth)

                    ForEach(Array((0..<4).reversed()), id: \.self) { stringIdx in
                        stringRow(stringIdx: stringIdx, cellW: cellW, cellH: cellH, labelWidth: labelWidth)
                    }
                }

                if showOverlay {
                    overlayBar
                }
            }
            .onTapGesture { showOverlayBriefly() }
        }
        .statusBarHidden(true)
        .onAppear { scheduleOverlayHide() }
    }

    // MARK: - Fret Numbers

    private func fretNumberRow(cellW: CGFloat, labelWidth: CGFloat) -> some View {
        HStack(spacing: 0) {
            Color.clear.frame(width: labelWidth, height: 18)
            ForEach(fretOrder, id: \.self) { fret in
                Text("\(fret)")
                    .font(.system(size: 9).monospacedDigit())
                    .foregroundStyle(.gray)
                    .frame(width: cellW, height: 18)
            }
        }
    }

    // MARK: - String Row

    private func stringRow(stringIdx: Int, cellW: CGFloat, cellH: CGFloat, labelWidth: CGFloat) -> some View {
        HStack(spacing: 0) {
            Text(stringNames[stringIdx])
                .font(.caption2.bold())
                .foregroundStyle(.white)
                .frame(width: labelWidth)

            ForEach(fretOrder, id: \.self) { fret in
                fretCell(stringIdx: stringIdx, fret: fret, cellW: cellW, cellH: cellH)
            }
        }
    }

    // MARK: - Fret Cell

    private func fretCell(stringIdx: Int, fret: Int, cellW: CGFloat, cellH: CGFloat) -> some View {
        let isSelected = fretboardVM.selections[stringIdx] == fret
        let noteInfo = fretboardVM.getNoteAt(stringIndex: stringIdx, fret: fret)
        let isCapoBlocked = fret > 0 && fret < fretboardVM.capoFret

        return Rectangle()
            .fill(cellColor(isSelected: isSelected, isCapoBlocked: isCapoBlocked))
            .frame(width: cellW, height: cellH)
            .overlay {
                VStack(spacing: 1) {
                    Text(noteInfo.name)
                        .font(.system(size: min(cellW * 0.35, 14), weight: .semibold))
                        .foregroundStyle(isSelected ? .white : .gray)
                    if fret == 0 {
                        Text("open")
                            .font(.system(size: 7))
                            .foregroundStyle(.gray.opacity(0.6))
                    }
                }
            }
            .border(Color.gray.opacity(0.3), width: 0.5)
            .onTapGesture {
                if !isCapoBlocked {
                    fretboardVM.toggleFret(stringIndex: stringIdx, fret: fret)
                }
                showOverlayBriefly()
            }
            .accessibilityLabel("\(stringNames[stringIdx]) string, fret \(fret), \(noteInfo.name)")
            .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    // MARK: - Overlay Bar

    private var overlayBar: some View {
        VStack {
            HStack(spacing: 16) {
                chordLabel
                Spacer()
                Button { fretboardVM.playChord() } label: {
                    Image(systemName: "play.fill")
                        .font(.title3)
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Play chord")

                Button { fretboardVM.clearAll() } label: {
                    Image(systemName: "arrow.counterclockwise")
                        .font(.title3)
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Reset fretboard")

                Button { dismiss() } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Close full screen")
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(.black.opacity(0.7))
            Spacer()
        }
    }

    // MARK: - Chord Label

    @ViewBuilder
    private var chordLabel: some View {
        switch fretboardVM.detectionResult {
        case .chordFound(let name, let quality, _, _, _, _, _):
            HStack(spacing: 6) {
                Text(name).font(.title2.bold()).foregroundStyle(.white)
                Text(quality).font(.caption).foregroundStyle(.gray)
            }
        case .singleNote(let name, _):
            Text(name).font(.title2.bold()).foregroundStyle(.white)
        default:
            Text("Tap frets").font(.subheadline).foregroundStyle(.gray)
        }
    }

    // MARK: - Helpers

    private func cellColor(isSelected: Bool, isCapoBlocked: Bool) -> Color {
        if isCapoBlocked { return Color.gray.opacity(0.15) }
        if isSelected { return .accentColor }
        return Color.white.opacity(0.05)
    }

    private func showOverlayBriefly() {
        showOverlay = true
        scheduleOverlayHide()
    }

    private func scheduleOverlayHide() {
        DispatchQueue.main.asyncAfter(deadline: .now() + autoHideDelay) {
            showOverlay = false
        }
    }
}

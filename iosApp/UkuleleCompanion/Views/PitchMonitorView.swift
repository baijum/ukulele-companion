import SwiftUI

private let canvasBackground = Color(red: 0.1, green: 0.125, blue: 0.19)
private let gridLineColor = Color(red: 0.545, green: 0.545, blue: 0.227)
private let cLineColor = Color(red: 0.667, green: 0.267, blue: 0.267)
private let pitchTraceColor = Color(red: 0.267, green: 0.6, blue: 1.0)
private let labelColor = Color(red: 0.667, green: 0.667, blue: 0.376)
private let chromaGlowColor = Color(red: 1.0, green: 0.667, blue: 0.2)

private let minMidi: Float = 48
private let maxMidi: Float = 84
private let timeWindowMs: Int64 = 8_000
private let labelMargin: CGFloat = 60

private let noteLabels: [Int: String] = [
    48: "C3", 50: "D3", 52: "E3", 53: "F3", 55: "G3", 57: "A3", 59: "B3",
    60: "C4", 62: "D4", 64: "E4", 65: "F4", 67: "G4", 69: "A4", 71: "B4",
    72: "C5", 74: "D5", 76: "E5", 77: "F5", 79: "G5", 81: "A5", 83: "B5",
    84: "C6"
]
private let cNotes: Set<Int> = [48, 60, 72, 84]

struct PitchMonitorView: View {
    @StateObject private var viewModel = PitchMonitorViewModel()
    @EnvironmentObject var settings: SettingsViewModel
    @State private var currentTimeMs: Int64 = 0

    private let displayLink = DisplayLinkTimer()

    var body: some View {
        VStack(spacing: 8) {
            // Top row: recent notes + detected chord
            HStack {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(Array(viewModel.recentNotes.enumerated()), id: \.offset) { index, note in
                            let isLatest = index == viewModel.recentNotes.count - 1
                            Text(note)
                                .font(.caption.bold())
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(isLatest ? Color.accentColor : Color(.secondarySystemGroupedBackground))
                                .foregroundColor(isLatest ? .white : .primary)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                }

                if let chord = viewModel.detectedChord {
                    VStack(spacing: 2) {
                        Text(chord)
                            .font(.title2.bold())
                        if viewModel.isArpeggioChord {
                            Text("arpeggio")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .background(Color.accentColor.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .accessibilityLabel("Detected chord")
                    .accessibilityValue(viewModel.isArpeggioChord ? "\(chord), arpeggio" : chord)
                    .accessibilityAddTraits(.updatesFrequently)
                } else {
                    Text("--")
                        .font(.title2)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .accessibilityLabel("Detected chord")
                        .accessibilityValue("No chord detected")
                }
            }
            .frame(height: 56)
            .padding(.horizontal)

            // Canvas
            Canvas { context, size in
                drawPitchCanvas(context: context, size: size,
                                pitchHistory: viewModel.pitchHistory,
                                currentTimeMs: currentTimeMs,
                                chromaEnergy: viewModel.chromaEnergy)
            }
            .background(canvasBackground)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(.horizontal)
            .accessibilityCombined(
                label: "Pitch history graph",
                value: viewModel.currentNote.map { "Current note: \($0)" } ?? "No pitch detected"
            )

            // Start/Stop
            Button(action: { viewModel.toggleCapture() }) {
                Label(
                    viewModel.isListening ? "Stop" : "Start Listening",
                    systemImage: viewModel.isListening ? "mic.slash.fill" : "mic.fill"
                )
                .font(.title3)
                .frame(maxWidth: .infinity)
                .padding()
                .background(viewModel.isListening ? Color.red : Color.accentColor)
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 16)
            .accessibilityHint("Double tap to start or stop listening")
        }
        .navigationTitle("Pitch Monitor")
        .onAppear {
            viewModel.noiseGateRms = Self.filteringToRms(settings.noiseGateFiltering)
            displayLink.start { [weak viewModel] in
                guard let vm = viewModel, vm.isListening else { return }
                currentTimeMs = Int64(Date().timeIntervalSince1970 * 1000)
            }
        }
        .onChange(of: settings.noiseGateFiltering) { _, newVal in
            viewModel.noiseGateRms = Self.filteringToRms(newVal)
        }
        .onDisappear {
            displayLink.stop()
            viewModel.stopCapture()
        }
    }

    private static func filteringToRms(_ filtering: Float) -> Float {
        0.002 + filtering * 0.051
    }
}

// MARK: - Drawing

private func drawPitchCanvas(context: GraphicsContext, size: CGSize,
                              pitchHistory: [PitchPoint], currentTimeMs: Int64,
                              chromaEnergy: [Float]) {
    let plotLeft = labelMargin
    let plotWidth = size.width - plotLeft
    let plotTop: CGFloat = 16
    let plotBottom = size.height - 12
    let plotHeight = plotBottom - plotTop
    let midiRange = maxMidi - minMidi

    // Grid lines and labels
    for midi in Int(minMidi)...Int(maxMidi) {
        let fraction = CGFloat(Float(midi) - minMidi) / CGFloat(midiRange)
        let y = plotBottom - fraction * plotHeight
        let isC = cNotes.contains(midi)
        let isNatural = noteLabels[midi] != nil

        context.stroke(
            Path { p in
                p.move(to: CGPoint(x: plotLeft, y: y))
                p.addLine(to: CGPoint(x: plotLeft + plotWidth, y: y))
            },
            with: .color(isC ? cLineColor : gridLineColor),
            lineWidth: isC ? 2 : (isNatural ? 1.2 : 0.4)
        )

        if let label = noteLabels[midi] {
            context.draw(
                Text(label).font(.system(size: 10)).foregroundColor(isC ? cLineColor : labelColor),
                at: CGPoint(x: 4, y: y),
                anchor: .leading
            )
        }
    }

    // Chroma glow
    let bandHeight = plotHeight / CGFloat(midiRange)
    for midi in Int(minMidi)...Int(maxMidi) {
        let pc = midi % 12
        let energy = pc < chromaEnergy.count ? chromaEnergy[pc] : 0
        guard energy > 0.10 else { continue }
        let fraction = CGFloat(Float(midi) - minMidi) / CGFloat(midiRange)
        let y = plotBottom - fraction * plotHeight
        let alpha = min(Double(energy) * 2.5, 0.4)
        context.fill(
            Path(CGRect(x: plotLeft, y: y - bandHeight / 2, width: plotWidth, height: bandHeight)),
            with: .color(chromaGlowColor.opacity(alpha))
        )
    }

    // Pitch trace
    let timeStart = currentTimeMs - timeWindowMs
    let visible = pitchHistory.filter { $0.timestampMs >= timeStart }
    guard !visible.isEmpty else { return }

    var path = Path()
    var pathStarted = false

    for point in visible {
        guard let midi = point.midiNote else {
            if pathStarted {
                context.stroke(path, with: .color(pitchTraceColor), lineWidth: 3)
                path = Path()
                pathStarted = false
            }
            continue
        }

        let timeFraction = CGFloat(point.timestampMs - timeStart) / CGFloat(timeWindowMs)
        let x = plotLeft + timeFraction * plotWidth
        let clamped = min(maxMidi, max(minMidi, midi))
        let midiFraction = CGFloat(clamped - minMidi) / CGFloat(midiRange)
        let y = plotBottom - midiFraction * plotHeight

        if !pathStarted {
            path.move(to: CGPoint(x: x, y: y))
            pathStarted = true
        } else {
            path.addLine(to: CGPoint(x: x, y: y))
        }
    }

    if pathStarted {
        context.stroke(path, with: .color(pitchTraceColor), style: StrokeStyle(lineWidth: 3, lineCap: .round))
    }

    // Current position dot
    if let last = visible.last(where: { $0.midiNote != nil }), let midi = last.midiNote {
        let timeFraction = CGFloat(last.timestampMs - timeStart) / CGFloat(timeWindowMs)
        let x = plotLeft + timeFraction * plotWidth
        let clamped = min(maxMidi, max(minMidi, midi))
        let midiFraction = CGFloat(clamped - minMidi) / CGFloat(midiRange)
        let y = plotBottom - midiFraction * plotHeight
        context.fill(Path(ellipseIn: CGRect(x: x - 6, y: y - 6, width: 12, height: 12)),
                     with: .color(pitchTraceColor))
    }
}

// MARK: - Display Link Timer

final class DisplayLinkTimer {
    private var displayLink: CADisplayLink?
    private var callback: (() -> Void)?

    func start(callback: @escaping () -> Void) {
        self.callback = callback
        let link = CADisplayLink(target: self, selector: #selector(tick))
        link.preferredFrameRateRange = CAFrameRateRange(minimum: 15, maximum: 30)
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    func stop() {
        displayLink?.invalidate()
        displayLink = nil
    }

    @objc private func tick() {
        callback?()
    }
}

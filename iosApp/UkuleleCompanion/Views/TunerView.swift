import SwiftUI
import shared

struct TunerView: View {
    @StateObject private var viewModel = TunerViewModel()
    @EnvironmentObject var settings: SettingsViewModel

    private var tuning: UkuleleTuning {
        UkuleleTuning.entries.first { $0.label == settings.selectedTuning } ?? .highG
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                // Tuning label with optional A4 reference
                tuningLabel

                // Neural status badge
                neuralBadge

                // Note display
                VStack {
                    Text(viewModel.noteName)
                        .font(.system(size: 96, weight: .bold, design: .rounded))
                        .foregroundStyle(viewModel.noteColor)

                    if let octave = viewModel.octave {
                        Text("Octave \(octave)")
                            .font(.title3)
                            .foregroundStyle(.secondary)
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(viewModel.noteAccessibilityLabel)
                .accessibilityAddTraits(.updatesFrequently)

                // Cents deviation gauge
                NeedleMeterView(cents: viewModel.displayCentsDeviation)
                    .frame(height: 100)
                    .padding(.horizontal, 24)
                    .accessibilityValue(viewModel.centsAccessibilityValue)

                // Frequency display
                if let freq = viewModel.frequency {
                    Text(String(format: "%.1f Hz", freq))
                        .font(.title2.monospacedDigit())
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("Frequency")
                        .accessibilityValue(String(format: "%.1f hertz", freq))
                }

                // String match
                if let stringMatch = viewModel.stringMatch {
                    VStack(spacing: 4) {
                        Text("String: \(stringMatch)")
                            .font(.title3)
                        Text(viewModel.tuningStatus)
                            .font(.subheadline)
                            .foregroundStyle(viewModel.noteColor)
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("String \(stringMatch), \(viewModel.tuningStatus)")
                }

                // String reference buttons
                stringButtonsRow
                    .padding(.horizontal, 24)

                Spacer()

                // Start/Stop button
                Button(action: { viewModel.toggleCapture() }) {
                    Label(
                        viewModel.isCapturing ? "Stop Tuning" : "Start Tuning",
                        systemImage: viewModel.isCapturing ? "stop.circle.fill" : "mic.circle.fill"
                    )
                    .font(.title2)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(viewModel.isCapturing ? Color.red : Color.accentColor)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 32)
                .accessibilityHint("Double tap to start or stop the tuner")
            }
            .navigationTitle("Tuner")
        }
        .onAppear {
            viewModel.noiseGateRms = Self.filteringToRms(settings.noiseGateFiltering)
            viewModel.spokenFeedback = settings.spokenFeedback
            viewModel.a4Reference = Double(settings.a4Reference)
            viewModel.precisionMode = settings.precisionMode
            viewModel.currentTuning = tuning
            if settings.autoStartTuner && !viewModel.isCapturing {
                viewModel.toggleCapture()
            }
        }
        .onChange(of: settings.noiseGateFiltering) { newVal in
            viewModel.noiseGateRms = Self.filteringToRms(newVal)
        }
        .onChange(of: settings.spokenFeedback) { newVal in
            viewModel.spokenFeedback = newVal
        }
        .onChange(of: settings.a4Reference) { newVal in
            viewModel.a4Reference = Double(newVal)
        }
        .onChange(of: settings.precisionMode) { newVal in
            viewModel.precisionMode = newVal
        }
        .onChange(of: settings.selectedTuning) { _ in
            viewModel.currentTuning = tuning
        }
        .onChange(of: viewModel.isInTune) { inTune in
            if inTune {
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            }
        }
        .onDisappear {
            viewModel.stopCapture()
        }
    }

    private static func filteringToRms(_ filtering: Float) -> Float {
        0.002 + filtering * 0.051
    }

    private var tuningLabel: some View {
        let stringNames = tuning.stringNameArray
        let a4 = settings.a4Reference
        let a4Text = a4 != 440.0 ? String(format: " (A4=%.1f Hz)", a4) : ""
        return Text("\(tuning.label) — \(stringNames.joined(separator: " "))\(a4Text)")
            .font(.subheadline)
            .foregroundStyle(.secondary)
    }

    private var neuralBadge: some View {
        Group {
            switch viewModel.neuralStatus {
            case .loading:
                Text("SwiftF0 Loading…")
                    .font(.caption2.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.secondary.opacity(0.15))
                    .foregroundStyle(.secondary)
                    .clipShape(Capsule())
                    .accessibilityLabel("Neural pitch detection loading")
            case .active:
                Text("SwiftF0 Active")
                    .font(.caption2.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.green.opacity(0.15))
                    .foregroundStyle(.green)
                    .clipShape(Capsule())
                    .accessibilityLabel("Neural pitch detection active")
            case .fallback:
                Text("SwiftF0 Fallback")
                    .font(.caption2.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.red.opacity(0.15))
                    .foregroundStyle(.red)
                    .clipShape(Capsule())
                    .accessibilityLabel("Neural pitch detection fallback mode")
            }
        }
    }

    // MARK: - String Reference Buttons

    private var stringButtonsRow: some View {
        let count = Int(tuning.stringNames.count)
        let stringNames = tuning.stringNameArray
        let pitchClasses = tuning.pitchClassInts

        return HStack(spacing: 12) {
            ForEach(0..<count, id: \.self) { idx in
                let isActive = viewModel.activeStringIndex == idx
                let isTuned = viewModel.stringProgress[idx]
                let isAutoTarget = settings.autoAdvance && viewModel.autoAdvanceTarget == idx

                StringButton(
                    label: stringNames[idx],
                    isActive: isActive,
                    isTuned: isTuned,
                    isAutoAdvanceTarget: isAutoTarget,
                    onTap: {
                        if settings.soundEnabled {
                            viewModel.tonePlayer.playNote(pitchClass: pitchClasses[idx])
                        }
                    }
                )
            }
        }
    }
}

// MARK: - String Button

struct StringButton: View {
    let label: String
    let isActive: Bool
    let isTuned: Bool
    let isAutoAdvanceTarget: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                ZStack {
                    Circle()
                        .fill(backgroundColor)
                        .frame(width: 50, height: 50)

                    if isAutoAdvanceTarget && !isTuned {
                        Circle()
                            .strokeBorder(Color.accentColor, lineWidth: 2)
                            .frame(width: 54, height: 54)
                    }

                    if isTuned {
                        Image(systemName: "checkmark")
                            .font(.title2.bold())
                            .foregroundStyle(.white)
                    } else {
                        Text(label)
                            .font(.title3.bold())
                            .foregroundStyle(isActive ? .white : .primary)
                    }
                }

                Text(label)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .opacity(isTuned ? 1 : 0.7)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityText)
        .accessibilityHint("Double tap to play reference tone")
    }

    private var backgroundColor: Color {
        if isTuned { return .green }
        if isActive { return .accentColor }
        return Color.secondary.opacity(0.15)
    }

    private var accessibilityText: String {
        var text = "\(label) string"
        if isTuned { text += ", tuned" }
        if isActive { text += ", currently detected" }
        if isAutoAdvanceTarget { text += ", next target" }
        return text
    }
}

// MARK: - Needle Meter (Semicircular)

struct NeedleMeterView: View {
    let cents: Double
    private let maxCents: Double = 50

    var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height
            let centerX = w / 2
            let centerY = h - 8
            let radius = min(w / 2 - 16, h - 16)

            // Background arc
            var bgArc = Path()
            bgArc.addArc(center: CGPoint(x: centerX, y: centerY),
                         radius: radius,
                         startAngle: .degrees(180), endAngle: .degrees(0),
                         clockwise: false)
            context.stroke(bgArc, with: .color(.gray.opacity(0.3)), lineWidth: 4)

            // In-tune zone (-6..+6 cents)
            let zoneStart = Angle.degrees(180 - (-6 + 50) / 100 * 180)
            let zoneEnd = Angle.degrees(180 - (6 + 50) / 100 * 180)
            var zonePath = Path()
            zonePath.addArc(center: CGPoint(x: centerX, y: centerY),
                            radius: radius,
                            startAngle: zoneStart, endAngle: zoneEnd,
                            clockwise: false)
            context.stroke(zonePath, with: .color(.green.opacity(0.5)), lineWidth: 6)

            // Tick marks at -50, -25, 0, +25, +50
            for tickCents in stride(from: -50.0, through: 50.0, by: 25.0) {
                let angle = Double.pi - (tickCents + 50) / 100 * Double.pi
                let innerR = radius - 8
                let outerR = radius + 4
                let cos = Foundation.cos(angle)
                let sin = Foundation.sin(angle)
                var tick = Path()
                tick.move(to: CGPoint(x: centerX + cos * innerR, y: centerY - sin * innerR))
                tick.addLine(to: CGPoint(x: centerX + cos * outerR, y: centerY - sin * outerR))
                context.stroke(tick, with: .color(.secondary), lineWidth: tickCents == 0 ? 2 : 1)

                let labelR = radius + 14
                let labelStr = tickCents == 0 ? "0" : (tickCents > 0 ? "+\(Int(tickCents))" : "\(Int(tickCents))")
                let text = Text(labelStr).font(.system(size: 9)).foregroundColor(.secondary)
                context.draw(context.resolve(text),
                             at: CGPoint(x: centerX + cos * labelR, y: centerY - sin * labelR),
                             anchor: .center)
            }

            // Needle
            let clampedCents = max(-maxCents, min(maxCents, cents))
            let needleAngle = Double.pi - (clampedCents + 50) / 100 * Double.pi
            let needleLen = radius - 12
            let tipX = centerX + Foundation.cos(needleAngle) * needleLen
            let tipY = centerY - Foundation.sin(needleAngle) * needleLen

            var needle = Path()
            needle.move(to: CGPoint(x: centerX, y: centerY))
            needle.addLine(to: CGPoint(x: tipX, y: tipY))
            context.stroke(needle, with: .color(needleColor), lineWidth: 2.5)

            // Pivot dot
            let pivotRect = CGRect(x: centerX - 5, y: centerY - 5, width: 10, height: 10)
            context.fill(Path(ellipseIn: pivotRect), with: .color(needleColor))
        }
        .accessibilityElement()
        .accessibilityLabel(meterAccessibilityLabel)
        .accessibilityValue(meterZone)
        .accessibilityAddTraits(.updatesFrequently)
    }

    private var needleColor: Color {
        let absCents = abs(cents)
        if absCents <= 6 { return .green }
        if absCents <= 15 { return .yellow }
        return .red
    }

    private var meterAccessibilityLabel: String {
        let absCents = abs(cents)
        if absCents <= 6 { return "Tuning meter, in tune" }
        let direction = cents > 0 ? "sharp" : "flat"
        return String(format: "Tuning meter, %.0f cents %@", absCents, direction)
    }

    private var meterZone: String {
        let absCents = abs(cents)
        if absCents <= 6 { return "In tune zone" }
        if absCents <= 15 { return "Close zone" }
        return cents > 0 ? "Sharp zone" : "Flat zone"
    }
}

struct TunerView_Previews: PreviewProvider {
    static var previews: some View { TunerView() }
}

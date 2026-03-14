import SwiftUI
import AVFoundation
import Foundation
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
                NeedleMeterView(cents: viewModel.centsDeviation)
                    .frame(height: 100)
                    .padding(.horizontal, 24)

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
        .onChange(of: settings.selectedTuning) { _ in
            viewModel.currentTuning = tuning
        }
        .onDisappear {
            viewModel.stopCapture()
        }
    }

    private static func filteringToRms(_ filtering: Float) -> Float {
        0.002 + filtering * 0.051
    }

    private var tuningLabel: some View {
        let stringNames = (0..<Int(tuning.stringNames.count)).map { tuning.stringNames[$0] as! String }
        let a4 = settings.a4Reference
        let a4Text = a4 != 440.0 ? String(format: " (A4=%.1f Hz)", a4) : ""
        return Text("\(tuning.label) — \(stringNames.joined(separator: " "))\(a4Text)")
            .font(.subheadline)
            .foregroundStyle(.secondary)
    }

    private var neuralBadge: some View {
        Group {
            switch viewModel.neuralStatus {
            case .active:
                Text("SwiftF0 Active")
                    .font(.caption2.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.green.opacity(0.15))
                    .foregroundStyle(.green)
                    .clipShape(Capsule())
            case .fallback:
                Text("SwiftF0 Fallback")
                    .font(.caption2.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.red.opacity(0.15))
                    .foregroundStyle(.red)
                    .clipShape(Capsule())
            }
        }
    }

    // MARK: - String Reference Buttons

    private var stringButtonsRow: some View {
        let count = Int(tuning.stringNames.count)
        let stringNames = (0..<count).map { tuning.stringNames[$0] as! String }
        let pitchClasses = (0..<count).map { (tuning.pitchClasses[$0] as! NSNumber).int32Value }

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
}

// MARK: - Neural Runtime Status

enum NeuralRuntimeStatus {
    case active, fallback
}

// MARK: - View Model

@MainActor
final class TunerViewModel: ObservableObject {
    @Published var noteName: String = "--"
    @Published var octave: Int?
    @Published var centsDeviation: Double = 0
    @Published var frequency: Double?
    @Published var stringMatch: String?
    @Published var tuningStatus: String = ""
    @Published var isCapturing: Bool = false
    @Published var neuralStatus: NeuralRuntimeStatus = .active

    // String progress tracking
    @Published var stringProgress: [Bool] = [false, false, false, false]
    @Published var activeStringIndex: Int? = nil
    @Published var autoAdvanceTarget: Int? = 0

    var noiseGateRms: Float = 0.005
    var spokenFeedback: Bool = false
    var a4Reference: Double = 440.0
    var currentTuning: UkuleleTuning = .highG

    private let audioEngine = AudioCaptureEngine()
    let tonePlayer = TonePlayer()
    private var previousFrequency: Double?
    private var settledFrames: Int = 0
    private static let settledThreshold = 8

    // TTS
    private let synthesizer = AVSpeechSynthesizer()
    private var lastTtsTime: Date = .distantPast
    private var lastTtsBucket: Int = Int.min
    private static let ttsNormalInterval: TimeInterval = 2.0
    private static let ttsInTuneInterval: TimeInterval = 3.0
    private static let ttsCentsBucketSize = 5

    // Neural pitch supervision
    private let neuralSupervisor: NeuralPitchSupervisor?
    private var neuralFrameCounter: Int = 0
    private var lastNeuralResult: NeuralPitchResult?
    private var neuralResultAgeFrames: Int = Int.max
    private var consecutiveNeuralFailures: Int = 0
    private var neuralConsistencyFrames: Int = 0
    private var lastNeuralFrequencyForConsistency: Double?

    private static let neuralSupervisorInterval = 5
    private static let neuralResultTTLFrames = 10
    private static let neuralFailureThreshold = 15
    private static let neuralConsistencyRequired = 2
    private static let arbitrationIgnoreSemitones = 1.5
    private static let arbitrationStrongSemitones = 2.5

    var noteAccessibilityLabel: String {
        if noteName == "--" { return "No note detected" }
        if let octave = octave {
            return "Detected note: \(noteName)\(octave)"
        }
        return "Detected note: \(noteName)"
    }

    var centsAccessibilityValue: String {
        let absCents = abs(centsDeviation)
        if noteName == "--" { return "No pitch detected" }
        if absCents <= 6 { return "In tune" }
        let direction = centsDeviation > 0 ? "sharp" : "flat"
        return String(format: "%.0f cents %@", absCents, direction)
    }

    private var lastAnnouncedNote: String = ""

    var noteColor: Color {
        let absCents = abs(centsDeviation)
        if noteName == "--" { return .primary }
        if absCents <= 6 { return .green }
        if absCents <= 15 { return .yellow }
        return .red
    }

    init() {
        neuralSupervisor = NeuralPitchSupervisor()
        audioEngine.onBuffer = { [weak self] samples in
            Task { @MainActor in
                self?.processAudioBuffer(samples)
            }
        }
    }

    func toggleCapture() {
        if isCapturing {
            stopCapture()
        } else {
            requestMicPermissionAndStart()
        }
    }

    func stopCapture() {
        audioEngine.stop()
        isCapturing = false
        resetDisplay()
    }

    private func requestMicPermissionAndStart() {
        AVAudioSession.sharedInstance().requestRecordPermission { [weak self] granted in
            DispatchQueue.main.async {
                if granted {
                    self?.startCapture()
                }
            }
        }
    }

    private func startCapture() {
        audioEngine.start()
        isCapturing = true
        previousFrequency = nil
    }

    private func processAudioBuffer(_ samples: [Float]) {
        let rms = sqrt(samples.reduce(0) { $0 + $1 * $1 } / Float(max(samples.count, 1)))
        if rms < noiseGateRms {
            return
        }

        // Convert [Float] to KotlinFloatArray for KMP interop
        let kotlinArray = KotlinFloatArray(size: Int32(samples.count))
        for i in 0..<samples.count {
            kotlinArray.set(index: Int32(i), value: samples[i])
        }

        let prevFreq = previousFrequency.map { KotlinDouble(value: $0) }

        let result = PitchDetector.shared.detect(
            samples: kotlinArray,
            sampleRate: 44100,
            threshold: PitchDetector.shared.DEFAULT_THRESHOLD,
            previousFrequency: prevFreq
        )

        // Run neural supervision
        let neuralResult = maybeRunNeural(samples)

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            if let result = result {
                // Apply arbitration between YIN and neural results
                let finalHz: Double
                if let neural = neuralResult {
                    let arbitrated = self.arbitrate(
                        yinFreq: result.frequencyHz,
                        yinConf: result.confidence,
                        neuralResult: neural
                    )
                    finalHz = arbitrated
                } else {
                    finalHz = result.frequencyHz
                }

                self.previousFrequency = finalHz

                if let noteInfo = TunerNoteMapper.shared.mapFrequency(hz: finalHz, a4Reference: self.a4Reference) {
                    let newNoteName = noteInfo.noteName
                    self.noteName = newNoteName
                    self.octave = Int(noteInfo.octave)
                    self.centsDeviation = noteInfo.centsDeviation
                    self.frequency = finalHz

                    // Announce note changes to VoiceOver
                    let noteWithOctave = "\(newNoteName)\(noteInfo.octave)"
                    if noteWithOctave != self.lastAnnouncedNote {
                        self.lastAnnouncedNote = noteWithOctave
                        AccessibilityAnnouncer.shared.announce(
                            "Detected \(newNoteName) \(noteInfo.octave)"
                        )
                    }

                    let stringResult = TunerNoteMapper.shared.findNearestString(
                        noteInfo: noteInfo,
                        tuning: self.currentTuning,
                        a4Reference: self.a4Reference
                    )
                    self.stringMatch = stringResult.stringName
                    let stringIdx = Int(stringResult.stringIndex)
                    self.activeStringIndex = stringIdx

                    let cents = stringResult.centsFromTarget
                    let justTuned: Bool
                    if abs(cents) <= 6 {
                        self.tuningStatus = "In tune!"
                        self.settledFrames += 1
                        if self.settledFrames >= Self.settledThreshold && !self.stringProgress[stringIdx] {
                            self.stringProgress[stringIdx] = true
                            justTuned = true
                            self.advanceToNextUntuned()
                        } else {
                            justTuned = false
                        }
                    } else {
                        justTuned = false
                        self.settledFrames = 0
                        if cents > 0 {
                            self.tuningStatus = String(format: "%.0f cents sharp", cents)
                        } else {
                            self.tuningStatus = String(format: "%.0f cents flat", abs(cents))
                        }
                    }

                    self.speakTunerState(
                        note: newNoteName,
                        cents: cents,
                        stringTuned: justTuned,
                        stringName: stringResult.stringName
                    )
                }
            } else {
                self.settledFrames = 0
            }
        }
    }

    // MARK: - Neural Pitch Supervision

    private func maybeRunNeural(_ samples: [Float]) -> NeuralPitchResult? {
        guard let supervisor = neuralSupervisor else { return nil }
        neuralFrameCounter += 1

        if neuralFrameCounter % Self.neuralSupervisorInterval == 0 {
            let estimate = supervisor.estimate(samples)
            lastNeuralResult = estimate
            neuralResultAgeFrames = 0
            if let estimate = estimate {
                consecutiveNeuralFailures = 0
                updateNeuralConsistency(estimate.frequencyHz)
                DispatchQueue.main.async { [weak self] in
                    self?.neuralStatus = .active
                }
            } else {
                consecutiveNeuralFailures += 1
                neuralConsistencyFrames = 0
                lastNeuralFrequencyForConsistency = nil
                if consecutiveNeuralFailures >= Self.neuralFailureThreshold {
                    DispatchQueue.main.async { [weak self] in
                        self?.neuralStatus = .fallback
                    }
                }
            }
        } else if neuralResultAgeFrames < Int.max {
            neuralResultAgeFrames += 1
        }

        if neuralResultAgeFrames <= Self.neuralResultTTLFrames {
            return lastNeuralResult
        }
        return nil
    }

    private func arbitrate(yinFreq: Double, yinConf: Double, neuralResult: NeuralPitchResult) -> Double {
        let semitoneGap = semitoneDistance(yinFreq, neuralResult.frequencyHz)

        if semitoneGap <= Self.arbitrationIgnoreSemitones {
            return yinFreq
        }

        if neuralConsistencyFrames < Self.neuralConsistencyRequired {
            return yinFreq
        }

        if isOctaveRelation(yinFreq, neuralResult.frequencyHz)
            && neuralResult.confidence >= 0.85
            && yinConf >= 0.12 {
            return neuralResult.frequencyHz
        }

        if semitoneGap >= Self.arbitrationStrongSemitones
            && neuralResult.confidence >= 0.93
            && yinConf >= 0.16 {
            return neuralResult.frequencyHz
        }

        return yinFreq
    }

    private func semitoneDistance(_ aHz: Double, _ bHz: Double) -> Double {
        guard aHz > 0, bHz > 0 else { return Double.greatestFiniteMagnitude }
        return abs(12.0 * log2(aHz / bHz))
    }

    private func isOctaveRelation(_ aHz: Double, _ bHz: Double) -> Bool {
        guard aHz > 0, bHz > 0 else { return false }
        let semitones = semitoneDistance(aHz, bHz)
        return abs(semitones - 12.0) <= 1.0 || abs(semitones - 24.0) <= 1.0
    }

    private func updateNeuralConsistency(_ neuralFrequencyHz: Double) {
        guard neuralFrequencyHz > 0 else {
            neuralConsistencyFrames = 0
            lastNeuralFrequencyForConsistency = nil
            return
        }

        if let previous = lastNeuralFrequencyForConsistency,
           semitoneDistance(previous, neuralFrequencyHz) <= 0.5 {
            neuralConsistencyFrames += 1
        } else {
            neuralConsistencyFrames = 1
        }
        lastNeuralFrequencyForConsistency = neuralFrequencyHz
    }

    // MARK: - Spoken Feedback

    private func speakTunerState(note: String, cents: Double, stringTuned: Bool, stringName: String?) {
        guard spokenFeedback else { return }
        let now = Date()
        let isInTune = abs(cents) <= 6
        let interval = isInTune ? Self.ttsInTuneInterval : Self.ttsNormalInterval
        let bucket = Int(cents / Double(Self.ttsCentsBucketSize))

        guard now.timeIntervalSince(lastTtsTime) >= interval || bucket != lastTtsBucket else { return }

        lastTtsTime = now
        lastTtsBucket = bucket

        let message: String
        if stringTuned, let name = stringName {
            message = "\(name) string tuned!"
        } else if isInTune {
            message = "\(note), in tune"
        } else {
            let direction = cents > 0 ? "sharp" : "flat"
            message = String(format: "%@, %.0f cents %@", note, abs(cents), direction)
        }

        let utterance = AVSpeechUtterance(string: message)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 1.1
        utterance.volume = 0.8
        synthesizer.stopSpeaking(at: .immediate)
        synthesizer.speak(utterance)
    }

    private func advanceToNextUntuned() {
        for i in 0..<4 {
            if !stringProgress[i] {
                autoAdvanceTarget = i
                return
            }
        }
        autoAdvanceTarget = nil
    }

    func resetProgress() {
        stringProgress = [false, false, false, false]
        autoAdvanceTarget = 0
        settledFrames = 0
    }

    private func resetDisplay() {
        noteName = "--"
        octave = nil
        centsDeviation = 0
        frequency = nil
        stringMatch = nil
        tuningStatus = ""
        previousFrequency = nil
        activeStringIndex = nil
        settledFrames = 0
    }
}

struct TunerView_Previews: PreviewProvider {
    static var previews: some View { TunerView() }
}

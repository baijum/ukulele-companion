import AVFoundation
import Combine
import Foundation
import os
import SwiftUI
@preconcurrency import shared

enum NeuralRuntimeStatus {
    case loading, active, fallback
}

@MainActor
final class TunerViewModel: ObservableObject {
    @Published var noteName: String = "--"
    @Published var octave: Int?
    @Published var centsDeviation: Double = 0
    @Published var displayCentsDeviation: Double = 0
    @Published var frequency: Double?
    @Published var stringMatch: String?
    @Published var tuningStatus: String = ""
    @Published var isCapturing: Bool = false
    @Published var neuralStatus: NeuralRuntimeStatus = .loading
    @Published var isInTune: Bool = false

    // String progress tracking
    @Published var stringProgress: [Bool] = [false, false, false, false]
    @Published var activeStringIndex: Int? = nil
    @Published var autoAdvanceTarget: Int? = 0

    var noiseGateRms: Float = 0.005
    var spokenFeedback: Bool = false
    var a4Reference: Double = 440.0
    var currentTuning: UkuleleTuning = .highG
    /// When on, the in-tune window tightens from ±6 to ±2 cents (mirrors
    /// Android's precision mode). Set from SettingsViewModel like a4Reference.
    var precisionMode: Bool = false

    // In-tune window (cents). Mirrors Android IN_TUNE_CENTS / PRECISION_IN_TUNE_CENTS.
    private static let inTuneCents = 6.0
    private static let precisionInTuneCents = 2.0
    private var effectiveInTuneCents: Double {
        precisionMode ? Self.precisionInTuneCents : Self.inTuneCents
    }

    private let audioEngine = AudioCaptureEngine()
    let tonePlayer = TonePlayer()
    private var previousFrequency: Double?

    // In-tune hold: a string must stay in-tune for this long before it is
    // marked done, tracked per string so frames from one string never count
    // toward the next. Mirrors Android IN_TUNE_HOLD_MS + inTuneStringIndex.
    private var inTuneFrames: Int = 0
    private var inTuneStringIndex: Int = -1
    /// Milliseconds a string must stay in-tune before it's marked done.
    private static let inTuneHoldMs = 1400
    /// Approximate interval between pitch readings, derived the same way as
    /// Android's FRAME_INTERVAL_MS (1024-sample hop at 44.1 kHz -> ~23 ms).
    private static let frameIntervalMs =
        AudioCaptureEngine.hopSize * 1000 / Int(AudioCaptureEngine.sampleRate)
    /// Consecutive in-tune frames required, derived from the millisecond hold.
    private static let inTuneHoldFrames = inTuneHoldMs / frameIntervalMs

    private var lostSignalFrames: Int = 0
    private static let lostSignalHoldFrames = 17

    // Frequency smoothing (5-frame rolling median)
    private var recentFrequencies: [Double] = []
    private static let smoothingWindow = 5

    // Display-cents EMA smoothing for the needle
    private static let displayCentsAlpha = 0.30
    private static let displayDeadbandCents = 0.8
    private var displayCentsFiltered = 0.0

    // String-switch hysteresis (cents)
    private static let stringSwitchHysteresisCents = 4.0

    // Onset (pluck attack) detection — adaptive threshold + blanking
    private static let onsetMinRatio: Float = 2.5
    private static let onsetMaxRatio: Float = 5.0
    private static let onsetEmaAlpha: Float = 0.15
    private static let blankingFrames = 2
    private var rmsEma: Float = 0
    private var blankingFramesRemaining = 0

    // TTS
    private let synthesizer = AVSpeechSynthesizer()
    private var lastTtsTime: Date = .distantPast
    private var lastTtsBucket: Int = Int.min
    private static let ttsNormalInterval: TimeInterval = 2.0
    private static let ttsInTuneInterval: TimeInterval = 3.0
    private static let ttsCentsBucketSize = 5

    // Frame-dropping (backpressure) — checked on audio thread, not MainActor
    private nonisolated(unsafe) let processingLock = OSAllocatedUnfairLock(initialState: false)
    private nonisolated(unsafe) let neuralInferenceLock = OSAllocatedUnfairLock(initialState: false)

    // Thread-safe previous frequency for YIN continuity (read from DSP queue, written from MainActor)
    private nonisolated(unsafe) let lastFrequencyLock = OSAllocatedUnfairLock<Double?>(initialState: nil)

    // Neural pitch supervision
    private var neuralSupervisor: NeuralPitchSupervisor?
    private let neuralArbitrator = NeuralArbitrator()

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

    private nonisolated(unsafe) let audioProcessingQueue = DispatchQueue(
        label: "com.baijum.ukufretboard.tuner.dsp", qos: .userInitiated
    )

    init() {
        neuralSupervisor = nil
        audioEngine.onBuffer = { [weak self] samples in
            guard let self else { return }
            let shouldProcess = self.processingLock.withLock { isProcessing -> Bool in
                if isProcessing { return false }
                isProcessing = true
                return true
            }
            guard shouldProcess else { return }
            self.audioProcessingQueue.async {
                let kotlinArray = KotlinFloatArray(size: Int32(samples.count))
                for i in 0..<samples.count {
                    kotlinArray.set(index: Int32(i), value: samples[i])
                }
                let prevFreq = self.lastFrequencyLock.withLock { $0 }.map { KotlinDouble(value: $0) }
                let result = PitchDetector.shared.detect(
                    samples: kotlinArray,
                    sampleRate: 44100,
                    threshold: PitchDetector.shared.DEFAULT_THRESHOLD,
                    previousFrequency: prevFreq
                )
                Task { @MainActor [weak self] in
                    guard let self else { return }
                    self.applyPitchResult(result, samples: samples)
                    self.processingLock.withLock { $0 = false }
                }
            }
        }
        Task {
            let supervisor = await Self.loadNeuralSupervisor()
            self.neuralSupervisor = supervisor
            self.neuralStatus = supervisor != nil ? .active : .fallback
        }
    }

    private nonisolated static func loadNeuralSupervisor() async -> NeuralPitchSupervisor? {
        NeuralPitchSupervisor()
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
        Task { @MainActor in
            let granted: Bool
            if #available(iOS 17.0, *) {
                granted = await AVAudioApplication.requestRecordPermission()
            } else {
                granted = await withCheckedContinuation { continuation in
                    AVAudioSession.sharedInstance().requestRecordPermission { g in
                        continuation.resume(returning: g)
                    }
                }
            }
            if granted {
                startCapture()
            }
        }
    }

    private func startCapture() {
        audioEngine.onInterrupted = { [weak self] in self?.stopCapture() }
        audioEngine.start()
        isCapturing = true
        previousFrequency = nil
        lastFrequencyLock.withLock { $0 = nil }
        rmsEma = 0
        blankingFramesRemaining = 0
    }

    private func applyPitchResult(_ yinResult: PitchResult?, samples: [Float]) {
        guard isCapturing else { return }

        let rms = sqrt(samples.reduce(0) { $0 + $1 * $1 } / Float(max(samples.count, 1)))

        // Adaptive onset detection: blank transient attack frames
        if rmsEma > 0 {
            let adaptiveRatio = min(max(Self.onsetMinRatio + (0.01 / rmsEma),
                                        Self.onsetMinRatio), Self.onsetMaxRatio)
            if rms / rmsEma > adaptiveRatio {
                blankingFramesRemaining = Self.blankingFrames
            }
        }
        rmsEma = rmsEma == 0 ? rms : rmsEma + Self.onsetEmaAlpha * (rms - rmsEma)

        if blankingFramesRemaining > 0 {
            blankingFramesRemaining -= 1
            return
        }

        if rms < noiseGateRms {
            lostSignalFrames = 0
            previousFrequency = nil
            lastFrequencyLock.withLock { $0 = nil }
            recentFrequencies.removeAll()
            displayCentsFiltered = 0.0
            inTuneFrames = 0
            inTuneStringIndex = -1
            isInTune = false
            noteName = "--"
            octave = nil
            centsDeviation = 0
            displayCentsDeviation = 0
            frequency = nil
            stringMatch = nil
            tuningStatus = ""
            activeStringIndex = nil
            neuralArbitrator.reset()
            return
        }

        let result = yinResult

        let neuralResult = maybeRunNeural(samples)

        if let result = result {
            lostSignalFrames = 0

            let arbitration = neuralArbitrator.arbitrate(
                yinResult: result,
                neuralResult: neuralResult
            )
            let finalHz = arbitration.frequencyHz

            previousFrequency = finalHz
            lastFrequencyLock.withLock { $0 = finalHz }

            recentFrequencies.append(finalHz)
            if recentFrequencies.count > Self.smoothingWindow {
                recentFrequencies.removeFirst()
            }
            let smoothedHz = medianFrequency()

            if let noteInfo = TunerNoteMapper.shared.mapFrequency(hz: smoothedHz, a4Reference: a4Reference) {
                let newNoteName = noteInfo.noteName
                noteName = newNoteName
                octave = Int(noteInfo.octave)
                frequency = finalHz

                // Announce note changes to VoiceOver
                let noteWithOctave = "\(newNoteName)\(noteInfo.octave)"
                if noteWithOctave != lastAnnouncedNote {
                    lastAnnouncedNote = noteWithOctave
                    AccessibilityAnnouncer.shared.announce(
                        "Detected \(newNoteName) \(noteInfo.octave)"
                    )
                }

                let stringResult = TunerNoteMapper.shared.findNearestStringWithHysteresis(
                    noteInfo: noteInfo,
                    tuning: currentTuning,
                    previousStringIndex: activeStringIndex.map { KotlinInt(int: Int32($0)) },
                    switchHysteresisCents: Self.stringSwitchHysteresisCents,
                    a4Reference: a4Reference
                )
                stringMatch = stringResult.stringName
                let stringIdx = Int(stringResult.stringIndex)
                activeStringIndex = stringIdx

                let cents = stringResult.centsFromTarget
                let clampedCents = min(max(cents, -50), 50)
                centsDeviation = clampedCents
                displayCentsDeviation = smoothDisplayCents(clampedCents)
                let justTuned: Bool
                let inTune = abs(cents) <= effectiveInTuneCents
                isInTune = inTune
                if inTune {
                    tuningStatus = "In tune!"
                    if stringIdx == inTuneStringIndex {
                        inTuneFrames += 1
                        if inTuneFrames >= Self.inTuneHoldFrames && !stringProgress[stringIdx] {
                            stringProgress[stringIdx] = true
                            justTuned = true
                            advanceToNextUntuned()
                        } else {
                            justTuned = false
                        }
                    } else {
                        // In-tune run moved to a different string: start over so
                        // frames accumulated on one string never mark the next.
                        inTuneStringIndex = stringIdx
                        inTuneFrames = 1
                        justTuned = false
                    }
                } else {
                    justTuned = false
                    inTuneFrames = 0
                    inTuneStringIndex = -1
                    if cents > 0 {
                        tuningStatus = String(format: "%.0f cents sharp", cents)
                    } else {
                        tuningStatus = String(format: "%.0f cents flat", abs(cents))
                    }
                }

                speakTunerState(
                    note: newNoteName,
                    cents: cents,
                    stringTuned: justTuned,
                    stringName: stringResult.stringName
                )
            }
        } else {
            lostSignalFrames += 1
            if lostSignalFrames < Self.lostSignalHoldFrames && noteName != "--" {
                return
            }
            lostSignalFrames = 0
            previousFrequency = nil
            lastFrequencyLock.withLock { $0 = nil }
            recentFrequencies.removeAll()
            displayCentsFiltered = 0.0
            inTuneFrames = 0
            inTuneStringIndex = -1
            isInTune = false
            noteName = "--"
            octave = nil
            centsDeviation = 0
            displayCentsDeviation = 0
            frequency = nil
            stringMatch = nil
            tuningStatus = ""
            activeStringIndex = nil
            neuralArbitrator.reset()
        }
    }

    // MARK: - Neural Pitch Supervision

    private func maybeRunNeural(_ samples: [Float]) -> NeuralPitchResult? {
        guard let supervisor = neuralSupervisor else { return nil }

        if neuralArbitrator.shouldRunInference() {
            let canRun = neuralInferenceLock.withLock { inFlight -> Bool in
                if inFlight { return false }
                inFlight = true
                return true
            }
            if canRun {
                Task.detached(priority: .userInitiated) { [weak self] in
                    let estimate = supervisor.estimate(samples)
                    await MainActor.run { [weak self] in
                        guard let self, self.isCapturing else { return }
                        if let estimate = estimate {
                            self.neuralArbitrator.onInferenceResult(result: estimate)
                            self.neuralStatus = .active
                        } else {
                            let disabled = self.neuralArbitrator.onInferenceFailure()
                            if disabled {
                                self.neuralStatus = .fallback
                            }
                        }
                    }
                    self?.neuralInferenceLock.withLock { $0 = false }
                }
            }
        }

        return neuralArbitrator.currentResult()
    }


    // MARK: - Spoken Feedback

    private func speakTunerState(note: String, cents: Double, stringTuned: Bool, stringName: String?) {
        guard spokenFeedback else { return }
        let now = Date()
        let bucket = Int(cents / Double(Self.ttsCentsBucketSize))

        // The "<string> string tuned!" confirmation is the single most important
        // spoken event in the tuner for a blind user — it is the only signal that
        // a string is done. Bypass the throttle entirely so it is never dropped as
        // routine chatter (mirrors Android TtsAnnouncementThrottler's
        // `if (justTuned) return true`).
        if stringTuned, let name = stringName {
            lastTtsTime = now
            lastTtsBucket = bucket
            speak("\(name) string tuned!")
            return
        }

        let isInTune = abs(cents) <= effectiveInTuneCents
        let interval = isInTune ? Self.ttsInTuneInterval : Self.ttsNormalInterval

        guard now.timeIntervalSince(lastTtsTime) >= interval || bucket != lastTtsBucket else { return }

        lastTtsTime = now
        lastTtsBucket = bucket

        let message: String
        if isInTune {
            message = "\(note), in tune"
        } else {
            let direction = cents > 0 ? "sharp" : "flat"
            message = String(format: "%@, %.0f cents %@", note, abs(cents), direction)
        }

        speak(message)
    }

    private func speak(_ message: String) {
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
        inTuneFrames = 0
        inTuneStringIndex = -1
    }

    private func resetDisplay() {
        noteName = "--"
        octave = nil
        centsDeviation = 0
        displayCentsDeviation = 0
        frequency = nil
        stringMatch = nil
        tuningStatus = ""
        previousFrequency = nil
        lastFrequencyLock.withLock { $0 = nil }
        recentFrequencies.removeAll()
        displayCentsFiltered = 0.0
        activeStringIndex = nil
        inTuneFrames = 0
        inTuneStringIndex = -1
        lostSignalFrames = 0
        isInTune = false
        neuralArbitrator.reset()
    }

    // MARK: - Smoothing

    private func medianFrequency() -> Double {
        guard !recentFrequencies.isEmpty else { return 0 }
        let sorted = recentFrequencies.sorted()
        let mid = sorted.count / 2
        if sorted.count % 2 == 0 && sorted.count >= 2 {
            return (sorted[mid - 1] + sorted[mid]) / 2.0
        }
        return sorted[mid]
    }

    private func smoothDisplayCents(_ rawCents: Double) -> Double {
        let target = abs(rawCents) < Self.displayDeadbandCents ? 0.0 : rawCents
        displayCentsFiltered += Self.displayCentsAlpha * (target - displayCentsFiltered)
        if abs(displayCentsFiltered) < Self.displayDeadbandCents / 2.0 {
            displayCentsFiltered = 0.0
        }
        return displayCentsFiltered
    }
}

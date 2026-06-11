import AVFoundation
import Combine
import Foundation
import os
import SwiftUI
import shared

enum NeuralRuntimeStatus {
    case loading, active, fallback
}

@MainActor
final class TunerViewModel: ObservableObject {
    @Published var noteName: String = "--"
    @Published var octave: Int?
    @Published var centsDeviation: Double = 0
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

    // Frame-dropping (backpressure) — checked on audio thread, not MainActor
    private nonisolated(unsafe) let processingLock = OSAllocatedUnfairLock(initialState: false)
    private nonisolated(unsafe) let neuralInferenceLock = OSAllocatedUnfairLock(initialState: false)

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
            Task { @MainActor in
                self.processAudioBuffer(samples)
                self.processingLock.withLock { $0 = false }
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
    }

    private func processAudioBuffer(_ samples: [Float]) {
        let rms = sqrt(samples.reduce(0) { $0 + $1 * $1 } / Float(max(samples.count, 1)))
        if rms < noiseGateRms {
            self.isInTune = false
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
                let arbitration = self.neuralArbitrator.arbitrate(
                    yinResult: result,
                    neuralResult: neuralResult
                )
                let finalHz = arbitration.frequencyHz

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
                    self.isInTune = abs(cents) <= 6
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
                self.isInTune = false
            }
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
                        guard let self else { return }
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
        isInTune = false
        neuralArbitrator.reset()
    }
}

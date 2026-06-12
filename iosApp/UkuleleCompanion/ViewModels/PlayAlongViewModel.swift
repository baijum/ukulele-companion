import Foundation
import AVFoundation
import os
import shared

/// Orchestrates Play Along: metronome timing, mic capture, chord detection, and scoring.
@MainActor
final class PlayAlongViewModel: ObservableObject {
    // MARK: - Published State

    @Published var isPlaying = false
    @Published var currentChordIndex = 0
    @Published var currentBeat = 0
    @Published var detectedChord: String?
    @Published var isChordCorrect: Bool?
    @Published var score: PlayAlongScorer.PlayAlongScore?
    @Published var errorMessage: String?

    // MARK: - Dependencies

    private let audioEngine = AudioCaptureEngine()
    private nonisolated(unsafe) let frameGate = FrameGate()
    private nonisolated(unsafe) let audioProcessingQueue = DispatchQueue(
        label: "com.baijum.ukufretboard.playalong.dsp", qos: .userInitiated
    )
    let tonePlayer = TonePlayer()
    private let scorer = PlayAlongScorer()

    // Chord detection state (simplified from PitchMonitorViewModel)
    private var chordFrameCounter: Int = 0
    private var lastChordName: String?
    private var chordHoldCount: Int = 0
    private var displayedChord: String?
    private var chordMissCount: Int = 0
    private static let chordDetectionInterval = 2
    private static let chordHoldFrames = 2
    private static let chordMissTolerance = 3

    // Beat timer
    private var timer: Timer?
    private var degrees: [ChordDegree] = []
    private var beatsPerChord: Int = 4
    private var bpm: Double = 100
    private var rootPitchClass: Int32 = 0
    private var isMinorScale: Bool = false

    // MARK: - Init

    init() {
        audioEngine.onBuffer = { [weak self] samples in
            guard let self else { return }
            guard self.frameGate.tryEnter() else { return }
            self.audioProcessingQueue.async {
                let kotlinArray = KotlinFloatArray(size: Int32(samples.count))
                for i in 0..<samples.count {
                    kotlinArray.set(index: Int32(i), value: samples[i])
                }
                let chordResult = AudioChordDetector.shared.detect(
                    samples: kotlinArray,
                    sampleRate: 44100,
                    threshold: 0.28,
                    preferredRootPitchClass: nil,
                    preferredRootWeight: 1.15
                )
                Task { @MainActor [weak self] in
                    guard let self else { return }
                    self.processAudioBuffer(samples, chordResult: chordResult)
                    self.frameGate.exit()
                }
            }
        }
    }

    // MARK: - Control

    func start(
        degrees: [ChordDegree],
        bpm: Double,
        beatsPerChord: Int,
        rootPitchClass: Int32,
        isMinorScale: Bool
    ) {
        guard !degrees.isEmpty else {
            errorMessage = "No chords in this progression. Please select a progression first."
            return
        }
        guard !isPlaying else { return }
        errorMessage = nil
        self.degrees = degrees
        self.bpm = bpm
        self.beatsPerChord = beatsPerChord
        self.rootPitchClass = rootPitchClass
        self.isMinorScale = isMinorScale

        scorer.reset()
        currentChordIndex = 0
        currentBeat = 0
        detectedChord = nil
        isChordCorrect = nil
        score = nil
        chordFrameCounter = 0
        lastChordName = nil
        chordHoldCount = 0
        displayedChord = nil
        chordMissCount = 0

        requestMicAndBegin()
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        audioEngine.stop()
        isPlaying = false
        score = scorer.getScore()
    }

    // MARK: - Mic Permission

    private func requestMicAndBegin() {
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
                beginSession()
            }
        }
    }

    private func beginSession() {
        audioEngine.onInterrupted = { [weak self] in self?.stop() }
        audioEngine.start()
        isPlaying = true

        timer?.invalidate()
        let interval = 60.0 / bpm
        timer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] _ in
            DispatchQueue.main.async {
                self?.onBeatTick()
            }
        }
    }

    // MARK: - Beat Timer

    private func onBeatTick() {
        currentBeat += 1

        if currentBeat == 1 {
            tonePlayer.play("click_high")
        } else {
            tonePlayer.play("click_low")
        }

        // Record the detected chord for this beat
        let expectedChord = resolvedChordName(degrees[currentChordIndex])
        scorer.recordBeat(
            expectedChord: expectedChord,
            detectedChord: displayedChord,
            confidence: displayedChord != nil ? 0.8 : 0.0
        )

        let normalized = displayedChord.map { Self.normalizeForComparison($0) }
        let expectedNorm = Self.normalizeForComparison(expectedChord)
        isChordCorrect = normalized == expectedNorm

        if currentBeat >= beatsPerChord {
            currentBeat = 0
            currentChordIndex += 1
            if currentChordIndex >= degrees.count {
                currentChordIndex = 0
            }
        }

        score = scorer.getScore()
    }

    // MARK: - Audio Processing

    private func processAudioBuffer(
        _ samples: [Float],
        chordResult: AudioChordDetector.ChordDetectionResult
    ) {
        var sumSq: Float = 0
        for s in samples { sumSq += s * s }
        let rms = sqrt(sumSq / Float(samples.count))
        if rms < 0.005 {
            detectedChord = nil
            return
        }

        if chordFrameCounter % Self.chordDetectionInterval == 0 {
            let rawChordName: String? = {
                if let found = chordResult.detection as? ChordDetector.DetectionResultChordFound {
                    return found.result.name
                }
                return nil
            }()

            if rawChordName == lastChordName && rawChordName != nil {
                chordHoldCount += 1
                chordMissCount = 0
            } else {
                lastChordName = rawChordName
                chordHoldCount = rawChordName != nil ? 1 : 0
                chordMissCount = rawChordName == nil ? chordMissCount + 1 : 0
            }

            if chordHoldCount >= Self.chordHoldFrames {
                displayedChord = rawChordName
            } else if rawChordName == nil && chordMissCount > Self.chordMissTolerance {
                displayedChord = nil
                lastChordName = nil
                chordHoldCount = 0
            }
        }
        chordFrameCounter += 1

        let currentDisplayed = displayedChord
        DispatchQueue.main.async { [weak self] in
            self?.detectedChord = currentDisplayed
        }
    }

    // MARK: - Helpers

    func resolvedChordName(_ degree: ChordDegree) -> String {
        let pitchClass = (Int(rootPitchClass) + Int(degree.interval)) % 12
        let name = Notes.shared.enharmonicForKey(
            pitchClass: Int32(pitchClass),
            keyRoot: KotlinInt(int: rootPitchClass),
            isMinor: isMinorScale
        )
        return name + degree.quality
    }

    nonisolated static func normalizeForComparison(_ name: String) -> String {
        name.replacingOccurrences(of: "7", with: "")
            .replacingOccurrences(of: "9", with: "")
            .replacingOccurrences(of: "maj", with: "")
            .trimmingCharacters(in: .whitespaces)
    }
}

import Foundation
import AVFoundation
import shared

struct PitchPoint {
    let timestampMs: Int64
    let midiNote: Float?
}

@MainActor
final class PitchMonitorViewModel: ObservableObject {
    @Published var isListening: Bool = false
    @Published var pitchHistory: [PitchPoint] = []
    @Published var currentNote: String?
    @Published var detectedChord: String?
    @Published var isArpeggioChord: Bool = false
    @Published var recentNotes: [String] = []
    @Published var chromaEnergy: [Float] = Array(repeating: 0, count: 12)

    var noiseGateRms: Float = 0.005

    private let audioEngine = AudioCaptureEngine()
    private let neuralSupervisor: NeuralPitchSupervisor?
    private var previousFrequency: Double?

    // Smoothing
    private var recentFrequencies: [Double] = []
    private static let smoothingWindow = 5
    private static let historyDurationMs: Int64 = 10_000

    // Neural state
    private var neuralFrameCounter: Int = 0
    private var lastNeuralResult: NeuralPitchResult?
    private var neuralResultAgeFrames: Int = Int.max
    private var neuralConsistencyFrames: Int = 0
    private var lastNeuralFrequencyForConsistency: Double?

    private static let neuralSupervisorInterval = 5
    private static let neuralResultTTLFrames = 10
    private static let arbitrationIgnoreSemitones = 1.5
    private static let arbitrationStrongSemitones = 2.5
    private static let neuralConsistencyRequired = 2

    // Onset detection
    private var previousRms: Float = 0
    private var blankingFramesRemaining: Int = 0
    private static let onsetRatioThreshold: Float = 3.0
    private static let blankingFrames = 2

    // Chord detection
    private var chordFrameCounter: Int = 0
    private var lastChordName: String?
    private var chordHoldCount: Int = 0
    private var displayedChord: String?
    private var chordMissCount: Int = 0
    private var lastChromaEnergy: [Float] = Array(repeating: 0, count: 12)
    private static let chordDetectionInterval = 2
    private static let chordHoldFrames = 2
    private static let chordMissTolerance = 3
    private var lastAnnouncedChord: String?

    // Arpeggio detection
    private let arpeggioDetector = ArpeggioDetector(windowMs: 3_000)
    private var lastArpeggioChordName: String?
    private var arpeggioHoldCount = 0
    private var displayedArpeggioChord: String?
    private var lastSimultaneousChordMs: Int64 = 0
    private var lastArpeggioChordMs: Int64 = 0
    private static let arpeggioHoldFrames = 2

    init() {
        neuralSupervisor = NeuralPitchSupervisor()
        audioEngine.onBuffer = { [weak self] samples in
            Task { @MainActor in
                self?.processBuffer(samples)
            }
        }
    }

    func toggleCapture() {
        if isListening { stopCapture() } else { requestMicAndStart() }
    }

    func stopCapture() {
        audioEngine.stop()
        isListening = false
        currentNote = nil
        detectedChord = nil
        isArpeggioChord = false
        recentNotes = []
        chromaEnergy = Array(repeating: 0, count: 12)
    }

    private func requestMicAndStart() {
        AVAudioSession.sharedInstance().requestRecordPermission { [weak self] granted in
            DispatchQueue.main.async {
                if granted { self?.startCapture() }
            }
        }
    }

    private func startCapture() {
        recentFrequencies.removeAll()
        previousFrequency = nil
        previousRms = 0
        blankingFramesRemaining = 0
        chordFrameCounter = 0
        lastChordName = nil
        chordHoldCount = 0
        displayedChord = nil
        chordMissCount = 0
        lastChromaEnergy = Array(repeating: 0, count: 12)
        neuralFrameCounter = 0
        lastNeuralResult = nil
        neuralResultAgeFrames = Int.max
        neuralConsistencyFrames = 0
        lastNeuralFrequencyForConsistency = nil

        arpeggioDetector.clear()
        lastArpeggioChordName = nil
        arpeggioHoldCount = 0
        displayedArpeggioChord = nil
        lastSimultaneousChordMs = 0
        lastArpeggioChordMs = 0

        audioEngine.start()
        isListening = true
    }

    private func processBuffer(_ samples: [Float]) {
        let kotlinArray = KotlinFloatArray(size: Int32(samples.count))
        for i in 0..<samples.count {
            kotlinArray.set(index: Int32(i), value: samples[i])
        }

        // RMS for onset detection
        var sumSq: Float = 0
        for s in samples { sumSq += s * s }
        let currentRms = sqrt(sumSq / Float(samples.count))

        if previousRms > 0 && currentRms / previousRms > Self.onsetRatioThreshold {
            blankingFramesRemaining = Self.blankingFrames
        }
        previousRms = currentRms

        let now = Int64(Date().timeIntervalSince1970 * 1000)

        if blankingFramesRemaining > 0 {
            blankingFramesRemaining -= 1
            let newPoint = PitchPoint(timestampMs: now, midiNote: nil)
            DispatchQueue.main.async { [weak self] in
                self?.appendPoint(newPoint, now: now)
            }
            return
        }

        // Noise gate
        if currentRms < noiseGateRms {
            previousFrequency = nil
            recentFrequencies.removeAll()
            let newPoint = PitchPoint(timestampMs: now, midiNote: nil)
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                self.appendPoint(newPoint, now: now)
                self.currentNote = nil
                self.chromaEnergy = Array(repeating: 0, count: 12)
            }
            return
        }

        // Pitch detection
        let prevFreq = previousFrequency.map { KotlinDouble(value: $0) }
        let yinResult = PitchDetector.shared.detect(
            samples: kotlinArray,
            sampleRate: 44100,
            threshold: PitchDetector.shared.DEFAULT_THRESHOLD,
            previousFrequency: prevFreq
        )

        // Neural supervision
        let neuralResult = maybeRunNeural(samples)

        // Arbitrate
        var finalHz: Double?
        if let yin = yinResult {
            if let neural = neuralResult {
                finalHz = arbitrate(yinFreq: yin.frequencyHz, yinConf: yin.confidence, neuralResult: neural)
            } else {
                finalHz = yin.frequencyHz
            }
        }

        var midiNote: Float?
        var noteStr: String?

        if let hz = finalHz {
            previousFrequency = hz
            recentFrequencies.append(hz)
            if recentFrequencies.count > Self.smoothingWindow {
                recentFrequencies.removeFirst()
            }
            let smoothed = medianFrequency()
            midiNote = Float(69.0 + 12.0 * log2(smoothed / 440.0))
            if let noteInfo = TunerNoteMapper.shared.mapFrequency(hz: smoothed, a4Reference: 440.0) {
                noteStr = "\(noteInfo.noteName)\(noteInfo.octave)"
            }
        } else {
            previousFrequency = nil
            recentFrequencies.removeAll()
        }

        let newPoint = PitchPoint(timestampMs: now, midiNote: midiNote)

        // Feed detected pitch to arpeggio detector
        if let midi = midiNote {
            let pitchClass = Int32(roundf(midi)) % 12
            arpeggioDetector.addNote(timestampMs: Int64(now), pitchClass: pitchClass)
        }

        // Chord detection (throttled)
        if chordFrameCounter % Self.chordDetectionInterval == 0 {
            let chordResult = AudioChordDetector.shared.detect(
                samples: kotlinArray,
                sampleRate: 44100,
                threshold: 0.28,
                preferredRootPitchClass: nil,
                preferredRootWeight: 1.15
            )
            let rawChordName: String? = {
                if let found = chordResult.detection as? ChordDetector.DetectionResultChordFound {
                    return found.result.name
                }
                return nil
            }()
            let chromagram = chordResult.chromagram
            var energy = [Float](repeating: 0, count: 12)
            for i in 0..<min(12, Int(chromagram.size)) {
                energy[i] = chromagram.get(index: Int32(i))
            }
            lastChromaEnergy = energy

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
                if rawChordName != nil { lastSimultaneousChordMs = now }
            } else if rawChordName == nil && chordMissCount > Self.chordMissTolerance {
                displayedChord = nil
                lastChordName = nil
                chordHoldCount = 0
            }
        }
        chordFrameCounter += 1

        // Arpeggio detection
        let arpeggioResult = arpeggioDetector.detect(currentTimeMs: Int64(now))
        let rawArpChordName = arpeggioResult?.result.name

        if rawArpChordName == lastArpeggioChordName && rawArpChordName != nil {
            arpeggioHoldCount += 1
        } else {
            lastArpeggioChordName = rawArpChordName
            arpeggioHoldCount = rawArpChordName != nil ? 1 : 0
        }

        if arpeggioHoldCount >= Self.arpeggioHoldFrames {
            displayedArpeggioChord = rawArpChordName
            if rawArpChordName != nil { lastArpeggioChordMs = now }
        } else if rawArpChordName == nil {
            displayedArpeggioChord = nil
        }

        // Merge: most-recent-wins between simultaneous and arpeggio
        let finalChord: String?
        let finalIsArpeggio: Bool

        if let simChord = displayedChord, let arpChord = displayedArpeggioChord {
            if lastSimultaneousChordMs >= lastArpeggioChordMs {
                finalChord = simChord; finalIsArpeggio = false
            } else {
                finalChord = arpChord; finalIsArpeggio = true
            }
        } else if let simChord = displayedChord {
            finalChord = simChord; finalIsArpeggio = false
        } else if let arpChord = displayedArpeggioChord {
            finalChord = arpChord; finalIsArpeggio = true
        } else {
            finalChord = nil; finalIsArpeggio = false
        }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.appendPoint(newPoint, now: now)
            self.currentNote = noteStr
            self.detectedChord = finalChord
            self.isArpeggioChord = finalIsArpeggio
            if let chord = finalChord, chord != self.lastAnnouncedChord {
                self.lastAnnouncedChord = chord
                let prefix = finalIsArpeggio ? "Arpeggio chord: " : "Chord: "
                AccessibilityAnnouncer.shared.announce("\(prefix)\(chord)", minInterval: 2.0)
            }
            self.chromaEnergy = self.lastChromaEnergy
            if let note = noteStr, note != self.recentNotes.last {
                self.recentNotes.append(note)
                if self.recentNotes.count > 20 {
                    self.recentNotes.removeFirst()
                }
            }
        }
    }

    private func appendPoint(_ point: PitchPoint, now: Int64) {
        let cutoff = now - Self.historyDurationMs
        pitchHistory = pitchHistory.filter { $0.timestampMs >= cutoff } + [point]
    }

    private func medianFrequency() -> Double {
        let sorted = recentFrequencies.sorted()
        let mid = sorted.count / 2
        if sorted.count % 2 == 0 && sorted.count >= 2 {
            return (sorted[mid - 1] + sorted[mid]) / 2.0
        }
        return sorted[mid]
    }

    // MARK: - Neural

    private func maybeRunNeural(_ samples: [Float]) -> NeuralPitchResult? {
        guard let supervisor = neuralSupervisor else { return nil }
        neuralFrameCounter += 1

        if neuralFrameCounter % Self.neuralSupervisorInterval == 0 {
            let estimate = supervisor.estimate(samples)
            lastNeuralResult = estimate
            neuralResultAgeFrames = 0
            if let estimate = estimate {
                updateNeuralConsistency(estimate.frequencyHz)
            } else {
                neuralConsistencyFrames = 0
                lastNeuralFrequencyForConsistency = nil
            }
        } else if neuralResultAgeFrames < Int.max {
            neuralResultAgeFrames += 1
        }

        return neuralResultAgeFrames <= Self.neuralResultTTLFrames ? lastNeuralResult : nil
    }

    private func arbitrate(yinFreq: Double, yinConf: Double, neuralResult: NeuralPitchResult) -> Double {
        let semitoneGap = semitoneDistance(yinFreq, neuralResult.frequencyHz)
        if semitoneGap <= Self.arbitrationIgnoreSemitones { return yinFreq }
        if neuralConsistencyFrames < Self.neuralConsistencyRequired { return yinFreq }

        if isOctaveRelation(yinFreq, neuralResult.frequencyHz)
            && neuralResult.confidence >= 0.85 && yinConf >= 0.12 {
            return neuralResult.frequencyHz
        }
        if semitoneGap >= Self.arbitrationStrongSemitones
            && neuralResult.confidence >= 0.93 && yinConf >= 0.16 {
            return neuralResult.frequencyHz
        }
        return yinFreq
    }

    private func semitoneDistance(_ a: Double, _ b: Double) -> Double {
        guard a > 0, b > 0 else { return Double.greatestFiniteMagnitude }
        return abs(12.0 * log2(a / b))
    }

    private func isOctaveRelation(_ a: Double, _ b: Double) -> Bool {
        guard a > 0, b > 0 else { return false }
        let st = semitoneDistance(a, b)
        return abs(st - 12.0) <= 1.0 || abs(st - 24.0) <= 1.0
    }

    private func updateNeuralConsistency(_ hz: Double) {
        guard hz > 0 else {
            neuralConsistencyFrames = 0
            lastNeuralFrequencyForConsistency = nil
            return
        }
        if let prev = lastNeuralFrequencyForConsistency,
           semitoneDistance(prev, hz) <= 0.5 {
            neuralConsistencyFrames += 1
        } else {
            neuralConsistencyFrames = 1
        }
        lastNeuralFrequencyForConsistency = hz
    }
}

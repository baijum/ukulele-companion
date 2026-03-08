import Foundation
import Combine

enum BeatType: String {
    case accent, normal, mute
}

@MainActor
final class MetronomeViewModel: ObservableObject {
    @Published var bpm: Double = 100
    @Published var beatsPerMeasure: Int = 4
    @Published var subdivision: Int = 1
    @Published var accentPattern: [BeatType] = [.accent, .normal, .normal, .normal]
    @Published var isPlaying: Bool = false
    @Published var currentBeat: Int = -1
    @Published var currentSubBeat: Int = 0
    @Published var measureCount: Int = 0
    @Published var isCompound: Bool = false
    @Published var timeSignatureLabel: String = "4/4"

    private let tonePlayer = TonePlayer()
    private var timer: AnyCancellable?
    private var tapTimestamps: [Date] = []

    func setBpm(_ value: Double) {
        bpm = min(300, max(30, value))
        if isPlaying { restartTimer() }
    }

    func setTimeSignature(_ label: String) {
        timeSignatureLabel = label
        switch label {
        case "6/8":
            isCompound = true
            beatsPerMeasure = 2
            subdivision = 3
            accentPattern = Self.defaultAccentPattern(beats: 2)
        case "12/8":
            isCompound = true
            beatsPerMeasure = 4
            subdivision = 3
            accentPattern = Self.defaultAccentPattern(beats: 4)
        default:
            isCompound = false
            let beats = Int(label.split(separator: "/").first.flatMap { Int($0) } ?? 4)
            let clampedBeats = min(7, max(2, beats))
            beatsPerMeasure = clampedBeats
            subdivision = 1
            accentPattern = Self.defaultAccentPattern(beats: clampedBeats)
        }
        stop()
    }

    func setSubdivision(_ value: Int) {
        guard !isCompound else { return }
        subdivision = min(4, max(1, value))
        if isPlaying { restartTimer() }
    }

    func toggleBeatType(_ index: Int) {
        guard index >= 0, index < accentPattern.count else { return }
        switch accentPattern[index] {
        case .accent: accentPattern[index] = .normal
        case .normal: accentPattern[index] = .mute
        case .mute: accentPattern[index] = .accent
        }
    }

    func togglePlayback() {
        if isPlaying { stop() } else { start() }
    }

    func tapTempo() {
        let now = Date()
        tapTimestamps.append(now)
        if tapTimestamps.count > 4 {
            tapTimestamps.removeFirst(tapTimestamps.count - 4)
        }
        guard tapTimestamps.count >= 2 else { return }
        var totalInterval: TimeInterval = 0
        for i in 1..<tapTimestamps.count {
            totalInterval += tapTimestamps[i].timeIntervalSince(tapTimestamps[i - 1])
        }
        let avgInterval = totalInterval / Double(tapTimestamps.count - 1)
        if avgInterval > 0 {
            setBpm(60.0 / avgInterval)
        }
    }

    func start() {
        isPlaying = true
        measureCount = 0
        currentBeat = -1
        currentSubBeat = 0
        startTimer()
    }

    func stop() {
        timer?.cancel()
        timer = nil
        isPlaying = false
        currentBeat = -1
        currentSubBeat = 0
    }

    private func startTimer() {
        let interval = 60.0 / (bpm * Double(subdivision))
        var totalSubBeat = 0

        timer = Timer.publish(every: interval, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self = self else { return }
                let beat = totalSubBeat / self.subdivision
                let subBeat = totalSubBeat % self.subdivision

                let wrappedBeat = beat % self.beatsPerMeasure
                self.currentBeat = wrappedBeat
                self.currentSubBeat = subBeat

                if wrappedBeat == 0 && subBeat == 0 && totalSubBeat > 0 {
                    self.measureCount += 1
                }

                let type = self.accentPattern.indices.contains(wrappedBeat)
                    ? self.accentPattern[wrappedBeat] : .normal

                switch type {
                case .mute:
                    break
                case .accent where subBeat == 0:
                    self.tonePlayer.play("click_high")
                case .normal where subBeat == 0:
                    self.tonePlayer.play("click_low")
                default:
                    if subBeat > 0 {
                        self.tonePlayer.play("click_low")
                    }
                }

                totalSubBeat += 1
            }
    }

    private func restartTimer() {
        timer?.cancel()
        startTimer()
    }

    private static func defaultAccentPattern(beats: Int) -> [BeatType] {
        (0..<beats).map { $0 == 0 ? .accent : .normal }
    }
}

import Foundation
import Combine
import shared

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
        bpm = Double(MetronomeStateLogic.shared.clampBpm(value: Int32(value)))
        if isPlaying { restartTimer() }
    }

    func setTimeSignature(_ label: String) {
        timeSignatureLabel = label
        let result = MetronomeStateLogic.shared.parseTimeSignature(label: label)
        isCompound = result.isCompound
        beatsPerMeasure = Int(result.beatsPerMeasure)
        subdivision = Int(result.subdivision)
        accentPattern = Self.convertAccentPattern(
            MetronomeStateLogic.shared.defaultAccentPattern(beatsPerMeasure: result.beatsPerMeasure)
        )
        stop()
    }

    func setSubdivision(_ value: Int) {
        guard !isCompound else { return }
        subdivision = Int(MetronomeStateLogic.shared.clampSubdivision(value: Int32(value)))
        if isPlaying { restartTimer() }
    }

    func toggleBeatType(_ index: Int) {
        guard index >= 0, index < accentPattern.count else { return }
        accentPattern[index] = Self.convertBeatType(
            MetronomeStateLogic.shared.cycleBeatType(
                current: Self.toKmpBeatType(accentPattern[index])
            )
        )
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

    private static func convertAccentPattern(_ kmpList: [shared.BeatType]) -> [BeatType] {
        kmpList.map { convertBeatType($0) }
    }

    private static func convertBeatType(_ kmp: shared.BeatType) -> BeatType {
        switch kmp {
        case .accent: return .accent
        case .normal: return .normal
        case .mute: return .mute
        default: return .normal
        }
    }

    private static func toKmpBeatType(_ local: BeatType) -> shared.BeatType {
        switch local {
        case .accent: return .accent
        case .normal: return .normal
        case .mute: return .mute
        }
    }
}

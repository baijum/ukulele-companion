import SwiftUI
import Combine
import shared

struct ProgressionPracticeView: View {
    let progression: Progression
    let keyRoot: Int32

    @State private var currentChordIndex = 0
    @State private var selectedVoicingIndex: [Int: Int] = [:]
    @State private var bpm: Float = 100
    @State private var beatsPerChord = 4
    @State private var loop = true
    @State private var isPlaying = false
    @State private var timer: AnyCancellable?

    private let tonePlayer = TonePlayer()
    private let leftHanded: Bool
    private let tuning: [shared.UkuleleString]
    private let cachedVoicings: [[ChordVoicing]]

    private static let sampleNames = [
        "uke_c", "uke_csharp", "uke_d", "uke_dsharp",
        "uke_e", "uke_f", "uke_fsharp", "uke_g",
        "uke_gsharp", "uke_a", "uke_asharp", "uke_b",
    ]

    init(progression: Progression, keyRoot: Int32) {
        self.progression = progression
        self.keyRoot = keyRoot
        let defaults = UserDefaults(suiteName: "app_settings") ?? .standard
        self.leftHanded = defaults.bool(forKey: "left_handed")
        let t = UkuleleTuning.highG
        let tuning = t.asUkuleleStrings
        self.tuning = tuning

        let degrees = progression.degrees.asArray(of: ChordDegree.self)
        let formulas = ChordFormulas.shared.ALL.asArray(of: ChordFormula.self)
        self.cachedVoicings = degrees.map { degree in
            let chordRoot = (keyRoot + degree.interval) % 12
            guard let formula = formulas.first(where: { $0.symbol == degree.quality }) else {
                return []
            }
            let voicings = VoicingGenerator.shared.generate(
                rootPitchClass: chordRoot,
                formula: formula,
                tuning: tuning,
                allowMutedStrings: false
            )
            return voicings.asArray(of: ChordVoicing.self)
        }
    }

    private func voicingFor(_ chordIdx: Int) -> ChordVoicing? {
        guard chordIdx < cachedVoicings.count, !cachedVoicings[chordIdx].isEmpty else { return nil }
        let idx = min(selectedVoicingIndex[chordIdx] ?? 0, cachedVoicings[chordIdx].count - 1)
        return cachedVoicings[chordIdx][max(0, idx)]
    }

    var body: some View {
        let voicings = cachedVoicings
        let currentVoicing = voicingFor(currentChordIndex)
        let currentVoicingCount = currentChordIndex < voicings.count ? voicings[currentChordIndex].count : 0
        let currentVoicingIdx = min(selectedVoicingIndex[currentChordIndex] ?? 0, max(0, currentVoicingCount - 1))
        let degrees = progression.degrees.asArray(of: ChordDegree.self)

        ScrollView {
            VStack(spacing: 12) {
                // Header
                VStack(spacing: 4) {
                    Text("Practice")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(progression.name)
                        .font(.headline)
                    Text("Key: \(Notes.shared.enharmonicForKey(pitchClass: keyRoot, keyRoot: nil, isMinor: false))")
                        .font(.subheadline)
                        .foregroundStyle(Color.accentColor)
                }
                .padding(.top, 8)

                // Chord timeline
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(0..<degrees.count, id: \.self) { index in
                            let degree = degrees[index]
                            let chordRoot = (keyRoot + degree.interval) % 12
                            let chordName = Notes.shared.enharmonicForKey(
                                pitchClass: chordRoot, keyRoot: nil, isMinor: false
                            ) + degree.quality
                            let isCurrent = index == currentChordIndex

                            Button {
                                if !isPlaying {
                                    currentChordIndex = index
                                }
                            } label: {
                                VStack(spacing: 2) {
                                    Text(chordName)
                                        .font(.subheadline.bold())
                                    Text(degree.numeral)
                                        .font(.caption2)
                                        .opacity(0.8)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(isCurrent ? Color.accentColor : Color(.systemGray5))
                                .foregroundStyle(isCurrent ? .white : .primary)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(isCurrent ? .isSelected : [])
                        }
                    }
                    .padding(.horizontal)
                }

                // Chord diagram
                if let voicing = currentVoicing {
                    let degree = degrees[currentChordIndex]
                    let chordRoot = (keyRoot + degree.interval) % 12
                    let chordName = Notes.shared.enharmonicForKey(
                        pitchClass: chordRoot, keyRoot: nil, isMinor: false
                    ) + degree.quality
                    ChordDiagramView(voicing: voicing, chordName: chordName)
                        .frame(maxWidth: .infinity)
                } else {
                    Text("No voicing available")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.vertical, 32)
                }

                // Voicing selector
                if currentVoicingCount > 0 {
                    HStack {
                        Button {
                            let cur = selectedVoicingIndex[currentChordIndex] ?? 0
                            selectedVoicingIndex[currentChordIndex] = cur > 0 ? cur - 1 : currentVoicingCount - 1
                        } label: {
                            Image(systemName: "chevron.left")
                        }
                        .disabled(currentVoicingCount <= 1)
                        .accessibilityLabel("Previous voicing")

                        Text("Voicing \(currentVoicingIdx + 1) of \(currentVoicingCount)")
                            .font(.subheadline)
                            .frame(width: 140)

                        Button {
                            let cur = selectedVoicingIndex[currentChordIndex] ?? 0
                            selectedVoicingIndex[currentChordIndex] = cur < currentVoicingCount - 1 ? cur + 1 : 0
                        } label: {
                            Image(systemName: "chevron.right")
                        }
                        .disabled(currentVoicingCount <= 1)
                        .accessibilityLabel("Next voicing")
                    }
                }

                // Controls card
                controlsCard(degrees: degrees)
            }
            .padding(.bottom, 16)
        }
        .navigationTitle("Practice")
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear { stopPlayback() }
        .onChange(of: currentChordIndex) { newIndex in
            guard isPlaying, newIndex < degrees.count else { return }
            let degree = degrees[newIndex]
            let chordRoot = (keyRoot + degree.interval) % 12
            let name = Notes.shared.enharmonicForKey(
                pitchClass: chordRoot, keyRoot: nil, isMinor: false
            ) + degree.quality
            AccessibilityAnnouncer.shared.announce(name, minInterval: 0.5)
        }
    }

    // MARK: - Controls Card

    private func controlsCard(degrees: [ChordDegree]) -> some View {
        VStack(spacing: 8) {
            // BPM slider
            HStack {
                Text("BPM: \(Int(bpm))")
                    .font(.caption)
                    .frame(width: 64, alignment: .leading)
                Slider(value: $bpm, in: 40...220)
                    .accessibilityValue("\(Int(bpm)) beats per minute")
                Button("Tap") { tapTempo() }
                    .font(.caption.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(.systemGray5))
                    .clipShape(Capsule())
            }

            // Beats per chord
            HStack(spacing: 6) {
                Text("Beats:")
                    .font(.caption)
                ForEach([1, 2, 4, 8], id: \.self) { beats in
                    Button {
                        beatsPerChord = beats
                    } label: {
                        Text("\(beats)")
                            .font(.caption.bold())
                            .padding(.horizontal, 12)
                            .padding(.vertical, 4)
                            .background(beatsPerChord == beats ? Color.accentColor : Color(.systemGray5))
                            .foregroundStyle(beatsPerChord == beats ? .white : .primary)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }

            // Loop + Play/Stop
            HStack {
                Button {
                    loop.toggle()
                } label: {
                    Label("Loop", systemImage: "repeat")
                        .font(.caption.bold())
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(loop ? Color.accentColor : Color(.systemGray5))
                        .foregroundStyle(loop ? .white : .primary)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Spacer()

                Button {
                    if isPlaying {
                        stopPlayback()
                    } else {
                        startPlayback(chordCount: degrees.count)
                    }
                } label: {
                    Image(systemName: isPlaying ? "stop.fill" : "play.fill")
                        .font(.title3)
                        .foregroundStyle(.white)
                        .frame(width: 40, height: 40)
                        .background(isPlaying ? Color.red : Color.accentColor)
                        .clipShape(Circle())
                }
                .accessibilityLabel(isPlaying ? "Stop" : "Play")
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }

    // MARK: - Playback

    private func startPlayback(chordCount: Int) {
        guard chordCount > 0 else { return }
        isPlaying = true
        currentChordIndex = 0
        var beatCount = 0
        var chordIdx = 0
        var lastChordPlayed = -1

        let interval = 60.0 / Double(bpm)
        timer = Timer.publish(every: interval, on: .main, in: .common)
            .autoconnect()
            .sink { _ in
                if chordIdx != lastChordPlayed {
                    lastChordPlayed = chordIdx
                    if let voicing = voicingFor(chordIdx) {
                        playVoicing(voicing)
                    }
                }
                currentChordIndex = chordIdx
                beatCount += 1

                if beatCount >= beatsPerChord {
                    beatCount = 0
                    chordIdx += 1
                    if chordIdx >= chordCount {
                        if loop {
                            chordIdx = 0
                        } else {
                            stopPlayback()
                        }
                    }
                }
            }
    }

    private func stopPlayback() {
        timer?.cancel()
        timer = nil
        tonePlayer.stop()
        isPlaying = false
    }

    private func playVoicing(_ voicing: ChordVoicing) {
        let frets = voicing.fretInts
        for (i, fret) in frets.enumerated() {
            guard fret >= 0 else { continue }
            let pc = (tuning[i].openPitchClass + Int32(fret)) % 12
            tonePlayer.play(Self.sampleNames[Int(pc)])
            return
        }
    }

    // MARK: - Tap Tempo

    @State private var tapTimestamps: [Date] = []

    private func tapTempo() {
        let now = Date()
        tapTimestamps.append(now)
        // Keep last 4 taps
        if tapTimestamps.count > 4 {
            tapTimestamps.removeFirst()
        }
        // Reset if gap > 2 seconds
        if tapTimestamps.count >= 2 {
            let last = tapTimestamps[tapTimestamps.count - 1]
            let prev = tapTimestamps[tapTimestamps.count - 2]
            if last.timeIntervalSince(prev) > 2.0 {
                tapTimestamps = [now]
                return
            }
        }
        guard tapTimestamps.count >= 2 else { return }
        let intervals = zip(tapTimestamps.dropFirst(), tapTimestamps).map { $0.timeIntervalSince($1) }
        let avg = intervals.reduce(0, +) / Double(intervals.count)
        let detected = 60.0 / avg
        bpm = Float(min(220, max(40, detected)))
    }
}

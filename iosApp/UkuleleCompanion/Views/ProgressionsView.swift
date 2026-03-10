import SwiftUI
import shared

struct ProgressionsView: View {
    @StateObject private var viewModel = ProgressionsViewModel()
    private let tonePlayer = TonePlayer()
    @State private var playbackState = ProgressionPlaybackState()

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                rootSelector
                scaleTypeSelector

                if !viewModel.customProgressions.isEmpty {
                    customSection
                }

                presetSection
            }
            .padding()
        }
        .navigationTitle("Chord Progressions")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { viewModel.showingCreateSheet = true } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Create new progression")
            }
        }
        .sheet(isPresented: $viewModel.showingCreateSheet) {
            CreateProgressionSheet(viewModel: viewModel)
        }
    }

    private var rootSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(0..<Int32(12), id: \.self) { pc in
                    let name = viewModel.rootNoteNames[Int(pc)]
                    Button(action: { viewModel.selectedRoot = pc }) {
                        Text(name)
                            .font(.subheadline.bold())
                            .frame(minWidth: 36, minHeight: 36)
                            .background(
                                viewModel.selectedRoot == pc
                                    ? Color.accentColor
                                    : Color(.systemGray5)
                            )
                            .foregroundStyle(
                                viewModel.selectedRoot == pc ? .white : .primary
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(viewModel.selectedRoot == pc ? .isSelected : [])
                }
            }
        }
    }

    private var scaleTypeSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.allScaleTypes, id: \.self) { scale in
                    Button(action: { viewModel.selectedScaleType = scale }) {
                        Text(scale.label)
                            .font(.caption.bold())
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                viewModel.selectedScaleType == scale
                                    ? Color.accentColor
                                    : Color(.systemGray5)
                            )
                            .foregroundStyle(
                                viewModel.selectedScaleType == scale ? .white : .primary
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(viewModel.selectedScaleType == scale ? .isSelected : [])
                }
            }
        }
    }

    private var customSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Custom")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)

            ForEach(viewModel.customProgressions) { custom in
                let chips: [(String, String)] = zip(custom.degreeNumerals, zip(custom.degreeIntervals, custom.degreeQualities)).map { numeral, intervalQuality in
                    let (interval, quality) = intervalQuality
                    let resolved = viewModel.resolvedChordNameForCustomDegree(interval: interval, quality: quality)
                    return (numeral, resolved)
                }
                progressionCard(
                    name: custom.name,
                    description: custom.description,
                    chordChips: chips,
                    shareText: nil,
                    onDelete: { viewModel.deleteCustom(id: custom.id) },
                    onDuplicate: {
                        viewModel.createCustom(
                            name: custom.name + " (Copy)",
                            description: custom.description,
                            selectedDegreeIndices: Set(0..<custom.degreeNumerals.count)
                        )
                    }
                )
            }
        }
    }

    private var presetSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Presets")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)

            let presets = viewModel.presetProgressions
            ForEach(0..<presets.count, id: \.self) { i in
                let progression = presets[i]
                let degrees = progression.degrees as! [ChordDegree]
                let chips = degrees.map { degree in
                    (degree.numeral, viewModel.resolvedChordName(degree: degree))
                }
                progressionCard(
                    name: progression.name,
                    description: progression.description_,
                    chordChips: chips,
                    shareText: viewModel.shareText(for: progression),
                    onDelete: nil,
                    onDuplicate: {
                        viewModel.createCustom(
                            name: progression.name + " (Copy)",
                            description: progression.description_,
                            selectedDegreeIndices: Set(0..<degrees.count)
                        )
                    },
                    progression: progression
                )
            }
        }
    }

    private func progressionCard(
        name: String,
        description: String,
        chordChips: [(String, String)],
        shareText: String?,
        onDelete: (() -> Void)?,
        onDuplicate: (() -> Void)? = nil,
        progression: Progression? = nil
    ) -> some View {
        let cardId = name
        let isThisPlaying = playbackState.isPlaying && playbackState.playingId == cardId

        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(name)
                    .font(.subheadline.bold())
                Spacer()
                if let shareText {
                    ShareLink(item: shareText) {
                        Image(systemName: "square.and.arrow.up")
                            .font(.caption)
                    }
                    .accessibilityLabel("Share \(name)")
                }
                if let onDelete {
                    Button(role: .destructive, action: onDelete) {
                        Image(systemName: "trash")
                            .font(.caption)
                    }
                    .accessibilityLabel("Delete \(name)")
                }
            }

            Text(description)
                .font(.caption)
                .foregroundStyle(.secondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(0..<chordChips.count, id: \.self) { j in
                        let (numeral, resolved) = chordChips[j]
                        let function = HarmonicFunctionKt.harmonicFunction(
                            numeral: numeral,
                            scaleType: viewModel.selectedScaleType
                        )
                        let isActive = isThisPlaying && playbackState.currentChordIndex == j
                        VStack(spacing: 2) {
                            Text(numeral)
                                .font(.caption2)
                                .foregroundStyle(isActive ? .white.opacity(0.8) : .secondary)
                            Text(resolved)
                                .font(.caption.bold())
                                .foregroundStyle(isActive ? .white : .primary)
                            Text(function.label)
                                .font(.system(size: 8))
                                .foregroundStyle(isActive ? .white.opacity(0.8) : harmonicFunctionColor(function))
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(isActive ? Color.accentColor : Color(.systemGray6))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .accessibilityCombined(label: "\(numeral), \(resolved), \(function.label)")
                    }
                }
            }

            ProgressionPlaybackBar(
                chordChips: chordChips,
                cardId: cardId,
                state: $playbackState,
                tonePlayer: tonePlayer,
                viewModel: viewModel
            )

            if let progression = progression {
                HStack(spacing: 8) {
                    NavigationLink {
                        VoiceLeadingView(progression: progression, keyRoot: viewModel.selectedRoot)
                    } label: {
                        Label("Voice Leading", systemImage: "arrow.triangle.swap")
                            .font(.caption2.bold())
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)

                    NavigationLink {
                        ProgressionPracticeView(progression: progression, keyRoot: viewModel.selectedRoot)
                    } label: {
                        Label("Practice", systemImage: "metronome")
                            .font(.caption2.bold())
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)

                    NavigationLink {
                        CapoCalculatorView(mode: .progression(progression: progression, keyRoot: viewModel.selectedRoot))
                    } label: {
                        Label("Capo", systemImage: "guitars")
                            .font(.caption2.bold())
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
        .contextMenu {
            if let onDuplicate {
                Button { onDuplicate() } label: {
                    Label("Duplicate", systemImage: "doc.on.doc")
                }
            }
            if let onDelete {
                Button(role: .destructive, action: onDelete) {
                    Label("Delete", systemImage: "trash")
                }
            }
        }
    }

    private func harmonicFunctionColor(_ function: HarmonicFunction) -> Color {
        switch function {
        case .tonic: .accentColor
        case .subdominant: .secondary
        case .dominant: .red
        default: .secondary
        }
    }
}

struct CreateProgressionSheet: View {
    @ObservedObject var viewModel: ProgressionsViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var description = ""
    @State private var selectedDegreeIndices: Set<Int> = []

    var body: some View {
        NavigationStack {
            Form {
                Section("Details") {
                    TextField("Name", text: $name)
                    TextField("Description", text: $description)
                }

                Section("Select Degrees") {
                    let degrees = viewModel.diatonicDegrees
                    ForEach(0..<degrees.count, id: \.self) { i in
                        let degree = degrees[i]
                        let resolved = viewModel.resolvedChordName(degree: degree)
                        Button {
                            if selectedDegreeIndices.contains(i) {
                                selectedDegreeIndices.remove(i)
                            } else {
                                selectedDegreeIndices.insert(i)
                            }
                        } label: {
                            HStack {
                                Image(systemName: selectedDegreeIndices.contains(i)
                                    ? "checkmark.circle.fill" : "circle")
                                Text(degree.numeral)
                                    .bold()
                                Text(resolved)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle("New Progression")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        viewModel.createCustom(
                            name: name,
                            description: description,
                            selectedDegreeIndices: selectedDegreeIndices
                        )
                        dismiss()
                    }
                    .disabled(name.isEmpty || selectedDegreeIndices.isEmpty)
                }
            }
        }
    }
}

// MARK: - Progression Playback State & Bar

struct ProgressionPlaybackState {
    var isPlaying = false
    var playingId: String?
    var currentChordIndex = 0
    var bpm: Double = 100
    var beatsPerChord: Int = 2
    var loop = false
}

private struct ProgressionPlaybackBar: View {
    let chordChips: [(String, String)]
    let cardId: String
    @Binding var state: ProgressionPlaybackState
    let tonePlayer: TonePlayer
    let viewModel: ProgressionsViewModel

    @State private var timer: Timer?

    private var isThisPlaying: Bool { state.isPlaying && state.playingId == cardId }
    private var interval: Double { Double(state.beatsPerChord) * 60.0 / state.bpm }

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Button(action: togglePlayback) {
                    Image(systemName: isThisPlaying ? "stop.fill" : "play.fill")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .tint(isThisPlaying ? .red : .accentColor)
                .accessibilityLabel(isThisPlaying ? "Stop" : "Play progression")

                Text("\(Int(state.bpm))")
                    .font(.caption.monospacedDigit())
                    .frame(width: 32)
                Slider(value: $state.bpm, in: 40...220, step: 1)
                    .frame(maxWidth: 100)
                    .accessibilityValue("\(Int(state.bpm)) BPM")

                Picker("Beats", selection: $state.beatsPerChord) {
                    Text("1").tag(1)
                    Text("2").tag(2)
                    Text("4").tag(4)
                }
                .pickerStyle(.segmented)
                .frame(width: 100)
                .accessibilityLabel("Beats per chord")

                Button(action: { state.loop.toggle() }) {
                    Image(systemName: state.loop ? "repeat.circle.fill" : "repeat")
                        .font(.caption)
                        .foregroundStyle(state.loop ? Color.accentColor : Color.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(state.loop ? "Loop on" : "Loop off")
            }
        }
    }

    private func togglePlayback() {
        if isThisPlaying {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private func startPlayback() {
        state.playingId = cardId
        state.isPlaying = true
        state.currentChordIndex = 0
        playCurrentChord()
        scheduleTimer()
    }

    private func stopPlayback() {
        timer?.invalidate()
        timer = nil
        state.isPlaying = false
        state.playingId = nil
        state.currentChordIndex = 0
    }

    private func scheduleTimer() {
        timer?.invalidate()
        let t = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { _ in
            DispatchQueue.main.async { advanceChord() }
        }
        timer = t
    }

    private func advanceChord() {
        let next = state.currentChordIndex + 1
        if next >= chordChips.count {
            if state.loop {
                state.currentChordIndex = 0
                playCurrentChord()
            } else {
                stopPlayback()
            }
        } else {
            state.currentChordIndex = next
            playCurrentChord()
        }
    }

    private func playCurrentChord() {
        guard state.currentChordIndex < chordChips.count else { return }
        let (_, resolved) = chordChips[state.currentChordIndex]
        playResolvedChord(resolved)
    }

    private func playResolvedChord(_ resolved: String) {
        let rootName = String(resolved.prefix(while: { $0.isLetter || $0 == "#" || $0 == "b" }))
        let quality = String(resolved.dropFirst(rootName.count))
        let noteMap = ["C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3,
                       "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8,
                       "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11]
        let rootPc = noteMap[rootName] ?? 0

        let tuning = (0..<4).map { i -> shared.UkuleleString in
            let t = UkuleleTuning.highG
            return shared.UkuleleString(
                name: t.stringNames[i] as! String,
                openPitchClass: (t.pitchClasses[i] as! NSNumber).int32Value,
                octave: (t.octaves[i] as! NSNumber).int32Value
            )
        }

        let formulas = ChordFormulas.shared.ALL as! [ChordFormula]
        let formula = formulas.first { $0.symbol == quality } ?? formulas.first { $0.symbol == "" }!
        let voicings = VoicingGenerator.shared.generate(
            rootPitchClass: Int32(rootPc),
            formula: formula,
            tuning: tuning,
            allowMutedStrings: false
        ) as! [ChordVoicing]

        if let voicing = voicings.first {
            let fretList = voicing.fretInts
            let pitchClasses = (0..<fretList.count).compactMap { i -> Int32? in
                let fret = fretList[i]
                guard fret >= 0 else { return nil }
                let openPc = (UkuleleTuning.highG.pitchClasses[i] as! NSNumber).int32Value
                return (openPc + Int32(fret)) % 12
            }
            tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
        }
    }
}

import SwiftUI
import shared

struct ProgressionsView: View {
    @StateObject private var viewModel = ProgressionsViewModel()
    private let tonePlayer = TonePlayer()
    @State private var playbackState = ProgressionPlaybackState()
    @State private var deletingCustomId: String?
    @State private var deletingCustomName: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                rootSelector
                scaleTypeSelector

                if !viewModel.filteredCustomProgressions.isEmpty {
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
        .sheet(item: editingCustomBinding) { custom in
            let degrees = zip(custom.degreeIntervals, zip(custom.degreeQualities, custom.degreeNumerals))
                .map { interval, qn in
                    ChordDegree(interval: Int32(interval), quality: qn.0, numeral: qn.1)
                }
            let scaleType = viewModel.allScaleTypes.first { $0.name == custom.scaleType } ?? .major
            CreateProgressionSheet(
                viewModel: viewModel,
                editingId: custom.id,
                initialName: custom.name,
                initialDescription: custom.description,
                initialDegrees: degrees,
                initialScaleType: scaleType
            )
        }
        .alert("Delete progression?", isPresented: showingDeleteAlert) {
            Button("Cancel", role: .cancel) {
                deletingCustomId = nil
                deletingCustomName = nil
            }
            Button("Delete", role: .destructive) {
                if let id = deletingCustomId {
                    viewModel.deleteCustom(id: id)
                }
                deletingCustomId = nil
                deletingCustomName = nil
            }
        } message: {
            Text("Are you sure you want to delete \"\(deletingCustomName ?? "")\"?")
        }
    }

    private var editingCustomBinding: Binding<CustomProgression?> {
        Binding(
            get: {
                guard let id = viewModel.editingCustomId else { return nil }
                return viewModel.customProgressions.first { $0.id == id }
            },
            set: { newValue in
                viewModel.editingCustomId = newValue?.id
            }
        )
    }

    private var showingDeleteAlert: Binding<Bool> {
        Binding(
            get: { deletingCustomId != nil },
            set: { if !$0 { deletingCustomId = nil; deletingCustomName = nil } }
        )
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

            ForEach(viewModel.filteredCustomProgressions) { custom in
                let progression = viewModel.toProgression(custom)
                let degrees = progression.degrees as! [ChordDegree]
                let chips = degrees.map { degree in
                    (degree.numeral, viewModel.resolvedChordName(degree: degree))
                }
                progressionCard(
                    name: custom.name,
                    description: custom.description,
                    chordChips: chips,
                    shareText: viewModel.shareTextForCustom(custom),
                    onDelete: {
                        deletingCustomId = custom.id
                        deletingCustomName = custom.name
                    },
                    onDuplicate: {
                        let dupDegrees = degrees.map { degree in
                            ChordDegree(interval: degree.interval, quality: degree.quality, numeral: degree.numeral)
                        }
                        let scaleType = viewModel.allScaleTypes.first { $0.name == custom.scaleType } ?? .major
                        viewModel.createCustomFromDegrees(
                            name: custom.name + " (Copy)",
                            description: custom.description,
                            degrees: dupDegrees,
                            scaleType: scaleType
                        )
                    },
                    onEdit: { viewModel.editingCustomId = custom.id },
                    progression: progression
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
                        viewModel.createCustomFromDegrees(
                            name: progression.name + " (Copy)",
                            description: progression.description_,
                            degrees: degrees,
                            scaleType: progression.scaleType
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
        onEdit: (() -> Void)? = nil,
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

                    Button {
                        UIPasteboard.general.string = shareText
                        AccessibilityAnnouncer.shared.announce("Copied to clipboard")
                    } label: {
                        Image(systemName: "doc.on.doc")
                            .font(.caption)
                    }
                    .accessibilityLabel("Copy \(name) to clipboard")
                }
                if let onEdit {
                    Button(action: onEdit) {
                        Image(systemName: "pencil")
                            .font(.caption)
                    }
                    .accessibilityLabel("Edit \(name)")
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
            if let onEdit {
                Button { onEdit() } label: {
                    Label("Edit", systemImage: "pencil")
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

    var editingId: String?
    var initialName: String = ""
    var initialDescription: String = ""
    var initialDegrees: [ChordDegree] = []
    var initialScaleType: ScaleType?

    @State private var name = ""
    @State private var description = ""
    @State private var scaleType: ScaleType = .major
    @State private var chosenDegrees: [ChordDegree] = []
    @State private var selectedQualitySymbol: String?
    @State private var didSetInitial = false

    private var isEditMode: Bool { editingId != nil }

    private var availableDegrees: [ChordDegree] {
        viewModel.diatonicDegreesForScale(scaleType)
    }

    private var qualityFormulas: [ChordFormula] {
        let all = ChordFormulas.shared.ALL as! [ChordFormula]
        return all.filter { $0.category != .triad }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(isEditMode ? "Edit Progression" : "Create Progression")
                        .font(.title2.bold())
                        .accessibilityAddTraits(.isHeader)

                    TextField("Name", text: $name)
                        .textFieldStyle(.roundedBorder)

                    TextField("Description", text: $description)
                        .textFieldStyle(.roundedBorder)

                    scaleTypeSection
                    chordQualitySection
                    diatonicChordChips
                    chosenSequenceSection

                    Button(action: save) {
                        Text(isEditMode ? "Update" : "Save")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || chosenDegrees.count < 2)

                    if !name.trimmingCharacters(in: .whitespaces).isEmpty && chosenDegrees.count < 2 {
                        Text("Add at least 2 chords")
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }
                .padding()
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .onAppear {
                guard !didSetInitial else { return }
                didSetInitial = true
                name = initialName
                description = initialDescription
                scaleType = initialScaleType ?? viewModel.selectedScaleType
                chosenDegrees = initialDegrees
            }
        }
    }

    private var scaleTypeSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Scale")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(viewModel.allScaleTypes, id: \.self) { scale in
                        Button {
                            if scaleType != scale {
                                scaleType = scale
                                chosenDegrees.removeAll()
                            }
                        } label: {
                            Text(scale.label)
                                .font(.caption.bold())
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(scaleType == scale ? Color.accentColor : Color(.systemGray5))
                                .foregroundStyle(scaleType == scale ? .white : .primary)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(scaleType == scale ? .isSelected : [])
                    }
                }
            }
        }
    }

    private var chordQualitySection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Chord quality")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            WrappingHStack(spacing: 6) {
                Button {
                    selectedQualitySymbol = nil
                } label: {
                    Text("Triad")
                        .font(.caption.bold())
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(selectedQualitySymbol == nil ? Color.orange : Color(.systemGray5))
                        .foregroundStyle(selectedQualitySymbol == nil ? .white : .primary)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selectedQualitySymbol == nil ? .isSelected : [])

                ForEach(qualityFormulas, id: \.symbol) { formula in
                    Button {
                        selectedQualitySymbol = formula.symbol
                    } label: {
                        Text(formula.symbol.isEmpty ? formula.quality : formula.symbol)
                            .font(.caption.bold())
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(selectedQualitySymbol == formula.symbol ? Color.orange : Color(.systemGray5))
                            .foregroundStyle(selectedQualitySymbol == formula.symbol ? .white : .primary)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(selectedQualitySymbol == formula.symbol ? .isSelected : [])
                }
            }
        }
    }

    private var diatonicChordChips: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Tap chords to add")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            WrappingHStack(spacing: 8) {
                ForEach(0..<availableDegrees.count, id: \.self) { i in
                    let degree = availableDegrees[i]
                    let quality = selectedQualitySymbol ?? degree.quality
                    let chordRoot = (Int(viewModel.selectedRoot) + Int(degree.interval)) % 12
                    let chordName = Notes.shared.enharmonicForKey(
                        pitchClass: Int32(chordRoot),
                        keyRoot: KotlinInt(int: viewModel.selectedRoot),
                        isMinor: scaleType == .minor
                    ) + quality
                    let numeral = selectedQualitySymbol != nil
                        ? degree.numeral + (selectedQualitySymbol ?? "")
                        : degree.numeral

                    Button {
                        chosenDegrees.append(
                            ChordDegree(interval: degree.interval, quality: quality, numeral: numeral)
                        )
                    } label: {
                        VStack(spacing: 2) {
                            Text(chordName)
                                .font(.caption.bold())
                            Text(numeral)
                                .font(.system(size: 9))
                                .foregroundStyle(.secondary)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color(.systemGray6))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Add \(chordName)")
                }
            }
        }
    }

    private var chosenSequenceSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Your chords (\(chosenDegrees.count))")
                .font(.caption.bold())
                .foregroundStyle(.secondary)

            if chosenDegrees.isEmpty {
                Text("Tap chords above to build your progression")
                    .font(.caption)
                    .foregroundStyle(.secondary.opacity(0.6))
                    .padding(.vertical, 8)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(Array(chosenDegrees.enumerated()), id: \.offset) { index, degree in
                            if index > 0 {
                                Text("\u{2192}")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            let chordRoot = (Int(viewModel.selectedRoot) + Int(degree.interval)) % 12
                            let chordName = Notes.shared.enharmonicForKey(
                                pitchClass: Int32(chordRoot),
                                keyRoot: KotlinInt(int: viewModel.selectedRoot),
                                isMinor: scaleType == .minor
                            ) + degree.quality

                            Button {
                                chosenDegrees.remove(at: index)
                            } label: {
                                HStack(spacing: 4) {
                                    Text(chordName)
                                        .font(.caption.bold())
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.system(size: 10))
                                        .foregroundStyle(.secondary)
                                }
                                .padding(.horizontal, 8)
                                .padding(.vertical, 5)
                                .background(Color.accentColor.opacity(0.15))
                                .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Remove \(chordName)")
                        }
                    }
                }

                Text(chosenDegrees.map(\.numeral).joined(separator: " \u{2013} "))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.top, 2)
            }
        }
    }

    private func save() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        guard !trimmedName.isEmpty, chosenDegrees.count >= 2 else { return }

        if let editingId {
            viewModel.updateCustom(
                id: editingId,
                name: trimmedName,
                description: description.trimmingCharacters(in: .whitespaces),
                degrees: chosenDegrees,
                scaleType: scaleType
            )
        } else {
            viewModel.createCustomFromDegrees(
                name: trimmedName,
                description: description.trimmingCharacters(in: .whitespaces),
                degrees: chosenDegrees,
                scaleType: scaleType
            )
        }
        dismiss()
    }
}

private struct WrappingHStack: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: .unspecified
            )
        }
    }

    private func arrangeSubviews(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            totalHeight = y + rowHeight
        }

        return (CGSize(width: maxWidth, height: totalHeight), positions)
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
                    Text("8").tag(8)
                }
                .pickerStyle(.segmented)
                .frame(width: 130)
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
        guard let parsed = ChordNameParser.shared.parse(input: resolved) else { return }
        let rootPc = parsed.rootPitchClass
        let formula = parsed.formula

        let tuning = (0..<4).map { i -> shared.UkuleleString in
            let t = UkuleleTuning.highG
            return shared.UkuleleString(
                name: t.stringNames[i] as! String,
                openPitchClass: (t.pitchClasses[i] as! NSNumber).int32Value,
                octave: (t.octaves[i] as! NSNumber).int32Value
            )
        }

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

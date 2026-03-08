import SwiftUI
import shared

struct StrumPatternsView: View {
    @StateObject private var customPatternsVM = CustomPatternsViewModel()
    @StateObject private var patternPlayer = PatternPlayer()
    @State private var selectedTab = 0
    @State private var selectedDifficulty: Difficulty? = nil
    @State private var expandedIndex: Int? = nil
    @State private var showCreateStrum = false
    @State private var showCreateFingerpick = false
    @State private var cardBpms: [String: Double] = [:]
    @State private var playingPatternId: String? = nil
    @State private var editingStrumPattern: CustomStrumPatternData? = nil
    @State private var editingFingerpickPattern: CustomFingerpickingData? = nil

    private func bpm(for key: String) -> Binding<Double> {
        Binding(
            get: { cardBpms[key, default: 100] },
            set: { cardBpms[key] = $0 }
        )
    }

    private var allStrumPatterns: [StrumPattern] {
        StrumPatterns.shared.ALL as! [StrumPattern]
    }

    private var filteredStrumPatterns: [StrumPattern] {
        guard let difficulty = selectedDifficulty else { return allStrumPatterns }
        return allStrumPatterns.filter { $0.difficulty == difficulty }
    }

    private var allFingerpickPatterns: [FingerpickingPattern] {
        FingerpickingPatterns.shared.ALL as! [FingerpickingPattern]
    }

    private var filteredFingerpickPatterns: [FingerpickingPattern] {
        guard let difficulty = selectedDifficulty else { return allFingerpickPatterns }
        return allFingerpickPatterns.filter { $0.difficulty == difficulty }
    }

    var body: some View {
        VStack(spacing: 0) {
            Picker("Pattern Type", selection: $selectedTab) {
                Text("Strumming").tag(0)
                Text("Fingerpicking").tag(1)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            .padding(.top, 8)
            .onChange(of: selectedTab) { _, _ in
                selectedDifficulty = nil
                expandedIndex = nil
            }

            ScrollView {
                VStack(spacing: 16) {
                    difficultyFilter
                    if selectedTab == 0 {
                        strumContent
                    } else {
                        fingerpickContent
                    }
                }
                .padding()
            }
        }
        .navigationTitle(selectedTab == 0 ? "Strumming Patterns" : "Fingerpicking Patterns")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    if selectedTab == 0 {
                        showCreateStrum = true
                    } else {
                        showCreateFingerpick = true
                    }
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel(selectedTab == 0 ? "Add strum pattern" : "Add fingerpicking pattern")
            }
        }
        .onDisappear { patternPlayer.stop() }
        .sheet(isPresented: $showCreateStrum) {
            CreateStrumPatternSheet(viewModel: customPatternsVM)
        }
        .sheet(isPresented: $showCreateFingerpick) {
            CreateFingerpickPatternSheet(viewModel: customPatternsVM)
        }
        .sheet(item: $editingStrumPattern) { pattern in
            CreateStrumPatternSheet(viewModel: customPatternsVM, initialPattern: pattern)
        }
        .sheet(item: $editingFingerpickPattern) { pattern in
            CreateFingerpickPatternSheet(viewModel: customPatternsVM, initialPattern: pattern)
        }
    }

    // MARK: - Strum Content

    private var strumContent: some View {
        VStack(spacing: 16) {
            if !customPatternsVM.strumPatterns.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("My Patterns")
                        .font(.headline)
                        .accessibilityAddTraits(.isHeader)
                    ForEach(customPatternsVM.strumPatterns) { custom in
                        customStrumCard(custom)
                    }
                }
            }
            patternList
        }
    }

    private func customStrumCard(_ pattern: CustomStrumPatternData) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(pattern.name)
                    .font(.subheadline.bold())
                Spacer()
                Text(pattern.timeSignature)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 4) {
                ForEach(0..<pattern.beats.count, id: \.self) { j in
                    let beat = pattern.beats[j]
                    Text(directionSymbolFromString(beat.direction))
                        .font(.title3.bold())
                        .foregroundStyle(beat.emphasis ? Color.accentColor : .primary)
                }
            }

            HStack {
                Button(action: { editingStrumPattern = pattern }) {
                    Label("Edit", systemImage: "pencil")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .controlSize(.small)

                Button(action: { duplicateCustomStrum(pattern) }) {
                    Label("Duplicate", systemImage: "doc.on.doc")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
        .contextMenu {
            Button { editingStrumPattern = pattern } label: {
                Label("Edit", systemImage: "pencil")
            }
            Button { duplicateCustomStrum(pattern) } label: {
                Label("Duplicate", systemImage: "doc.on.doc")
            }
            Button(role: .destructive) {
                customPatternsVM.deleteStrumPattern(id: pattern.id)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    private func duplicateCustomStrum(_ pattern: CustomStrumPatternData) {
        let copy = CustomStrumPatternData(
            id: UUID().uuidString,
            name: pattern.name + " (Copy)",
            beats: pattern.beats,
            createdAt: Date().timeIntervalSince1970,
            timeSignature: pattern.timeSignature
        )
        customPatternsVM.saveStrumPattern(copy)
    }

    // MARK: - Fingerpick Content

    private var fingerpickContent: some View {
        VStack(spacing: 16) {
            if !customPatternsVM.fingerpickingPatterns.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("My Patterns")
                        .font(.headline)
                        .accessibilityAddTraits(.isHeader)
                    ForEach(customPatternsVM.fingerpickingPatterns) { custom in
                        customFingerpickCard(custom)
                    }
                }
            }
            fingerpickPatternList
        }
    }

    private func customFingerpickCard(_ pattern: CustomFingerpickingData) -> some View {
        let stringNames = ["G", "C", "E", "A"]
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(pattern.name)
                    .font(.subheadline.bold())
                Spacer()
                Text(pattern.timeSignature)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 4) {
                ForEach(0..<pattern.steps.count, id: \.self) { j in
                    let step = pattern.steps[j]
                    let label = "\(step.finger)(\(stringNames[safe: step.stringIndex] ?? "?"))"
                    Text(label)
                        .font(.caption.monospaced())
                        .foregroundStyle(step.emphasis ? Color.accentColor : .primary)
                }
            }

            HStack {
                Button(action: { editingFingerpickPattern = pattern }) {
                    Label("Edit", systemImage: "pencil")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .controlSize(.small)

                Button(action: { duplicateCustomFingerpick(pattern) }) {
                    Label("Duplicate", systemImage: "doc.on.doc")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
        .contextMenu {
            Button { editingFingerpickPattern = pattern } label: {
                Label("Edit", systemImage: "pencil")
            }
            Button { duplicateCustomFingerpick(pattern) } label: {
                Label("Duplicate", systemImage: "doc.on.doc")
            }
            Button(role: .destructive) {
                customPatternsVM.deleteFingerpickingPattern(id: pattern.id)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    private func duplicateCustomFingerpick(_ pattern: CustomFingerpickingData) {
        let copy = CustomFingerpickingData(
            id: UUID().uuidString,
            name: pattern.name + " (Copy)",
            steps: pattern.steps,
            createdAt: Date().timeIntervalSince1970,
            timeSignature: pattern.timeSignature
        )
        customPatternsVM.saveFingerpickingPattern(copy)
    }

    private var fingerpickPatternList: some View {
        let patterns = filteredFingerpickPatterns
        return ForEach(0..<patterns.count, id: \.self) { i in
            fingerpickPatternCard(pattern: patterns[i], index: i)
        }
    }

    private func fingerpickPatternCard(pattern: FingerpickingPattern, index: Int) -> some View {
        let cardKey = "fp_\(pattern.name)"
        let steps = pattern.steps as! [FingerpickStep]
        let stringNames = FingerpickingPatterns.shared.STRING_NAMES as! [String]
        let isThisPlaying = patternPlayer.isPlaying && playingPatternId == cardKey
        let currentBpm = bpm(for: cardKey)

        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(pattern.name)
                    .font(.subheadline.bold())
                Spacer()
                difficultyBadge(pattern.difficulty)
                Text(pattern.timeSignature)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 4) {
                ForEach(0..<steps.count, id: \.self) { j in
                    let step = steps[j]
                    let label = "\(step.finger.label)(\(stringNames[Int(step.stringIndex)]))"
                    let isActiveBeat = isThisPlaying && patternPlayer.currentIndex == j
                    Text(label)
                        .font(.caption.monospaced())
                        .foregroundStyle(isActiveBeat ? .white : (step.emphasis ? Color.accentColor : .primary))
                        .padding(2)
                        .background(isActiveBeat ? Color.accentColor : .clear, in: RoundedRectangle(cornerRadius: 4))
                }
            }

            HStack {
                Button(action: {
                    if isThisPlaying {
                        patternPlayer.stop()
                        playingPatternId = nil
                    } else {
                        playingPatternId = cardKey
                        patternPlayer.playFingerpick(pattern: pattern, bpm: Int(currentBpm.wrappedValue))
                    }
                }) {
                    Image(systemName: isThisPlaying ? "stop.fill" : "play.fill")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .tint(isThisPlaying ? .red : .accentColor)
                .accessibilityLabel(isThisPlaying ? "Stop" : "Play")

                Text("\(Int(currentBpm.wrappedValue))")
                    .font(.caption.monospacedDigit())
                    .frame(width: 32)
                Slider(value: currentBpm, in: 40...220, step: 1)
                    .frame(maxWidth: 120)
                    .accessibilityValue("\(Int(currentBpm.wrappedValue)) BPM")
            }

            if expandedIndex == index {
                Divider()
                Text(pattern.description_)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if !pattern.notation.isEmpty {
                    Text(pattern.notation)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
        .onTapGesture {
            withAnimation {
                expandedIndex = expandedIndex == index ? nil : index
            }
        }
        .accessibilityAddTraits(.isButton)
        .accessibilityHint(expandedIndex == index ? "Double-tap to collapse" : "Double-tap to expand")
        .contextMenu {
            Button { duplicatePresetFingerpick(pattern) } label: {
                Label("Duplicate", systemImage: "doc.on.doc")
            }
        }
    }

    private func duplicatePresetFingerpick(_ pattern: FingerpickingPattern) {
        let steps = (pattern.steps as! [FingerpickStep]).map {
            FingerpickStepData(finger: $0.finger.label, stringIndex: Int($0.stringIndex), emphasis: $0.emphasis)
        }
        let copy = CustomFingerpickingData(
            id: UUID().uuidString,
            name: pattern.name + " (Copy)",
            steps: steps,
            createdAt: Date().timeIntervalSince1970,
            timeSignature: pattern.timeSignature
        )
        customPatternsVM.saveFingerpickingPattern(copy)
    }

    // MARK: - Shared UI

    private var difficultyFilter: some View {
        HStack(spacing: 8) {
            filterButton(label: "All", isSelected: selectedDifficulty == nil) {
                selectedDifficulty = nil
            }
            filterButton(label: "Beginner", isSelected: selectedDifficulty == .beginner) {
                selectedDifficulty = .beginner
            }
            filterButton(label: "Intermediate", isSelected: selectedDifficulty == .intermediate) {
                selectedDifficulty = .intermediate
            }
            filterButton(label: "Advanced", isSelected: selectedDifficulty == .advanced) {
                selectedDifficulty = .advanced
            }
        }
    }

    private func filterButton(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.caption.bold())
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.accentColor : Color(.systemGray5))
                .foregroundStyle(isSelected ? .white : .primary)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func beatDescription(_ direction: StrumDirection) -> String {
        switch direction {
        case .down: "down"
        case .up: "up"
        case .chuck: "chuck"
        case .miss: "miss"
        case .pause: "pause"
        default: "unknown"
        }
    }

    private var patternList: some View {
        let patterns = filteredStrumPatterns
        return ForEach(0..<patterns.count, id: \.self) { i in
            patternCard(pattern: patterns[i], index: i)
        }
    }

    private func patternCard(pattern: StrumPattern, index: Int) -> some View {
        let cardKey = "strum_\(pattern.name)"
        let isThisPlaying = patternPlayer.isPlaying && playingPatternId == pattern.name
        let beats = pattern.beats as! [StrumBeat]
        let currentBpm = bpm(for: cardKey)

        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(pattern.name)
                    .font(.subheadline.bold())
                Spacer()
                difficultyBadge(pattern.difficulty)
                Text(pattern.timeSignature)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 4) {
                ForEach(0..<beats.count, id: \.self) { j in
                    let beat = beats[j]
                    let isActiveBeat = isThisPlaying && patternPlayer.currentIndex == j
                    Text(directionSymbol(beat.direction))
                        .font(.title3.bold())
                        .foregroundStyle(isActiveBeat ? .white : (beat.emphasis ? Color.accentColor : .primary))
                        .padding(2)
                        .background(isActiveBeat ? Color.accentColor : .clear, in: RoundedRectangle(cornerRadius: 4))
                }
            }
            .accessibilityCombined(label: "Pattern: " + beats.map { beatDescription($0.direction) }.joined(separator: ", "))

            if !pattern.counting.isEmpty {
                Text(pattern.counting)
                    .font(.caption.monospaced())
                    .foregroundStyle(.secondary)
            }

            let genres = pattern.genres as! [String]
            if !genres.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(genres, id: \.self) { genre in
                            Text(genre)
                                .font(.caption2)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color.accentColor.opacity(0.1))
                                .clipShape(Capsule())
                        }
                    }
                }
                .accessibilityCombined(label: "Genres: " + genres.joined(separator: ", "))
            }

            HStack {
                Button(action: {
                    if isThisPlaying {
                        patternPlayer.stop()
                        playingPatternId = nil
                    } else {
                        playingPatternId = pattern.name
                        patternPlayer.playStrum(pattern: pattern, bpm: Int(currentBpm.wrappedValue))
                    }
                }) {
                    Image(systemName: isThisPlaying ? "stop.fill" : "play.fill")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .tint(isThisPlaying ? .red : .accentColor)
                .accessibilityLabel(isThisPlaying ? "Stop" : "Play")

                Text("\(Int(currentBpm.wrappedValue))")
                    .font(.caption.monospacedDigit())
                    .frame(width: 32)
                Slider(value: currentBpm, in: 40...220, step: 1)
                    .frame(maxWidth: 120)
                    .accessibilityValue("\(Int(currentBpm.wrappedValue)) BPM")
            }

            if expandedIndex == index {
                Divider()
                Text(pattern.description_)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if !pattern.notation.isEmpty {
                    Text(pattern.notation)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
        .onTapGesture {
            withAnimation {
                expandedIndex = expandedIndex == index ? nil : index
            }
        }
        .accessibilityAddTraits(.isButton)
        .accessibilityHint(expandedIndex == index ? "Double-tap to collapse" : "Double-tap to expand")
        .contextMenu {
            Button { duplicatePresetStrum(pattern) } label: {
                Label("Duplicate", systemImage: "doc.on.doc")
            }
        }
    }

    private func duplicatePresetStrum(_ pattern: StrumPattern) {
        let beats = (pattern.beats as! [StrumBeat]).map {
            StrumBeatData(direction: directionString($0.direction), emphasis: $0.emphasis)
        }
        let copy = CustomStrumPatternData(
            id: UUID().uuidString,
            name: pattern.name + " (Copy)",
            beats: beats,
            createdAt: Date().timeIntervalSince1970,
            timeSignature: pattern.timeSignature
        )
        customPatternsVM.saveStrumPattern(copy)
    }

    private func directionString(_ direction: StrumDirection) -> String {
        switch direction {
        case .down: "DOWN"
        case .up: "UP"
        case .chuck: "CHUCK"
        case .miss: "MISS"
        case .pause: "PAUSE"
        default: "DOWN"
        }
    }

    private func difficultyBadge(_ difficulty: Difficulty) -> some View {
        Text(difficulty.label)
            .font(.caption2.bold())
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(difficultyColor(difficulty).opacity(0.2))
            .foregroundStyle(difficultyColor(difficulty))
            .clipShape(Capsule())
    }

    private func difficultyColor(_ difficulty: Difficulty) -> Color {
        switch difficulty {
        case .beginner: .green
        case .intermediate: .orange
        case .advanced: .red
        default: .gray
        }
    }

    private func directionSymbol(_ direction: StrumDirection) -> String {
        switch direction {
        case .down: "\u{2193}"
        case .up: "\u{2191}"
        case .chuck: "X"
        case .miss: "\u{00D7}"
        case .pause: "\u{2014}"
        default: "?"
        }
    }

    private func directionSymbolFromString(_ direction: String) -> String {
        switch direction {
        case "DOWN": "\u{2193}"
        case "UP": "\u{2191}"
        case "CHUCK": "X"
        case "MISS": "\u{00D7}"
        case "PAUSE": "\u{2014}"
        default: "?"
        }
    }
}

// MARK: - Safe Array Subscript

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

// MARK: - Create Strum Pattern Sheet

struct CreateStrumPatternSheet: View {
    @ObservedObject var viewModel: CustomPatternsViewModel
    var initialPattern: CustomStrumPatternData? = nil
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var timeSignature = "4/4"
    @State private var beats: [StrumBeatData] = []

    private let timeSignatures = ["4/4", "3/4", "6/8"]
    private let directions = ["DOWN", "UP", "CHUCK", "MISS", "PAUSE"]

    private var isEditing: Bool { initialPattern != nil }

    var body: some View {
        NavigationStack {
            Form {
                Section("Pattern Name") {
                    TextField("Name", text: $name)
                }
                Section("Time Signature") {
                    Picker("Time Signature", selection: $timeSignature) {
                        ForEach(timeSignatures, id: \.self) { Text($0).tag($0) }
                    }
                    .pickerStyle(.segmented)
                }
                Section("Beats") {
                    ForEach(0..<beats.count, id: \.self) { i in
                        HStack {
                            Picker("Direction", selection: $beats[i].direction) {
                                ForEach(directions, id: \.self) { Text($0).tag($0) }
                            }
                            .labelsHidden()
                            Toggle("Accent", isOn: $beats[i].emphasis)
                        }
                    }
                    .onDelete { beats.remove(atOffsets: $0) }
                    Button("Add Beat") {
                        beats.append(StrumBeatData(direction: "DOWN", emphasis: false))
                    }
                }
            }
            .navigationTitle(isEditing ? "Edit Strum Pattern" : "New Strum Pattern")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let existing = initialPattern {
                            viewModel.deleteStrumPattern(id: existing.id)
                        }
                        let pattern = CustomStrumPatternData(
                            id: initialPattern?.id ?? UUID().uuidString,
                            name: name,
                            beats: beats,
                            createdAt: initialPattern?.createdAt ?? Date().timeIntervalSince1970,
                            timeSignature: timeSignature
                        )
                        viewModel.saveStrumPattern(pattern)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || beats.isEmpty)
                }
            }
            .onAppear {
                if let p = initialPattern {
                    name = p.name
                    timeSignature = p.timeSignature
                    beats = p.beats
                }
            }
        }
    }
}

// MARK: - Create Fingerpick Pattern Sheet

struct CreateFingerpickPatternSheet: View {
    @ObservedObject var viewModel: CustomPatternsViewModel
    var initialPattern: CustomFingerpickingData? = nil
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var timeSignature = "4/4"
    @State private var steps: [FingerpickStepData] = []

    private let timeSignatures = ["4/4", "3/4", "6/8"]
    private let fingers = ["T", "I", "M", "R"]
    private let stringNames = ["G", "C", "E", "A"]

    private var isEditing: Bool { initialPattern != nil }

    var body: some View {
        NavigationStack {
            Form {
                Section("Pattern Name") {
                    TextField("Name", text: $name)
                }
                Section("Time Signature") {
                    Picker("Time Signature", selection: $timeSignature) {
                        ForEach(timeSignatures, id: \.self) { Text($0).tag($0) }
                    }
                    .pickerStyle(.segmented)
                }
                Section("Steps") {
                    ForEach(0..<steps.count, id: \.self) { i in
                        HStack {
                            Picker("Finger", selection: $steps[i].finger) {
                                ForEach(fingers, id: \.self) { Text($0).tag($0) }
                            }
                            .labelsHidden()
                            .frame(width: 60)
                            Picker("String", selection: $steps[i].stringIndex) {
                                ForEach(0..<stringNames.count, id: \.self) { j in
                                    Text(stringNames[j]).tag(j)
                                }
                            }
                            .labelsHidden()
                            .frame(width: 60)
                            Toggle("Accent", isOn: $steps[i].emphasis)
                        }
                    }
                    .onDelete { steps.remove(atOffsets: $0) }
                    Button("Add Step") {
                        steps.append(FingerpickStepData(finger: "T", stringIndex: 0, emphasis: false))
                    }
                }
            }
            .navigationTitle(isEditing ? "Edit Fingerpicking Pattern" : "New Fingerpicking Pattern")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let existing = initialPattern {
                            viewModel.deleteFingerpickingPattern(id: existing.id)
                        }
                        let pattern = CustomFingerpickingData(
                            id: initialPattern?.id ?? UUID().uuidString,
                            name: name,
                            steps: steps,
                            createdAt: initialPattern?.createdAt ?? Date().timeIntervalSince1970,
                            timeSignature: timeSignature
                        )
                        viewModel.saveFingerpickingPattern(pattern)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || steps.isEmpty)
                }
            }
            .onAppear {
                if let p = initialPattern {
                    name = p.name
                    timeSignature = p.timeSignature
                    steps = p.steps
                }
            }
        }
    }
}

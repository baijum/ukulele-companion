import SwiftUI
import shared

/// Guided songwriting flow: choose key → build progression → write lyrics → transpose → save.
struct SongwriterModeFlow: View {
    @StateObject private var songbookViewModel = SongbookViewModel()
    @StateObject private var progressionsViewModel = ProgressionsViewModel()

    @State private var currentStep = 0
    @State private var selectedRoot: Int32 = 0
    @State private var selectedScale: ScaleType = .major
    @State private var chosenDegrees: [ChordDegree] = []
    @State private var songTitle = ""
    @State private var songContent = ""
    @State private var transposeSemitones: Int = 0

    private let totalSteps = 5

    var body: some View {
        VStack(spacing: 0) {
            ProgressView(value: Double(currentStep + 1), total: Double(totalSteps))
                .padding(.horizontal)

            Text("Step \(currentStep + 1) of \(totalSteps)")
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
                .accessibilityAddTraits(.updatesFrequently)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    stepContent
                }
                .padding()
            }

            Spacer()

            navigationButtons
                .padding()
        }
        .navigationTitle("Start a Song")
        .onChange(of: selectedRoot) { _ in
            chosenDegrees = []
            transposeSemitones = 0
        }
        .onChange(of: selectedScale) { _ in
            chosenDegrees = []
            transposeSemitones = 0
        }
    }

    @ViewBuilder
    private var stepContent: some View {
        switch currentStep {
        case 0: stepChooseKeyScale
        case 1: stepBuildProgression
        case 2: stepAddLyrics
        case 3: stepTranspose
        case 4: stepReviewAndSave
        default: EmptyView()
        }
    }

    // MARK: - Step 1: Choose Key & Scale

    private var stepChooseKeyScale: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Choose a key and scale")
                .font(.title3.bold())
                .accessibilityAddTraits(.isHeader)

            Text("Pick the musical key and scale for your song. This determines which chords will be suggested.")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Text("Root")
                .font(.headline)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(0..<12, id: \.self) { pc in
                        let name = Notes.shared.pitchClassToName(pitchClass: Int32(pc))
                        Button(name) { selectedRoot = Int32(pc) }
                            .buttonStyle(.bordered)
                            .tint(selectedRoot == Int32(pc) ? .accentColor : .secondary)
                    }
                }
            }

            Text("Scale")
                .font(.headline)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(progressionsViewModel.allScaleTypes, id: \.name) { scale in
                        Button(scale.label) { selectedScale = scale }
                            .buttonStyle(.bordered)
                            .tint(selectedScale == scale ? .accentColor : .secondary)
                    }
                }
            }
        }
    }

    // MARK: - Step 2: Build Progression

    private var stepBuildProgression: some View {
        let degrees = progressionsViewModel.diatonicDegreesForScale(selectedScale)

        return VStack(alignment: .leading, spacing: 12) {
            Text("Build a chord progression")
                .font(.title3.bold())
                .accessibilityAddTraits(.isHeader)

            Text("Tap the diatonic chords below to build your progression.")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Text("Tap chords to add")
                .font(.headline)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(0..<degrees.count, id: \.self) { i in
                        let degree = degrees[i]
                        let chordRoot = (selectedRoot + degree.interval) % 12
                        let chordName = Notes.shared.enharmonicForKey(
                            pitchClass: Int32(chordRoot),
                            keyRoot: KotlinInt(int: selectedRoot),
                            isMinor: selectedScale == .minor
                        ) + degree.quality
                        Button(chordName) {
                            chosenDegrees.append(degree)
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }

            if !chosenDegrees.isEmpty {
                Text("Your progression (\(chosenDegrees.count) chords)")
                    .font(.headline)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(0..<chosenDegrees.count, id: \.self) { i in
                            let degree = chosenDegrees[i]
                            let chordRoot = (selectedRoot + degree.interval) % 12
                            let chordName = Notes.shared.enharmonicForKey(
                                pitchClass: Int32(chordRoot),
                                keyRoot: KotlinInt(int: selectedRoot),
                                isMinor: selectedScale == .minor
                            ) + degree.quality
                            Button(chordName) {
                                chosenDegrees.remove(at: i)
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                }

                Button("Reset") { chosenDegrees.removeAll() }
                    .font(.caption)
            } else {
                Text("Tap the chords above to build your progression")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            if chosenDegrees.count == 1 {
                Text("Add at least 2 chords")
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
    }

    // MARK: - Step 3: Add Lyrics

    private var stepAddLyrics: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Add lyrics and chords")
                .font(.title3.bold())
                .accessibilityAddTraits(.isHeader)

            Text("Write your lyrics below. Tap the chord chips to insert [Chord] markers.")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            TextField("Song title", text: $songTitle)
                .textFieldStyle(.roundedBorder)

            if !chosenDegrees.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(0..<chosenDegrees.count, id: \.self) { i in
                            let degree = chosenDegrees[i]
                            let chordRoot = (selectedRoot + degree.interval) % 12
                            let chordName = Notes.shared.enharmonicForKey(
                                pitchClass: Int32(chordRoot),
                                keyRoot: KotlinInt(int: selectedRoot),
                                isMinor: selectedScale == .minor
                            ) + degree.quality
                            Button(chordName) {
                                songContent += "[\(chordName)]"
                            }
                            .buttonStyle(.bordered)
                            .controlSize(.small)
                            .accessibilityHint("Inserts [\(chordName)] into lyrics")
                        }
                    }
                }
            }

            TextEditor(text: $songContent)
                .frame(minHeight: 200)
                .accessibilityLabel("Song lyrics editor")
                .accessibilityHint("Edit lyrics; chord buttons above insert bracketed chords")
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(.systemGray4), lineWidth: 1)
                )
        }
    }

    // MARK: - Step 4: Transpose

    private var stepTranspose: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Transpose (optional)")
                .font(.title3.bold())
                .accessibilityAddTraits(.isHeader)

            Text("Shift the key up or down if needed. The transposition will be applied when you save.")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            HStack {
                Spacer()
                Button { transposeSemitones -= 1 } label: {
                    Image(systemName: "minus.circle")
                }
                .accessibilityLabel("Transpose down")

                Text("\(transposeSemitones > 0 ? "+" : "")\(transposeSemitones)")
                    .font(.headline.monospacedDigit())
                    .frame(minWidth: 40)
                    .multilineTextAlignment(.center)

                Button { transposeSemitones += 1 } label: {
                    Image(systemName: "plus.circle")
                }
                .accessibilityLabel("Transpose up")

                if transposeSemitones != 0 {
                    Button("Reset") { transposeSemitones = 0 }
                        .font(.caption)
                }
                Spacer()
            }

            if !songContent.isEmpty && transposeSemitones != 0 {
                Text("Preview:")
                    .font(.headline)

                let transposed = ChordSheetTranspose.shared.transpose(
                    content: songContent,
                    semitones: Int32(transposeSemitones)
                )
                Text(transposed)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    // MARK: - Step 5: Review & Save

    private var stepReviewAndSave: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Review and save")
                .font(.title3.bold())
                .accessibilityAddTraits(.isHeader)

            Text("Check your song below, then tap Save to add it to your songbook.")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            let keyName = Notes.shared.pitchClassToName(pitchClass: selectedRoot)
            Text("Key: \(keyName) \(selectedScale.label)")
                .font(.subheadline.bold())
                .foregroundStyle(Color.accentColor)

            if !chosenDegrees.isEmpty {
                let chordNames = chosenDegrees.map { degree -> String in
                    let chordRoot = (selectedRoot + degree.interval) % 12
                    return Notes.shared.enharmonicForKey(
                        pitchClass: Int32(chordRoot),
                        keyRoot: KotlinInt(int: selectedRoot),
                        isMinor: selectedScale == .minor
                    ) + degree.quality
                }
                Text("Progression: \(chordNames.joined(separator: " – "))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if transposeSemitones != 0 {
                Text("Transpose: \(transposeSemitones > 0 ? "+" : "")\(transposeSemitones) semitones")
                    .font(.caption)
                    .foregroundStyle(.orange)
            }

            Divider()

            Text(songTitle.isEmpty ? "Untitled" : songTitle)
                .font(.headline)

            let finalContent: String = {
                if transposeSemitones != 0 {
                    return ChordSheetTranspose.shared.transpose(
                        content: songContent,
                        semitones: Int32(transposeSemitones)
                    )
                }
                return songContent
            }()
            Text(finalContent.isEmpty ? "(no lyrics yet)" : finalContent)
                .font(.body)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Navigation Buttons

    private var navigationButtons: some View {
        HStack {
            if currentStep > 0 {
                Button("Back") { currentStep -= 1 }
                    .buttonStyle(.bordered)
            }

            Spacer()

            if currentStep < totalSteps - 1 {
                Button("Next") { currentStep += 1 }
                    .buttonStyle(.borderedProminent)
                    .disabled(currentStep == 1 && chosenDegrees.count < 2)
            } else {
                Button("Save") {
                    let finalContent: String
                    if transposeSemitones != 0 {
                        finalContent = ChordSheetTranspose.shared.transpose(
                            content: songContent,
                            semitones: Int32(transposeSemitones)
                        )
                    } else {
                        finalContent = songContent
                    }

                    let finalRoot = (selectedRoot + Int32(transposeSemitones) % 12 + 12) % 12
                    let finalKey = Notes.shared.pitchClassToName(pitchClass: finalRoot) + " " + selectedScale.label

                    let song = StoredSong(
                        id: UUID().uuidString,
                        title: songTitle.isEmpty ? "My Song" : songTitle,
                        artist: "",
                        content: finalContent,
                        key: finalKey,
                        capo: 0,
                        strumPatternName: "",
                        labels: [],
                        createdAt: Date().timeIntervalSince1970 * 1000,
                        updatedAt: Date().timeIntervalSince1970 * 1000
                    )
                    songbookViewModel.save(song: song)

                    if chosenDegrees.count >= 2 {
                        let keyName = Notes.shared.pitchClassToName(pitchClass: selectedRoot)
                        progressionsViewModel.createCustomFromDegrees(
                            name: "\(keyName) \(selectedScale.label) song progression",
                            description: "Created in Songwriter Mode",
                            degrees: chosenDegrees,
                            scaleType: selectedScale
                        )
                    }

                    currentStep = 0
                    songTitle = ""
                    songContent = ""
                    chosenDegrees = []
                    transposeSemitones = 0
                }
                .buttonStyle(.borderedProminent)
                .disabled(songTitle.isEmpty && songContent.isEmpty)
            }
        }
    }
}

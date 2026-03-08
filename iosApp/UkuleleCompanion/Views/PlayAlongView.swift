import SwiftUI
import shared
import Combine

struct PlayAlongView: View {
    @StateObject private var viewModel = PlayAlongViewModel()
    @State private var selectedRoot: Int32 = 0
    @State private var selectedScaleType: ScaleType = .major
    @State private var selectedProgression: Progression? = nil
    @State private var bpm: Double = 100
    @State private var beatsPerChord: Int = 4

    private var rootNoteNames: [String] {
        (0..<Int32(12)).map { Notes.shared.pitchClassToName(pitchClass: $0) }
    }

    private var presetProgressions: [Progression] {
        Progressions.shared.forScale(scaleType: selectedScaleType) as! [Progression]
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack {
                    Text("Key")
                        .font(.headline)
                    Picker("Key", selection: $selectedRoot) {
                        ForEach(0..<12, id: \.self) { i in
                            Text(rootNoteNames[i]).tag(Int32(i))
                        }
                    }
                }
                .padding(.horizontal)

                HStack {
                    Text("Scale")
                        .font(.headline)
                    Picker("Scale", selection: $selectedScaleType) {
                        Text("Major").tag(ScaleType.major)
                        Text("Minor").tag(ScaleType.minor)
                    }
                    .pickerStyle(.segmented)
                }
                .padding(.horizontal)
                .onChange(of: selectedScaleType) { _, _ in
                    selectedProgression = nil
                }

                HStack {
                    Text("Progression")
                        .font(.headline)
                    Picker("Progression", selection: $selectedProgression) {
                        Text("Select...").tag(nil as Progression?)
                        ForEach(presetProgressions, id: \.name) { prog in
                            Text(prog.name).tag(prog as Progression?)
                        }
                    }
                }
                .padding(.horizontal)

                VStack {
                    Text("BPM: \(Int(bpm))")
                        .font(.headline)
                    Slider(value: $bpm, in: 40...200, step: 5)
                        .disabled(viewModel.isPlaying)
                        .accessibilityValue("\(Int(bpm)) beats per minute")
                }
                .padding(.horizontal)

                HStack {
                    Text("Beats/Chord")
                        .font(.subheadline)
                    Picker("Beats", selection: $beatsPerChord) {
                        Text("2").tag(2)
                        Text("4").tag(4)
                        Text("8").tag(8)
                    }
                    .pickerStyle(.segmented)
                    .disabled(viewModel.isPlaying)
                }
                .padding(.horizontal)

                if let prog = selectedProgression {
                    let degrees = prog.degrees as! [ChordDegree]

                    VStack(spacing: 12) {
                        if viewModel.isPlaying && viewModel.currentChordIndex < degrees.count {
                            let degree = degrees[viewModel.currentChordIndex]
                            let chordName = viewModel.resolvedChordName(degree)

                            // Expected chord
                            Text(chordName)
                                .font(.system(size: 56, weight: .bold))
                                .foregroundStyle(Color.accentColor)
                                .accessibilityAddTraits(.updatesFrequently)
                                .accessibilityLabel("Expected chord: \(chordName)")
                                .onChange(of: viewModel.currentChordIndex) { _, _ in
                                    AccessibilityAnnouncer.shared.announce(chordName, minInterval: 0.5)
                                }

                            // Beat indicators
                            HStack(spacing: 4) {
                                ForEach(0..<beatsPerChord, id: \.self) { beat in
                                    Circle()
                                        .fill(beat < viewModel.currentBeat ? Color.accentColor : Color(.systemGray4))
                                        .frame(width: 10, height: 10)
                                }
                            }

                            // Detected chord feedback
                            chordFeedbackView

                            // Chord progression strip
                            HStack {
                                ForEach(Array(degrees.enumerated()), id: \.offset) { i, d in
                                    Text(viewModel.resolvedChordName(d))
                                        .font(.caption)
                                        .padding(4)
                                        .background(
                                            i == viewModel.currentChordIndex
                                                ? Color.accentColor.opacity(0.3)
                                                : Color(.systemGray5)
                                        )
                                        .cornerRadius(4)
                                }
                            }
                        } else {
                            HStack {
                                ForEach(Array(degrees.enumerated()), id: \.offset) { _, d in
                                    Text(viewModel.resolvedChordName(d))
                                        .font(.title3)
                                        .padding(8)
                                        .background(Color(.systemGray5))
                                        .cornerRadius(6)
                                }
                            }
                        }
                    }
                    .padding(.vertical, 16)

                    // Score display
                    if let score = viewModel.score {
                        scoreView(score)
                    }

                    // Controls
                    HStack(spacing: 16) {
                        if viewModel.isPlaying {
                            Button("Stop") { viewModel.stop() }
                                .buttonStyle(.borderedProminent)
                                .tint(.red)
                        } else {
                            Button("Play") {
                                viewModel.start(
                                    degrees: degrees,
                                    bpm: bpm,
                                    beatsPerChord: beatsPerChord,
                                    rootPitchClass: selectedRoot,
                                    isMinorScale: selectedScaleType == .minor
                                )
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Play Along")
        .onDisappear { viewModel.stop() }
    }

    // MARK: - Chord Feedback

    private var chordFeedbackView: some View {
        HStack(spacing: 8) {
            if let detected = viewModel.detectedChord {
                Image(systemName: viewModel.isChordCorrect == true ? "checkmark.circle.fill" : "xmark.circle.fill")
                    .foregroundStyle(viewModel.isChordCorrect == true ? .green : .red)
                    .font(.title2)

                Text("Heard: \(detected)")
                    .font(.title3)
                    .foregroundStyle(viewModel.isChordCorrect == true ? .green : .red)
            } else {
                Image(systemName: "mic.fill")
                    .foregroundStyle(.secondary)
                    .font(.title2)
                Text("Listening...")
                    .font(.title3)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 8)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(chordFeedbackAccessibilityLabel)
        .accessibilityAddTraits(.updatesFrequently)
    }

    private var chordFeedbackAccessibilityLabel: String {
        if let detected = viewModel.detectedChord {
            let correct = viewModel.isChordCorrect == true ? "correct" : "incorrect"
            return "Heard \(detected), \(correct)"
        }
        return "Listening for chord"
    }

    // MARK: - Score

    @ViewBuilder
    private func scoreView(_ score: PlayAlongScorer.PlayAlongScore) -> some View {
        VStack(spacing: 8) {
            HStack(spacing: 20) {
                VStack {
                    Text(score.grade)
                        .font(.system(size: 36, weight: .bold, design: .rounded))
                        .foregroundStyle(gradeColor(score.grade))
                    Text("Grade")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack {
                    Text("\(score.accuracyPercent)%")
                        .font(.title2.bold().monospacedDigit())
                    Text("Accuracy")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack {
                    Text("\(score.bestStreak)")
                        .font(.title2.bold().monospacedDigit())
                    Text("Best streak")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Score: \(score.accuracyPercent) percent, grade \(score.grade), best streak \(score.bestStreak)")
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }

    private func gradeColor(_ grade: String) -> Color {
        switch grade {
        case "S": return .green
        case "A": return .blue
        case "B": return .cyan
        case "C": return .yellow
        case "D": return .orange
        default: return .red
        }
    }
}

import SwiftUI
import shared

struct ChordTransitionsView: View {
    @StateObject private var viewModel = ChordTransitionsViewModel()
    @State private var root1: Int = 0
    @State private var quality1: Int = 0
    @State private var root2: Int = 7
    @State private var quality2: Int = 0

    private let rootNames = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
    private let qualityNames = ["", "m", "7", "m7", "maj7", "dim", "aug", "sus2", "sus4"]
    private let qualityLabels = ["Major", "Minor", "7", "m7", "maj7", "dim", "aug", "sus2", "sus4"]

    private func chordName(root: Int, quality: Int) -> String {
        rootNames[root] + qualityNames[quality]
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                chordSelector(label: "Chord 1", root: $root1, quality: $quality1)
                    .disabled(viewModel.isRunning)

                if let v1 = viewModel.voicingForChord(name: chordName(root: root1, quality: quality1)) {
                    ChordDiagramView(voicing: v1, chordName: chordName(root: root1, quality: quality1))
                        .onTapGesture { viewModel.playChord(name: chordName(root: root1, quality: quality1)) }
                        .accessibilityLabel("Chord 1 diagram, tap to play")
                }

                Image(systemName: "arrow.left.arrow.right")
                    .font(.title2)
                    .foregroundStyle(.secondary)

                chordSelector(label: "Chord 2", root: $root2, quality: $quality2)
                    .disabled(viewModel.isRunning)

                if let v2 = viewModel.voicingForChord(name: chordName(root: root2, quality: quality2)) {
                    ChordDiagramView(voicing: v2, chordName: chordName(root: root2, quality: quality2))
                        .onTapGesture { viewModel.playChord(name: chordName(root: root2, quality: quality2)) }
                        .accessibilityLabel("Chord 2 diagram, tap to play")
                }

                VStack {
                    HStack {
                        Text("BPM: \(Int(viewModel.bpm))")
                            .font(.headline)
                        Spacer()
                        Button(action: { viewModel.tapTempo() }) {
                            Label("Tap", systemImage: "hand.tap")
                                .font(.caption.bold())
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .accessibilityLabel("Tap tempo")
                    }
                    Slider(value: $viewModel.bpm, in: 40...200, step: 1)
                        .disabled(viewModel.isRunning)
                        .accessibilityValue("\(Int(viewModel.bpm)) beats per minute")
                }
                .padding(.horizontal)

                HStack {
                    Text("Beats per chord")
                        .font(.subheadline)
                    Picker("Beats", selection: $viewModel.beatsPerChord) {
                        Text("1").tag(1)
                        Text("2").tag(2)
                        Text("4").tag(4)
                        Text("8").tag(8)
                    }
                    .pickerStyle(.segmented)
                    .disabled(viewModel.isRunning)
                }
                .padding(.horizontal)

                Toggle("Play chord on transition", isOn: $viewModel.playChordOnTransition)
                    .font(.subheadline)
                    .padding(.horizontal)

                if viewModel.isRunning {
                    VStack(spacing: 8) {
                        Text(viewModel.currentChord)
                            .font(.system(size: 72, weight: .bold))
                            .foregroundStyle(Color.accentColor)
                            .accessibilityAddTraits(.updatesFrequently)
                            .accessibilityLabel("Current chord: \(viewModel.currentChord)")
                            .onChange(of: viewModel.currentChord) { newChord in
                                AccessibilityAnnouncer.shared.announce(newChord, minInterval: 0.5)
                            }

                        HStack(spacing: 4) {
                            ForEach(0..<viewModel.beatsPerChord, id: \.self) { beat in
                                Circle()
                                    .fill(beat < viewModel.currentBeat ? Color.accentColor : Color(.systemGray4))
                                    .frame(width: 12, height: 12)
                            }
                        }
                    }
                    .padding(.vertical, 24)
                }

                // Stats
                VStack(spacing: 4) {
                    Text("Transitions: \(viewModel.transitionCount)")
                        .font(.title2.weight(.medium))
                    if viewModel.elapsedSeconds > 0 {
                        HStack(spacing: 16) {
                            Label(formatTime(viewModel.elapsedSeconds), systemImage: "clock")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Label(String(format: "%.1f/min", viewModel.switchesPerMinute), systemImage: "arrow.triangle.swap")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                HStack(spacing: 16) {
                    if viewModel.isRunning {
                        Button("Stop") { viewModel.stop() }
                            .buttonStyle(.borderedProminent)
                            .tint(.red)
                    } else {
                        Button("Start") { viewModel.start() }
                            .buttonStyle(.borderedProminent)
                    }

                    Button("Reset") { viewModel.reset() }
                        .buttonStyle(.bordered)
                        .disabled(viewModel.isRunning)
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Chord Transitions")
        .onAppear { syncChordNames() }
        .onChange(of: root1) { _ in syncChordNames() }
        .onChange(of: quality1) { _ in syncChordNames() }
        .onChange(of: root2) { _ in syncChordNames() }
        .onChange(of: quality2) { _ in syncChordNames() }
    }

    private func formatTime(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%d:%02d", m, s)
    }

    private func syncChordNames() {
        viewModel.chord1 = chordName(root: root1, quality: quality1)
        viewModel.chord2 = chordName(root: root2, quality: quality2)
    }

    private func chordSelector(label: String, root: Binding<Int>, quality: Binding<Int>) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .accessibilityAddTraits(.isHeader)

            Text(chordName(root: root.wrappedValue, quality: quality.wrappedValue))
                .font(.title2.bold())

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(0..<rootNames.count, id: \.self) { i in
                        Button(action: { root.wrappedValue = i }) {
                            Text(rootNames[i])
                                .font(.caption.bold())
                                .frame(minWidth: 28, minHeight: 28)
                                .background(root.wrappedValue == i ? Color.accentColor : Color(.systemGray5))
                                .foregroundStyle(root.wrappedValue == i ? .white : .primary)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(root.wrappedValue == i ? .isSelected : [])
                    }
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(0..<qualityLabels.count, id: \.self) { i in
                        Button(action: { quality.wrappedValue = i }) {
                            Text(qualityLabels[i])
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(quality.wrappedValue == i ? Color.accentColor.opacity(0.2) : Color(.systemGray6))
                                .foregroundStyle(quality.wrappedValue == i ? Color.accentColor : .primary)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(quality.wrappedValue == i ? .isSelected : [])
                    }
                }
            }
        }
        .padding(.horizontal)
    }
}

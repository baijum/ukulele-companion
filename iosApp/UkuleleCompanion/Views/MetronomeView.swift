import SwiftUI

struct MetronomeView: View {
    @StateObject private var viewModel = MetronomeViewModel()

    private let timeSignatures = ["2/4", "3/4", "4/4", "5/4", "6/4", "7/4", "6/8", "12/8"]
    private let subdivisionLabels: [(Int, String)] = [
        (1, "Quarter"), (2, "Eighth"), (3, "Triplet"), (4, "Sixteenth")
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // BPM display
                Text("\(Int(viewModel.bpm))")
                    .font(.system(size: 72, weight: .bold, design: .rounded))
                    .foregroundStyle(.primary)
                    .accessibilityLabel("Tempo")
                    .accessibilityValue("\(Int(viewModel.bpm)) beats per minute")

                HStack {
                    Button { viewModel.setBpm(viewModel.bpm - 1) } label: {
                        Image(systemName: "minus.circle")
                            .font(.title2)
                    }
                    .accessibilityLabel("Decrease tempo")
                    Text("BPM")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                    Button { viewModel.setBpm(viewModel.bpm + 1) } label: {
                        Image(systemName: "plus.circle")
                            .font(.title2)
                    }
                    .accessibilityLabel("Increase tempo")
                }

                // BPM slider
                Slider(value: $viewModel.bpm, in: 30...300, step: 1)
                    .padding(.horizontal)
                    .accessibilityLabel("Tempo")
                    .accessibilityValue("\(Int(viewModel.bpm)) BPM")
                    .onChange(of: viewModel.bpm) { _, newValue in
                        if viewModel.isPlaying {
                            viewModel.setBpm(newValue)
                        }
                    }

                // Tap Tempo
                Button("Tap Tempo") {
                    viewModel.tapTempo()
                }
                .buttonStyle(.bordered)

                // Time Signature
                VStack(spacing: 8) {
                    Text("Time Signature")
                        .font(.subheadline.bold())
                        .foregroundStyle(.secondary)
                        .accessibilityAddTraits(.isHeader)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(timeSignatures, id: \.self) { label in
                                Button(label) {
                                    viewModel.setTimeSignature(label)
                                }
                                .buttonStyle(.bordered)
                                .tint(viewModel.timeSignatureLabel == label ? .accentColor : .secondary)
                                .accessibilityAddTraits(viewModel.timeSignatureLabel == label ? .isSelected : [])
                            }
                        }
                        .padding(.horizontal)
                    }
                }

                // Beat indicators
                VStack(spacing: 8) {
                    Text("Accent Pattern")
                        .font(.subheadline.bold())
                        .foregroundStyle(.secondary)
                        .accessibilityAddTraits(.isHeader)
                    Text("Tap to change")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                    HStack(spacing: 12) {
                        ForEach(0..<viewModel.accentPattern.count, id: \.self) { index in
                            BeatIndicatorView(
                                index: index,
                                type: viewModel.accentPattern[index],
                                isActive: viewModel.isPlaying && viewModel.currentBeat == index
                            ) {
                                viewModel.toggleBeatType(index)
                            }
                        }
                    }
                }

                // Subdivision
                VStack(spacing: 8) {
                    Text("Subdivision")
                        .font(.subheadline.bold())
                        .foregroundStyle(.secondary)
                        .accessibilityAddTraits(.isHeader)
                    HStack(spacing: 8) {
                        ForEach(subdivisionLabels, id: \.0) { value, label in
                            Button(label) {
                                viewModel.setSubdivision(value)
                            }
                            .buttonStyle(.bordered)
                            .tint(viewModel.subdivision == value ? .accentColor : .secondary)
                            .disabled(viewModel.isCompound)
                            .accessibilityAddTraits(viewModel.subdivision == value ? .isSelected : [])
                        }
                    }
                    if viewModel.isCompound {
                        Text("Subdivision is fixed for compound meters")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                // Play/Stop
                Button(action: { viewModel.togglePlayback() }) {
                    Image(systemName: viewModel.isPlaying ? "stop.fill" : "play.fill")
                        .font(.title)
                        .frame(width: 72, height: 72)
                        .background(viewModel.isPlaying ? Color.red : Color.accentColor)
                        .foregroundColor(.white)
                        .clipShape(Circle())
                }
                .accessibilityLabel(viewModel.isPlaying ? "Stop metronome" : "Start metronome")
                .padding(.top, 8)

                // Measure counter
                if viewModel.isPlaying && viewModel.measureCount > 0 {
                    Text("Measure \(viewModel.measureCount)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Spacer(minLength: 32)
            }
            .padding(.top, 16)
        }
        .navigationTitle("Metronome")
        .onDisappear {
            viewModel.stop()
        }
    }
}

struct BeatIndicatorView: View {
    let index: Int
    let type: BeatType
    let isActive: Bool
    let onTap: () -> Void

    var body: some View {
        VStack(spacing: 4) {
            Circle()
                .strokeBorder(borderColor, lineWidth: 2)
                .background(Circle().fill(fillColor))
                .frame(width: 44, height: 44)
                .scaleEffect(isActive ? 1.3 : 1.0)
                .animation(.easeInOut(duration: 0.1), value: isActive)
                .onTapGesture { onTap() }
            Text("\(index + 1)")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Beat \(index + 1), \(type.rawValue)")
        .accessibilityHint("Double tap to change accent type")
        .accessibilityAddTraits(.isButton)
        .accessibilityAction(.default) { onTap() }
    }

    private var borderColor: Color {
        switch type {
        case .accent: return .blue
        case .normal: return .green
        case .mute: return .gray
        }
    }

    private var fillColor: Color {
        switch (isActive, type) {
        case (true, .accent): return .blue
        case (true, .normal): return .green
        case (true, .mute): return .gray.opacity(0.5)
        case (false, .accent): return .blue.opacity(0.3)
        case (false, .normal): return .green.opacity(0.2)
        case (false, .mute): return .clear
        }
    }
}

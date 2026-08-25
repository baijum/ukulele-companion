import SwiftUI
import shared

struct VoiceLeadingView: View {
    let progression: Progression
    let keyRoot: Int32

    @State private var currentStep = 0
    @State private var path: VoiceLeading.Path?

    @StateObject private var tonePlayer = TonePlayer()
    // Voice-leading paths and note names follow the selected tuning (#576).
    // Left-handed mirroring now comes from the diagram's environment.
    private let tuning: [shared.UkuleleString] = FretboardPreferences.tuning.asUkuleleStrings

    var body: some View {
        Group {
            if let path = path {
                pathContent(path)
            } else {
                VStack(spacing: 16) {
                    Text("Unable to compute voice leading path.")
                        .foregroundStyle(.secondary)
                    Text("Some chords may not have playable voicings in this key.")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
                .padding()
            }
        }
        .navigationTitle("Voice Leading")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { computePath() }
    }

    private func computePath() {
        path = VoiceLeading.shared.computeOptimalPath(
            progression: progression,
            keyRoot: keyRoot,
            tuning: tuning
        )
    }

    @ViewBuilder
    private func pathContent(_ path: VoiceLeading.Path) -> some View {
        let steps = path.steps.asArray(of: VoiceLeading.Step.self)
        let transitions = path.transitions.asArray(of: VoiceLeading.TransitionInfo.self)
        let hasTransitions = !transitions.isEmpty

        VStack(spacing: 0) {
            // Header info
            VStack(spacing: 4) {
                Text(path.progressionName)
                    .font(.headline)
                Text("Key of \(Notes.shared.enharmonicForKey(pitchClass: path.keyRoot, keyRoot: nil, isMinor: false))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 8)

            Divider()

            // Chord timeline
            chordTimeline(steps: steps, hasTransitions: hasTransitions)
                .padding(.vertical, 8)

            // Main content
            ScrollView {
                VStack(spacing: 16) {
                    if hasTransitions {
                        let fromStep = steps[currentStep]
                        let toStep = steps[currentStep + 1]
                        let transition = transitions[currentStep]

                        transitionDiagrams(from: fromStep, to: toStep, transition: transition)
                        transitionSummaryCard(from: fromStep, to: toStep, transition: transition)
                        totalPathSummary(path: path)
                    } else {
                        Text("Single chord - no transitions to show.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .padding(.vertical, 16)

                        if let voicing = steps.first?.voicing {
                            ChordDiagramView(voicing: voicing, chordName: steps[0].chordName)
                        }
                    }
                }
                .padding(.horizontal)
            }

            // Bottom controls
            if hasTransitions {
                Divider()
                bottomControls(totalTransitions: transitions.count)
            }
        }
    }

    // MARK: - Chord Timeline

    private func chordTimeline(steps: [VoiceLeading.Step], hasTransitions: Bool) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(0..<steps.count, id: \.self) { i in
                    if i > 0 {
                        Text("\u{2192}")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }

                    let isFrom = hasTransitions && i == currentStep
                    let isTo = hasTransitions && i == currentStep + 1

                    Button {
                        let transCount = (self.path?.transitions as? [Any])?.count ?? 0
                        if i < transCount {
                            currentStep = i
                        } else if i > 0 {
                            currentStep = i - 1
                        }
                    } label: {
                        VStack(spacing: 2) {
                            Text(steps[i].numeral)
                                .font(.caption2)
                            Text(steps[i].chordName)
                                .font(.caption.bold())
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            isFrom ? Color.accentColor :
                            isTo ? Color.accentColor.opacity(0.3) :
                            Color(.systemGray5)
                        )
                        .foregroundStyle(isFrom ? .white : .primary)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                    .accessibilityCombined(
                        label: "\(steps[i].numeral) \(steps[i].chordName)",
                        value: isFrom ? "Current from" : isTo ? "Current to" : nil
                    )
                }
            }
            .padding(.horizontal)
        }
    }

    // MARK: - Transition Diagrams

    private func transitionDiagrams(
        from fromStep: VoiceLeading.Step,
        to toStep: VoiceLeading.Step,
        transition: VoiceLeading.TransitionInfo
    ) -> some View {
        let commonIndices = ((transition.commonToneIndices as? Set<KotlinInt>) ?? Set())
            .map { $0.intValue }
        let commonSet = Set(commonIndices)

        return HStack(alignment: .top) {
            // From diagram
            VStack(spacing: 4) {
                Text(fromStep.numeral)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ChordDiagramView(voicing: fromStep.voicing, chordName: fromStep.chordName,
                                 commonToneIndices: commonSet)
                Button {
                    playVoicing(fromStep.voicing)
                } label: {
                    Image(systemName: "play.fill")
                        .font(.caption)
                }
                .accessibilityLabel("Play \(fromStep.chordName)")
            }
            .frame(maxWidth: .infinity)

            // Arrow
            Image(systemName: "arrow.right")
                .foregroundStyle(.secondary)
                .padding(.top, 60)
                .accessibilityHidden(true)

            // To diagram
            VStack(spacing: 4) {
                Text(toStep.numeral)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ChordDiagramView(voicing: toStep.voicing, chordName: toStep.chordName,
                                 commonToneIndices: commonSet)
                Button {
                    playVoicing(toStep.voicing)
                } label: {
                    Image(systemName: "play.fill")
                        .font(.caption)
                }
                .accessibilityLabel("Play \(toStep.chordName)")
            }
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Transition Summary Card

    private func transitionSummaryCard(
        from fromStep: VoiceLeading.Step,
        to toStep: VoiceLeading.Step,
        transition: VoiceLeading.TransitionInfo
    ) -> some View {
        let commonIndices = (transition.commonToneIndices as? Set<KotlinInt>) ?? Set()
        let commonCount = commonIndices.count
        let movements = transition.movements.asInts

        return VStack(alignment: .leading, spacing: 8) {
            let commonLabel = commonCount == 1 ? "1 common tone" : "\(commonCount) common tones"
            let distLabel = transition.totalDistance == 1
                ? "1 fret movement"
                : "\(transition.totalDistance) frets movement"

            Text("\(commonLabel) \u{00B7} \(distLabel)")
                .font(.subheadline.bold())

            let fromFrets = fromStep.voicing.fretInts
            let toFrets = toStep.voicing.fretInts

            ForEach(0..<tuning.count, id: \.self) { i in
                let fromFret = fromFrets[i]
                let toFret = toFrets[i]
                let movement = movements[i]
                let isCommon = commonIndices.contains(KotlinInt(int: Int32(i)))

                HStack(spacing: 4) {
                    Text("\(tuning[i].name):")
                        .font(.caption.bold())
                        .frame(width: 24, alignment: .leading)

                    if isCommon {
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 8, height: 8)
                        let fretLabel = fromFret == 0 ? "open" : "fret \(fromFret)"
                        let noteName = noteName(stringIndex: i, fret: fromFret)
                        Text("stays \(fretLabel) (\(noteName))")
                            .font(.caption)
                            .foregroundStyle(.blue)
                    } else {
                        let fromLabel = fromFret == 0 ? "open" : "fret \(fromFret)"
                        let toLabel = toFret == 0 ? "open" : "fret \(toFret)"
                        let fromNote = noteName(stringIndex: i, fret: fromFret)
                        let toNote = noteName(stringIndex: i, fret: toFret)
                        let arrow = movement > 0 ? "\u{2191}\(movement)" : movement < 0 ? "\u{2193}\(-movement)" : ""
                        Text("\(fromLabel) (\(fromNote)) \u{2192} \(toLabel) (\(toNote))  \(arrow)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if commonCount > 0 {
                let commonStrings = commonIndices.sorted { $0.int32Value < $1.int32Value }
                    .map { tuning[Int($0.int32Value)].name }
                    .joined(separator: ", ")
                let tip = commonCount == 1
                    ? "Keep your finger on string \(commonStrings) - it stays the same!"
                    : "Keep your fingers on strings \(commonStrings) - they stay the same!"
                Text(tip)
                    .font(.caption)
                    .foregroundStyle(Color.accentColor)
                    .padding(.top, 4)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Total Path Summary

    private func totalPathSummary(path: VoiceLeading.Path) -> some View {
        let transCount = path.transitions.count
        let transLabel = "\(transCount) transition\(transCount == 1 ? "" : "s")"
        return Text("Total path: \(path.totalDistance) frets across \(transLabel)")
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Bottom Controls

    private func bottomControls(totalTransitions: Int) -> some View {
        VStack(spacing: 4) {
            HStack {
                Button {
                    if currentStep > 0 { currentStep -= 1 }
                } label: {
                    Text("\u{25C0} Prev")
                }
                .disabled(currentStep == 0)

                Spacer()

                Button {
                    playAllVoicings()
                } label: {
                    Label("Play All", systemImage: "play.fill")
                }
                .buttonStyle(.borderedProminent)

                Spacer()

                Button {
                    if currentStep < totalTransitions - 1 { currentStep += 1 }
                } label: {
                    Text("Next \u{25B6}")
                }
                .disabled(currentStep >= totalTransitions - 1)
            }
            .padding(.horizontal)

            Text("\(currentStep + 1) of \(totalTransitions)")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 8)
    }

    // MARK: - Audio

    private func playVoicing(_ voicing: ChordVoicing) {
        let frets = voicing.fretInts
        let pitchClasses = frets.enumerated().compactMap { i, fret -> Int32? in
            guard fret >= 0 else { return nil }
            return (tuning[i].openPitchClass + Int32(fret)) % 12
        }
        tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
    }

    private func playAllVoicings() {
        guard let path = path else { return }
        let steps = path.steps.asArray(of: VoiceLeading.Step.self)
        for (i, step) in steps.enumerated() {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.6) {
                self.playVoicing(step.voicing)
            }
        }
    }

    // MARK: - Helpers

    private func noteName(stringIndex: Int, fret: Int) -> String {
        if fret < 0 { return "x" }
        let pc = (tuning[stringIndex].openPitchClass + Int32(fret)) % 12
        return Notes.shared.pitchClassToName(pitchClass: pc)
    }
}

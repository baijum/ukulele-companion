import SwiftUI
import shared

/// Container view for the Play tab: interactive fretboard + chord library navigation.
struct ExplorerView: View {
    @StateObject private var fretboardVM = FretboardViewModel()
    @EnvironmentObject var settings: SettingsViewModel
    @State private var shareInfo: ShareChordInfo?
    @State private var showScaleSelector = false
    @State private var showFullScreen = false
    @State private var showLibrarySheet = false
    @State private var libraryPreselectedRoot: Int32 = 0
    @State private var libraryPreselectedFormula: String = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    // Interactive fretboard
                    FretboardView(viewModel: fretboardVM, leftHanded: settings.leftHanded)
                        .padding(.top, 8)

                    // Action bar
                    HStack {
                        Button(action: { fretboardVM.clearAll() }) {
                            Label("Reset", systemImage: "arrow.counterclockwise")
                                .font(.subheadline)
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)

                        Spacer()

                        // Scale overlay toggle
                        Button(action: { showScaleSelector.toggle() }) {
                            Label("Scales", systemImage: "music.note.list")
                                .font(.subheadline)
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .tint(fretboardVM.scaleOverlay.enabled ? .green : nil)
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 12)

                    // Capo selector
                    CapoSelectorView(
                        capoFret: fretboardVM.capoFret,
                        lastFret: FretboardViewModel.fretCount - 1,
                        onCapoChange: { fretboardVM.setCapoFret($0) }
                    )
                    .padding(.horizontal)

                    // Scale selector (collapsible)
                    if showScaleSelector {
                        ScaleSelectorView(
                            state: fretboardVM.scaleOverlay,
                            tuningPitchClasses: fretboardVM.tuning.map { $0.openPitchClass },
                            onRootChanged: { fretboardVM.setScaleRoot($0) },
                            onScaleChanged: { fretboardVM.setScale($0) },
                            onToggle: { fretboardVM.toggleScaleOverlay() },
                            onPositionChanged: { fretboardVM.setScalePositionRange($0) },
                            onChordTapped: { pitchClasses in
                                fretboardVM.clearAll()
                                for (i, pc) in pitchClasses.enumerated() where i < FretboardViewModel.stringCount {
                                    if pc >= 0 {
                                        fretboardVM.selections[i] = Int(pc)
                                    }
                                }
                            }
                        )
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                    }

                    ChordResultView(
                        result: fretboardVM.detectionResult,
                        onPlayChord: { fretboardVM.playChord() },
                        onShareChord: buildShareInfo() != nil ? { shareInfo = buildShareInfo() } : nil,
                        frets: currentFrets,
                        tuning: fretboardVM.tuning,
                        showTips: settings.showTips,
                        onSuggestedChord: { chordName in
                            applySuggestedChord(chordName)
                        },
                        onShowInLibrary: { name, formulaSymbol in
                            openLibrary(chordName: name, formulaSymbol: formulaSymbol)
                        },
                        onAlternateTapped: { altName in
                            openLibrary(chordName: altName, formulaSymbol: "")
                        }
                    )

                    Spacer(minLength: 20)
                }
            }
            .navigationTitle("Explorer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showFullScreen = true
                    } label: {
                        Image(systemName: "arrow.up.left.and.arrow.down.right")
                            .accessibilityLabel("Full screen fretboard")
                    }
                }
            }
            .fullScreenCover(isPresented: $showFullScreen) {
                FullScreenFretboardView(
                    fretboardVM: fretboardVM,
                    leftHanded: settings.leftHanded
                )
            }
            .sheet(item: $shareInfo) { info in
                ShareChordSheet(info: info)
            }
            .sheet(isPresented: $showLibrarySheet) {
                NavigationStack {
                    ChordLibraryView(
                        onApplyVoicing: { voicing, root, formula in
                            fretboardVM.applyVoicing(voicing, rootPitchClass: root, formula: formula)
                            showLibrarySheet = false
                        },
                        fretboardVM: fretboardVM
                    )
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Done") { showLibrarySheet = false }
                        }
                    }
                }
            }
            .onAppear {
                fretboardVM.applySoundSettings(from: settings)
            }
            .onChange(of: settings.soundEnabled) { _ in fretboardVM.applySoundSettings(from: settings) }
            .onChange(of: settings.volume) { _ in fretboardVM.applySoundSettings(from: settings) }
            .onChange(of: settings.strumDelayMs) { _ in fretboardVM.applySoundSettings(from: settings) }
            .onChange(of: settings.strumDown) { _ in fretboardVM.applySoundSettings(from: settings) }
        }
    }

    private var currentFrets: [Int32] {
        (0..<4).map { s in
            if let maybeFret = fretboardVM.selections[s], let fret = maybeFret {
                return Int32(fret)
            }
            return 0
        }
    }

    private func applySuggestedChord(_ name: String) {
        let rootName = String(name.prefix(while: { $0.isLetter || $0 == "#" || $0 == "b" }))
        let quality = String(name.dropFirst(rootName.count))
        let noteMap: [String: Int32] = [
            "C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3,
            "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8,
            "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11
        ]
        let rootPc = noteMap[rootName] ?? 0

        let formulas = ChordFormulas.shared.ALL as! [ChordFormula]
        let formula = formulas.first { $0.symbol == quality } ?? formulas.first { $0.symbol == "" }!
        let voicings = VoicingGenerator.shared.generate(
            rootPitchClass: rootPc,
            formula: formula,
            tuning: fretboardVM.tuning,
            allowMutedStrings: false
        ) as! [ChordVoicing]

        if let voicing = voicings.first {
            fretboardVM.applyVoicing(voicing, rootPitchClass: rootPc, formula: formula)
        }
    }

    private func openLibrary(chordName: String, formulaSymbol: String) {
        showLibrarySheet = true
    }

    private func buildShareInfo() -> ShareChordInfo? {
        guard case .chordFound(let name, _, _, _, _, _, _) = fretboardVM.detectionResult else {
            return nil
        }
        let fretValues: [Int32] = (0..<4).map { s in
            if let maybeFret = fretboardVM.selections[s], let fret = maybeFret {
                return Int32(fret)
            }
            return -1
        }
        let kotlinFrets = fretValues.map { KotlinInt(int: $0) }
        let positives = fretValues.filter { $0 > 0 }
        let voicing = ChordVoicing(
            frets: kotlinFrets,
            notes: fretValues.map { _ in NSNull() },
            minFret: positives.min() ?? 0,
            maxFret: positives.max() ?? 0
        )
        return ShareChordInfo(voicing: voicing, chordName: name)
    }
}

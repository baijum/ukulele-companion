import SwiftUI
import shared

struct SongbookView: View {
    @StateObject private var viewModel = SongbookViewModel()
    @State private var showingImportSheet = false
    @State private var importText = ""
    @State private var selectedSong: StoredSong? = nil
    @State private var showingNewSong = false

    var body: some View {
        VStack(spacing: 0) {
            searchAndFilter
            songList
        }
        .navigationTitle("Songbook")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button { showingNewSong = true } label: {
                        Label("New Song", systemImage: "plus")
                    }
                    Button { showingImportSheet = true } label: {
                        Label("Import ChordPro", systemImage: "square.and.arrow.down")
                    }
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Add song or import")
            }
        }
        .sheet(isPresented: $showingImportSheet) {
            importSheet
        }
        .sheet(isPresented: $showingNewSong) {
            NavigationStack {
                SongEditorView(viewModel: viewModel, song: nil)
            }
        }
        .sheet(item: $selectedSong) { song in
            NavigationStack {
                SongViewerView(viewModel: viewModel, song: song)
            }
        }
    }

    private var searchAndFilter: some View {
        VStack(spacing: 8) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
                TextField("Search songs...", text: $viewModel.searchQuery)
                    .textFieldStyle(.plain)
            }
            .padding(8)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(.horizontal)

            HStack {
                Picker("Sort", selection: $viewModel.sortOrder) {
                    ForEach(SongSortOrder.allCases, id: \.self) { order in
                        Text(order.rawValue).tag(order)
                    }
                }
                .pickerStyle(.menu)
                .font(.caption)

                Spacer()
            }
            .padding(.horizontal)

            if !viewModel.allLabels.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(viewModel.allLabels, id: \.self) { label in
                            Button {
                                if viewModel.selectedLabels.contains(label) {
                                    viewModel.selectedLabels.remove(label)
                                } else {
                                    viewModel.selectedLabels.insert(label)
                                }
                            } label: {
                                Text(label)
                                    .font(.caption)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(
                                        viewModel.selectedLabels.contains(label)
                                            ? Color.accentColor.opacity(0.2)
                                            : Color(.systemGray6)
                                    )
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal)
                }
            }
        }
        .padding(.vertical, 8)
    }

    private var songList: some View {
        List {
            let songs = viewModel.filteredSongs
            if songs.isEmpty {
                Text("No songs yet. Tap + to add one.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(songs) { song in
                    Button {
                        selectedSong = song
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(song.title)
                                .font(.subheadline.bold())
                            if !song.artist.isEmpty {
                                Text(song.artist)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            if !song.labels.isEmpty {
                                HStack(spacing: 4) {
                                    ForEach(song.labels, id: \.self) { label in
                                        Text(label)
                                            .font(.caption2)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color.accentColor.opacity(0.1))
                                            .clipShape(Capsule())
                                    }
                                }
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
                .onDelete { offsets in
                    let songs = viewModel.filteredSongs
                    for offset in offsets {
                        viewModel.delete(id: songs[offset].id)
                    }
                }
            }
        }
        .listStyle(.plain)
    }

    private var importSheet: some View {
        NavigationStack {
            VStack {
                Text("Paste ChordPro content below:")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.top)

                TextEditor(text: $importText)
                    .font(.system(.body, design: .monospaced))
                    .padding(4)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .strokeBorder(Color(.systemGray4), lineWidth: 1)
                    )
                    .padding()
            }
            .navigationTitle("Import ChordPro")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        importText = ""
                        showingImportSheet = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Import") {
                        viewModel.importChordPro(text: importText)
                        importText = ""
                        showingImportSheet = false
                    }
                    .disabled(importText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

struct SongViewerView: View {
    @ObservedObject var viewModel: SongbookViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var transposeSemitones: Int = 0
    @State private var showingEditor = false
    @State private var tappedChord: String? = nil
    @State private var isAutoScrolling = false
    @State private var scrollSpeed: Double = 1.0
    @State private var scrollTimer: Timer?
    @State private var scrollOffset: CGFloat = 0

    private let tonePlayer = TonePlayer()

    let song: StoredSong

    private var displaySong: StoredSong {
        if transposeSemitones == 0 { return song }
        return viewModel.transpose(song: song, semitones: transposeSemitones)
    }

    private var detectedKey: String? {
        let chords = ChordParser.shared.extractChords(text: displaySong.content) as! [String]
        guard !chords.isEmpty else { return nil }
        guard let result = KeyDetector.shared.detectKey(chordNames: chords) else { return nil }
        return result.displayName
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    songInfoSection

                    parsedContentView(song: displaySong)

                    Color.clear.frame(height: 1).id("bottom")
                }
                .padding()
            }
            .overlay(alignment: .bottomTrailing) {
                autoScrollControls(proxy: proxy)
                    .padding()
            }
        }
        .navigationTitle(song.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Done") { dismiss() }
            }
            ToolbarItemGroup(placement: .primaryAction) {
                HStack(spacing: 4) {
                    Button { transposeSemitones -= 1 } label: {
                        Image(systemName: "minus")
                    }
                    .accessibilityLabel("Transpose down")
                    Text("\(transposeSemitones > 0 ? "+" : "")\(transposeSemitones)")
                        .font(.caption.monospacedDigit())
                        .frame(minWidth: 28)
                    Button { transposeSemitones += 1 } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Transpose up")
                }

                Button { showingEditor = true } label: {
                    Image(systemName: "pencil")
                }
                .accessibilityLabel("Edit song")

                ShareLink(item: viewModel.exportChordPro(song: displaySong))

                Button(role: .destructive) {
                    viewModel.delete(id: song.id)
                    dismiss()
                } label: {
                    Image(systemName: "trash")
                }
                .accessibilityLabel("Delete song")
            }
        }
        .sheet(isPresented: $showingEditor) {
            NavigationStack {
                SongEditorView(viewModel: viewModel, song: song)
            }
        }
        .popover(isPresented: Binding(
            get: { tappedChord != nil },
            set: { if !$0 { tappedChord = nil } }
        )) {
            if let chord = tappedChord {
                chordPopover(chord: chord)
            }
        }
        .onDisappear { stopAutoScroll() }
    }

    // MARK: - Song Info

    private var songInfoSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !song.artist.isEmpty {
                Text(song.artist)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 8) {
                if song.capo > 0 {
                    Label("Capo \(song.capo)", systemImage: "guitars")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                if let key = detectedKey {
                    Label("Key: \(key)", systemImage: "music.note")
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.accentColor.opacity(0.1))
                        .clipShape(Capsule())
                }

                if !song.strumPatternName.isEmpty {
                    Label(song.strumPatternName, systemImage: "waveform")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            if !song.labels.isEmpty {
                HStack(spacing: 4) {
                    ForEach(song.labels, id: \.self) { label in
                        Text(label)
                            .font(.caption2)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.accentColor.opacity(0.1))
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }

    // MARK: - Auto-scroll

    private func autoScrollControls(proxy: ScrollViewProxy) -> some View {
        VStack(spacing: 6) {
            if isAutoScrolling {
                HStack(spacing: 6) {
                    ForEach([0.5, 1.0, 2.0, 3.0], id: \.self) { speed in
                        Button {
                            scrollSpeed = speed
                        } label: {
                            Text("\(speed, specifier: "%.1f")x")
                                .font(.caption2.bold())
                                .padding(.horizontal, 6)
                                .padding(.vertical, 4)
                                .background(scrollSpeed == speed ? Color.accentColor : Color(.systemGray5))
                                .foregroundStyle(scrollSpeed == speed ? .white : .primary)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Button {
                if isAutoScrolling {
                    stopAutoScroll()
                } else {
                    startAutoScroll(proxy: proxy)
                }
            } label: {
                Image(systemName: isAutoScrolling ? "pause.circle.fill" : "play.circle.fill")
                    .font(.title)
                    .foregroundStyle(isAutoScrolling ? .red : .accentColor)
            }
            .accessibilityLabel(isAutoScrolling ? "Pause auto-scroll" : "Start auto-scroll")
        }
        .padding(8)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func startAutoScroll(proxy: ScrollViewProxy) {
        isAutoScrolling = true
        scrollOffset = 0
        let interval = 1.0 / (scrollSpeed * 2)
        scrollTimer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { _ in
            DispatchQueue.main.async {
                scrollOffset += 1
                proxy.scrollTo("bottom", anchor: .init(x: 0.5, y: min(1.0, scrollOffset / 500)))
            }
        }
    }

    private func stopAutoScroll() {
        isAutoScrolling = false
        scrollTimer?.invalidate()
        scrollTimer = nil
    }

    // MARK: - Content Parsing

    private func parsedContentView(song: StoredSong) -> some View {
        let lines = song.content.components(separatedBy: "\n")
        return VStack(alignment: .leading, spacing: 2) {
            ForEach(0..<lines.count, id: \.self) { i in
                let segments = ChordParser.shared.parseLine(line: lines[i])
                parsedLineView(segments: segments as! [ChordParser.TextSegment])
            }
        }
    }

    private func parsedLineView(segments: [ChordParser.TextSegment]) -> some View {
        HStack(spacing: 0) {
            ForEach(0..<segments.count, id: \.self) { i in
                let segment = segments[i]
                if let chord = segment as? ChordParser.TextSegmentChord {
                    Text(chord.name)
                        .font(.system(.body, design: .monospaced))
                        .foregroundColor(.accentColor)
                        .bold()
                        .onTapGesture { tappedChord = chord.name }
                        .accessibilityLabel("\(chord.name), tap to view diagram")
                } else if let plain = segment as? ChordParser.TextSegmentPlainText {
                    Text(plain.text)
                        .font(.system(.body, design: .monospaced))
                }
            }
        }
    }

    // MARK: - Chord Popover

    @ViewBuilder
    private func chordPopover(chord: String) -> some View {
        let rootName = String(chord.prefix(while: { $0.isLetter || $0 == "#" || $0 == "b" }))
        let quality = String(chord.dropFirst(rootName.count))
        let noteMap: [String: Int32] = [
            "C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3,
            "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8,
            "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11
        ]
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
            rootPitchClass: rootPc,
            formula: formula,
            tuning: tuning,
            allowMutedStrings: false
        ) as! [ChordVoicing]

        VStack(spacing: 8) {
            Text(chord)
                .font(.headline)
            if let voicing = voicings.first {
                ChordDiagramView(voicing: voicing, chordName: chord)
                    .frame(width: 150, height: 150)
                Button {
                    let pitchClasses = (0..<voicing.frets.count).compactMap { i -> Int32? in
                        let fret = (voicing.frets[i] as! NSNumber).intValue
                        guard fret >= 0 else { return nil }
                        let openPc = (UkuleleTuning.highG.pitchClasses[i] as! NSNumber).int32Value
                        return (openPc + Int32(fret)) % 12
                    }
                    tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
                } label: {
                    Label("Play", systemImage: "play.fill")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
            } else {
                Text("No voicing found")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
    }
}

import SwiftUI
import UniformTypeIdentifiers
import shared

struct SongbookView: View {
    @StateObject private var viewModel = SongbookViewModel()
    @State private var showingImportSheet = false
    @State private var showingFileImporter = false
    @State private var importText = ""
    @State private var selectedSong: StoredSong? = nil
    @State private var showingNewSong = false
    @State private var showImportError = false
    @State private var songToDelete: StoredSong? = nil

    private let chordProExtensions = ["cho", "chordpro", "chopro", "crd", "pro"]

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
                        Label("Import ChordPro", systemImage: "doc.text")
                    }
                    Button { showingFileImporter = true } label: {
                        Label("Import from File", systemImage: "square.and.arrow.down")
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
        .fileImporter(
            isPresented: $showingFileImporter,
            allowedContentTypes: [.plainText, .text, .data],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                guard let url = urls.first else { return }
                guard url.startAccessingSecurityScopedResource() else {
                    showImportError = true
                    return
                }
                defer { url.stopAccessingSecurityScopedResource() }
                do {
                    let content = try String(contentsOf: url, encoding: .utf8)
                    let filename = url.lastPathComponent
                    let ext = url.pathExtension.lowercased()
                    if chordProExtensions.contains(ext) {
                        viewModel.importChordPro(text: content)
                    } else {
                        viewModel.importPlainText(content: content, filename: filename)
                    }
                } catch {
                    showImportError = true
                }
            case .failure:
                showImportError = true
            }
        }
        .alert("Import Failed", isPresented: $showImportError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("The file could not be imported. Please check the file format and try again.")
        }
        .alert("Delete Song?", isPresented: Binding(
            get: { songToDelete != nil },
            set: { if !$0 { songToDelete = nil } }
        )) {
            Button("Cancel", role: .cancel) { songToDelete = nil }
            Button("Delete", role: .destructive) {
                if let song = songToDelete {
                    viewModel.delete(id: song.id)
                    songToDelete = nil
                }
            }
        } message: {
            if let song = songToDelete {
                Text("Are you sure you want to delete \"\(song.title.isEmpty ? "Untitled" : song.title)\"?")
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
                if !viewModel.searchQuery.isEmpty {
                    Button {
                        viewModel.searchQuery = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                    .accessibilityLabel("Clear search")
                }
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
                        if !viewModel.selectedLabels.isEmpty {
                            Button {
                                viewModel.selectedLabels.removeAll()
                            } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: "xmark")
                                        .font(.caption2)
                                    Text("Clear")
                                        .font(.caption)
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color(.systemGray5))
                                .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Clear label filters")
                        }
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
                            .accessibilityLabel("Filter by \(label)")
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
                    let displayTitle = song.title.isEmpty ? "Untitled" : song.title
                    Button {
                        selectedSong = song
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(displayTitle)
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
                    .accessibilityLabel(song.artist.isEmpty ? displayTitle : "\(displayTitle) by \(song.artist)")
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            songToDelete = song
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
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
    @State private var showDeleteConfirmation = false
    @State private var showingStrumPicker = false
    @State private var showingAddLabel = false
    @State private var newLabelText = ""

    private let tonePlayer = TonePlayer()

    let song: StoredSong

    private var currentSong: StoredSong {
        viewModel.songs.first(where: { $0.id == song.id }) ?? song
    }

    private var displaySong: StoredSong {
        if transposeSemitones == 0 { return currentSong }
        return viewModel.transpose(song: currentSong, semitones: transposeSemitones)
    }

    private var detectedKey: String? {
        let chords = ChordParser.shared.extractChords(text: displaySong.content) as! [String]
        guard !chords.isEmpty else { return nil }
        guard let result = KeyDetector.shared.detectKey(chordNames: chords) else { return nil }
        return result.displayName
    }

    private var displayTitle: String {
        currentSong.title.isEmpty ? "Untitled" : currentSong.title
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    songInfoSection

                    transposeSection

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
        .navigationTitle(displayTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Done") { dismiss() }
            }
            ToolbarItemGroup(placement: .primaryAction) {
                Button { showingEditor = true } label: {
                    Image(systemName: "pencil")
                }
                .accessibilityLabel("Edit song")

                shareMenu

                Button(role: .destructive) {
                    showDeleteConfirmation = true
                } label: {
                    Image(systemName: "trash")
                }
                .accessibilityLabel("Delete song")
            }
        }
        .alert("Delete Song?", isPresented: $showDeleteConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                viewModel.delete(id: song.id)
                dismiss()
            }
        } message: {
            Text("Are you sure you want to delete \"\(displayTitle)\"?")
        }
        .sheet(isPresented: $showingEditor) {
            NavigationStack {
                SongEditorView(viewModel: viewModel, song: currentSong)
            }
        }
        .sheet(isPresented: $showingStrumPicker) {
            strumPatternPicker
        }
        .alert("Add Label", isPresented: $showingAddLabel) {
            TextField("Label name", text: $newLabelText)
            Button("Cancel", role: .cancel) { newLabelText = "" }
            Button("Add") {
                let trimmed = newLabelText.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty && !currentSong.labels.contains(trimmed) {
                    viewModel.updateLabels(id: song.id, labels: currentSong.labels + [trimmed])
                }
                newLabelText = ""
            }
        } message: {
            Text("Enter a label for this song.")
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

    // MARK: - Share Menu

    private var shareMenu: some View {
        Menu {
            Button {
                let formatted = viewModel.formattedDisplay(song: currentSong)
                shareText(formatted)
            } label: {
                Label("Share as text", systemImage: "doc.plaintext")
            }
            if transposeSemitones != 0 {
                Button {
                    let formatted = viewModel.formattedDisplay(song: displaySong)
                    shareText(formatted)
                } label: {
                    Label("Share transposed text", systemImage: "doc.plaintext")
                }
            }
            Button {
                let chordPro = viewModel.exportChordPro(song: currentSong)
                shareText(chordPro)
            } label: {
                Label("Export ChordPro", systemImage: "square.and.arrow.up")
            }
            if transposeSemitones != 0 {
                Button {
                    let chordPro = viewModel.exportChordPro(song: displaySong)
                    shareText(chordPro)
                } label: {
                    Label("Export transposed ChordPro", systemImage: "square.and.arrow.up")
                }
            }
        } label: {
            Image(systemName: "square.and.arrow.up")
        }
        .accessibilityLabel("Share")
    }

    private func shareText(_ text: String) {
        let activityVC = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else { return }
        var presenter = rootVC
        while let presented = presenter.presentedViewController {
            presenter = presented
        }
        if let popover = activityVC.popoverPresentationController {
            popover.sourceView = presenter.view
            popover.sourceRect = CGRect(x: presenter.view.bounds.midX, y: presenter.view.bounds.midY, width: 0, height: 0)
            popover.permittedArrowDirections = []
        }
        presenter.present(activityVC, animated: true)
    }

    // MARK: - Song Info

    private var songInfoSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !currentSong.artist.isEmpty {
                Text(currentSong.artist)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 8) {
                if currentSong.capo > 0 {
                    Label("Capo \(currentSong.capo)", systemImage: "guitars")
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
            }

            strumPatternSection

            labelManagementSection
        }
    }

    // MARK: - Strum Pattern

    private var strumPatternSection: some View {
        Group {
            if !currentSong.strumPatternName.isEmpty {
                let patterns = StrumPatterns.shared.ALL as! [StrumPattern]
                let resolvedPattern = patterns.first { $0.name == currentSong.strumPatternName }
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Label(currentSong.strumPatternName, systemImage: "waveform")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        if let pattern = resolvedPattern {
                            Text(pattern.notation)
                                .font(.caption2.monospaced())
                                .foregroundStyle(.secondary)
                        }
                    }
                    HStack(spacing: 12) {
                        Button("Change") { showingStrumPicker = true }
                            .font(.caption)
                        Button("Remove") {
                            viewModel.updateStrumPattern(id: song.id, patternName: "")
                        }
                        .font(.caption)
                        .foregroundStyle(.red)
                    }
                }
            } else {
                HStack {
                    Text("Strum Pattern:")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button("Select") { showingStrumPicker = true }
                        .font(.caption)
                }
            }
        }
    }

    private var strumPatternPicker: some View {
        NavigationStack {
            let patterns = StrumPatterns.shared.ALL as! [StrumPattern]
            List(0..<patterns.count, id: \.self) { i in
                let pattern = patterns[i]
                let isSelected = pattern.name == currentSong.strumPatternName
                Button {
                    viewModel.updateStrumPattern(id: song.id, patternName: pattern.name)
                    showingStrumPicker = false
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(pattern.name)
                            .font(.body)
                            .fontWeight(isSelected ? .bold : .regular)
                            .foregroundStyle(isSelected ? Color.accentColor : .primary)
                        Text(pattern.notation)
                            .font(.caption.monospaced())
                            .foregroundStyle(.secondary)
                    }
                }
                .accessibilityLabel("Strum pattern \(pattern.name)")
            }
            .navigationTitle("Select Strum Pattern")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showingStrumPicker = false }
                }
            }
        }
    }

    // MARK: - Label Management

    private var labelManagementSection: some View {
        HStack(spacing: 4) {
            ForEach(currentSong.labels, id: \.self) { label in
                Text(label)
                    .font(.caption2)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.accentColor.opacity(0.1))
                    .clipShape(Capsule())
            }
            Button {
                showingAddLabel = true
            } label: {
                HStack(spacing: 2) {
                    Image(systemName: "plus")
                        .font(.caption2)
                    Text("Label")
                        .font(.caption2)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Color(.systemGray5))
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Add label")
        }
    }

    // MARK: - Transpose

    private var transposeSection: some View {
        VStack(spacing: 4) {
            HStack {
                Text("Transpose")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Button { transposeSemitones -= 1 } label: {
                    Image(systemName: "minus.circle")
                }
                .accessibilityLabel("Transpose down")
                Text("\(transposeSemitones > 0 ? "+" : "")\(transposeSemitones)")
                    .font(.caption.monospacedDigit())
                    .frame(minWidth: 28)
                Button { transposeSemitones += 1 } label: {
                    Image(systemName: "plus.circle")
                }
                .accessibilityLabel("Transpose up")
                if transposeSemitones != 0 {
                    Button("Reset") { transposeSemitones = 0 }
                        .font(.caption)
                }
            }

            if transposeSemitones != 0 {
                let capoFret = ((transposeSemitones % 12) + 12) % 12
                if capoFret > 0 {
                    Text("Equivalent: Capo fret \(capoFret)")
                        .font(.caption2)
                        .foregroundStyle(.orange)
                        .fontWeight(.semibold)
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

            HStack(spacing: 8) {
                if isAutoScrolling {
                    Button {
                        stopAutoScroll()
                    } label: {
                        Image(systemName: "stop.circle.fill")
                            .font(.title)
                            .foregroundStyle(.red)
                    }
                    .accessibilityLabel("Stop auto-scroll")
                }

                Button {
                    if isAutoScrolling {
                        pauseAutoScroll()
                    } else {
                        startAutoScroll(proxy: proxy)
                    }
                } label: {
                    Image(systemName: isAutoScrolling ? "pause.circle.fill" : "play.circle.fill")
                        .font(.title)
                        .foregroundStyle(isAutoScrolling ? .orange : .accentColor)
                }
                .accessibilityLabel(isAutoScrolling ? "Pause auto-scroll" : "Start auto-scroll")
            }
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

    private func pauseAutoScroll() {
        isAutoScrolling = false
        scrollTimer?.invalidate()
        scrollTimer = nil
    }

    private func stopAutoScroll() {
        isAutoScrolling = false
        scrollTimer?.invalidate()
        scrollTimer = nil
        scrollOffset = 0
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
                Button {
                    let fretList = voicing.fretInts
                    let pitchClasses = (0..<fretList.count).compactMap { i -> Int32? in
                        let fret = fretList[i]
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

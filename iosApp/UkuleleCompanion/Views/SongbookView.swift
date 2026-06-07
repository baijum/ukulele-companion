import SwiftUI
import UniformTypeIdentifiers
import shared

struct SongbookView: View {
    @EnvironmentObject var viewModel: SongbookViewModel
    @State private var showingImportSheet = false
    @State private var showingFileImporter = false
    @State private var importText = ""
    @State private var showingNewSong = false
    @State private var showImportError = false
    @State private var songToDelete: StoredSong? = nil
    @State private var multiSelection = Set<String>()
    @State private var isSelectionMode = false
    @State private var showDeleteSelectedConfirmation = false

    private let chordProExtensions = ["cho", "chordpro", "chopro", "crd", "pro"]

    var body: some View {
        VStack(spacing: 0) {
            if isSelectionMode {
                HStack {
                    Text("\(multiSelection.count) selected")
                        .font(.subheadline.bold())
                    Spacer()
                    Button("Select All") {
                        multiSelection = Set(viewModel.filteredSongs.map { $0.id })
                    }
                    Button("Delete", role: .destructive) {
                        showDeleteSelectedConfirmation = true
                    }
                    .disabled(multiSelection.isEmpty)
                    Button("Cancel") {
                        isSelectionMode = false
                        multiSelection.removeAll()
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
            }
            searchAndFilter
            songList
        }
        .alert("Delete Selected?", isPresented: $showDeleteSelectedConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Delete \(multiSelection.count) songs", role: .destructive) {
                for id in multiSelection {
                    viewModel.delete(id: id)
                }
                multiSelection.removeAll()
                isSelectionMode = false
            }
        } message: {
            Text("This will permanently delete the selected songs.")
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
                    Divider()
                    Button {
                        isSelectionMode.toggle()
                        if !isSelectionMode { multiSelection.removeAll() }
                    } label: {
                        Label(isSelectionMode ? "Cancel Selection" : "Select Songs", systemImage: "checkmark.circle")
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
                    if isSelectionMode {
                        Button {
                            if multiSelection.contains(song.id) {
                                multiSelection.remove(song.id)
                            } else {
                                multiSelection.insert(song.id)
                            }
                        } label: {
                            songRowContent(song: song, displayTitle: displayTitle)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(song.artist.isEmpty ? displayTitle : "\(displayTitle) by \(song.artist)")
                        .accessibilityValue(multiSelection.contains(song.id) ? "Selected" : "Not selected")
                        .accessibilityHint("Double-tap to toggle selection")
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                songToDelete = song
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    } else {
                        NavigationLink {
                            SongViewerView(viewModel: viewModel, song: song)
                        } label: {
                            songRowContent(song: song, displayTitle: displayTitle)
                        }
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
        }
        .listStyle(.plain)
    }

    @ViewBuilder
    private func songRowContent(song: StoredSong, displayTitle: String) -> some View {
        HStack {
            if isSelectionMode {
                Image(systemName: multiSelection.contains(song.id) ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(multiSelection.contains(song.id) ? .accentColor : .secondary)
            }
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

private struct ScrollOffsetKey: PreferenceKey {
    static let defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct SongViewerView: View {
    @ObservedObject var viewModel: SongbookViewModel
    @EnvironmentObject var customPatternsVM: CustomPatternsViewModel
    @EnvironmentObject var metronomeVM: MetronomeViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var transposeSemitones: Int = 0
    @State private var showingEditor = false
    @State private var tappedChord: String? = nil
    @State private var isAutoScrolling = false
    @State private var scrollSpeed: Double = 1.0
    @State private var scrollTimer: Timer?
    @State private var scrollOffset: CGFloat = 0
    @State private var trackedScrollOffset: CGFloat = 0
    @State private var contentHeight: CGFloat = 1
    @State private var viewportHeight: CGFloat = 1
    @State private var shouldRestartFromTop = false
    @State private var showDeleteConfirmation = false
    @State private var showingStrumPicker = false
    @State private var showingAddLabel = false
    @State private var newLabelText = ""
    @AppStorage("songFontSize") private var songFontSize: Double = 16.0
    @State private var viewOpenedAt: Date?
    @State private var showPerformanceMode = false

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

                    sectionNavigation(proxy: proxy, content: displaySong.content)

                    tempoSection(content: displaySong.content)

                    parsedContentView(song: displaySong)

                    Color.clear.frame(height: 1).id("bottom")
                }
                .padding()
                .background(
                    GeometryReader { geo in
                        Color.clear
                            .preference(key: ScrollOffsetKey.self,
                                        value: -geo.frame(in: .named("songScroll")).minY)
                            .onChange(of: geo.size.height) { newHeight in
                                contentHeight = newHeight
                            }
                            .onAppear { contentHeight = geo.size.height }
                    }
                )
            }
            .coordinateSpace(name: "songScroll")
            .onPreferenceChange(ScrollOffsetKey.self) { value in
                trackedScrollOffset = max(0, value)
            }
            .background(
                GeometryReader { geo in
                    Color.clear.onAppear { viewportHeight = geo.size.height }
                        .onChange(of: geo.size.height) { newHeight in
                            viewportHeight = newHeight
                        }
                }
            )
            .overlay(alignment: .bottomTrailing) {
                autoScrollControls(proxy: proxy)
                    .padding()
            }
            .onChange(of: scrollSpeed) { _ in
                guard isAutoScrolling else { return }
                scrollTimer?.invalidate()
                scrollTimer = createScrollTimer(proxy: proxy)
            }
        }
        .navigationTitle(displayTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button {
                    songFontSize = max(10, songFontSize - 2)
                } label: {
                    Image(systemName: "textformat.size.smaller")
                }
                .accessibilityLabel("Decrease font size")

                Button {
                    songFontSize = min(32, songFontSize + 2)
                } label: {
                    Image(systemName: "textformat.size.larger")
                }
                .accessibilityLabel("Increase font size")

                Button {
                    _ = viewModel.duplicate(song: currentSong)
                    dismiss()
                } label: {
                    Image(systemName: "doc.on.doc")
                }
                .accessibilityLabel("Duplicate song")

                Button { showingEditor = true } label: {
                    Image(systemName: "pencil")
                }
                .accessibilityLabel("Edit song")

                Button { showPerformanceMode = true } label: {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                }
                .accessibilityLabel("Performance mode")

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
        .onAppear {
            viewModel.recordView(id: song.id)
            viewOpenedAt = Date()
        }
        .onDisappear {
            stopAutoScroll()
            if let opened = viewOpenedAt {
                let elapsedMs = Date().timeIntervalSince(opened) * 1000
                viewModel.recordViewTime(id: song.id, elapsedMs: elapsedMs)
                viewOpenedAt = nil
            }
        }
        .fullScreenCover(isPresented: $showPerformanceMode) {
            PerformanceModeView(
                content: displaySong.content,
                font: songFont
            )
        }
    }

    @State private var showShareDialog = false

    // MARK: - Share Menu

    private var shareMenu: some View {
        let effectiveSong = transposeSemitones != 0 ? displaySong : currentSong
        return Button { showShareDialog = true } label: {
            Image(systemName: "square.and.arrow.up")
        }
        .accessibilityLabel("Share")
        .accessibilityHint("Choose export format")
        .confirmationDialog("Share chord sheet as", isPresented: $showShareDialog, titleVisibility: .visible) {
            Button("ChordPro (.cho)") {
                let chordPro = viewModel.exportChordPro(song: effectiveSong)
                shareText(chordPro)
            }
            Button("Plain text") {
                let formatted = viewModel.formattedDisplay(song: effectiveSong)
                shareText(formatted)
            }
            Button("Copy to clipboard") {
                let formatted = viewModel.formattedDisplay(song: effectiveSong)
                UIPasteboard.general.string = formatted
                AccessibilityAnnouncer.shared.announce("Copied to clipboard")
            }
            Button("Export as PDF") {
                let formatted = viewModel.formattedDisplay(song: effectiveSong)
                let title = effectiveSong.title.isEmpty ? "Untitled" : effectiveSong.title
                sharePdf(title: title, text: formatted)
            }
            Button("Cancel", role: .cancel) {}
        }
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

    private func sharePdf(title: String, text: String) {
        let pageWidth: CGFloat = 595
        let pageHeight: CGFloat = 842
        let margin: CGFloat = 50
        let lineHeight: CGFloat = 14
        let fontSize: CGFloat = 11

        let font = UIFont.monospacedSystemFont(ofSize: fontSize, weight: .regular)
        let titleFont = UIFont.systemFont(ofSize: 18, weight: .bold)
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineBreakMode = .byClipping

        let textAttributes: [NSAttributedString.Key: Any] = [
            .font: font,
            .paragraphStyle: paragraphStyle,
        ]
        let titleAttributes: [NSAttributedString.Key: Any] = [.font: titleFont]

        let lines = text.components(separatedBy: "\n")
        let usableHeight = pageHeight - 2 * margin
        let linesPerPage = Int(usableHeight / lineHeight)

        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight))

        let data = renderer.pdfData { ctx in
            var lineIdx = 0
            var pageNum = 0
            while lineIdx < lines.count {
                ctx.beginPage()
                pageNum += 1
                var y = margin

                if pageNum == 1 {
                    let titleStr = title as NSString
                    titleStr.draw(at: CGPoint(x: margin, y: y), withAttributes: titleAttributes)
                    y += 28
                }

                var linesOnPage = 0
                while lineIdx < lines.count && linesOnPage < linesPerPage {
                    let line = lines[lineIdx] as NSString
                    line.draw(at: CGPoint(x: margin, y: y), withAttributes: textAttributes)
                    y += lineHeight
                    lineIdx += 1
                    linesOnPage += 1
                }
            }
        }

        let sanitized = title.replacingOccurrences(of: "[^a-zA-Z0-9._-]", with: "_", options: .regularExpression)
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(sanitized).pdf")
        try? data.write(to: url)

        let activityVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
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

            if currentSong.viewCount > 0 {
                songStatsSection
            }
        }
    }

    private var songStatsSection: some View {
        HStack(spacing: 16) {
            Label("\(currentSong.viewCount) views", systemImage: "eye")
            if currentSong.lastViewedAt > 0 {
                let date = Date(timeIntervalSince1970: currentSong.lastViewedAt / 1000)
                Text(date, style: .relative)
            }
            let totalMinutes = Int(currentSong.totalViewTimeMs / 60_000)
            if totalMinutes >= 1 {
                Label(totalMinutes < 60
                      ? "\(totalMinutes) min"
                      : "\(totalMinutes / 60)h \(totalMinutes % 60)m",
                      systemImage: "clock")
            } else {
                Label("< 1 min", systemImage: "clock")
            }
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }

    // MARK: - Strum Pattern

    private var strumPatternSection: some View {
        Group {
            if !currentSong.strumPatternName.isEmpty {
                let builtInPatterns = StrumPatterns.shared.ALL as! [StrumPattern]
                let resolvedPattern = builtInPatterns.first { $0.name == currentSong.strumPatternName }
                let resolvedCustom = customPatternsVM.strumPatterns.first { $0.name == currentSong.strumPatternName }
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Label(currentSong.strumPatternName, systemImage: "waveform")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        if let pattern = resolvedPattern {
                            Text(pattern.notation)
                                .font(.caption2.monospaced())
                                .foregroundStyle(.secondary)
                        } else if let custom = resolvedCustom {
                            Text(custom.beats.map { $0.direction.prefix(1).uppercased() }.joined())
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
            let builtInPatterns = StrumPatterns.shared.ALL as! [StrumPattern]
            List {
                Section("Built-in") {
                    ForEach(0..<builtInPatterns.count, id: \.self) { i in
                        let pattern = builtInPatterns[i]
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
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
                    }
                }
                if !customPatternsVM.strumPatterns.isEmpty {
                    Section("Custom") {
                        ForEach(customPatternsVM.strumPatterns) { custom in
                            let isSelected = custom.name == currentSong.strumPatternName
                            Button {
                                viewModel.updateStrumPattern(id: song.id, patternName: custom.name)
                                showingStrumPicker = false
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(custom.name)
                                        .font(.body)
                                        .fontWeight(isSelected ? .bold : .regular)
                                        .foregroundStyle(isSelected ? Color.accentColor : .primary)
                                    Text(custom.beats.map { $0.direction.prefix(1).uppercased() }.joined())
                                        .font(.caption.monospaced())
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .accessibilityLabel("Custom strum pattern \(custom.name)")
                            .accessibilityAddTraits(isSelected ? .isSelected : [])
                        }
                    }
                }
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

                Button {
                    let transposed = viewModel.transpose(song: currentSong, semitones: transposeSemitones)
                    viewModel.save(song: transposed)
                    transposeSemitones = 0
                } label: {
                    Text("Save in this key")
                        .font(.caption.bold())
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
                .accessibilityLabel("Save song in transposed key")
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
                        .accessibilityLabel("Speed \(speed, specifier: "%.1f")x")
                        .accessibilityAddTraits(scrollSpeed == speed ? .isSelected : [])
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

    @discardableResult
    private func createScrollTimer(proxy: ScrollViewProxy) -> Timer {
        let interval = 1.0 / (scrollSpeed * 2)
        let timer = Timer(timeInterval: interval, repeats: true) { [self] _ in
            DispatchQueue.main.async {
                guard isAutoScrolling else { return }
                scrollOffset += 1
                proxy.scrollTo("bottom", anchor: .init(x: 0.5, y: min(1.0, scrollOffset / 500)))
            }
        }
        RunLoop.current.add(timer, forMode: .common)
        return timer
    }

    private func startAutoScroll(proxy: ScrollViewProxy) {
        isAutoScrolling = true
        if shouldRestartFromTop {
            scrollOffset = 0
            shouldRestartFromTop = false
        } else {
            let maxScroll = max(1, contentHeight - viewportHeight)
            scrollOffset = (trackedScrollOffset / maxScroll) * 500
        }
        scrollTimer = createScrollTimer(proxy: proxy)
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
        shouldRestartFromTop = true
    }

    // MARK: - Section Navigation

    @ViewBuilder
    private func sectionNavigation(proxy: ScrollViewProxy, content: String) -> some View {
        let sections = ChordSheetFormatter.shared.extractSections(content: content)
        if !sections.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(sections.enumerated()), id: \.offset) { _, section in
                        Button {
                            withAnimation {
                                proxy.scrollTo("line_\(section.lineIndex)", anchor: .top)
                            }
                        } label: {
                            Text(section.label)
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.accentColor.opacity(0.15))
                                .clipShape(Capsule())
                        }
                        .accessibilityLabel("Jump to \(section.label)")
                    }
                }
            }
        }
    }

    // MARK: - Tempo / Metronome

    @ViewBuilder
    private func tempoSection(content: String) -> some View {
        let kotlinBpm = ChordSheetFormatter.shared.extractTempo(content: content)
        if let bpmValue = kotlinBpm?.intValue {
            HStack {
                Image(systemName: "metronome")
                    .font(.body)
                Text("Tempo: \(bpmValue) BPM")
                    .font(.subheadline)
                Spacer()
                Button {
                    metronomeVM.bpm = Double(bpmValue)
                    if !metronomeVM.isPlaying {
                        metronomeVM.start()
                    }
                } label: {
                    Text("Start Metronome")
                        .font(.subheadline.weight(.medium))
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
                .accessibilityLabel("Start metronome at \(bpmValue) beats per minute")
            }
            .padding(.vertical, 4)
        }
    }

    // MARK: - Content Parsing

    private func parsedContentView(song: StoredSong) -> some View {
        let lines = song.content.components(separatedBy: "\n")
        return VStack(alignment: .leading, spacing: 2) {
            ForEach(0..<lines.count, id: \.self) { i in
                let segments = ChordParser.shared.parseLine(line: lines[i])
                parsedLineView(segments: segments as! [ChordParser.TextSegment])
                    .id("line_\(i)")
            }
        }
    }

    private var songFont: Font {
        .system(size: CGFloat(songFontSize), design: .monospaced)
    }

    @ViewBuilder
    private func parsedLineView(segments: [ChordParser.TextSegment]) -> some View {
        let hasChords = segments.contains(where: { $0 is ChordParser.TextSegmentChord })
        if hasChords {
            let result = Self.buildChordsAboveLyrics(segments: segments)
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    ForEach(0..<result.chordElements.count, id: \.self) { i in
                        let element = result.chordElements[i]
                        if element.isChord {
                            Text(element.text)
                                .font(songFont)
                                .foregroundColor(.accentColor)
                                .bold()
                                .onTapGesture { tappedChord = element.text }
                                .accessibilityLabel(element.text)
                                .accessibilityAddTraits(.isButton)
                                .accessibilityHint("Show chord diagram")
                        } else {
                            Text(element.text)
                                .font(songFont)
                                .accessibilityHidden(true)
                        }
                    }
                }

                if !result.lyrics.trimmingCharacters(in: .whitespaces).isEmpty {
                    Text(result.lyrics)
                        .font(songFont)
                }
            }
        } else {
            let text = segments.compactMap { ($0 as? ChordParser.TextSegmentPlainText)?.text }.joined()
            Text(text.isEmpty ? " " : text)
                .font(songFont)
        }
    }

    private static func buildChordsAboveLyrics(segments: [ChordParser.TextSegment])
        -> (chordElements: [(text: String, isChord: Bool)], lyrics: String, chordNames: [String])
    {
        var lyrics = ""
        var chordEntries: [(position: Int, name: String)] = []

        for segment in segments {
            if let chord = segment as? ChordParser.TextSegmentChord {
                chordEntries.append((position: lyrics.count, name: chord.name))
            } else if let plain = segment as? ChordParser.TextSegmentPlainText {
                lyrics += plain.text
            }
        }

        var chordElements: [(text: String, isChord: Bool)] = []
        var pos = 0
        for entry in chordEntries {
            let gap = entry.position - pos
            if gap > 0 {
                chordElements.append((String(repeating: " ", count: gap), false))
                pos = entry.position
            } else if pos > 0 {
                chordElements.append((" ", false))
                pos += 1
            }
            chordElements.append((entry.name, true))
            pos += entry.name.count
        }

        return (chordElements, lyrics, chordEntries.map { $0.name })
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

// MARK: - Performance Mode

struct PerformanceModeView: View {
    @Environment(\.dismiss) private var dismiss
    let content: String
    let font: Font

    @State private var isAutoScrolling = false
    @State private var scrollSpeed: Double = 1.0
    @State private var scrollTimer: Timer?
    @State private var showControls = true

    var body: some View {
        ZStack(alignment: .topTrailing) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(content.components(separatedBy: "\n").enumerated()), id: \.offset) { _, line in
                        if line.trimmingCharacters(in: .whitespaces).isEmpty {
                            Spacer().frame(height: 20)
                        } else {
                            Text(line)
                                .font(font)
                        }
                    }
                }
                .padding(24)
            }

            if showControls {
                HStack(spacing: 12) {
                    Button {
                        isAutoScrolling.toggle()
                    } label: {
                        Image(systemName: isAutoScrolling ? "pause.fill" : "play.fill")
                        Text(isAutoScrolling ? "Pause" : "Scroll")
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .accessibilityLabel(isAutoScrolling ? "Pause auto-scroll" : "Start auto-scroll")

                    if isAutoScrolling {
                        Button { scrollSpeed = max(0.5, scrollSpeed - 0.5) } label: {
                            Text("\u{2212}")
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .accessibilityLabel("Decrease speed")

                        Text(String(format: "%.1fx", scrollSpeed))
                            .font(.caption.monospacedDigit())
                            .accessibilityLabel("Speed \(String(format: "%.1f", scrollSpeed))x")

                        Button { scrollSpeed = min(5.0, scrollSpeed + 0.5) } label: {
                            Text("+")
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .accessibilityLabel("Increase speed")
                    }

                    Button { dismiss() } label: {
                        Image(systemName: "arrow.down.right.and.arrow.up.left")
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .accessibilityLabel("Exit performance mode")
                }
                .padding(12)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(16)
            }
        }
        .background(Color(uiColor: .systemBackground))
        .onTapGesture {
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            } else {
                showControls.toggle()
            }
        }
        .accessibilityHint(UIAccessibility.isVoiceOverRunning ? "Controls remain visible for VoiceOver" : "Tap anywhere to show or hide controls")
        .statusBarHidden(true)
        .onAppear {
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIAccessibility.voiceOverStatusDidChangeNotification)) { _ in
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            }
        }
    }
}

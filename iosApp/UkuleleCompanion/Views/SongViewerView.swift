import SwiftUI
import shared

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
    @EnvironmentObject var settingsVM: SettingsViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
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
    // Persisted rather than per-screen: a player who prefers the compact reading
    // layout wants it on the next song too.
    @AppStorage("songDetailsCollapsed") private var detailsCollapsed = false
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
        let chords = ChordParser.shared.extractChords(text: displaySong.content).asStrings
        guard !chords.isEmpty else { return nil }
        guard let result = KeyDetector.shared.detectKey(chordNames: chords) else { return nil }
        return result.displayName
    }

    /// A key stored on the song (from a ChordPro `{key: ...}` directive or the editor)
    /// is authoritative and wins over the detector's guess; the detector only fills in
    /// when the song does not declare one. `displaySong` already carries the transposed
    /// key, so a transpose preview moves both together.
    private var displayKey: String? {
        let stored = displaySong.key.trimmingCharacters(in: .whitespaces)
        return stored.isEmpty ? detectedKey : stored
    }

    private var displayTitle: String {
        currentSong.title.isEmpty ? "Untitled" : currentSong.title
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if !detailsCollapsed {
                        songInfoSection

                        transposeSection

                        sectionNavigation(proxy: proxy, content: displaySong.content)

                        tempoSection(content: displaySong.content)

                        chordDiagramRail(content: displaySong.content)
                    }

                    detailsToggle

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
                ChordDetailPopover(chord: chord, tonePlayer: tonePlayer)
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
                font: songFont,
                chordDisplayStyle: settingsVM.chordDisplayStyle,
                chordColor: resolvedChordColor,
                tonePlayer: tonePlayer
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
        do {
            try data.write(to: url)
        } catch {
            AccessibilityAnnouncer.shared.announce("Failed to create PDF")
            return
        }

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
            if !currentSong.subtitle.isEmpty {
                Text(currentSong.subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            if !currentSong.artist.isEmpty {
                Text(currentSong.artist)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 8) {
                if currentSong.capo > 0 {
                    Label(
                        String(
                            format: NSLocalizedString("songbook_capo_value", comment: ""),
                            currentSong.capo
                        ),
                        systemImage: "guitars"
                    )
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                if let key = displayKey {
                    Label(
                        NSLocalizedString("songbook_key_prefix", comment: "") + key,
                        systemImage: "music.note"
                    )
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
                let builtInPatterns = StrumPatterns.shared.ALL.asArray(of: StrumPattern.self)
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
            let builtInPatterns = StrumPatterns.shared.ALL.asArray(of: StrumPattern.self)
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
        nonisolated(unsafe) let scrollProxy = proxy
        let timer = Timer(timeInterval: interval, repeats: true) { [self] _ in
            MainActor.assumeIsolated {
                guard self.isAutoScrolling else { return }
                self.scrollOffset += 1
                scrollProxy.scrollTo("bottom", anchor: .init(x: 0.5, y: min(1.0, self.scrollOffset / 500)))
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
                            withAnimation(reduceMotion ? nil : .default) {
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

    // MARK: - Details Toggle

    /// Collapses the song details above the lyrics (issue #501). While actually
    /// playing, the subtitle, key, strum pattern, labels and chord rail have all been
    /// read already and only cost the vertical space the lyrics need.
    ///
    /// It sits at the boundary between the details and the lyrics rather than in the
    /// toolbar, which already carries seven controls.
    private var detailsToggle: some View {
        Button {
            withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.2)) {
                detailsCollapsed.toggle()
            }
        } label: {
            Image(systemName: detailsCollapsed ? "chevron.down" : "chevron.up")
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            detailsCollapsed
                ? String(localized: "songbook_expand_details")
                : String(localized: "songbook_collapse_details")
        )
    }

    // MARK: - Content Parsing

    private func parsedContentView(song: StoredSong) -> some View {
        ChordSheetContentView(
            content: song.content,
            font: songFont,
            chordDisplayStyle: settingsVM.chordDisplayStyle,
            chordColor: resolvedChordColor,
            onChordTap: { tappedChord = $0 }
        )
    }

    @ViewBuilder
    private func chordDiagramRail(content: String) -> some View {
        if settingsVM.showChordDiagramRail {
            let chordNames = ChordParser.shared.extractChords(text: content)
                .asArray(of: NSString.self).map { $0 as String }
            let voicings: [(String, ChordVoicing)] = chordNames.compactMap { name in
                guard let parsed = ChordNameParser.shared.parse(input: name) else { return nil }
                let tuning = UkuleleTuning.highG.asUkuleleStrings
                let generated = VoicingGenerator.shared.generate(
                    rootPitchClass: Int32(parsed.rootPitchClass),
                    formula: parsed.formula,
                    tuning: tuning,
                    allowMutedStrings: false
                ).asArray(of: ChordVoicing.self)
                guard let voicing = generated.first else { return nil }
                return (name, voicing)
            }
            if !voicings.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(0..<voicings.count, id: \.self) { i in
                            let (name, voicing) = voicings[i]
                            VStack(spacing: 4) {
                                ChordDiagramView(voicing: voicing, chordName: name)
                                Text(name)
                                    .font(.caption2)
                                    .bold()
                            }
                            .onTapGesture { tappedChord = name }
                            .accessibilityElement(children: .combine)
                            .accessibilityLabel("\(name) chord diagram")
                            .accessibilityAddTraits(.isButton)
                            .accessibilityHint("Show chord options")
                        }
                    }
                    .padding(.horizontal)
                }
                Divider()
            }
        }
    }

    private var songFont: Font {
        .system(size: CGFloat(songFontSize), design: .monospaced)
    }

    private var resolvedChordColor: Color {
        switch settingsVM.chordColor {
        case "red": return .red
        case "blue": return .blue
        case "green": return .green
        case "orange": return .orange
        case "purple": return .purple
        default: return .accentColor
        }
    }
}

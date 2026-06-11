import SwiftUI
import shared

struct SongEditorView: View {
    @ObservedObject var viewModel: SongbookViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var title: String
    @State private var artist: String
    @State private var content: String
    @State private var key: String
    @State private var capo: Int
    @State private var strumPatternName: String
    @State private var labels: [String]
    @State private var labelInput: String = ""
    @State private var showPreview = false
    @State private var showDiscardDialog = false
    @EnvironmentObject var customPatternsVM: CustomPatternsViewModel

    private let existingSong: StoredSong?

    private let initialTitle: String
    private let initialArtist: String
    private let initialContent: String
    private let initialKey: String
    private let initialCapo: Int
    private let initialStrumPatternName: String
    private let initialLabels: [String]

    init(viewModel: SongbookViewModel, song: StoredSong?) {
        self.viewModel = viewModel
        self.existingSong = song

        let t = song?.title ?? ""
        let a = song?.artist ?? ""
        let c = song?.content ?? ""
        let k = song?.key ?? ""
        let cp = song?.capo ?? 0
        let sp = song?.strumPatternName ?? ""
        let lb = song?.labels ?? []

        _title = State(initialValue: t)
        _artist = State(initialValue: a)
        _content = State(initialValue: c)
        _key = State(initialValue: k)
        _capo = State(initialValue: cp)
        _strumPatternName = State(initialValue: sp)
        _labels = State(initialValue: lb)

        initialTitle = t
        initialArtist = a
        initialContent = c
        initialKey = k
        initialCapo = cp
        initialStrumPatternName = sp
        initialLabels = lb
    }

    private var hasChanges: Bool {
        title != initialTitle ||
        artist != initialArtist ||
        content != initialContent ||
        key != initialKey ||
        capo != initialCapo ||
        strumPatternName != initialStrumPatternName ||
        labels != initialLabels
    }

    var body: some View {
        Form {
            Section("Song Info") {
                TextField("Title", text: $title)
                TextField("Artist", text: $artist)
            }

            Section("Key & Capo") {
                Picker("Key", selection: $key) {
                    Text("None").tag("")
                    ForEach(0..<12, id: \.self) { pc in
                        Text(Notes.shared.pitchClassToName(pitchClass: Int32(pc)))
                            .tag(Notes.shared.pitchClassToName(pitchClass: Int32(pc)))
                    }
                }

                Stepper("Capo: \(capo)", value: $capo, in: 0...12)
            }

            Section("Labels") {
                labelEditorSection
            }

            Section("Strum Pattern") {
                let builtInPatterns = StrumPatterns.shared.ALL.asArray(of: StrumPattern.self)
                Picker("Pattern", selection: $strumPatternName) {
                    Text("None").tag("")
                    Section("Built-in") {
                        ForEach(0..<builtInPatterns.count, id: \.self) { i in
                            Text(builtInPatterns[i].name).tag(builtInPatterns[i].name)
                        }
                    }
                    if !customPatternsVM.strumPatterns.isEmpty {
                        Section("Custom") {
                            ForEach(customPatternsVM.strumPatterns) { custom in
                                Text(custom.name).tag(custom.name)
                            }
                        }
                    }
                }
            }

            Section {
                Picker("Mode", selection: $showPreview) {
                    Text("Edit").tag(false)
                    Text("Preview").tag(true)
                }
                .pickerStyle(.segmented)

                if showPreview {
                    previewContent
                } else {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(chordChipNames, id: \.self) { chord in
                                Button(action: { content += "[\(chord)]" }) {
                                    Text(chord)
                                        .font(.caption.bold())
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 6)
                                        .background(Color.accentColor.opacity(0.15))
                                        .foregroundStyle(Color.accentColor)
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("Insert \(chord) chord")
                            }
                        }
                    }

                    TextEditor(text: $content)
                        .font(.system(.body, design: .monospaced))
                        .frame(minHeight: 200)
                }
            } header: {
                Text("Content (use [Chord] markers)")
            }
        }
        .navigationTitle(existingSong != nil ? "Edit Song" : "New Song")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    if hasChanges {
                        showDiscardDialog = true
                    } else {
                        dismiss()
                    }
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    saveSong()
                    dismiss()
                }
                .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
        .alert("Discard Changes?", isPresented: $showDiscardDialog) {
            Button("Cancel", role: .cancel) {}
            Button("Discard", role: .destructive) { dismiss() }
        } message: {
            Text("You have unsaved changes. Are you sure you want to discard them?")
        }
    }

    // MARK: - Label Editor

    private var labelEditorSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            if !labels.isEmpty {
                FlowLayout(spacing: 6) {
                    ForEach(labels, id: \.self) { label in
                        HStack(spacing: 4) {
                            Text(label)
                                .font(.caption)
                            Button {
                                labels.removeAll { $0 == label }
                            } label: {
                                Image(systemName: "xmark")
                                    .font(.caption2)
                            }
                            .accessibilityLabel("Remove \(label) label")
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.accentColor.opacity(0.15))
                        .clipShape(Capsule())
                    }
                }
            }

            HStack {
                TextField("Add label...", text: $labelInput)
                    .textFieldStyle(.plain)
                    .onSubmit { addCurrentLabel() }
                if !labelInput.isEmpty {
                    Button {
                        addCurrentLabel()
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .foregroundStyle(Color.accentColor)
                    }
                    .accessibilityLabel("Add label")
                }
            }

            let suggestions = labelSuggestions
            if !suggestions.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(suggestions, id: \.self) { suggestion in
                            Button {
                                if !labels.contains(suggestion) {
                                    labels.append(suggestion)
                                }
                                labelInput = ""
                            } label: {
                                Text(suggestion)
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color(.systemGray5))
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private var labelSuggestions: [String] {
        let existing = Set(labels)
        let allLabels = viewModel.allLabels
        if labelInput.isEmpty {
            return allLabels.filter { !existing.contains($0) }
        }
        let query = labelInput.lowercased()
        return allLabels.filter { !existing.contains($0) && $0.lowercased().contains(query) }
    }

    private func addCurrentLabel() {
        let trimmed = labelInput.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty && !labels.contains(trimmed) {
            labels.append(trimmed)
        }
        labelInput = ""
    }

    // MARK: - Preview

    private var previewContent: some View {
        let lines = content.components(separatedBy: "\n")
        return VStack(alignment: .leading, spacing: 2) {
            ForEach(0..<lines.count, id: \.self) { i in
                let segments = ChordParser.shared.parseLine(line: lines[i]).asArray(of: ChordParser.TextSegment.self)
                previewLineView(segments: segments)
            }
        }
        .frame(minHeight: 200, alignment: .topLeading)
    }

    @ViewBuilder
    private func previewLineView(segments: [ChordParser.TextSegment]) -> some View {
        let hasChords = segments.contains(where: { $0 is ChordParser.TextSegmentChord })
        if hasChords {
            let result = Self.buildChordAndLyricLines(segments: segments)
            VStack(alignment: .leading, spacing: 0) {
                Text(result.chordLine)
                    .font(.system(.body, design: .monospaced))
                    .foregroundColor(.accentColor)
                    .bold()
                if !result.lyrics.trimmingCharacters(in: .whitespaces).isEmpty {
                    Text(result.lyrics)
                        .font(.system(.body, design: .monospaced))
                }
            }
        } else {
            let text = segments.compactMap { ($0 as? ChordParser.TextSegmentPlainText)?.text }.joined()
            Text(text.isEmpty ? " " : text)
                .font(.system(.body, design: .monospaced))
        }
    }

    private static func buildChordAndLyricLines(segments: [ChordParser.TextSegment])
        -> (chordLine: String, lyrics: String)
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

        var chordLine = ""
        var pos = 0
        for entry in chordEntries {
            let gap = entry.position - pos
            if gap > 0 {
                chordLine += String(repeating: " ", count: gap)
                pos = entry.position
            } else if pos > 0 {
                chordLine += " "
                pos += 1
            }
            chordLine += entry.name
            pos += entry.name.count
        }

        return (chordLine, lyrics)
    }

    private let chordChipNames = ["C", "G", "Am", "F", "Em", "Dm", "D", "A", "E", "Bm"]

    private func saveSong() {
        let now = Date().timeIntervalSince1970 * 1000
        let song = StoredSong(
            id: existingSong?.id ?? UUID().uuidString,
            title: title.trimmingCharacters(in: .whitespaces),
            subtitle: existingSong?.subtitle ?? "",
            artist: artist.trimmingCharacters(in: .whitespaces),
            content: content,
            key: key,
            capo: capo,
            strumPatternName: strumPatternName,
            labels: labels,
            createdAt: existingSong?.createdAt ?? now,
            updatedAt: now
        )
        viewModel.save(song: song)
    }
}

// MARK: - Flow Layout

struct FlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: ProposedViewSize(subviews[index].sizeThatFits(.unspecified))
            )
        }
    }

    private func arrangeSubviews(proposal: ProposedViewSize, subviews: Subviews)
        -> (size: CGSize, positions: [CGPoint])
    {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var maxX: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            maxX = max(maxX, x - spacing)
        }

        return (CGSize(width: maxX, height: y + rowHeight), positions)
    }
}

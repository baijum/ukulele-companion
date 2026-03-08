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
    @State private var labelsText: String
    @State private var showPreview = false

    private let existingSong: StoredSong?

    init(viewModel: SongbookViewModel, song: StoredSong?) {
        self.viewModel = viewModel
        self.existingSong = song
        _title = State(initialValue: song?.title ?? "")
        _artist = State(initialValue: song?.artist ?? "")
        _content = State(initialValue: song?.content ?? "")
        _key = State(initialValue: song?.key ?? "")
        _capo = State(initialValue: song?.capo ?? 0)
        _strumPatternName = State(initialValue: song?.strumPatternName ?? "")
        _labelsText = State(initialValue: song?.labels.joined(separator: ", ") ?? "")
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

            Section("Labels (comma-separated)") {
                TextField("e.g. favorites, practice", text: $labelsText)
            }

            Section("Strum Pattern") {
                let patterns = StrumPatterns.shared.ALL as! [StrumPattern]
                Picker("Pattern", selection: $strumPatternName) {
                    Text("None").tag("")
                    ForEach(0..<patterns.count, id: \.self) { i in
                        Text(patterns[i].name).tag(patterns[i].name)
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
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    saveSong()
                    dismiss()
                }
                .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
    }

    private var previewContent: some View {
        let lines = content.components(separatedBy: "\n")
        return VStack(alignment: .leading, spacing: 2) {
            ForEach(0..<lines.count, id: \.self) { i in
                let segments = ChordParser.shared.parseLine(line: lines[i]) as! [ChordParser.TextSegment]
                let built = segments.reduce(Text("")) { result, segment in
                    if let chord = segment as? ChordParser.TextSegmentChord {
                        return result + Text(chord.name)
                            .foregroundColor(.accentColor)
                            .bold()
                    } else if let plain = segment as? ChordParser.TextSegmentPlainText {
                        return result + Text(plain.text)
                    }
                    return result
                }
                built.font(.system(.body, design: .monospaced))
            }
        }
        .frame(minHeight: 200, alignment: .topLeading)
    }

    private let chordChipNames = ["C", "G", "Am", "F", "Em", "Dm", "D", "A", "E", "Bm"]

    private func saveSong() {
        let labels = labelsText
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }

        let now = Date().timeIntervalSince1970 * 1000
        let song = StoredSong(
            id: existingSong?.id ?? UUID().uuidString,
            title: title.trimmingCharacters(in: .whitespaces),
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

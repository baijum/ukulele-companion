import SwiftUI
import UniformTypeIdentifiers
import shared

struct SongbookView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
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
                    // Detect by content first — ChordPro files are widely shipped
                    // with a .txt extension. See issue #500.
                    if ChordProParser.shared.looksLikeChordPro(content: content)
                        || ChordProParser.shared.isChordProFile(filename: filename) {
                        viewModel.importChordPro(text: content, filename: filename)
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



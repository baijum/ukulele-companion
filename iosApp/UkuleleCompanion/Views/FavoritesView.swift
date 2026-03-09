import SwiftUI
import shared

struct FavoritesView: View {
    @StateObject private var viewModel = FavoritesViewModel()
    @State private var selectedFolderId: String?
    @State private var showCreateFolder = false
    @State private var renamingFolder: FavoriteFolderData?
    @State private var deletingFolder: FavoriteFolderData?
    @State private var managingVoicing: FavoriteVoicingData?
    @State private var isReordering = false
    @State private var shareInfo: ShareChordInfo?
    private let tonePlayer = TonePlayer()

    var body: some View {
        Group {
            if viewModel.favorites.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "heart")
                        .font(.system(size: 48))
                        .foregroundStyle(.secondary)
                    Text("No Favorites Yet")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                    Text("Add favorites from the Chord Library by tapping the heart icon.")
                        .font(.subheadline)
                        .foregroundStyle(.tertiary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            } else {
                VStack(spacing: 0) {
                    // Folder chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            Button {
                                selectedFolderId = nil
                            } label: {
                                Text("All (\(viewModel.favorites.count))")
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(selectedFolderId == nil ? Color.accentColor : Color(.secondarySystemGroupedBackground))
                                    .foregroundColor(selectedFolderId == nil ? .white : .primary)
                                    .clipShape(Capsule())
                            }
                            .accessibilityAddTraits(selectedFolderId == nil ? .isSelected : [])

                            ForEach(viewModel.folders) { folder in
                                let count = viewModel.favorites.filter { $0.folderIds.contains(folder.id) }.count
                                Menu {
                                    Button { selectedFolderId = folder.id } label: {
                                        Label("View", systemImage: "folder")
                                    }
                                    Button { renamingFolder = folder } label: {
                                        Label("Rename", systemImage: "pencil")
                                    }
                                    Button(role: .destructive) { deletingFolder = folder } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                } label: {
                                    Text("\(folder.name) (\(count))")
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(selectedFolderId == folder.id ? Color.accentColor : Color(.secondarySystemGroupedBackground))
                                        .foregroundColor(selectedFolderId == folder.id ? .white : .primary)
                                        .clipShape(Capsule())
                                }
                                .onTapGesture { selectedFolderId = folder.id }
                                .accessibilityAddTraits(selectedFolderId == folder.id ? .isSelected : [])
                            }

                            Button { showCreateFolder = true } label: {
                                Image(systemName: "plus")
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color(.secondarySystemGroupedBackground))
                                    .clipShape(Capsule())
                            }
                            .accessibilityLabel("Create new folder")
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    }

                    // Grid or reorderable list
                    let displayed: [FavoriteVoicingData] = {
                        if let fid = selectedFolderId {
                            return viewModel.favoritesInFolder(fid)
                        }
                        return viewModel.favorites
                    }()

                    if selectedFolderId != nil && isReordering {
                        List {
                            ForEach(displayed) { fav in
                                HStack {
                                    Image(systemName: "line.3.horizontal")
                                        .foregroundStyle(.secondary)
                                    Text(fav.chordName)
                                        .font(.headline)
                                    Spacer()
                                    Text(fav.frets.map { String($0) }.joined(separator: " "))
                                        .font(.caption.monospacedDigit())
                                        .foregroundStyle(.secondary)
                                }
                                .accessibilityLabel("\(fav.chordName), drag to reorder")
                            }
                            .onMove { indices, newOffset in
                                guard let folderId = selectedFolderId else { return }
                                var keys = displayed.map { $0.key }
                                keys.move(fromOffsets: indices, toOffset: newOffset)
                                viewModel.reorderInFolder(folderId: folderId, orderedKeys: keys)
                            }
                        }
                        .environment(\.editMode, .constant(.active))
                    } else {
                        ScrollView {
                            LazyVGrid(columns: [GridItem(.adaptive(minimum: 160), spacing: 12)], spacing: 12) {
                                ForEach(displayed) { fav in
                                    FavoriteCard(
                                        favorite: fav,
                                        onRemove: { viewModel.removeFavorite(key: fav.key) },
                                        onManageFolders: { managingVoicing = fav },
                                        onPlay: { playFavorite(fav) },
                                        shareInfo: $shareInfo
                                    )
                                }
                            }
                            .padding()
                        }
                    }
                }
            }
        }
        .navigationTitle("Favorites")
        .toolbar {
            if selectedFolderId != nil {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        isReordering.toggle()
                    } label: {
                        Image(systemName: isReordering ? "checkmark" : "arrow.up.arrow.down")
                    }
                    .accessibilityLabel(isReordering ? "Done reordering" : "Reorder favorites")
                }
            }
        }
        .alert("New Folder", isPresented: $showCreateFolder) {
            CreateFolderAlert { name in
                viewModel.createFolder(name: name)
            }
        }
        .alert("Rename Folder", isPresented: Binding(
            get: { renamingFolder != nil },
            set: { if !$0 { renamingFolder = nil } }
        )) {
            if let folder = renamingFolder {
                RenameFolderAlert(currentName: folder.name) { newName in
                    viewModel.renameFolder(id: folder.id, name: newName)
                    renamingFolder = nil
                }
            }
        }
        .confirmationDialog("Delete Folder?", isPresented: Binding(
            get: { deletingFolder != nil },
            set: { if !$0 { deletingFolder = nil } }
        )) {
            if let folder = deletingFolder {
                Button("Delete \"\(folder.name)\"", role: .destructive) {
                    if selectedFolderId == folder.id { selectedFolderId = nil }
                    viewModel.deleteFolder(id: folder.id)
                    deletingFolder = nil
                }
            }
        }
        .sheet(item: $shareInfo) { info in
            ShareChordSheet(info: info)
        }
        .sheet(item: $managingVoicing) { voicing in
            FolderManagementSheet(
                folders: viewModel.folders,
                selectedFolderIds: voicing.folderIds,
                onSave: { ids in
                    viewModel.setFolders(voicingKey: voicing.key, folderIds: ids)
                    managingVoicing = nil
                },
                onDismiss: { managingVoicing = nil }
            )
        }
    }
    private func playFavorite(_ fav: FavoriteVoicingData) {
        let pitchClasses = fav.frets.enumerated().compactMap { i, fret -> Int32? in
            guard fret >= 0 else { return nil }
            let openPc = (UkuleleTuning.highG.pitchClasses[i] as! NSNumber).int32Value
            return (openPc + Int32(fret)) % 12
        }
        tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
    }
}

// MARK: - Subviews

private struct FavoriteCard: View {
    let favorite: FavoriteVoicingData
    let onRemove: () -> Void
    let onManageFolders: () -> Void
    var onPlay: (() -> Void)?
    var shareInfo: Binding<ShareChordInfo?>?

    private var voicing: ChordVoicing {
        let kotlinFrets = favorite.frets.map { KotlinInt(int: Int32($0)) }
        let positives = favorite.frets.filter { $0 > 0 }
        return ChordVoicing(
            frets: kotlinFrets,
            notes: favorite.frets.map { _ in NSNull() },
            minFret: Int32(positives.min() ?? 0),
            maxFret: Int32(positives.max() ?? 0)
        )
    }

    var body: some View {
        VStack(spacing: 4) {
            HStack {
                Button(action: onManageFolders) {
                    Image(systemName: "folder")
                        .font(.caption)
                }
                .accessibilityLabel("Manage folders for \(favorite.chordName)")
                Spacer()

                if let onPlay {
                    Button(action: onPlay) {
                        Image(systemName: "speaker.wave.2")
                            .font(.caption)
                    }
                    .accessibilityLabel("Play \(favorite.chordName)")
                }

                if let shareInfo {
                    Button {
                        shareInfo.wrappedValue = ShareChordInfo(
                            voicing: voicing,
                            chordName: favorite.chordName
                        )
                    } label: {
                        Image(systemName: "square.and.arrow.up")
                            .font(.caption)
                    }
                    .accessibilityLabel("Share \(favorite.chordName)")
                }

                Button(action: onRemove) {
                    Image(systemName: "heart.fill")
                        .font(.caption)
                        .foregroundColor(.red)
                }
                .accessibilityLabel("Remove \(favorite.chordName) from favorites")
            }

            ChordDiagramView(voicing: voicing, chordName: favorite.chordName)
        }
        .padding(12)
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct FolderManagementSheet: View {
    let folders: [FavoriteFolderData]
    let selectedFolderIds: [String]
    let onSave: ([String]) -> Void
    let onDismiss: () -> Void

    @State private var selection: Set<String>

    init(folders: [FavoriteFolderData], selectedFolderIds: [String],
         onSave: @escaping ([String]) -> Void, onDismiss: @escaping () -> Void) {
        self.folders = folders
        self.selectedFolderIds = selectedFolderIds
        self.onSave = onSave
        self.onDismiss = onDismiss
        _selection = State(initialValue: Set(selectedFolderIds))
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(folders) { folder in
                    Button {
                        if selection.contains(folder.id) {
                            selection.remove(folder.id)
                        } else {
                            selection.insert(folder.id)
                        }
                    } label: {
                        HStack {
                            Text(folder.name)
                            Spacer()
                            if selection.contains(folder.id) {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                    .foregroundColor(.primary)
                }
            }
            .navigationTitle("Assign Folders")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { onSave(Array(selection)) }
                }
            }
        }
    }
}

@MainActor @ViewBuilder
private func CreateFolderAlert(onCreate: @escaping (String) -> Void) -> some View {
    @State var name = ""
    TextField("Folder name", text: $name)
    Button("Create") { if !name.isEmpty { onCreate(name) } }
    Button("Cancel", role: .cancel) {}
}

@MainActor @ViewBuilder
private func RenameFolderAlert(currentName: String, onRename: @escaping (String) -> Void) -> some View {
    @State var name = currentName
    TextField("Folder name", text: $name)
    Button("Rename") { if !name.isEmpty { onRename(name) } }
    Button("Cancel", role: .cancel) {}
}

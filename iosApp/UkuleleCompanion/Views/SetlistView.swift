import SwiftUI

struct SetlistView: View {
    @ObservedObject var viewModel: SetlistViewModel
    @ObservedObject var songbookViewModel: SongbookViewModel
    @State private var showingCreateSheet = false
    @State private var newSetlistName = ""
    @State private var selectedSetlist: StoredSetlist? = nil

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.setlists.isEmpty {
                    VStack(spacing: 12) {
                        Text("No Setlists Yet")
                            .font(.title2)
                            .accessibilityAddTraits(.isHeader)
                        Text("Create a setlist to organize songs for gigs or practice.")
                            .foregroundColor(.secondary)
                    }
                    .padding()
                } else {
                    List {
                        ForEach(viewModel.setlists) { setlist in
                            Button {
                                selectedSetlist = setlist
                            } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(setlist.name)
                                        .font(.headline)
                                    Text("\(setlist.songIds.count) songs")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("\(setlist.name), \(setlist.songIds.count) songs")
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.delete(id: setlist.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Setlists")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        newSetlistName = ""
                        showingCreateSheet = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Create setlist")
                }
            }
            .alert("New Setlist", isPresented: $showingCreateSheet) {
                TextField("Setlist name", text: $newSetlistName)
                Button("Create") {
                    let name = newSetlistName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty {
                        viewModel.create(name: name)
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(item: $selectedSetlist) { setlist in
                NavigationStack {
                    SetlistDetailView(
                        setlist: setlist,
                        viewModel: viewModel,
                        songbookViewModel: songbookViewModel
                    )
                }
            }
        }
    }
}

struct SetlistDetailView: View {
    let setlist: StoredSetlist
    @ObservedObject var viewModel: SetlistViewModel
    @ObservedObject var songbookViewModel: SongbookViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showingAddSong = false

    private var currentSetlist: StoredSetlist {
        viewModel.setlists.first(where: { $0.id == setlist.id }) ?? setlist
    }

    private var setlistSongs: [StoredSong] {
        let songMap = Dictionary(uniqueKeysWithValues: songbookViewModel.songs.map { ($0.id, $0) })
        return currentSetlist.songIds.compactMap { songMap[$0] }
    }

    var body: some View {
        List {
            if setlistSongs.isEmpty {
                Text("No songs in this setlist. Tap + to add songs.")
                    .foregroundColor(.secondary)
            } else {
                ForEach(Array(setlistSongs.enumerated()), id: \.element.id) { index, song in
                    HStack {
                        Text("\(index + 1).")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .frame(width: 30)
                        VStack(alignment: .leading) {
                            Text(song.title.isEmpty ? "Untitled" : song.title)
                                .font(.body)
                            if !song.artist.isEmpty {
                                Text(song.artist)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .accessibilityLabel("\(index + 1). \(song.title.isEmpty ? "Untitled" : song.title)")
                }
                .onMove { source, destination in
                    guard let from = source.first else { return }
                    let to = destination > from ? destination - 1 : destination
                    viewModel.moveSong(setlistId: currentSetlist.id, from: from, to: to)
                }
                .onDelete { offsets in
                    for index in offsets {
                        let songId = currentSetlist.songIds[index]
                        viewModel.removeSong(setlistId: currentSetlist.id, songId: songId)
                    }
                }
            }
        }
        .navigationTitle(currentSetlist.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Done") { dismiss() }
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showingAddSong = true
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Add song to setlist")
            }
            ToolbarItem(placement: .primaryAction) {
                EditButton()
            }
        }
        .sheet(isPresented: $showingAddSong) {
            NavigationStack {
                AddSongToSetlistView(
                    setlist: currentSetlist,
                    allSongs: songbookViewModel.songs,
                    onAdd: { songId in
                        viewModel.addSong(setlistId: currentSetlist.id, songId: songId)
                    }
                )
            }
        }
    }
}

struct AddSongToSetlistView: View {
    let setlist: StoredSetlist
    let allSongs: [StoredSong]
    let onAdd: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    private var availableSongs: [StoredSong] {
        allSongs.filter { !setlist.songIds.contains($0.id) }
    }

    var body: some View {
        List {
            if availableSongs.isEmpty {
                Text("All songs are already in this setlist.")
                    .foregroundColor(.secondary)
            } else {
                ForEach(availableSongs) { song in
                    Button {
                        onAdd(song.id)
                        dismiss()
                    } label: {
                        VStack(alignment: .leading) {
                            Text(song.title.isEmpty ? "Untitled" : song.title)
                            if !song.artist.isEmpty {
                                Text(song.artist)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .navigationTitle("Add Song")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
        }
    }
}

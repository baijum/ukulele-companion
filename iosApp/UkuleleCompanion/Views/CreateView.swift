import SwiftUI

struct CreateView: View {
    @Binding var showSettings: Bool
    @EnvironmentObject var setlistVM: SetlistViewModel
    @EnvironmentObject var songbookVM: SongbookViewModel

    var body: some View {
        NavigationStack {
            List {
                NavigationLink {
                    SongwriterModeFlow()
                } label: {
                    Label("Start a Song", systemImage: "wand.and.stars")
                }

                NavigationLink {
                    ProgressionsView()
                } label: {
                    Label("Chord Progressions", systemImage: "play.circle")
                }

                NavigationLink {
                    StrumPatternsView()
                } label: {
                    Label("Strumming Patterns", systemImage: "metronome")
                }

                NavigationLink {
                    SongbookView()
                } label: {
                    Label("Songbook", systemImage: "music.note.list")
                }

                NavigationLink {
                    SetlistView(viewModel: setlistVM, songbookViewModel: songbookVM)
                } label: {
                    Label("Setlists", systemImage: "list.number")
                }

                NavigationLink {
                    MelodyNotepadView()
                } label: {
                    Label("Melody Notepad", systemImage: "pianokeys")
                }
            }
            .navigationTitle("Create")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape")
                            .accessibilityLabel("Settings")
                    }
                }
            }
        }
    }
}

import SwiftUI

struct CreateView: View {
    @Binding var showSettings: Bool

    var body: some View {
        NavigationStack {
            List {
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

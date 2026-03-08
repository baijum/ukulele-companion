import SwiftUI

struct PlayView: View {
    @Binding var showSettings: Bool

    var body: some View {
        NavigationStack {
            List {
                NavigationLink {
                    ExplorerView()
                } label: {
                    Label("Explorer", systemImage: "guitars")
                }

                NavigationLink {
                    TunerView()
                } label: {
                    Label("Tuner", systemImage: "tuningfork")
                }

                NavigationLink {
                    PitchMonitorView()
                } label: {
                    Label("Pitch Monitor", systemImage: "waveform")
                }

                NavigationLink {
                    MetronomeView()
                } label: {
                    Label("Metronome", systemImage: "metronome")
                }

                NavigationLink {
                    ChordLibraryView()
                } label: {
                    Label("Chord Library", systemImage: "music.note.list")
                }

                NavigationLink {
                    FavoritesView()
                } label: {
                    Label("Favorites", systemImage: "heart")
                }
            }
            .navigationTitle("Play")
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

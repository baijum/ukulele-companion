import SwiftUI

struct ReferenceView: View {
    @Binding var showSettings: Bool

    var body: some View {
        NavigationStack {
            List {
                NavigationLink {
                    GlossaryView()
                } label: {
                    Label("Glossary", systemImage: "character.book.closed")
                }

                NavigationLink {
                    CapoGuideView()
                } label: {
                    Label("Capo Guide", systemImage: "guitars")
                }

                NavigationLink {
                    FretboardNoteMapView()
                } label: {
                    Label("Fretboard Note Map", systemImage: "square.grid.3x3")
                }

                NavigationLink {
                    ChordSubstitutionsView()
                } label: {
                    Label("Chord Substitutions", systemImage: "arrow.triangle.swap")
                }

                NavigationLink {
                    ScaleChordsView()
                } label: {
                    Label("Scale Chords", systemImage: "music.note.tv")
                }

                NavigationLink {
                    CircleOfFifthsView()
                } label: {
                    Label("Circle of Fifths", systemImage: "circle.dotted")
                }
            }
            .navigationTitle("Reference")
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

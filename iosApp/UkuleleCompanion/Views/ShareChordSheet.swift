import SwiftUI
import shared

/// Data needed to present the chord sharing sheet.
struct ShareChordInfo: Identifiable {
    let id = UUID()
    let voicing: ChordVoicing
    let chordName: String
    let inversionLabel: String?

    init(voicing: ChordVoicing, chordName: String, inversionLabel: String? = nil) {
        self.voicing = voicing
        self.chordName = chordName
        self.inversionLabel = inversionLabel
    }
}

/// Sheet that previews a shareable chord diagram and offers a "Share as Image" button.
struct ShareChordSheet: View {
    let info: ShareChordInfo
    // ImageRenderer content cannot read the diagram environment values, so the
    // handedness and tuning must be handed to ShareableChordDiagramView
    // explicitly. Read them from the shared SettingsViewModel (#638).
    @EnvironmentObject private var settings: SettingsViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var renderedImage: UIImage?

    /// Open-string names for the current tuning, resolved the same way the
    /// on-screen diagram's `\.diagramStringNames` environment value is (#576).
    private var stringNames: [String] {
        settings.resolvedTuning.stringNameArray
    }

    /// Open-string pitch classes for the current tuning, needed so the exported
    /// note names are correct for Baritone (D-G-B-E) as well as G-C-E-A tunings.
    private var openPitchClasses: [Int] {
        settings.resolvedTuning.pitchClassInts.map { Int($0) }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                ShareableChordDiagramView(
                    voicing: info.voicing,
                    chordName: info.chordName,
                    inversionLabel: info.inversionLabel,
                    leftHanded: settings.leftHanded,
                    stringNames: stringNames,
                    openPitchClasses: openPitchClasses
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .shadow(color: .black.opacity(0.1), radius: 4, y: 2)

                if let image = renderedImage {
                    ShareLink(
                        item: Image(uiImage: image),
                        preview: SharePreview(
                            "\(info.chordName) — Ukulele Companion",
                            image: Image(uiImage: image)
                        )
                    ) {
                        Label("Share as Image", systemImage: "square.and.arrow.up")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                } else {
                    Button(action: {}) {
                        Label("Rendering...", systemImage: "square.and.arrow.up")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(true)
                }
            }
            .padding(24)
            .navigationTitle("Share Chord")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .onAppear { renderImage() }
    }

    @MainActor
    private func renderImage() {
        let diagram = ShareableChordDiagramView(
            voicing: info.voicing,
            chordName: info.chordName,
            inversionLabel: info.inversionLabel,
            leftHanded: settings.leftHanded,
            stringNames: stringNames,
            openPitchClasses: openPitchClasses
        )
        let renderer = ImageRenderer(content: diagram)
        renderer.scale = 3.0  // High-res for sharing
        renderedImage = renderer.uiImage
    }
}

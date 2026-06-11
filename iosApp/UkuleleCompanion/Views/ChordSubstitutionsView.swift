import SwiftUI
import shared

struct ChordSubstitutionsView: View {
    @State private var selectedKey: Int32 = 0

    private let noteNamesSharp = Notes.shared.NOTE_NAMES_SHARP.asStrings
    private let noteNamesFlat = Notes.shared.NOTE_NAMES_FLAT.asStrings
    private let categories = ChordSubstitutions.shared.CATEGORIES.asArray(of: SubstitutionCategory.self)

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Key selector
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(0..<Int32(12), id: \.self) { pc in
                            let name = Notes.shared.pitchClassToName(pitchClass: pc)
                            Button {
                                selectedKey = pc
                            } label: {
                                Text(name)
                                    .font(.subheadline.bold())
                                    .frame(minWidth: 36, minHeight: 36)
                                    .background(
                                        selectedKey == pc
                                            ? Color.accentColor
                                            : Color(.systemGray5)
                                    )
                                    .foregroundStyle(selectedKey == pc ? .white : .primary)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                            .accessibilityAddTraits(selectedKey == pc ? .isSelected : [])
                        }
                    }
                    .padding(.horizontal)
                }

                Text("Key of \(Notes.shared.pitchClassToName(pitchClass: selectedKey)) Major")
                    .font(.headline)

                // Category cards
                ForEach(Array(categories.enumerated()), id: \.offset) { _, category in
                    categoryCard(category)
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Chord Substitutions")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func categoryCard(_ category: SubstitutionCategory) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(category.title)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)

            Text(category.explanation)
                .font(.caption)
                .foregroundStyle(.secondary)

            let subs = category.substitutions.asArray(of: Substitution.self)
            ForEach(Array(subs.enumerated()), id: \.offset) { _, sub in
                substitutionRow(sub)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }

    @ViewBuilder
    private func substitutionRow(_ sub: Substitution) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(sub.original)
                    .font(.subheadline.bold())
                Image(systemName: "arrow.right")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                Text(sub.substitute)
                    .font(.subheadline.bold())
                    .foregroundStyle(Color.accentColor)
            }
            .accessibilityCombined(label: "\(sub.original) can be replaced by \(sub.substitute)")

            if sub.sharedNotes != "\u{2014}" {
                Text("Shared: \(sub.sharedNotes)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Text(transposeExample(sub.exampleInC))
                .font(.caption)
                .italic()
                .foregroundStyle(.secondary)
        }
        .padding(8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemGray6))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func transposeExample(_ example: String) -> String {
        if selectedKey == 0 { return example }
        var result = example
        // Replace longer names first (sharps/flats before naturals) to avoid partial matches
        let sortedNotes = (noteNamesSharp + noteNamesFlat)
            .filter { $0.count > 1 }
            .sorted { $0.count > $1.count }
        let naturalNotes = noteNamesSharp.filter { $0.count == 1 }

        // Build a mapping from note name to transposed note name
        var replacements: [(String, String)] = []

        for (i, name) in noteNamesSharp.enumerated() {
            let transposedPC = (Int32(i) + selectedKey) % 12
            let transposedName = Notes.shared.pitchClassToName(pitchClass: transposedPC)
            replacements.append((name, transposedName))
        }
        for (i, name) in noteNamesFlat.enumerated() {
            let transposedPC = (Int32(i) + selectedKey) % 12
            let transposedName = Notes.shared.pitchClassToName(pitchClass: transposedPC)
            if !replacements.contains(where: { $0.0 == name }) {
                replacements.append((name, transposedName))
            }
        }

        // Sort by length descending to avoid partial replacement issues
        replacements.sort { $0.0.count > $1.0.count }

        // Use placeholder-based replacement to avoid double-replacing
        var placeholders: [(String, String)] = []
        for (i, (original, transposed)) in replacements.enumerated() {
            let placeholder = "\u{FFFC}\(i)\u{FFFC}"
            result = result.replacingOccurrences(of: original, with: placeholder)
            placeholders.append((placeholder, transposed))
        }
        for (placeholder, transposed) in placeholders {
            result = result.replacingOccurrences(of: placeholder, with: transposed)
        }

        return result
    }
}

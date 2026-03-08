import SwiftUI
import shared

struct GlossaryView: View {
    @State private var searchText = ""
    @State private var expandedTerm: String?

    private var entries: [GlossaryEntry] {
        let all = Glossary.shared.ALL as! [GlossaryEntry]
        if searchText.isEmpty { return all }
        let query = searchText.lowercased()
        return all.filter {
            $0.term.lowercased().contains(query) ||
            $0.definition.lowercased().contains(query)
        }
    }

    private var groupedEntries: [(String, [GlossaryEntry])] {
        let grouped = Dictionary(grouping: entries) { entry in
            String(entry.term.prefix(1)).uppercased()
        }
        return grouped.sorted { $0.key < $1.key }
    }

    var body: some View {
        List {
            ForEach(groupedEntries, id: \.0) { letter, items in
                Section(header: Text(letter)) {
                    ForEach(items, id: \.term) { entry in
                        glossaryRow(entry)
                    }
                }
            }
        }
        .searchable(text: $searchText, prompt: "Search terms")
        .navigationTitle("Glossary")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func glossaryRow(_ entry: GlossaryEntry) -> some View {
        let isExpanded = expandedTerm == entry.term
        Button {
            withAnimation {
                expandedTerm = isExpanded ? nil : entry.term
            }
        } label: {

            VStack(alignment: .leading, spacing: 4) {
                Text(entry.term)
                    .font(.headline)
                    .foregroundStyle(.primary)

                Text(entry.definition)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(isExpanded ? nil : 2)

                if isExpanded, let example = entry.example {
                    Text(example)
                        .font(.subheadline)
                        .italic()
                        .foregroundStyle(.secondary)
                        .padding(.top, 2)
                }
            }
            .padding(.vertical, 2)
        }
        .buttonStyle(.plain)
        .accessibilityHint(isExpanded ? "Double-tap to collapse" : "Double-tap to expand")
    }
}

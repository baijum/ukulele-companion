import SwiftUI
import shared

struct ChordLibraryView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @StateObject private var viewModel = ChordLibraryViewModel()
    @EnvironmentObject private var favoritesVM: FavoritesViewModel
    var onApplyVoicing: ((ChordVoicing, Int32, ChordFormula) -> Void)?
    var fretboardVM: FretboardViewModel?
    @State private var shareInfo: ShareChordInfo?
    @State private var showCapoCalculator = false
    @State private var capoVoicing: ChordVoicing?
    @State private var showCapoVisualizer = false
    @State private var inversionFilter: ChordInfo.Inversion? = nil
    @State private var filtersExpanded: Bool = true
    @State private var transposeSemitones: Int = 0
    @State private var folderSheetVoicing: FolderSheetVoicingInfo?

    private let columns = [GridItem(.adaptive(minimum: 160), spacing: 12)]

    @FocusState private var searchFocused: Bool

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                chordSearchBar

                filterToggleRow

                if filtersExpanded {
                    rootSelector
                    transposeRow
                    categorySelector
                    formulaSelector

                    if viewModel.selectedFormula != nil && !viewModel.voicings.isEmpty {
                        inversionFilterSection
                    }
                }

                voicingGrid
            }
            .padding()
        }
        .navigationTitle("Chord Library")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: viewModel.selectedFormula?.symbol) { _ in inversionFilter = nil }
        .onChange(of: viewModel.selectedRoot) { _ in
            inversionFilter = nil
        }
        .sheet(item: $shareInfo) { info in
            ShareChordSheet(info: info)
        }
        .sheet(item: $folderSheetVoicing) { info in
            let isFavorited = favoritesVM.isFavorite(
                rootPitchClass: info.rootPitchClass,
                chordSymbol: info.chordSymbol,
                frets: info.frets
            )
            let currentFolderIds = favoritesVM.folderIdsForVoicing(
                rootPitchClass: info.rootPitchClass,
                chordSymbol: info.chordSymbol,
                frets: info.frets
            )
            FavoriteFolderSheet(
                folders: favoritesVM.folders,
                selectedFolderIds: currentFolderIds,
                isAlreadyFavorited: isFavorited,
                onSave: { selectedIds in
                    favoritesVM.saveFavoriteToFolders(
                        rootPitchClass: info.rootPitchClass,
                        chordSymbol: info.chordSymbol,
                        frets: info.frets,
                        folderIds: selectedIds
                    )
                    folderSheetVoicing = nil
                },
                onRemove: {
                    favoritesVM.removeFavorite(
                        rootPitchClass: info.rootPitchClass,
                        chordSymbol: info.chordSymbol,
                        frets: info.frets
                    )
                    folderSheetVoicing = nil
                },
                onCreateFolder: { name in
                    favoritesVM.createFolder(name: name)
                },
                onDismiss: { folderSheetVoicing = nil }
            )
        }
        .navigationDestination(isPresented: $showCapoCalculator) {
            if let formula = viewModel.selectedFormula {
                CapoCalculatorView(mode: .singleChord(
                    rootPitchClass: viewModel.selectedRoot,
                    formula: formula
                ))
            }
        }
        .navigationDestination(isPresented: $showCapoVisualizer) {
            if let voicing = capoVoicing, let formula = viewModel.selectedFormula {
                CapoVisualizerView(
                    voicing: voicing,
                    rootPitchClass: viewModel.selectedRoot,
                    formula: formula
                )
            }
        }
    }

    // MARK: - Search Bar

    private var chordSearchBar: some View {
        VStack(spacing: 0) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
                TextField("Search chord (e.g., Cm7, C/E)", text: Binding(
                    get: { viewModel.searchQuery },
                    set: { viewModel.updateSearchQuery($0) }
                ))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($searchFocused)
                .accessibilityLabel("Search chords")

                if !viewModel.searchQuery.isEmpty {
                    Button(action: {
                        viewModel.updateSearchQuery("")
                        searchFocused = false
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                    .accessibilityLabel("Clear search")
                }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            if !viewModel.searchQuery.isEmpty {
                VStack(spacing: 0) {
                    if viewModel.searchResults.isEmpty {
                        Text("No matching chords")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .padding()
                    } else {
                        let results = viewModel.searchResults
                        ForEach(0..<results.count, id: \.self) { i in
                            ChordSuggestionRow(result: results[i]) {
                                _ = viewModel.selectSearchResult(results[i])
                                searchFocused = false
                            }
                            if i < results.count - 1 {
                                Divider().padding(.leading, 12)
                            }
                        }
                        if results.count > 8 {
                            Text("\(results.count) matches")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .padding(.vertical, 6)
                        }
                    }
                }
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                .padding(.top, 4)
            }
        }
    }

    // MARK: - Filter Toggle

    private var filterToggleRow: some View {
        Button(action: { withAnimation(reduceMotion ? nil : .default) { filtersExpanded.toggle() } }) {
            HStack {
                Image(systemName: "line.3.horizontal.decrease.circle")
                if filtersExpanded {
                    Text("Filters")
                } else {
                    let rootName = viewModel.rootNoteNames[Int(viewModel.selectedRoot)]
                    let formulaName = viewModel.selectedFormula?.symbol ?? ""
                    let invLabel = inversionFilter?.label
                    Text("\(rootName)\(formulaName)\(invLabel.map { " · \($0)" } ?? "")")
                }
                Spacer()
                Image(systemName: filtersExpanded ? "chevron.up" : "chevron.down")
            }
            .font(.subheadline.bold())
            .foregroundStyle(.primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(filtersExpanded ? "Collapse filters" : "Expand filters")
    }

    // MARK: - Root Selector

    private var rootSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(0..<Int32(12), id: \.self) { pc in
                    let name = viewModel.rootNoteNames[Int(pc)]
                    Button(action: { viewModel.selectRoot(pc) }) {
                        Text(name)
                            .font(.subheadline.bold())
                            .frame(minWidth: 36, minHeight: 36)
                            .background(
                                viewModel.selectedRoot == pc
                                    ? Color.accentColor
                                    : Color(.systemGray5)
                            )
                            .foregroundStyle(
                                viewModel.selectedRoot == pc ? .white : .primary
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(viewModel.selectedRoot == pc ? .isSelected : [])
                }
            }
        }
    }

    // MARK: - Transpose Row

    private var transposeRow: some View {
        HStack {
            Text("Transpose")
                .font(.caption.bold())
                .foregroundStyle(.secondary)
            Spacer()
            Button(action: { applyTranspose(-1) }) {
                Image(systemName: "minus.circle")
                    .font(.title3)
            }
            .accessibilityLabel("Transpose down one semitone")

            Text(transposeSemitones == 0 ? "Original" : (transposeSemitones > 0 ? "+\(transposeSemitones)" : "\(transposeSemitones)"))
                .font(.subheadline.bold())
                .frame(minWidth: 60)
                .multilineTextAlignment(.center)

            Button(action: { applyTranspose(1) }) {
                Image(systemName: "plus.circle")
                    .font(.title3)
            }
            .accessibilityLabel("Transpose up one semitone")
        }
        .padding(.horizontal, 4)
    }

    private func applyTranspose(_ delta: Int) {
        transposeSemitones += delta
        let newRoot = (viewModel.selectedRoot + Int32(delta) + 12) % 12
        viewModel.selectRoot(newRoot)
    }

    // MARK: - Category Selector

    private var categorySelector: some View {
        HStack(spacing: 8) {
            ForEach(allCategories, id: \.self) { category in
                Button(action: { viewModel.selectCategory(category) }) {
                    Text(category.label)
                        .font(.caption.bold())
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            viewModel.selectedCategory == category
                                ? Color.accentColor
                                : Color(.systemGray5)
                        )
                        .foregroundStyle(
                            viewModel.selectedCategory == category ? .white : .primary
                        )
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(viewModel.selectedCategory == category ? .isSelected : [])
            }
        }
    }

    // MARK: - Formula Selector

    private var formulaSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                let formulas = viewModel.formulasForCategory(viewModel.selectedCategory)
                ForEach(0..<formulas.count, id: \.self) { i in
                    let formula = formulas[i]
                    let rootName = viewModel.rootNoteNames[Int(viewModel.selectedRoot)]
                    let displayName = rootName + formula.symbol
                    Button(action: { viewModel.selectFormula(formula) }) {
                        Text(displayName)
                            .font(.caption)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(
                                viewModel.selectedFormula == formula
                                    ? Color.accentColor.opacity(0.2)
                                    : Color(.systemGray6)
                            )
                            .foregroundStyle(
                                viewModel.selectedFormula == formula
                                    ? Color.accentColor : Color.primary
                            )
                            .clipShape(Capsule())
                            .overlay(
                                Capsule().strokeBorder(
                                    viewModel.selectedFormula == formula
                                        ? Color.accentColor : .clear,
                                    lineWidth: 1
                                )
                            )
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(viewModel.selectedFormula == formula ? .isSelected : [])
                }
            }
        }
    }

    // MARK: - Inversion Filter

    private var filteredVoicings: [ChordVoicing] {
        guard let filter = inversionFilter, let formula = viewModel.selectedFormula else {
            return viewModel.voicings
        }
        let tuning = Self.tuningStrings
        return viewModel.voicings.filter { voicing in
            let fretList = voicing.fretInts
            let kotlinFrets = fretList.map { KotlinInt(int: Int32($0)) }
            let inv = ChordInfo.shared.determineInversion(
                frets: kotlinFrets,
                rootPitchClass: viewModel.selectedRoot,
                formula: formula,
                tuning: tuning
            )
            return inv == filter
        }
    }

    private func inversionCount(_ inv: ChordInfo.Inversion) -> Int {
        guard let formula = viewModel.selectedFormula else { return 0 }
        let tuning = Self.tuningStrings
        return viewModel.voicings.filter { voicing in
            let fretList = voicing.fretInts
            let kotlinFrets = fretList.map { KotlinInt(int: Int32($0)) }
            let determined = ChordInfo.shared.determineInversion(
                frets: kotlinFrets,
                rootPitchClass: viewModel.selectedRoot,
                formula: formula,
                tuning: tuning
            )
            return determined == inv
        }.count
    }

    private var inversionFilterSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Inversions")
                    .font(.caption.bold())
                    .foregroundStyle(.secondary)
                    .accessibilityAddTraits(.isHeader)

                Spacer()

                if let fbVM = fretboardVM, filteredVoicings.count > 1 {
                    Button(action: {
                        playAllInversions(fbVM: fbVM)
                    }) {
                        Label("Play all", systemImage: "play.fill")
                            .font(.caption)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .accessibilityLabel("Play all inversions")
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    inversionChip(label: "All", count: viewModel.voicings.count, isSelected: inversionFilter == nil) {
                        inversionFilter = nil
                    }

                    let inversions: [ChordInfo.Inversion] = [.root, .first, .second, .third]
                    ForEach(inversions, id: \.self) { inv in
                        let count = inversionCount(inv)
                        if count > 0 {
                            inversionChip(label: inv.label, count: count, isSelected: inversionFilter == inv) {
                                inversionFilter = inversionFilter == inv ? nil : inv
                            }
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func inversionChip(label: String, count: Int, isSelected: Bool, onTap: @escaping () -> Void) -> some View {
        Button(action: onTap) {
            HStack(spacing: 4) {
                Text(label)
                Text("(\(count))")
                    .foregroundStyle(isSelected ? .white.opacity(0.8) : .secondary)
            }
            .font(.caption)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(isSelected ? Color.accentColor : Color.secondary.opacity(0.15), in: Capsule())
            .foregroundStyle(isSelected ? .white : .primary)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func playAllInversions(fbVM: FretboardViewModel) {
        let voicings = filteredVoicings.map { voicing -> [Int] in
            voicing.fretInts
        }
        fbVM.playVoicingsSequentially(voicings: voicings)
    }

    // MARK: - Voicing Grid

    private var voicingGrid: some View {
        let displayVoicings = filteredVoicings
        return Group {
            if displayVoicings.isEmpty {
                Text("No voicings available")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.top, 32)
            } else {
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(0..<displayVoicings.count, id: \.self) { i in
                        let voicing = displayVoicings[i]
                        let fretList = voicing.fretInts
                        let symbol = viewModel.selectedFormula?.symbol ?? ""
                        let isFav = favoritesVM.isFavorite(
                            rootPitchClass: Int(viewModel.selectedRoot),
                            chordSymbol: symbol,
                            frets: fretList
                        )
                        let invLabel: String = {
                            guard let formula = viewModel.selectedFormula else { return "" }
                            let kotlinFrets = fretList.map { KotlinInt(int: Int32($0)) }
                            let inv = ChordInfo.shared.determineInversion(
                                frets: kotlinFrets,
                                rootPitchClass: viewModel.selectedRoot,
                                formula: formula,
                                tuning: Self.tuningStrings
                            )
                            return inv.label
                        }()

                        VStack(spacing: 0) {
                            Button(action: {
                                if let formula = viewModel.selectedFormula {
                                    onApplyVoicing?(voicing, viewModel.selectedRoot, formula)
                                    fretboardVM?.playVoicing(frets: fretList)
                                }
                            }) {
                                ChordDiagramView(
                                    voicing: voicing,
                                    chordName: viewModel.currentChordName,
                                    bassStringIndex: bassStringIndex(voicing)
                                )
                                .padding(4)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Apply \(viewModel.currentChordName) voicing, \(invLabel)")
                            .accessibilityHint("Applies this voicing to the fretboard")

                            Divider()

                            HStack {
                                Button {
                                    folderSheetVoicing = FolderSheetVoicingInfo(
                                        rootPitchClass: Int(viewModel.selectedRoot),
                                        chordSymbol: symbol,
                                        frets: fretList
                                    )
                                } label: {
                                    Image(systemName: isFav ? "heart.fill" : "heart")
                                        .font(.caption)
                                        .foregroundStyle(isFav ? .red : .secondary)
                                        .frame(minWidth: 44, minHeight: 44)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel(isFav ? "Manage \(invLabel) favorites" : "Add \(invLabel) to favorites")

                                Spacer()

                                Button {
                                    shareInfo = ShareChordInfo(
                                        voicing: voicing,
                                        chordName: viewModel.currentChordName
                                    )
                                } label: {
                                    Image(systemName: "square.and.arrow.up")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                        .frame(minWidth: 44, minHeight: 44)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("Share \(viewModel.currentChordName), \(invLabel)")
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                        }
                        .background(Color(.systemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
                        .contextMenu {
                            Button {
                                shareInfo = ShareChordInfo(
                                    voicing: voicing,
                                    chordName: viewModel.currentChordName
                                )
                            } label: {
                                Label("Share as Image", systemImage: "square.and.arrow.up")
                            }

                            if viewModel.selectedFormula != nil {
                                Button {
                                    showCapoCalculator = true
                                } label: {
                                    Label("Capo Positions", systemImage: "guitars")
                                }

                                Button {
                                    capoVoicing = voicing
                                    showCapoVisualizer = true
                                } label: {
                                    Label("Capo Visualizer", systemImage: "slider.horizontal.3")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private var allCategories: [ChordCategory] {
        [.triad, .seventh, .suspended, .extended]
    }

    private static let tuningStrings: [shared.UkuleleString] = {
        let t = UkuleleTuning.highG
        return (0..<4).map { i in
            shared.UkuleleString(
                name: t.stringNames[i] as! String,
                openPitchClass: (t.pitchClasses[i] as! NSNumber).int32Value,
                octave: (t.octaves[i] as! NSNumber).int32Value
            )
        }
    }()

    private func bassStringIndex(_ voicing: ChordVoicing) -> Int? {
        let frets = voicing.fretInts.map { KotlinInt(int: Int32($0)) }
        return Int(ChordInfo.shared.findBassStringIndex(frets: frets, tuning: Self.tuningStrings))
    }
}

// MARK: - Chord Suggestion Row

private struct ChordSuggestionRow: View {
    let result: ChordSearchResult
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(result.displayName)
                        .font(.subheadline.bold())
                    let aliases = result.formula.aliases as [String]
                    if !aliases.isEmpty {
                        Text(aliases.joined(separator: ", "))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                Text(result.quality)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .contentShape(Rectangle())
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
        .accessibilityCombined(label: "\(result.displayName), \(result.quality)")
    }
}

private struct FolderSheetVoicingInfo: Identifiable {
    let rootPitchClass: Int
    let chordSymbol: String
    let frets: [Int]
    var id: String { "\(rootPitchClass)|\(chordSymbol)|\(frets.map(String.init).joined(separator: ","))" }
}

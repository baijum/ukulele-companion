import SwiftUI

enum SidebarDestination: String, Hashable {
    case explorer, tuner, pitchMonitor, metronome, chordLibrary, favorites
    case progressions, strumPatterns, songbook, melodyNotepad
    case dailyChallenge, practiceRoutine, chordTransitions, playAlong
    case intervalTrainer, chordEarTraining, noteQuiz, scalePractice
    case theoryLessons, theoryQuiz
    case learningProgress, achievements
    case glossary, capoGuide, fretboardNoteMap, chordSubstitutions, scaleChords, circleOfFifths
}

struct ContentView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedTab = 0
    @State private var showSettings = false
    @State private var showOnboarding: Bool
    @State private var selectedDestination: SidebarDestination? = .explorer
    @StateObject private var settingsVM = SettingsViewModel()
    @StateObject private var learnVM = LearnViewModel()

    init() {
        let completed = UserDefaults(suiteName: "app_settings")?.bool(forKey: "onboarding_completed") ?? false
        _showOnboarding = State(initialValue: !completed)
    }

    private var colorScheme: ColorScheme? {
        switch settingsVM.themeMode {
        case "Light": .light
        case "Dark", "High Contrast": .dark
        default: nil
        }
    }

    private var isHighContrast: Bool { settingsVM.themeMode == "High Contrast" }

    var body: some View {
        if showOnboarding {
            OnboardingView {
                showOnboarding = false
            }
        } else {
            Group {
                if sizeClass == .regular {
                    sidebarContent
                } else {
                    tabContent
                }
            }
            .preferredColorScheme(colorScheme)
        }
    }

    // MARK: - Tab layout (iPhone / iPad portrait)

    private var tabContent: some View {
        TabView(selection: $selectedTab) {
            PlayView(showSettings: $showSettings)
                .tabItem { Label("Play", systemImage: "play.circle") }
                .tag(0)

            CreateView(showSettings: $showSettings)
                .tabItem { Label("Create", systemImage: "pencil.circle") }
                .tag(1)

            if settingsVM.showLearnTab {
                LearnView(showSettings: $showSettings)
                    .tabItem { Label("Learn", systemImage: "book") }
                    .tag(2)
            }

            if settingsVM.showReferenceTab {
                ReferenceView(showSettings: $showSettings)
                    .tabItem { Label("Reference", systemImage: "list.bullet") }
                    .tag(3)
            }
        }
        .environmentObject(settingsVM)
        .environmentObject(learnVM)
        .tint(isHighContrast ? .yellow : nil)
        .sheet(isPresented: $showSettings) { SettingsView() }
        .onChange(of: showSettings) { isShowing in
            if !isShowing { settingsVM.load() }
        }
    }

    // MARK: - Sidebar layout (iPad landscape)

    private var sidebarContent: some View {
        NavigationSplitView {
            sidebarList
        } detail: {
            detailView
        }
        .environmentObject(settingsVM)
        .environmentObject(learnVM)
        .tint(isHighContrast ? .yellow : nil)
        .sheet(isPresented: $showSettings) { SettingsView() }
        .onChange(of: showSettings) { isShowing in
            if !isShowing { settingsVM.load() }
        }
    }

    private var sidebarList: some View {
        List(selection: $selectedDestination) {
            Section {
                sidebarLink(.explorer, "Explorer", icon: "guitars")
                sidebarLink(.tuner, "Tuner", icon: "tuningfork")
                sidebarLink(.pitchMonitor, "Pitch Monitor", icon: "waveform")
                sidebarLink(.metronome, "Metronome", icon: "metronome")
                sidebarLink(.chordLibrary, "Chord Library", icon: "music.note.list")
                sidebarLink(.favorites, "Favorites", icon: "heart")
            } header: {
                Text("Play").accessibilityAddTraits(.isHeader)
            }

            Section {
                sidebarLink(.progressions, "Chord Progressions", icon: "play.circle")
                sidebarLink(.strumPatterns, "Strumming Patterns", icon: "metronome")
                sidebarLink(.songbook, "Songbook", icon: "music.note.list")
                sidebarLink(.melodyNotepad, "Melody Notepad", icon: "pianokeys")
            } header: {
                Text("Create").accessibilityAddTraits(.isHeader)
            }

            if settingsVM.showLearnTab {
                Section {
                    sidebarLink(.dailyChallenge, "Daily Challenge", icon: "star.circle")
                    sidebarLink(.practiceRoutine, "Practice Routine", icon: "list.bullet.clipboard")
                    sidebarLink(.chordTransitions, "Chord Transitions", icon: "arrow.left.arrow.right")
                    sidebarLink(.playAlong, "Play Along", icon: "play.fill")
                } header: {
                    Text("Practice").accessibilityAddTraits(.isHeader)
                }

                Section {
                    sidebarLink(.intervalTrainer, "Interval Trainer", icon: "waveform.path")
                    sidebarLink(.chordEarTraining, "Chord Ear Training", icon: "ear")
                    sidebarLink(.noteQuiz, "Note Quiz", icon: "music.note")
                    sidebarLink(.scalePractice, "Scale Practice", icon: "music.quarternote.3")
                } header: {
                    Text("Training").accessibilityAddTraits(.isHeader)
                }

                Section {
                    sidebarLink(.theoryLessons, "Theory Lessons", icon: "book")
                    sidebarLink(.theoryQuiz, "Theory Quiz", icon: "questionmark.circle")
                } header: {
                    Text("Knowledge").accessibilityAddTraits(.isHeader)
                }

                Section {
                    sidebarLink(.learningProgress, "Learning Progress", icon: "chart.bar")
                    sidebarLink(.achievements, "Achievements", icon: "trophy")
                } header: {
                    Text("Progress").accessibilityAddTraits(.isHeader)
                }
            }

            if settingsVM.showReferenceTab {
                Section {
                    sidebarLink(.glossary, "Glossary", icon: "character.book.closed")
                    sidebarLink(.capoGuide, "Capo Guide", icon: "guitars")
                    sidebarLink(.fretboardNoteMap, "Fretboard Note Map", icon: "square.grid.3x3")
                    sidebarLink(.chordSubstitutions, "Chord Substitutions", icon: "arrow.triangle.swap")
                    sidebarLink(.scaleChords, "Scale Chords", icon: "music.note.tv")
                    sidebarLink(.circleOfFifths, "Circle of Fifths", icon: "circle.dotted")
                } header: {
                    Text("Reference").accessibilityAddTraits(.isHeader)
                }
            }
        }
        .navigationTitle("Ukulele Companion")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button { showSettings = true } label: {
                    Image(systemName: "gearshape")
                        .accessibilityLabel("Settings")
                }
            }
        }
    }

    private func sidebarLink(_ destination: SidebarDestination, _ title: String, icon: String) -> some View {
        NavigationLink(value: destination) {
            Label(title, systemImage: icon)
        }
    }

    @ViewBuilder
    private var detailView: some View {
        switch selectedDestination {
        case .explorer: ExplorerView()
        case .tuner: TunerView()
        case .pitchMonitor: PitchMonitorView()
        case .metronome: MetronomeView()
        case .chordLibrary: ChordLibraryView()
        case .favorites: FavoritesView()
        case .progressions: ProgressionsView()
        case .strumPatterns: StrumPatternsView()
        case .songbook: SongbookView()
        case .melodyNotepad: MelodyNotepadView()
        case .dailyChallenge: DailyChallengeView()
        case .practiceRoutine: PracticeRoutineView()
        case .chordTransitions: ChordTransitionsView()
        case .playAlong: PlayAlongView()
        case .intervalTrainer: IntervalTrainerView()
        case .chordEarTraining: ChordEarTrainingView()
        case .noteQuiz: NoteQuizView()
        case .scalePractice: ScalePracticeView()
        case .theoryLessons: TheoryLessonsView()
        case .theoryQuiz: TheoryQuizView()
        case .learningProgress: LearningProgressView()
        case .achievements: AchievementsView()
        case .glossary: GlossaryView()
        case .capoGuide: CapoGuideView()
        case .fretboardNoteMap: FretboardNoteMapView()
        case .chordSubstitutions: ChordSubstitutionsView()
        case .scaleChords: ScaleChordsView()
        case .circleOfFifths: CircleOfFifthsView()
        case nil:
            Text("Select an item from the sidebar")
                .font(.title2)
                .foregroundStyle(.secondary)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View { ContentView() }
}

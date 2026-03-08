import SwiftUI

struct LearnView: View {
    @Binding var showSettings: Bool
    @StateObject private var learnVM = LearnViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section("Practice") {
                    NavigationLink {
                        DailyChallengeView()
                    } label: {
                        Label("Daily Challenge", systemImage: "star.circle")
                    }

                    NavigationLink {
                        PracticeRoutineView()
                    } label: {
                        Label("Practice Routine", systemImage: "list.bullet.clipboard")
                    }

                    NavigationLink {
                        ChordTransitionsView()
                    } label: {
                        Label("Chord Transitions", systemImage: "arrow.left.arrow.right")
                    }

                    NavigationLink {
                        PlayAlongView()
                    } label: {
                        Label("Play Along", systemImage: "play.fill")
                    }
                }

                Section("Training") {
                    NavigationLink {
                        IntervalTrainerView()
                    } label: {
                        Label("Interval Trainer", systemImage: "waveform.path")
                    }

                    NavigationLink {
                        ChordEarTrainingView()
                    } label: {
                        Label("Chord Ear Training", systemImage: "ear")
                    }

                    NavigationLink {
                        NoteQuizView()
                    } label: {
                        Label("Note Quiz", systemImage: "music.note")
                    }

                    NavigationLink {
                        ScalePracticeView()
                    } label: {
                        Label("Scale Practice", systemImage: "music.quarternote.3")
                    }
                }

                Section("Knowledge") {
                    NavigationLink {
                        TheoryLessonsView()
                    } label: {
                        Label("Theory Lessons", systemImage: "book")
                    }

                    NavigationLink {
                        TheoryQuizView()
                    } label: {
                        Label("Theory Quiz", systemImage: "questionmark.circle")
                    }
                }

                Section("Progress") {
                    NavigationLink {
                        LearningProgressView()
                    } label: {
                        Label("Learning Progress", systemImage: "chart.bar")
                    }

                    NavigationLink {
                        AchievementsView()
                    } label: {
                        Label("Achievements", systemImage: "trophy")
                    }
                }
            }
            .navigationTitle("Learn")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape")
                            .accessibilityLabel("Settings")
                    }
                }
            }
        }
        .environmentObject(learnVM)
    }
}

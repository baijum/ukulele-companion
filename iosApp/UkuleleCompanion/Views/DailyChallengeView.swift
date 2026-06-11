import SwiftUI
import shared

struct DailyChallengeView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @State private var challenges: [DailyChallengeGenerator.DailyChallenge] = []
    @State private var completedIndices: Set<Int> = []

    private static let tips = [
        "Even 5 minutes of daily practice is better than 1 hour once a week.",
        "Use a metronome when practising chord changes — start slow and build up.",
        "When learning a new chord, play each string individually to check clarity.",
        "Try humming along while strumming to develop your musical ear.",
        "Practice in front of a mirror to check your hand position.",
        "Focus on smooth transitions rather than speed — speed comes with time.",
        "Learn songs you love! Motivation makes practice enjoyable."
    ]

    private var tipOfTheDay: String {
        let dayOfYear = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 1
        return Self.tips[dayOfYear % Self.tips.count]
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if challenges.isEmpty {
                    Text("Loading challenges...")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(Array(challenges.enumerated()), id: \.offset) { index, challenge in
                        challengeCard(challenge, index: index)
                    }
                }

                tipCard
            }
            .padding()
        }
        .navigationTitle("Daily Challenge")
        .onAppear {
            loadChallenges()
        }
    }

    private var tipCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "lightbulb.fill")
                    .foregroundStyle(.yellow)
                    .font(.title2)
                Text("Tip of the Day")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
            }
            Text(tipOfTheDay)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }

    private func challengeCard(_ challenge: DailyChallengeGenerator.DailyChallenge, index: Int) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: iconForType(challenge.type))
                    .font(.title2)
                    .foregroundStyle(Color.accentColor)
                VStack(alignment: .leading) {
                    Text(challenge.title)
                        .font(.headline)
                    Text(challenge.type.label)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if completedIndices.contains(index) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                        .font(.title2)
                }
            }
            .accessibilityCombined(label: "\(challenge.title), \(challenge.type.label)", value: completedIndices.contains(index) ? "completed" : "not completed")

            Text(challenge.description_)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            if !completedIndices.contains(index) {
                Button("Mark Complete") {
                    completedIndices.insert(index)
                    saveDayProgress(index: index)
                    learnVM.recordActivity()
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }

    private func iconForType(_ type: DailyChallengeGenerator.ChallengeType) -> String {
        switch type {
        case .learnChord: return "music.note"
        case .theoryQuiz: return "questionmark.circle"
        case .practiceSong: return "music.note.list"
        case .scalePractice: return "music.quarternote.3"
        case .chordTransition: return "arrow.left.arrow.right"
        case .earTraining: return "ear"
        default: return "star"
        }
    }

    private func loadChallenges() {
        let list = DailyChallengeGenerator.shared.today()
        challenges = list.asArray(of: DailyChallengeGenerator.DailyChallenge.self)
        loadDayProgress()
    }

    private var dayKey: String {
        let cal = Calendar.current
        let year = cal.component(.year, from: Date())
        let day = cal.ordinality(of: .day, in: .year, for: Date()) ?? 1
        return "daily_\(year)_\(day)"
    }

    private func saveDayProgress(index: Int) {
        UserDefaults.standard.set(true, forKey: "\(dayKey)_\(index)")
    }

    private func loadDayProgress() {
        completedIndices = []
        for i in 0..<3 {
            if UserDefaults.standard.bool(forKey: "\(dayKey)_\(i)") {
                completedIndices.insert(i)
            }
        }
    }
}

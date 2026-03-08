import SwiftUI

private struct HelpEntry: Identifiable {
    let id: String
    let title: String
    let description: String
}

private struct HelpSection: Identifiable {
    let id: String
    let title: String
    let entries: [HelpEntry]
}

private let helpSections: [HelpSection] = [
    HelpSection(id: "play", title: "Play", entries: [
        HelpEntry(id: "explorer", title: "Explorer",
                  description: "The interactive fretboard lets you tap fret positions to build chords. The app detects what chord you've formed and shows its name. Use the Play button to hear how it sounds."),
        HelpEntry(id: "tuner", title: "Tuner",
                  description: "A chromatic tuner that listens to your ukulele through the microphone. It shows the detected note, how many cents sharp or flat you are, and which string is closest. Green means in tune!"),
        HelpEntry(id: "pitch_monitor", title: "Pitch Monitor",
                  description: "A real-time scrolling pitch visualization. Play your ukulele and watch the pitch trace on a piano-roll style canvas. It also detects chords from both simultaneous strums and arpeggiated playing."),
        HelpEntry(id: "chords", title: "Chord Library",
                  description: "Browse chord voicings organized by root note and chord type. Tap a voicing to apply it to the fretboard. Long press to share a chord diagram image."),
        HelpEntry(id: "favorites", title: "Favorites",
                  description: "Save your most-used chord voicings for quick access. Organize them into custom folders and reorder within folders by dragging."),
    ]),
    HelpSection(id: "create", title: "Create", entries: [
        HelpEntry(id: "songs", title: "Songbook",
                  description: "Create and store songs with chord progressions. Type chord names in sequence and the app formats them into a readable chord sheet you can follow while playing."),
        HelpEntry(id: "melody", title: "Melody Notepad",
                  description: "Record melodic ideas by playing single notes on your ukulele. The app transcribes the pitches in real time, building a simple melody notation you can review and play back."),
        HelpEntry(id: "patterns", title: "Strum Patterns",
                  description: "A library of common strumming patterns with visual notation. Each pattern shows the rhythm using up/down arrows. Tap to hear the pattern played."),
        HelpEntry(id: "progressions", title: "Progressions",
                  description: "Explore common chord progressions in any key. Select a scale type to see diatonic progressions, or create your own custom progressions from the available scale degrees."),
    ]),
    HelpSection(id: "learn", title: "Learn", entries: [
        HelpEntry(id: "theory", title: "Theory Lessons",
                  description: "Structured music theory lessons covering notes, intervals, chords, scales, and more. Each lesson includes explanations and interactive examples."),
        HelpEntry(id: "theory_quiz", title: "Theory Quiz",
                  description: "Test your music theory knowledge with multiple-choice questions. Topics cover notes, intervals, chords, scales, and key signatures."),
        HelpEntry(id: "interval_trainer", title: "Interval Trainer",
                  description: "Train your ear to recognize musical intervals. Listen to two notes and identify the interval between them. Supports ascending and descending intervals."),
        HelpEntry(id: "note_quiz", title: "Note Quiz",
                  description: "Identify notes on the ukulele fretboard. A note is highlighted and you guess its name. Great for memorizing the fretboard."),
        HelpEntry(id: "chord_ear", title: "Chord Ear Training",
                  description: "Listen to chords and identify their quality (major, minor, diminished, etc.). Builds your ability to recognize chord types by ear."),
        HelpEntry(id: "scale_practice", title: "Scale Practice",
                  description: "Practice scales with a guided fretboard overlay. Choose any root note and scale type. Includes play-along mode with adjustable tempo."),
        HelpEntry(id: "daily_challenge", title: "Daily Challenge",
                  description: "A new music challenge every day covering different skills: ear training, theory, rhythm, and fretboard knowledge. Track your streak!"),
        HelpEntry(id: "practice_routine", title: "Practice Routine",
                  description: "Follow structured practice routines that combine different skills. Each routine has timed sections for warm-up, technique, and repertoire."),
        HelpEntry(id: "chord_transitions", title: "Chord Transitions",
                  description: "Practice switching between chords with a timer. Select two or more chords and try to switch cleanly at the target tempo."),
        HelpEntry(id: "play_along", title: "Play Along",
                  description: "Follow chord changes at a set tempo. The app shows upcoming chords and highlights the current one, helping you practice smooth transitions."),
        HelpEntry(id: "progress", title: "Learning Progress",
                  description: "Track your learning journey with statistics on practice time, quiz scores, and skills mastered. See your improvement over time."),
        HelpEntry(id: "achievements", title: "Achievements",
                  description: "Earn badges for reaching milestones: practice streaks, quiz scores, chords learned, and more. A fun way to stay motivated."),
    ]),
    HelpSection(id: "reference", title: "Reference", entries: [
        HelpEntry(id: "capo_guide", title: "Capo Guide",
                  description: "See how a capo changes the effective key. Select a capo position and original chord to find what chord shape to use."),
        HelpEntry(id: "circle_of_fifths", title: "Circle of Fifths",
                  description: "Interactive circle of fifths showing key relationships, relative minors, and common chord progressions for each key."),
        HelpEntry(id: "chord_subs", title: "Chord Substitutions",
                  description: "Find substitute chords that work in place of a given chord. Useful for adding variety to arrangements or finding easier alternatives."),
        HelpEntry(id: "scale_chords", title: "Scale Chords",
                  description: "See the diatonic triads built from any scale. Select a root note and scale type to see all 7 chords with their Roman numeral notation, quality, and notes."),
        HelpEntry(id: "fretboard_notes", title: "Fretboard Note Map",
                  description: "A complete map of every note on the ukulele fretboard up to fret 12. Tap a note to highlight all instances of that pitch class."),
        HelpEntry(id: "glossary", title: "Glossary",
                  description: "A searchable glossary of music terms commonly used in ukulele playing and music theory."),
    ]),
    HelpSection(id: "other", title: "Other", entries: [
        HelpEntry(id: "settings", title: "Settings",
                  description: "Customize sound, display, tuning, fretboard, and tuner preferences. Includes backup and restore functionality."),
        HelpEntry(id: "sharing", title: "Sharing",
                  description: "Share chord diagrams, songs, and progressions as images or text. Long press on chord diagrams to access sharing options."),
        HelpEntry(id: "fullscreen", title: "Full Screen Mode",
                  description: "Expand the fretboard to fill the entire screen for easier playing and visibility. Access it from the fretboard view toolbar. Tap anywhere to exit full screen."),
        HelpEntry(id: "backup", title: "Backup & Restore",
                  description: "Export all your data (favorites, songs, progressions, settings) to a JSON file. Restore from a backup to merge data back in."),
    ]),
]

struct HelpView: View {
    @State private var expandedEntry: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Feature Guide")
                    .font(.title2.bold())
                Text("Tap any feature to learn more about it.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                ForEach(helpSections) { section in
                    Text(section.title)
                        .font(.headline)
                        .foregroundColor(.accentColor)
                        .padding(.top, 4)
                        .accessibilityAddTraits(.isHeader)

                    VStack(spacing: 0) {
                        ForEach(Array(section.entries.enumerated()), id: \.element.id) { index, entry in
                            VStack(alignment: .leading, spacing: 4) {
                                Button {
                                    withAnimation {
                                        expandedEntry = expandedEntry == entry.id ? nil : entry.id
                                    }
                                } label: {
                                    HStack {
                                        Text(entry.title)
                                            .font(.body.bold())
                                            .foregroundColor(.primary)
                                        Spacer()
                                        Image(systemName: expandedEntry == entry.id ? "chevron.up" : "chevron.down")
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                            .accessibilityHidden(true)
                                    }
                                }
                                .accessibilityHint(expandedEntry == entry.id ? "Double-tap to collapse" : "Double-tap to expand")

                                if expandedEntry == entry.id {
                                    Text(entry.description)
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                        .padding(.top, 2)
                                } else {
                                    Text(entry.description.prefix(60) + (entry.description.count > 60 ? "..." : ""))
                                        .font(.caption)
                                        .foregroundStyle(.tertiary)
                                        .lineLimit(1)
                                }
                            }
                            .padding(.vertical, 8)
                            .padding(.horizontal, 12)

                            if index < section.entries.count - 1 {
                                Divider()
                                    .padding(.horizontal, 12)
                            }
                        }
                    }
                    .background(Color(.secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding()
        }
        .navigationTitle("Help")
    }
}

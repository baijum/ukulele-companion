import SwiftUI

struct OnboardingView: View {
    @StateObject private var settingsVM = SettingsViewModel()
    let onFinished: () -> Void

    @State private var currentPage = 0
    private let pageCount = 6

    var body: some View {
        VStack(spacing: 0) {
            if currentPage < pageCount - 1 {
                HStack {
                    Spacer()
                    Button("Skip") {
                        completeOnboarding()
                    }
                    .padding()
                }
            }

            TabView(selection: $currentPage) {
                welcomePage.tag(0)
                featuresPage.tag(1)
                accessibilityPage.tag(2)
                privacyPage.tag(3)
                navigationGuidePage.tag(4)
                setupPage.tag(5)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            bottomBar
        }
    }

    // MARK: - Bottom Bar

    private var bottomBar: some View {
        VStack(spacing: 16) {
            pageIndicator

            HStack {
                if currentPage > 0 {
                    Button("Back") {
                        withAnimation { currentPage -= 1 }
                    }
                } else {
                    Spacer().frame(width: 1)
                }

                Spacer()

                if currentPage == pageCount - 1 {
                    Button("Get Started") {
                        completeOnboarding()
                    }
                    .buttonStyle(.borderedProminent)
                } else {
                    Button("Next") {
                        withAnimation { currentPage += 1 }
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
        }
        .padding()
    }

    private var pageIndicator: some View {
        HStack(spacing: 8) {
            ForEach(0..<pageCount, id: \.self) { index in
                Circle()
                    .fill(index == currentPage ? Color.accentColor : Color(.systemGray4))
                    .frame(width: index == currentPage ? 10 : 8,
                           height: index == currentPage ? 10 : 8)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Page \(currentPage + 1) of \(pageCount)")
    }

    // MARK: - Pages

    private var welcomePage: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "music.note")
                .font(.system(size: 64))
                .foregroundStyle(Color.accentColor)
                .accessibilityHidden(true)
            Text("Welcome to Ukulele Companion")
                .font(.title)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .accessibilityAddTraits(.isHeader)
            Text("Your all-in-one ukulele companion for chords, scales, music theory, and composition.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.horizontal, 32)
    }

    private var featuresPage: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text("Everything you need")
                    .font(.title)
                    .fontWeight(.bold)
                    .accessibilityAddTraits(.isHeader)
                    .padding(.top, 16)
                featureRow(icon: "tuningfork", text: "Chromatic tuner with real-time pitch detection")
                featureRow(icon: "music.note.list", text: "Comprehensive chord library with every voicing")
                featureRow(icon: "guitars", text: "Interactive fretboard explorer")
                featureRow(icon: "book", text: "Personal songbook with ChordPro support")
                featureRow(icon: "graduationcap", text: "Lessons, quizzes, and ear training")
            }
            .padding(.horizontal, 32)
        }
    }

    private var accessibilityPage: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Image(systemName: "accessibility")
                    .font(.system(size: 48))
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
                    .padding(.top, 16)
                Text("Built for everyone")
                    .font(.title)
                    .fontWeight(.bold)
                    .accessibilityAddTraits(.isHeader)
                featureRow(icon: "checkmark.circle", text: "Full VoiceOver and screen reader support")
                featureRow(icon: "speaker.wave.2", text: "Spoken tuner feedback for hands-free tuning")
                featureRow(icon: "circle.lefthalf.filled", text: "High-contrast support for better visibility")
            }
            .padding(.horizontal, 32)
        }
    }

    private var privacyPage: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Image(systemName: "shield.checkered")
                    .font(.system(size: 48))
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
                    .padding(.top, 16)
                Text("Private and offline")
                    .font(.title)
                    .fontWeight(.bold)
                    .accessibilityAddTraits(.isHeader)
                featureRow(icon: "wifi.slash", text: "Works completely offline \u{2014} no internet needed")
                featureRow(icon: "lock.shield", text: "No tracking, no account required")
                featureRow(icon: "iphone", text: "Your data stays on your device")
            }
            .padding(.horizontal, 32)
        }
    }

    private var navigationGuidePage: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Image(systemName: "square.grid.2x2")
                    .font(.system(size: 48))
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
                    .padding(.top, 16)
                Text("Finding your way around")
                    .font(.title)
                    .fontWeight(.bold)
                    .accessibilityAddTraits(.isHeader)
                Text("Use the tab bar at the bottom to navigate between sections.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                featureRow(icon: "tuningfork", text: "Tuner \u{2014} Chromatic tuner and pitch monitor")
                featureRow(icon: "play.circle", text: "Play \u{2014} Explorer, Chords, Favorites, Metronome")
                featureRow(icon: "pencil.circle", text: "Create \u{2014} Songbook, Melody Notepad, Patterns")
                featureRow(icon: "book", text: "Learn \u{2014} Lessons, Quizzes, Ear Training")
                featureRow(icon: "list.bullet", text: "Reference \u{2014} Capo Guide, Circle of Fifths, Glossary")
            }
            .padding(.horizontal, 32)
        }
    }

    private var setupPage: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Quick setup")
                    .font(.title)
                    .fontWeight(.bold)
                    .accessibilityAddTraits(.isHeader)
                    .padding(.top, 16)
                Text("You can change these anytime in Settings.")
                    .font(.body)
                    .foregroundStyle(.secondary)

                // Tuning
                Text("Tuning")
                    .font(.headline)
                    .padding(.top, 8)
                let tuningOptions = [
                    "High-G (Standard)", "Low-G", "Baritone (DGBE)", "D-Tuning (ADF#B)",
                    "Slack Key (GCEG)", "Open A (AC#EA)", "Low A (GCEa)", "Half-Step Down"
                ]
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 8)], spacing: 8) {
                    ForEach(tuningOptions, id: \.self) { tuning in
                        Button {
                            settingsVM.selectedTuning = tuning
                            settingsVM.save()
                        } label: {
                            Text(tuning)
                                .font(.caption)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                                .background(settingsVM.selectedTuning == tuning
                                            ? Color.accentColor.opacity(0.2) : Color(.systemGray6))
                                .foregroundStyle(settingsVM.selectedTuning == tuning
                                                 ? Color.accentColor : .primary)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(settingsVM.selectedTuning == tuning ? .isSelected : [])
                    }
                }

                // Left-handed
                Toggle("Left-handed", isOn: $settingsVM.leftHanded)
                    .onChange(of: settingsVM.leftHanded) { _ in settingsVM.save() }
                    .padding(.top, 8)

                // Theme
                Text("Theme")
                    .font(.headline)
                    .padding(.top, 8)
                Picker("Theme", selection: $settingsVM.themeMode) {
                    Text("Light").tag("Light")
                    Text("Dark").tag("Dark")
                    Text("System").tag("System")
                    Text("High Contrast").tag("High Contrast")
                }
                .onChange(of: settingsVM.themeMode) { _ in settingsVM.save() }
            }
            .padding(.horizontal, 32)
        }
    }

    // MARK: - Helpers

    private func featureRow(icon: String, text: String) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Color.accentColor)
                .frame(width: 28)
                .accessibilityHidden(true)
            Text(text)
                .font(.body)
        }
    }

    private func completeOnboarding() {
        UserDefaults(suiteName: "app_settings")?.set(true, forKey: "onboarding_completed")
        onFinished()
    }
}

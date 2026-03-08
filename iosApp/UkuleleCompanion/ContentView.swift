import SwiftUI

struct ContentView: View {
    @State private var selectedTab = 0
    @State private var showSettings = false
    @State private var showOnboarding: Bool
    @StateObject private var settingsVM = SettingsViewModel()

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
            mainContent
                .preferredColorScheme(colorScheme)
        }
    }

    private var mainContent: some View {
        TabView(selection: $selectedTab) {
            PlayView(showSettings: $showSettings)
                .tabItem {
                    Label("Play", systemImage: "play.circle")
                }
                .tag(0)

            CreateView(showSettings: $showSettings)
                .tabItem {
                    Label("Create", systemImage: "pencil.circle")
                }
                .tag(1)

            if settingsVM.showLearnTab {
                LearnView(showSettings: $showSettings)
                    .tabItem {
                        Label("Learn", systemImage: "book")
                    }
                    .tag(2)
            }

            if settingsVM.showReferenceTab {
                ReferenceView(showSettings: $showSettings)
                    .tabItem {
                        Label("Reference", systemImage: "list.bullet")
                    }
                    .tag(3)
            }
        }
        .environmentObject(settingsVM)
        .tint(isHighContrast ? .yellow : nil)
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
        .onChange(of: showSettings) { isShowing in
            if !isShowing { settingsVM.load() }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View { ContentView() }
}

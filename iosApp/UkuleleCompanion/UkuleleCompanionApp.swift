import SwiftUI
import AVFoundation

@main
struct UkuleleCompanionApp: App {
    @StateObject private var practiceTimerVM = PracticeTimerViewModel()
    @Environment(\.scenePhase) private var scenePhase
    @State private var sessionStartDate: Date?

    init() {
        configureAudioSession()
        ReviewPromptManager.shared.initFirstLaunch()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(practiceTimerVM)
        }
        .onChange(of: scenePhase) { newPhase in
            switch newPhase {
            case .active:
                sessionStartDate = Date()
            case .background:
                if let start = sessionStartDate {
                    let seconds = Int(Date().timeIntervalSince(start))
                    if seconds >= 60 {
                        practiceTimerVM.recordSession(durationSeconds: seconds)
                    }
                }
                sessionStartDate = nil
            default:
                break
            }
        }
    }

    private func configureAudioSession() {
        #if targetEnvironment(simulator)
        // Audio session activation can deadlock on simulator — defer until actually needed
        return
        #else
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .default, options: [.defaultToSpeaker])
            try session.setPreferredSampleRate(44100)
            try session.setActive(true)
        } catch {
            print("Failed to configure audio session: \(error)")
        }
        #endif
    }
}

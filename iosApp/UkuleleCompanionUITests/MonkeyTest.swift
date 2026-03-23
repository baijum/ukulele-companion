import XCTest

/// SplitMix64 generator seeded from MONKEY_SEED env var (or random).
/// Allows deterministic replay of monkey test sequences that caused crashes.
struct SeededRNG: RandomNumberGenerator {
    private var state: UInt64

    init(seed: UInt64) {
        state = seed
    }

    mutating func next() -> UInt64 {
        // Murmur-style LCG (same constants as SplitMix64)
        state &+= 0x9e3779b97f4a7c15
        var z = state
        z = (z ^ (z >> 30)) &* 0xbf58476d1ce4e5b9
        z = (z ^ (z >> 27)) &* 0x94d049bb133111eb
        return z ^ (z >> 31)
    }
}

/// Monkey test: randomly exercises the app for a configurable duration to surface crashes.
///
/// Crash vectors targeted:
/// - Data race in AudioCaptureEngine (rapid tuner start/stop)
/// - Kotlin as! bridge casts (navigate every screen, esp. VoiceLeading, CapoGuide, CircleOfFifths)
/// - .first! on empty collections (fresh-install state)
/// - nonisolated(unsafe) TonePlayer captures (rapid audio toggle)
final class MonkeyTest: XCTestCase {

    var app: XCUIApplication!
    var rng: SeededRNG!

    let testDurationSeconds: TimeInterval = 300

    override func setUpWithError() throws {
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["RUN_STRESS_TESTS"] == "1",
            "MonkeyTests skipped in CI; set RUN_STRESS_TESTS=1 to run"
        )

        continueAfterFailure = true

        let seed: UInt64
        if let envSeed = ProcessInfo.processInfo.environment["MONKEY_SEED"],
           let parsed = UInt64(envSeed) {
            seed = parsed
        } else {
            seed = UInt64.random(in: 0...UInt64.max)
        }
        rng = SeededRNG(seed: seed)
        print("MonkeyTest seed: \(seed)")

        app = XCUIApplication()
        app.launchArguments += ["-monkey_test_mode", "1"]
        launchWithRetry(app, maxAttempts: 3)
    }

    override func tearDownWithError() throws {
        XCUIDevice.shared.orientation = .portrait
        app?.terminate()
    }

    // MARK: - Main Monkey Test

    func testMonkeyRandom() throws {
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["RUN_STRESS_TESTS"] == "1",
            "Skipped in CI; set RUN_STRESS_TESTS=1 for local runs"
        )

        let deadline = Date().addingTimeInterval(testDurationSeconds)
        var eventCount = 0

        while Date() < deadline {
            let action = Int.random(in: 0..<100, using: &rng)

            switch action {
            case 0..<35:
                tapRandom()
            case 35..<48:
                swipeRandom()
            case 48..<57:
                switchRandomTab()
            case 57..<63:
                swipeBack()
            case 63..<68:
                openAndDismissSettings()
            case 68..<73:
                backgroundAndForeground()
            case 73..<78:
                navigateToRiskyScreen()
            case 78..<83:
                rapidTabStress()
            case 83..<88:
                rotateDevice()
            case 88..<92:
                longPressRandom()
            case 92..<95:
                pinchRandom()
            default:
                tapRandom()
            }

            eventCount += 1
            usleep(UInt32.random(in: 50_000...300_000, using: &rng))

            if eventCount % 50 == 0 {
                XCTAssertTrue(app.exists, "App should still be running at event \(eventCount)")
                if !app.exists {
                    app.launch()
                    sleep(2)
                }
                print("MonkeyTest: event=\(eventCount) running")
            }
        }

        print("MonkeyTest: completed \(eventCount) events without crash")
    }

    // MARK: - Targeted Stress Tests

    func testTunerRapidStartStop() throws {
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["RUN_STRESS_TESTS"] == "1",
            "Skipped in CI; set RUN_STRESS_TESTS=1 for local runs"
        )

        navigateToTab(index: 0)
        sleep(1)

        let tunerButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'tuner'")).firstMatch
        if tunerButton.exists {
            tunerButton.tap()
        } else {
            let tunerCell = app.cells.matching(NSPredicate(format: "label CONTAINS[c] 'tuner'")).firstMatch
            if tunerCell.exists { tunerCell.tap() }
        }
        sleep(1)

        for i in 0..<30 {
            let startBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'start' OR label CONTAINS[c] 'stop' OR label CONTAINS[c] 'microphone'")).firstMatch
            if startBtn.exists {
                startBtn.tap()
                usleep(200_000)
            } else {
                tapAt(x: 0.5, y: 0.5)
                usleep(200_000)
            }
            XCTAssertTrue(app.exists, "App crashed during tuner toggle #\(i)")
        }
    }

    func testAllScreensNavigation() throws {
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["RUN_STRESS_TESTS"] == "1",
            "Skipped in CI; set RUN_STRESS_TESTS=1 for local runs"
        )

        let riskyScreenKeywords = [
            "voice lead",
            "capo guide",
            "capo calculator",
            "circle of fifths",
            "chord substitut",
            "scale chord",
            "fretboard note",
            "play along",
            "chord ear",
            "melody",
            "achievements",
            "daily challenge",
        ]

        for keyword in riskyScreenKeywords {
            for tabIdx in 0..<4 {
                navigateToTab(index: tabIdx)
                sleep(1)

                let match = app.cells.matching(NSPredicate(format: "label CONTAINS[c] %@", keyword)).firstMatch
                if match.exists {
                    match.tap()
                    sleep(2)
                    XCTAssertTrue(app.exists, "App crashed navigating to '\(keyword)'")
                    swipeBack()
                    sleep(1)
                    break
                }

                let btn = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] %@", keyword)).firstMatch
                if btn.exists {
                    btn.tap()
                    sleep(2)
                    XCTAssertTrue(app.exists, "App crashed navigating to '\(keyword)' via button")
                    swipeBack()
                    sleep(1)
                    break
                }
            }
        }
    }

    func testRapidRotation() throws {
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["RUN_STRESS_TESTS"] == "1",
            "Skipped in CI; set RUN_STRESS_TESTS=1 for local runs"
        )

        navigateToTab(index: 0)
        sleep(1)

        let tunerCell = app.cells.matching(NSPredicate(format: "label CONTAINS[c] 'tuner'")).firstMatch
        if tunerCell.exists {
            tunerCell.tap()
            sleep(1)
            tapAt(x: 0.5, y: 0.5)
            sleep(1)
        }

        let orientations: [UIDeviceOrientation] = [.portrait, .landscapeLeft, .landscapeRight, .portraitUpsideDown]
        for i in 0..<20 {
            XCUIDevice.shared.orientation = orientations[i % orientations.count]
            usleep(300_000)
            XCTAssertTrue(app.exists, "App crashed during rotation #\(i)")
        }
        XCUIDevice.shared.orientation = .portrait
    }

    func testBackgroundForegroundWithAudio() throws {
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["RUN_STRESS_TESTS"] == "1",
            "Skipped in CI; set RUN_STRESS_TESTS=1 for local runs"
        )

        navigateToTab(index: 0)
        sleep(1)

        let tunerCell = app.cells.matching(NSPredicate(format: "label CONTAINS[c] 'tuner'")).firstMatch
        if tunerCell.exists {
            tunerCell.tap()
            sleep(1)
            tapAt(x: 0.5, y: 0.5)
            sleep(1)
        }

        for i in 0..<10 {
            XCUIDevice.shared.press(.home)
            sleep(1)
            app.activate()
            sleep(1)
            XCTAssertTrue(app.exists, "App crashed on background/foreground cycle #\(i)")
        }
    }

    // MARK: - Helpers

    private func tapRandom() {
        let x = Double.random(in: 0.1...0.9, using: &rng)
        let y = Double.random(in: 0.15...0.85, using: &rng)
        tapAt(x: x, y: y)
    }

    private func tapAt(x: Double, y: Double) {
        app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: y)).tap()
    }

    private func swipeRandom() {
        let choice = Int.random(in: 0..<4, using: &rng)
        let velocity = XCUIGestureVelocity(rawValue: Double.random(in: 200...800, using: &rng))
        switch choice {
        case 0: app.swipeUp(velocity: velocity)
        case 1: app.swipeDown(velocity: velocity)
        case 2: app.swipeLeft(velocity: velocity)
        default: app.swipeRight(velocity: velocity)
        }
    }

    private func switchRandomTab() {
        let tabIdx = Int.random(in: 0..<4, using: &rng)
        navigateToTab(index: tabIdx)
    }

    private func navigateToTab(index: Int) {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.exists else { return }
        let buttons = tabBar.buttons
        guard index < buttons.count else { return }
        buttons.element(boundBy: index).tap()
    }

    private func swipeBack() {
        let startPoint = app.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
        let endPoint = app.coordinate(withNormalizedOffset: CGVector(dx: 0.7, dy: 0.5))
        startPoint.press(forDuration: 0, thenDragTo: endPoint)
    }

    private func openAndDismissSettings() {
        let settingsBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'settings' OR label CONTAINS[c] 'gear'")).firstMatch
        if settingsBtn.exists {
            settingsBtn.tap()
            sleep(1)
            app.swipeDown()
            usleep(500_000)
        }
    }

    private func backgroundAndForeground() {
        XCUIDevice.shared.press(.home)
        usleep(UInt32.random(in: 500_000...2_000_000, using: &rng))
        app.activate()
        usleep(500_000)
    }

    private func navigateToRiskyScreen() {
        let riskyKeywords = [
            "voice lead", "capo", "circle", "substitut",
            "scale chord", "fretboard note", "play along", "melody",
            "tuner", "pitch monitor", "metronome",
        ]
        guard let keyword = riskyKeywords.randomElement(using: &rng) else { return }

        for tabIdx in 0..<4 {
            navigateToTab(index: tabIdx)
            usleep(500_000)

            let cell = app.cells.matching(NSPredicate(format: "label CONTAINS[c] %@", keyword)).firstMatch
            if cell.exists {
                cell.tap()
                sleep(1)
                swipeBack()
                return
            }
        }
    }

    private func rapidTabStress() {
        for _ in 0..<Int.random(in: 5...15, using: &rng) {
            switchRandomTab()
            usleep(UInt32.random(in: 50_000...200_000, using: &rng))
        }
    }

    private func rotateDevice() {
        let orientations: [UIDeviceOrientation] = [.portrait, .landscapeLeft, .landscapeRight, .portraitUpsideDown]
        if let orientation = orientations.randomElement(using: &rng) {
            XCUIDevice.shared.orientation = orientation
        }
        usleep(300_000)
    }

    private func longPressRandom() {
        let x = Double.random(in: 0.1...0.9, using: &rng)
        let y = Double.random(in: 0.15...0.85, using: &rng)
        let duration = Double.random(in: 1.0...3.0, using: &rng)
        app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: y))
            .press(forDuration: duration)
    }

    private func pinchRandom() {
        let scale = CGFloat.random(in: 0.5...2.0, using: &rng)
        let velocity = CGFloat.random(in: 0.5...2.0, using: &rng)
        app.pinch(withScale: scale, velocity: velocity)
    }

    /// Retries app.launch() to handle transient "Failed to get background assertion" errors
    /// that occur on CI when the simulator isn't fully ready.
    private func launchWithRetry(_ application: XCUIApplication, maxAttempts: Int) {
        for attempt in 1...maxAttempts {
            application.launch()
            if application.wait(for: .runningForeground, timeout: 15) {
                sleep(3)
                return
            }
            print("MonkeyTest: app.launch() attempt \(attempt)/\(maxAttempts) did not reach foreground, retrying...")
            application.terminate()
            sleep(5)
        }
        XCTFail("App failed to launch after \(maxAttempts) attempts")
    }
}

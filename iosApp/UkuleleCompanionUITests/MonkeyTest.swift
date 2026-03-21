import XCTest

/// Monkey test: randomly exercises the app for a configurable duration to surface crashes.
///
/// Crash vectors targeted:
/// - Data race in AudioCaptureEngine (rapid tuner start/stop)
/// - Kotlin as! bridge casts (navigate every screen, esp. VoiceLeading, CapoGuide, CircleOfFifths)
/// - .first! on empty collections (fresh-install state)
/// - nonisolated(unsafe) TonePlayer captures (rapid audio toggle)
@MainActor
final class MonkeyTest: XCTestCase {

    var app: XCUIApplication!

    let testDurationSeconds: TimeInterval = 300

    override func setUpWithError() throws {
        continueAfterFailure = true
        app = XCUIApplication()
        app.launchArguments += ["-monkey_test_mode", "1"]
        app.launch()
        sleep(3)
    }

    override func tearDownWithError() throws {
        app.terminate()
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
            let action = Int.random(in: 0..<100)

            switch action {
            case 0..<40:
                tapRandom()
            case 40..<55:
                swipeRandom()
            case 55..<65:
                switchRandomTab()
            case 65..<72:
                swipeBack()
            case 72..<78:
                openAndDismissSettings()
            case 78..<83:
                backgroundAndForeground()
            case 83..<88:
                navigateToRiskyScreen()
            case 88..<93:
                rapidTabStress()
            default:
                tapRandom()
            }

            eventCount += 1
            usleep(UInt32.random(in: 50_000...300_000))

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
        let x = Double.random(in: 0.1...0.9)
        let y = Double.random(in: 0.15...0.85)
        tapAt(x: x, y: y)
    }

    private func tapAt(x: Double, y: Double) {
        app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: y)).tap()
    }

    private func swipeRandom() {
        let choice = Int.random(in: 0..<4)
        let velocity = XCUIGestureVelocity(rawValue: Double.random(in: 200...800))
        switch choice {
        case 0: app.swipeUp(velocity: velocity)
        case 1: app.swipeDown(velocity: velocity)
        case 2: app.swipeLeft(velocity: velocity)
        default: app.swipeRight(velocity: velocity)
        }
    }

    private func switchRandomTab() {
        let tabIdx = Int.random(in: 0..<4)
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
        usleep(UInt32.random(in: 500_000...2_000_000))
        app.activate()
        usleep(500_000)
    }

    private func navigateToRiskyScreen() {
        let riskyKeywords = [
            "voice lead", "capo", "circle", "substitut",
            "scale chord", "fretboard note", "play along", "melody",
            "tuner", "pitch monitor", "metronome",
        ]
        guard let keyword = riskyKeywords.randomElement() else { return }

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
        for _ in 0..<Int.random(in: 5...15) {
            switchRandomTab()
            usleep(UInt32.random(in: 50_000...200_000))
        }
    }
}

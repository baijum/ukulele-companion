import XCTest

/// Monkey test: randomly exercises the app for a configurable duration to surface crashes.
///
/// Crash vectors targeted:
/// - Data race in AudioCaptureEngine (rapid tuner start/stop)
/// - Kotlin as! bridge casts (navigate every screen, esp. VoiceLeading, CapoGuide, CircleOfFifths)
/// - .first! on empty collections (fresh-install state)
/// - nonisolated(unsafe) TonePlayer captures (rapid audio toggle)
final class MonkeyTest: XCTestCase {

    var app: XCUIApplication!

    // Increase this for longer runs. CI uses 120s; local stress runs use 600s.
    let testDurationSeconds: TimeInterval = 300

    override func setUpWithError() throws {
        continueAfterFailure = true
        app = XCUIApplication()
        app.launchArguments += ["-monkey_test_mode", "1"]
        app.launch()
        // Let the app fully initialize
        sleep(3)
    }

    override func tearDownWithError() throws {
        app.terminate()
    }

    // MARK: - Main Monkey Test

    func testMonkeyRandom() throws {
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

            // Short pause between events
            usleep(UInt32.random(in: 50_000...300_000)) // 50-300ms

            // Every 50 events, verify app is still alive
            if eventCount % 50 == 0 {
                XCTAssertTrue(app.exists, "App should still be running at event \(eventCount)")
                if !app.exists {
                    app.launch()
                    sleep(2)
                }
                let elapsed = testDurationSeconds - Date().timeIntervalSince(Date().addingTimeInterval(-testDurationSeconds + Date().timeIntervalSince(deadline) + testDurationSeconds))
                print("MonkeyTest: event=\(eventCount) running")
            }
        }

        print("MonkeyTest: completed \(eventCount) events without crash")
    }

    // MARK: - Targeted Stress Tests

    /// Stress test the tuner: rapidly toggle capture 30 times.
    func testTunerRapidStartStop() throws {
        navigateToTab(index: 0) // Play tab
        sleep(1)

        // Try to find and tap the Tuner menu item
        let tunerButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'tuner'")).firstMatch
        if tunerButton.exists {
            tunerButton.tap()
        } else {
            // Try sidebar/list navigation
            let tunerCell = app.cells.matching(NSPredicate(format: "label CONTAINS[c] 'tuner'")).firstMatch
            if tunerCell.exists { tunerCell.tap() }
        }
        sleep(1)

        // Rapidly tap the start/stop button 30 times
        for i in 0..<30 {
            let startBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'start' OR label CONTAINS[c] 'stop' OR label CONTAINS[c] 'microphone'")).firstMatch
            if startBtn.exists {
                startBtn.tap()
                usleep(200_000)
            } else {
                // Tap anywhere in the center to trigger UI
                tapAt(x: 0.5, y: 0.5)
                usleep(200_000)
            }
            XCTAssertTrue(app.exists, "App crashed during tuner toggle #\(i)")
        }
    }

    /// Navigate to each known risky screen in sequence.
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
            // Search all tabs for the keyword
            for tabIdx in 0..<4 {
                navigateToTab(index: tabIdx)
                sleep(1)

                let match = app.cells.matching(NSPredicate(format: "label CONTAINS[c] %@", keyword)).firstMatch
                if match.exists {
                    match.tap()
                    sleep(2) // Wait for view to load and any as! casts to execute
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

    /// Test background/foreground cycling while audio is active.
    func testBackgroundForegroundWithAudio() throws {
        navigateToTab(index: 0)
        sleep(1)

        // Start tuner if possible
        let tunerCell = app.cells.matching(NSPredicate(format: "label CONTAINS[c] 'tuner'")).firstMatch
        if tunerCell.exists {
            tunerCell.tap()
            sleep(1)
            tapAt(x: 0.5, y: 0.5) // Try to start capture
            sleep(1)
        }

        // Cycle background/foreground 10 times
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
        let y = Double.random(in: 0.15...0.85) // Avoid status bar and home indicator
        tapAt(x: x, y: y)
    }

    private func tapAt(x: Double, y: Double) {
        let screenSize = app.windows.firstMatch.frame
        let point = CGPoint(x: screenSize.width * x, y: screenSize.height * y)
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
        // Edge swipe to go back
        let startPoint = app.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
        let endPoint = app.coordinate(withNormalizedOffset: CGVector(dx: 0.7, dy: 0.5))
        startPoint.press(forDuration: 0, thenDragTo: endPoint)
    }

    private func openAndDismissSettings() {
        // Settings gear button
        let settingsBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'settings' OR label CONTAINS[c] 'gear'")).firstMatch
        if settingsBtn.exists {
            settingsBtn.tap()
            sleep(1)
            // Dismiss via swipe down
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
        let keyword = riskyKeywords.randomElement()!

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


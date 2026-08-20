import XCTest
import shared
@testable import UkuleleCompanion

/// Storage-side tests for `ReviewPromptManager`.
///
/// The gate arithmetic is covered by `ReviewPromptEligibilityTest` in `:shared`.
/// What is left here is the part only iOS can get wrong: which defaults key each
/// piece of state is written to, whether the snapshot wires every field to the
/// right gate, and the epoch-seconds to milliseconds conversion at the boundary.
///
/// The key names are deliberately repeated in this file rather than read from
/// the manager. They are an on-disk contract with every existing install —
/// renaming one silently orphans that install's data, and `dismiss_count` in
/// particular is a legacy name kept on purpose.
final class ReviewPromptManagerTests: XCTestCase {

    private let suiteName = "review_prompt_tests"
    private var defaults: UserDefaults!

    private let day: TimeInterval = 86_400

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removePersistentDomain(forName: suiteName)
        defaults = UserDefaults(suiteName: suiteName)
    }

    override func tearDown() {
        UserDefaults.standard.removePersistentDomain(forName: suiteName)
        defaults = nil
        super.tearDown()
    }

    @MainActor
    private func makeManager() -> ReviewPromptManager {
        ReviewPromptManager(defaults: defaults)
    }

    /// Seeds a stored state that passes every gate, so a test can break one thing.
    private func seedEligible() {
        let now = Date().timeIntervalSince1970
        let minDays = Int(ReviewPromptEligibility.shared.MIN_ACTIVE_DAYS)
        defaults.set(now - 30 * day, forKey: "first_launch")
        defaults.set((1...minDays).map { "2020-01-0\($0)" }, forKey: "active_days")
        defaults.set(false, forKey: "has_reviewed")
        defaults.set(0, forKey: "dismiss_count")
        defaults.set(0.0, forKey: "last_prompted")
    }

    // MARK: - First launch

    @MainActor
    func testInitFirstLaunchStampsTheClockOnce() {
        makeManager().initFirstLaunch()
        XCTAssertGreaterThan(defaults.double(forKey: "first_launch"), 0)
    }

    @MainActor
    func testInitFirstLaunchDoesNotOverwriteAnExistingStamp() {
        let original = Date().timeIntervalSince1970 - 100 * day
        defaults.set(original, forKey: "first_launch")

        makeManager().initFirstLaunch()

        // Overwriting would restart the time-since-install gate on every launch,
        // so the user could never become eligible.
        XCTAssertEqual(defaults.double(forKey: "first_launch"), original, accuracy: 0.001)
    }

    // MARK: - Active days

    @MainActor
    func testRecordActiveDayIsIdempotentWithinOneCalendarDay() {
        let manager = makeManager()
        manager.recordActiveDay()
        manager.recordActiveDay()
        manager.recordActiveDay()

        XCTAssertEqual(manager.activeDaysCount(), 1)
    }

    @MainActor
    func testRecordActiveDayStopsWritingOnceTheGateIsSatisfied() {
        let minDays = Int(ReviewPromptEligibility.shared.MIN_ACTIVE_DAYS)
        let banked = (1...minDays).map { "2020-01-0\($0)" }
        defaults.set(banked, forKey: "active_days")

        makeManager().recordActiveDay()

        // Today is not added: the set is capped so it cannot grow without bound.
        XCTAssertEqual(Set(defaults.stringArray(forKey: "active_days") ?? []), Set(banked))
    }

    // MARK: - Attempt bookkeeping

    @MainActor
    func testRecordPromptShownIncrementsTheCountAndStampsTheClock() {
        let manager = makeManager()
        manager.recordPromptShown()
        XCTAssertEqual(manager.promptCount(), 1)

        manager.recordPromptShown()
        XCTAssertEqual(manager.promptCount(), 2)
        XCTAssertGreaterThan(defaults.double(forKey: "last_prompted"), 0)
    }

    @MainActor
    func testPromptCountReadsTheLegacyDismissCountKey() {
        // Renaming this key would hand every existing install a fresh budget of
        // three prompts, which is exactly what the cap exists to prevent.
        defaults.set(2, forKey: "dismiss_count")
        XCTAssertEqual(makeManager().promptCount(), 2)
    }

    @MainActor
    func testHasReviewedReadsTheLatchedFlag() {
        XCTAssertFalse(makeManager().hasReviewed())
        defaults.set(true, forKey: "has_reviewed")
        XCTAssertTrue(makeManager().hasReviewed())
    }

    // MARK: - Snapshot wiring
    //
    // Each of these breaks exactly one stored value and asserts the decision
    // flips, which is what proves that value reaches the gate it belongs to.

    @MainActor
    func testEligibleWhenEveryStoredValuePasses() {
        seedEligible()
        XCTAssertTrue(makeManager().isEligible())
    }

    @MainActor
    func testNotEligibleWhenHasReviewedIsStored() {
        seedEligible()
        defaults.set(true, forKey: "has_reviewed")
        XCTAssertFalse(makeManager().isEligible())
    }

    @MainActor
    func testNotEligibleWhenStoredPromptCountHitsTheCap() {
        seedEligible()
        defaults.set(Int(ReviewPromptEligibility.shared.MAX_PROMPTS), forKey: "dismiss_count")
        XCTAssertFalse(makeManager().isEligible())
    }

    @MainActor
    func testNotEligibleWhenTooFewActiveDaysAreStored() {
        seedEligible()
        defaults.set(["2020-01-01"], forKey: "active_days")
        XCTAssertFalse(makeManager().isEligible())
    }

    @MainActor
    func testNotEligibleWhenFirstLaunchWasNeverStored() {
        seedEligible()
        defaults.removeObject(forKey: "first_launch")
        XCTAssertFalse(makeManager().isEligible())
    }

    @MainActor
    func testNotEligibleWhenInstalledTooRecently() {
        seedEligible()
        defaults.set(Date().timeIntervalSince1970 - day, forKey: "first_launch")
        XCTAssertFalse(makeManager().isEligible())
    }

    // MARK: - Epoch seconds to milliseconds

    @MainActor
    func testStoredSecondsAreConvertedToMillisecondsForTheCooldown() {
        // These timestamps have been persisted as epoch *seconds* since the
        // first release. Handing a seconds value to the shared rules as if it
        // were milliseconds would date it to 1970, so every cooldown would look
        // long expired and the user would be prompted again immediately.
        seedEligible()
        defaults.set(1, forKey: "dismiss_count")
        defaults.set(Date().timeIntervalSince1970 - day, forKey: "last_prompted")

        XCTAssertFalse(makeManager().isEligible())
    }

    @MainActor
    func testEligibleAgainOnceTheStoredCooldownHasElapsed() {
        seedEligible()
        let elapsed = (TimeInterval(ReviewPromptEligibility.shared.COOLDOWN_DAYS) + 1) * day
        defaults.set(1, forKey: "dismiss_count")
        defaults.set(Date().timeIntervalSince1970 - elapsed, forKey: "last_prompted")

        XCTAssertTrue(makeManager().isEligible())
    }
}

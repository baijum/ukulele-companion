import Foundation
import shared

/// Stores in-app review prompt state.
///
/// Tracks distinct calendar days the app was opened ("active days") along with
/// attempt history, to decide when to request the system review sheet after an
/// achievement unlock.
///
/// The rules themselves live in the shared `ReviewPromptEligibility` so Android
/// and iOS cannot drift apart; this type only persists and reads back the state.
///
/// Requesting the sheet is the view's job, via SwiftUI's `\.requestReview`
/// environment action, which targets the scene the view actually lives in.
@MainActor
final class ReviewPromptManager {

    static let shared = ReviewPromptManager()

    private let defaults: UserDefaults

    private init() {
        self.defaults = UserDefaults(suiteName: "review_prompt") ?? .standard
    }

    /// Injects the store so tests can run against a scratch suite. Production
    /// always goes through `shared` and the `review_prompt` suite above.
    init(defaults: UserDefaults) {
        self.defaults = defaults
    }

    // MARK: - First launch

    func initFirstLaunch() {
        if defaults.double(forKey: Keys.firstLaunch) == 0 {
            defaults.set(Date().timeIntervalSince1970, forKey: Keys.firstLaunch)
        }
    }

    // MARK: - Active day tracking

    /// Records today as an active usage day.
    ///
    /// Called once per launch from the root view, unconditionally: "active"
    /// means the app was opened, not that any particular tab was used.
    /// Idempotent per calendar day, and stops writing once the gate is
    /// satisfied so the stored set cannot grow without bound.
    func recordActiveDay() {
        var days = activeDays()
        guard ReviewPromptEligibility.shared.shouldRecordActiveDay(activeDayCount: Int32(clamping: days.count)) else { return }
        let today = Self.todayKey()
        if !days.contains(today) {
            days.insert(today)
            defaults.set(Array(days), forKey: Keys.activeDays)
        }
    }

    func activeDaysCount() -> Int {
        activeDays().count
    }

    // MARK: - Review state

    /// Read-only: StoreKit never reports that a review was submitted, so nothing
    /// sets this any more. It is still honoured so installs that latched it under
    /// the old prompt are never asked again.
    func hasReviewed() -> Bool {
        defaults.bool(forKey: Keys.hasReviewed)
    }

    func promptCount() -> Int {
        defaults.integer(forKey: Keys.promptCount)
    }

    /// Records an attempt to request the system review sheet.
    ///
    /// StoreKit reports neither whether the sheet appeared nor what the user
    /// did, so an attempt is all that can be recorded. Capping attempts and
    /// applying the cooldown is what keeps this from nagging.
    func recordPromptShown() {
        defaults.set(promptCount() + 1, forKey: Keys.promptCount)
        defaults.set(Date().timeIntervalSince1970, forKey: Keys.lastPrompted)
    }

    // MARK: - Eligibility

    /// Whether every gate in the shared `ReviewPromptEligibility` passes for the
    /// stored state.
    func isEligible() -> Bool {
        ReviewPromptEligibility.shared.isEligible(
            state: snapshot(),
            nowMillis: Self.millis(from: Date().timeIntervalSince1970)
        )
    }

    // MARK: - Private

    /// Counts are clamped rather than converted: `UserDefaults` hands back a
    /// 64-bit `Int`, and a value outside `Int32` — from a corrupt store or a
    /// restored backup — would trap on the way into the shared rules. Clamping
    /// a huge count to `Int32.max` also lands on the safe side of the caps.
    private func snapshot() -> ReviewPromptEligibility.State {
        ReviewPromptEligibility.State(
            activeDayCount: Int32(clamping: activeDaysCount()),
            firstLaunchMillis: Self.millis(from: defaults.double(forKey: Keys.firstLaunch)),
            hasReviewed: hasReviewed(),
            promptCount: Int32(clamping: promptCount()),
            lastPromptedMillis: Self.millis(from: defaults.double(forKey: Keys.lastPrompted))
        )
    }

    /// These timestamps are persisted as epoch *seconds* and have been since the
    /// first release, so they are converted here rather than migrated — the
    /// shared rules work in milliseconds like the rest of the KMP module.
    private static func millis(from seconds: TimeInterval) -> Int64 {
        let millis = (seconds * 1000).rounded()
        // A value outside Int64 could only come from corrupt defaults, and
        // converting it would trap. Fall back to "never recorded" instead.
        guard millis.isFinite, millis > Double(Int64.min), millis < Double(Int64.max) else { return 0 }
        return Int64(millis)
    }

    private func activeDays() -> Set<String> {
        Set(defaults.stringArray(forKey: Keys.activeDays) ?? [])
    }

    private nonisolated static let dayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    private static func todayKey() -> String {
        dayFormatter.string(from: Date())
    }

    private enum Keys {
        static let firstLaunch = "first_launch"
        static let activeDays = "active_days"
        static let hasReviewed = "has_reviewed"
        // Key kept as "dismiss_count" so installs that already recorded
        // dismissals under the old prompt keep their attempt budget.
        static let promptCount = "dismiss_count"
        static let lastPrompted = "last_prompted"
    }
}

import Foundation

/// Manages in-app review prompt state and eligibility.
///
/// Tracks distinct calendar days the app was opened ("active days") along with
/// attempt history, to decide when to request the system review sheet after an
/// achievement unlock.
///
/// There is deliberately no "user said no" state: Apple forbids preceding the
/// sheet with a custom opinion prompt, so the app never learns whether the user
/// reviewed or declined. Attempts are capped instead.
///
/// This type owns eligibility and state only. Requesting the sheet itself is the
/// view's job, via SwiftUI's `\.requestReview` environment action, which targets
/// the scene the view actually lives in.
@MainActor
final class ReviewPromptManager {

    static let shared = ReviewPromptManager()

    private let defaults: UserDefaults

    private init() {
        self.defaults = UserDefaults(suiteName: "review_prompt") ?? .standard
    }

    // MARK: - First launch

    func initFirstLaunch() {
        if defaults.double(forKey: Keys.firstLaunch) == 0 {
            defaults.set(Date().timeIntervalSince1970, forKey: Keys.firstLaunch)
        }
    }

    // MARK: - Active day tracking

    /// Records today as an active usage day. Idempotent per calendar day, and
    /// stops writing once the gate is satisfied so the stored set cannot grow
    /// without bound.
    func recordActiveDay() {
        var days = activeDays()
        guard days.count < Constants.minActiveDays else { return }
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

    /// Returns `true` when all eligibility gates pass:
    /// - 5+ distinct active days
    /// - 7+ days since first launch
    /// - Not already reviewed
    /// - Fewer than 3 prior attempts
    /// - 90+ days since last attempt (or never attempted)
    func isEligible() -> Bool {
        guard !hasReviewed() else { return false }
        guard promptCount() < Constants.maxPrompts else { return false }
        guard activeDaysCount() >= Constants.minActiveDays else { return false }

        let firstLaunch = defaults.double(forKey: Keys.firstLaunch)
        guard firstLaunch > 0 else { return false }
        let daysSinceInstall = (Date().timeIntervalSince1970 - firstLaunch) / 86400
        guard daysSinceInstall >= Double(Constants.minDaysSinceInstall) else { return false }

        let lastPrompted = defaults.double(forKey: Keys.lastPrompted)
        if lastPrompted > 0 {
            let daysSincePrompt = (Date().timeIntervalSince1970 - lastPrompted) / 86400
            guard daysSincePrompt >= Double(Constants.cooldownDays) else { return false }
        }

        return true
    }

    // MARK: - Private

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

    private enum Constants {
        static let minActiveDays = 5
        static let minDaysSinceInstall = 7
        static let maxPrompts = 3
        static let cooldownDays = 90
    }
}

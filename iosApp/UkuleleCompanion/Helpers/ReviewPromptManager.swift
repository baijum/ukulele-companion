import Foundation
import StoreKit
import UIKit

/// Manages in-app review prompt state and eligibility.
///
/// Tracks distinct calendar days where the user visited a Play or Create screen
/// ("active days"), along with review/dismissal history, to determine when to
/// show the two-step review prompt after an achievement unlock.
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

    /// Records today as an active Play/Create usage day. Idempotent per calendar day.
    func recordActiveDay() {
        var days = activeDays()
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

    func hasReviewed() -> Bool {
        defaults.bool(forKey: Keys.hasReviewed)
    }

    func dismissCount() -> Int {
        defaults.integer(forKey: Keys.dismissCount)
    }

    func recordReviewed() {
        defaults.set(true, forKey: Keys.hasReviewed)
    }

    func recordDismissal() {
        defaults.set(dismissCount() + 1, forKey: Keys.dismissCount)
        defaults.set(Date().timeIntervalSince1970, forKey: Keys.lastPrompted)
    }

    func recordPromptShown() {
        defaults.set(Date().timeIntervalSince1970, forKey: Keys.lastPrompted)
    }

    // MARK: - Eligibility

    /// Returns `true` when all eligibility gates pass:
    /// - 5+ distinct active days
    /// - 7+ days since first launch
    /// - Not already reviewed
    /// - Fewer than 3 dismissals
    /// - 90+ days since last prompt (or never prompted)
    func isEligible() -> Bool {
        guard !hasReviewed() else { return false }
        guard dismissCount() < Constants.maxDismissals else { return false }
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

    // MARK: - Native review API

    /// Requests the App Store review dialog via SKStoreReviewController.
    @MainActor
    func requestReview() {
        if let windowScene = UIApplication.shared
            .connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first {
            SKStoreReviewController.requestReview(in: windowScene)
        }
    }

    // MARK: - Private

    private func activeDays() -> Set<String> {
        Set(defaults.stringArray(forKey: Keys.activeDays) ?? [])
    }

    private static func todayKey() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: Date())
    }

    private enum Keys {
        static let firstLaunch = "first_launch"
        static let activeDays = "active_days"
        static let hasReviewed = "has_reviewed"
        static let dismissCount = "dismiss_count"
        static let lastPrompted = "last_prompted"
    }

    private enum Constants {
        static let minActiveDays = 5
        static let minDaysSinceInstall = 7
        static let maxDismissals = 3
        static let cooldownDays = 90
    }
}

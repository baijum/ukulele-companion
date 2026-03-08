import SwiftUI
import UIKit

// MARK: - Accessibility Announcer

/// Singleton that throttles VoiceOver announcements to prevent flooding.
@MainActor
final class AccessibilityAnnouncer {
    static let shared = AccessibilityAnnouncer()

    private var lastAnnouncementTime: TimeInterval = 0
    private var lastAnnouncementText: String = ""

    private init() {}

    /// Post a VoiceOver announcement, throttled to avoid flooding.
    /// - Parameters:
    ///   - message: The text to announce.
    ///   - minInterval: Minimum seconds between announcements (default 1.5s for polite, use 0.5 for assertive).
    func announce(_ message: String, minInterval: TimeInterval = 1.5) {
        guard UIAccessibility.isVoiceOverRunning else { return }
        let now = ProcessInfo.processInfo.systemUptime
        guard message != lastAnnouncementText || (now - lastAnnouncementTime) >= minInterval else { return }
        lastAnnouncementTime = now
        lastAnnouncementText = message
        UIAccessibility.post(notification: .announcement, argument: message)
    }
}

// MARK: - View Extension

extension View {
    /// Combines common accessibility modifiers: ignores children, sets label/value/hint.
    func accessibilityCombined(
        label: String,
        value: String? = nil,
        hint: String? = nil
    ) -> some View {
        self
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .modifier(OptionalAccessibilityValue(value: value))
            .modifier(OptionalAccessibilityHint(hint: hint))
    }
}

private struct OptionalAccessibilityValue: ViewModifier {
    let value: String?
    func body(content: Content) -> some View {
        if let value = value {
            content.accessibilityValue(value)
        } else {
            content
        }
    }
}

private struct OptionalAccessibilityHint: ViewModifier {
    let hint: String?
    func body(content: Content) -> some View {
        if let hint = hint {
            content.accessibilityHint(hint)
        } else {
            content
        }
    }
}

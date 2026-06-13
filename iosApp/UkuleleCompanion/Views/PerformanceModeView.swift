import SwiftUI

struct PerformanceModeView: View {
    @Environment(\.dismiss) private var dismiss
    let content: String
    let font: Font

    @State private var isAutoScrolling = false
    @State private var scrollSpeed: Double = 1.0
    @State private var scrollTimer: Timer?
    @State private var showControls = true

    var body: some View {
        ZStack(alignment: .topTrailing) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(content.components(separatedBy: "\n").enumerated()), id: \.offset) { _, line in
                        if line.trimmingCharacters(in: .whitespaces).isEmpty {
                            Spacer().frame(height: 20)
                        } else {
                            Text(line)
                                .font(font)
                        }
                    }
                }
                .padding(24)
            }

            if showControls {
                HStack(spacing: 12) {
                    Button {
                        isAutoScrolling.toggle()
                    } label: {
                        Image(systemName: isAutoScrolling ? "pause.fill" : "play.fill")
                        Text(isAutoScrolling ? "Pause" : "Scroll")
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .accessibilityLabel(isAutoScrolling ? "Pause auto-scroll" : "Start auto-scroll")

                    if isAutoScrolling {
                        Button { scrollSpeed = max(0.5, scrollSpeed - 0.5) } label: {
                            Text("\u{2212}")
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .accessibilityLabel("Decrease speed")

                        Text(String(format: "%.1fx", scrollSpeed))
                            .font(.caption.monospacedDigit())
                            .accessibilityLabel("Speed \(String(format: "%.1f", scrollSpeed))x")

                        Button { scrollSpeed = min(5.0, scrollSpeed + 0.5) } label: {
                            Text("+")
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .accessibilityLabel("Increase speed")
                    }

                    Button { dismiss() } label: {
                        Image(systemName: "arrow.down.right.and.arrow.up.left")
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .accessibilityLabel("Exit performance mode")
                }
                .padding(12)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(16)
            }
        }
        .background(Color(uiColor: .systemBackground))
        .onTapGesture {
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            } else {
                showControls.toggle()
            }
        }
        .accessibilityHint(UIAccessibility.isVoiceOverRunning ? "Controls remain visible for VoiceOver" : "Tap anywhere to show or hide controls")
        .statusBarHidden(true)
        .onAppear {
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIAccessibility.voiceOverStatusDidChangeNotification)) { _ in
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            }
        }
    }
}

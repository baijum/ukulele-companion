import SwiftUI

private struct ScrollOffsetKey: PreferenceKey {
    static let defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct PerformanceModeView: View {
    @Environment(\.dismiss) private var dismiss
    let content: String
    let font: Font
    /// Mirrors the Settings value verbatim; "inline" keeps the `[C]` brackets.
    var chordDisplayStyle: String = "above"
    var chordColor: Color = .accentColor

    @State private var isAutoScrolling = false
    @State private var scrollSpeed: Double = 1.0
    @State private var scrollTimer: Timer?
    @State private var showControls = true
    @State private var scrollOffset: CGFloat = 0
    @State private var trackedScrollOffset: CGFloat = 0
    @State private var contentHeight: CGFloat = 1
    @State private var viewportHeight: CGFloat = 1
    @State private var tappedChord: String?

    private let tonePlayer = TonePlayer()

    var body: some View {
        ZStack(alignment: .topTrailing) {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        ChordSheetContentView(
                            content: content,
                            font: font,
                            chordDisplayStyle: chordDisplayStyle,
                            chordColor: chordColor,
                            onChordTap: { tappedChord = $0 }
                        )
                        Color.clear.frame(height: 1).id("bottom")
                    }
                    .padding(24)
                    .background(
                        GeometryReader { geo in
                            Color.clear
                                .preference(key: ScrollOffsetKey.self,
                                            value: -geo.frame(in: .named("perfScroll")).minY)
                                .onChange(of: geo.size.height) { newHeight in
                                    contentHeight = newHeight
                                }
                                .onAppear { contentHeight = geo.size.height }
                        }
                    )
                }
                .coordinateSpace(name: "perfScroll")
                .onPreferenceChange(ScrollOffsetKey.self) { value in
                    trackedScrollOffset = max(0, value)
                }
                .background(
                    GeometryReader { geo in
                        Color.clear.onAppear { viewportHeight = geo.size.height }
                            .onChange(of: geo.size.height) { newHeight in
                                viewportHeight = newHeight
                            }
                    }
                )
                .onChange(of: scrollSpeed) { _ in
                    guard isAutoScrolling else { return }
                    scrollTimer?.invalidate()
                    scrollTimer = createScrollTimer(proxy: proxy)
                }
                .onChange(of: isAutoScrolling) { scrolling in
                    if scrolling {
                        startAutoScroll(proxy: proxy)
                    } else {
                        pauseAutoScroll()
                    }
                }
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

                    Button {
                        pauseAutoScroll()
                        dismiss()
                    } label: {
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
        .popover(isPresented: Binding(
            get: { tappedChord != nil },
            set: { if !$0 { tappedChord = nil } }
        )) {
            if let chord = tappedChord {
                ChordDetailPopover(chord: chord, tonePlayer: tonePlayer)
            }
        }
        .accessibilityHint(UIAccessibility.isVoiceOverRunning ? "Controls remain visible for VoiceOver" : "Tap anywhere to show or hide controls")
        .statusBarHidden(true)
        .onAppear {
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            }
        }
        .onDisappear {
            pauseAutoScroll()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIAccessibility.voiceOverStatusDidChangeNotification)) { _ in
            if UIAccessibility.isVoiceOverRunning {
                showControls = true
            }
        }
    }

    // MARK: - Auto-scroll helpers

    @discardableResult
    private func createScrollTimer(proxy: ScrollViewProxy) -> Timer {
        let interval = 1.0 / (scrollSpeed * 2)
        nonisolated(unsafe) let scrollProxy = proxy
        let timer = Timer(timeInterval: interval, repeats: true) { [self] _ in
            MainActor.assumeIsolated {
                guard self.isAutoScrolling else { return }
                self.scrollOffset += 1
                let maxScroll = max(1, self.contentHeight - self.viewportHeight)
                let anchorY = min(1.0, self.scrollOffset / maxScroll)
                scrollProxy.scrollTo("bottom", anchor: .init(x: 0.5, y: 1.0 - anchorY))
            }
        }
        RunLoop.current.add(timer, forMode: .common)
        return timer
    }

    private func startAutoScroll(proxy: ScrollViewProxy) {
        let maxScroll = max(1, contentHeight - viewportHeight)
        scrollOffset = (trackedScrollOffset / maxScroll) * maxScroll
        scrollTimer = createScrollTimer(proxy: proxy)
    }

    private func pauseAutoScroll() {
        isAutoScrolling = false
        scrollTimer?.invalidate()
        scrollTimer = nil
    }
}

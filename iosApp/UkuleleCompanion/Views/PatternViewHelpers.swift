import SwiftUI
import shared

func patternDifficultyBadge(_ difficulty: Difficulty) -> some View {
    Text(difficulty.label)
        .font(.caption2.bold())
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(patternDifficultyColor(difficulty).opacity(0.2))
        .foregroundStyle(patternDifficultyColor(difficulty))
        .clipShape(Capsule())
}

func patternDifficultyColor(_ difficulty: Difficulty) -> Color {
    switch difficulty {
    case .beginner: .green
    case .intermediate: .orange
    case .advanced: .red
    default: .gray
    }
}

func patternDirectionSymbol(_ direction: StrumDirection) -> String {
    switch direction {
    case .down: "\u{2193}"
    case .up: "\u{2191}"
    case .chuck: "X"
    case .miss: "\u{00D7}"
    case .pause: "\u{2014}"
    default: "?"
    }
}

func patternDirectionSymbolFromString(_ direction: String) -> String {
    switch direction {
    case "DOWN": "\u{2193}"
    case "UP": "\u{2191}"
    case "CHUCK": "X"
    case "MISS": "\u{00D7}"
    case "PAUSE": "\u{2014}"
    default: "?"
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

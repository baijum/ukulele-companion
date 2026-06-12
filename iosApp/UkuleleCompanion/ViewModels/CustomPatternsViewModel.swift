import Foundation

// MARK: - Custom Strum Pattern

struct StrumBeatData: Codable {
    var direction: String // DOWN, UP, CHUCK, MISS, PAUSE
    var emphasis: Bool
}

struct CustomStrumPatternData: Codable, Identifiable {
    let id: String
    var name: String
    var beats: [StrumBeatData]
    let createdAt: Double
    var timeSignature: String
}

// MARK: - Custom Fingerpicking Pattern

struct FingerpickStepData: Codable {
    var finger: String // T, I, M, R
    var stringIndex: Int // 0=G, 1=C, 2=E, 3=A
    var emphasis: Bool
}

struct CustomFingerpickingData: Codable, Identifiable {
    let id: String
    var name: String
    var steps: [FingerpickStepData]
    let createdAt: Double
    var timeSignature: String
}

// MARK: - ViewModel

@MainActor
final class CustomPatternsViewModel: ObservableObject {
    @Published var strumPatterns: [CustomStrumPatternData] = []
    @Published var fingerpickingPatterns: [CustomFingerpickingData] = []

    private let repository: CustomPatternsRepository

    init(repository: CustomPatternsRepository = CustomPatternsRepository()) {
        self.repository = repository
        strumPatterns = repository.getAllStrum()
        fingerpickingPatterns = repository.getAllFingerpicking()
    }

    // MARK: - Strum Patterns

    func saveStrumPattern(_ pattern: CustomStrumPatternData) {
        if let idx = strumPatterns.firstIndex(where: { $0.id == pattern.id }) {
            strumPatterns[idx] = pattern
        } else {
            strumPatterns.insert(pattern, at: 0)
        }
        repository.saveStrum(strumPatterns)
    }

    func deleteStrumPattern(id: String) {
        strumPatterns.removeAll { $0.id == id }
        repository.saveStrum(strumPatterns)
    }

    // MARK: - Fingerpicking Patterns

    func saveFingerpickingPattern(_ pattern: CustomFingerpickingData) {
        if let idx = fingerpickingPatterns.firstIndex(where: { $0.id == pattern.id }) {
            fingerpickingPatterns[idx] = pattern
        } else {
            fingerpickingPatterns.insert(pattern, at: 0)
        }
        repository.saveFingerpicking(fingerpickingPatterns)
    }

    func deleteFingerpickingPattern(id: String) {
        fingerpickingPatterns.removeAll { $0.id == id }
        repository.saveFingerpicking(fingerpickingPatterns)
    }

    // MARK: - Export/Import for Backup

    func exportStrumData() -> [[String: Any]] {
        repository.exportStrumData(strumPatterns)
    }

    func importStrumData(_ items: [[String: Any]]) {
        repository.importStrumData(items, into: &strumPatterns)
    }

    func exportFingerpickData() -> [[String: Any]] {
        repository.exportFingerpickData(fingerpickingPatterns)
    }

    func importFingerpickData(_ items: [[String: Any]]) {
        repository.importFingerpickData(items, into: &fingerpickingPatterns)
    }
}

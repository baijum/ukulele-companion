import XCTest
@testable import UkuleleCompanion

final class PlayAlongAndNeuralTests: XCTestCase {

    // MARK: - PlayAlongViewModel.normalizeForComparison

    func testNormalizeRemoves7() {
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("C7"), "C")
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("Am7"), "Am")
    }

    func testNormalizeRemoves9() {
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("G9"), "G")
    }

    func testNormalizeRemovesMaj() {
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("Cmaj"), "C")
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("Fmaj7"), "F")
    }

    func testNormalizeTrimsWhitespace() {
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("  Am  "), "Am")
    }

    func testNormalizePreservesSimpleChord() {
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("C"), "C")
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("Am"), "Am")
        XCTAssertEqual(PlayAlongViewModel.normalizeForComparison("F#m"), "F#m")
    }

    // MARK: - NeuralPitchSupervisor bounds

    func testValidEstimateInRange() {
        XCTAssertTrue(NeuralPitchSupervisor.isValidEstimate(frequencyHz: 440.0, confidence: 0.85))
    }

    func testEstimateBelowMinFreqInvalid() {
        XCTAssertFalse(NeuralPitchSupervisor.isValidEstimate(frequencyHz: 30.0, confidence: 0.90))
    }

    func testEstimateAboveMaxFreqInvalid() {
        XCTAssertFalse(NeuralPitchSupervisor.isValidEstimate(frequencyHz: 3000.0, confidence: 0.90))
    }

    func testEstimateBelowMinConfidenceInvalid() {
        XCTAssertFalse(NeuralPitchSupervisor.isValidEstimate(frequencyHz: 440.0, confidence: 0.40))
    }

    func testEstimateAtBoundaryMinFreq() {
        XCTAssertTrue(NeuralPitchSupervisor.isValidEstimate(frequencyHz: 46.875, confidence: 0.50))
    }

    func testEstimateAtBoundaryMaxFreq() {
        XCTAssertTrue(NeuralPitchSupervisor.isValidEstimate(frequencyHz: 2093.75, confidence: 0.50))
    }

    func testBoundsConstants() {
        XCTAssertEqual(NeuralPitchSupervisor.minFreqHz, 46.875, accuracy: 0.001)
        XCTAssertEqual(NeuralPitchSupervisor.maxFreqHz, 2093.75, accuracy: 0.001)
        XCTAssertEqual(NeuralPitchSupervisor.minConfidence, 0.50, accuracy: 0.001)
    }
}

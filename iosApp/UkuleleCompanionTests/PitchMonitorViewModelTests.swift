import XCTest
@testable import UkuleleCompanion

final class PitchMonitorViewModelTests: XCTestCase {

    // MARK: - semitoneDistance

    func testSemitoneDistanceUnison() {
        let st = PitchMonitorViewModel.semitoneDistance(440.0, 440.0)
        XCTAssertEqual(st, 0, accuracy: 0.001)
    }

    func testSemitoneDistanceOctave() {
        let st = PitchMonitorViewModel.semitoneDistance(440.0, 880.0)
        XCTAssertEqual(st, 12.0, accuracy: 0.001)
    }

    func testSemitoneDistanceDoubleOctave() {
        let st = PitchMonitorViewModel.semitoneDistance(440.0, 1760.0)
        XCTAssertEqual(st, 24.0, accuracy: 0.001)
    }

    func testSemitoneDistanceFifth() {
        // Perfect fifth = 7 semitones (ratio ~1.4983)
        let st = PitchMonitorViewModel.semitoneDistance(440.0, 659.255)
        XCTAssertEqual(st, 7.0, accuracy: 0.01)
    }

    func testSemitoneDistanceSymmetry() {
        let st1 = PitchMonitorViewModel.semitoneDistance(440.0, 880.0)
        let st2 = PitchMonitorViewModel.semitoneDistance(880.0, 440.0)
        XCTAssertEqual(st1, st2, accuracy: 0.001)
    }

    func testSemitoneDistanceZeroReturnsInfinity() {
        let st = PitchMonitorViewModel.semitoneDistance(0, 440.0)
        XCTAssertEqual(st, Double.greatestFiniteMagnitude)
    }

    func testSemitoneDistanceNegativeReturnsInfinity() {
        let st = PitchMonitorViewModel.semitoneDistance(-1.0, 440.0)
        XCTAssertEqual(st, Double.greatestFiniteMagnitude)
    }

    // MARK: - isOctaveRelation

    func testIsOctaveRelationExact() {
        XCTAssertTrue(PitchMonitorViewModel.isOctaveRelation(440.0, 880.0))
        XCTAssertTrue(PitchMonitorViewModel.isOctaveRelation(880.0, 440.0))
    }

    func testIsOctaveRelationDoubleOctave() {
        XCTAssertTrue(PitchMonitorViewModel.isOctaveRelation(440.0, 1760.0))
    }

    func testIsOctaveRelationSlightlyDetuned() {
        // A4=440 vs A5=878 (slightly flat) -- within 1 semitone tolerance
        XCTAssertTrue(PitchMonitorViewModel.isOctaveRelation(440.0, 878.0))
    }

    func testIsOctaveRelationNonOctave() {
        // Perfect fifth (7 semitones) should not be an octave relation
        XCTAssertFalse(PitchMonitorViewModel.isOctaveRelation(440.0, 659.255))
    }

    func testIsOctaveRelationZeroReturnsFalse() {
        XCTAssertFalse(PitchMonitorViewModel.isOctaveRelation(0, 440.0))
    }

    // MARK: - medianFrequency

    func testMedianFrequencyOddCount() {
        let median = PitchMonitorViewModel.medianFrequency(from: [100.0, 300.0, 200.0])
        XCTAssertEqual(median, 200.0, accuracy: 0.001)
    }

    func testMedianFrequencyEvenCount() {
        let median = PitchMonitorViewModel.medianFrequency(from: [100.0, 200.0, 300.0, 400.0])
        XCTAssertEqual(median, 250.0, accuracy: 0.001)
    }

    func testMedianFrequencySingleValue() {
        let median = PitchMonitorViewModel.medianFrequency(from: [440.0])
        XCTAssertEqual(median, 440.0, accuracy: 0.001)
    }

    func testMedianFrequencyEmpty() {
        let median = PitchMonitorViewModel.medianFrequency(from: [])
        XCTAssertEqual(median, 0)
    }
}

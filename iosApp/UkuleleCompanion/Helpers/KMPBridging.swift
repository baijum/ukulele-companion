import Foundation
import shared

// MARK: - Collection element conversions (works on both NSArray and typed [T])

extension Sequence {
    private func bridged<T>(_ transform: (Element) -> T?) -> [T] {
        var result: [T] = []
        var total = 0
        for element in self {
            total += 1
            if let value = transform(element) { result.append(value) }
        }
        #if DEBUG
        if result.count != total {
            assertionFailure("KMP bridge conversion dropped \(total - result.count) element(s)")
        }
        #endif
        return result
    }

    /// Converts elements via NSNumber to a Swift `[Int]`.
    var asInts: [Int] {
        bridged { ($0 as? NSNumber)?.intValue }
    }

    /// Converts elements via NSNumber to a Swift `[Int32]`.
    var asInt32s: [Int32] {
        bridged { ($0 as? NSNumber)?.int32Value }
    }

    /// Converts elements via NSNumber to a Swift `[Float]`.
    var asFloats: [Float] {
        bridged { ($0 as? NSNumber)?.floatValue }
    }

    /// Converts elements to a Swift `[String]`.
    var asStrings: [String] {
        bridged { $0 as? String }
    }

    /// Safely casts every element to `T`, dropping any that don't match.
    func asArray<T>(of _: T.Type) -> [T] {
        bridged { $0 as? T }
    }
}

// MARK: - Kotlin numeric unwrapping

extension KotlinInt {
    /// Unwraps to a Swift `Int`.
    var asInt: Int { intValue }

    /// Unwraps to a Swift `Int32`.
    var asInt32: Int32 { int32Value }
}

// MARK: - Set<KotlinInt> helpers

extension Set where Element == KotlinInt {
    /// Converts a `Set<KotlinInt>` to `Set<Int>`.
    var asIntSet: Set<Int> {
        Set<Int>(map(\.intValue))
    }
}

// MARK: - UkuleleTuning convenience

extension UkuleleTuning {
    /// Builds the `[shared.UkuleleString]` array for this tuning.
    var asUkuleleStrings: [shared.UkuleleString] {
        let count = min(Int(stringNames.count), Int(pitchClasses.count), Int(octaves.count))
        #if DEBUG
        if count != 4 { assertionFailure("Unexpected tuning element count: \(count)") }
        #endif
        return (0..<count).map { i in
            shared.UkuleleString(
                name: (stringNames[i] as? String) ?? "",
                openPitchClass: (pitchClasses[i] as? NSNumber)?.int32Value ?? 0,
                octave: (octaves[i] as? NSNumber)?.int32Value ?? 0
            )
        }
    }

    /// Pitch classes as a Swift `[Int32]` array.
    var pitchClassInts: [Int32] {
        (0..<Int(pitchClasses.count)).map { (pitchClasses[$0] as? NSNumber)?.int32Value ?? 0 }
    }

    /// String names as a Swift `[String]` array.
    var stringNameArray: [String] {
        (0..<Int(stringNames.count)).map { (stringNames[$0] as? String) ?? "" }
    }

    /// Octave number of each open string as a Swift `[Int32]` array.
    var octaveInts: [Int32] {
        (0..<Int(octaves.count)).map { (octaves[$0] as? NSNumber)?.int32Value ?? 0 }
    }
}

// MARK: - ChordVoicing convenience

extension ChordVoicing {
    /// Decodes the KMP-bridged `frets` list to a native Swift `[Int]`.
    var fretInts: [Int] {
        (0..<frets.count).map { (frets[$0] as? NSNumber)?.intValue ?? 0 }
    }
}

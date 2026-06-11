import Foundation
import shared

// MARK: - NSArray / KotlinArray element conversions

extension NSArray {
    /// Converts a KMP-bridged list of NSNumber to a Swift `[Int]`.
    var asInts: [Int] {
        compactMap { ($0 as? NSNumber)?.intValue }
    }

    /// Converts a KMP-bridged list of NSNumber to a Swift `[Int32]`.
    var asInt32s: [Int32] {
        compactMap { ($0 as? NSNumber)?.int32Value }
    }

    /// Converts a KMP-bridged list of NSNumber to a Swift `[Float]`.
    var asFloats: [Float] {
        compactMap { ($0 as? NSNumber)?.floatValue }
    }

    /// Converts a KMP-bridged list to a Swift `[String]`.
    var asStrings: [String] {
        compactMap { $0 as? String }
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

// MARK: - Typed list casts (KMP collections bridged as NSArray)

extension NSArray {
    /// Safely casts every element to `T`, dropping any that don't match.
    func asArray<T>(of _: T.Type) -> [T] {
        compactMap { $0 as? T }
    }
}

// MARK: - UkuleleTuning convenience

extension UkuleleTuning {
    /// Builds the `[shared.UkuleleString]` array for this tuning.
    var asUkuleleStrings: [shared.UkuleleString] {
        (0..<4).map { i in
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
}

// MARK: - ChordVoicing convenience

extension ChordVoicing {
    /// Decodes the KMP-bridged `frets` list to a native Swift `[Int]`.
    var fretInts: [Int] {
        (0..<frets.count).map { (frets[$0] as? NSNumber)?.intValue ?? 0 }
    }
}

import shared

extension ChordVoicing {
    /// Decodes the KMP-bridged `frets` list to a native Swift `[Int]`.
    var fretInts: [Int] {
        (0..<frets.count).map { (frets[$0] as! NSNumber).intValue }
    }
}

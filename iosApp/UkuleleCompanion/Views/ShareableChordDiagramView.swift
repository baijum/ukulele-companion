import SwiftUI
import shared

/// A presentation-quality chord diagram for sharing as an image.
/// White background, green dots with finger numbers, note names, fret numbers, and attribution.
struct ShareableChordDiagramView: View {
    let voicing: ChordVoicing
    let chordName: String
    var inversionLabel: String?

    // ImageRenderer draws this view outside the SwiftUI hierarchy, so it cannot
    // read `\.leftHanded` / `\.diagramStringNames` from the environment the way
    // the on-screen ChordDiagramView does (#637). The two settings are therefore
    // passed in explicitly from SettingsViewModel at the call site (#638) so the
    // exported PNG mirrors for left-handed players and labels strings/notes for
    // the selected tuning (High-G, Low-G, Baritone, ...).
    var leftHanded: Bool = false
    var stringNames: [String] = ["G", "C", "E", "A"]
    var openPitchClasses: [Int] = [7, 0, 4, 9] // G C E A

    private let stringCount = 4
    private let minFretRows = 5
    private let stringSpacing: CGFloat = 36
    private let fretSpacing: CGFloat = 36
    private let dotRadius: CGFloat = 13
    private let openCircleRadius: CGFloat = 8
    private let nutThickness: CGFloat = 5
    private let dotColor = Color(red: 0.18, green: 0.49, blue: 0.20) // #2E7D32

    /// Voicing frets in draw order — reversed for a left-handed player so the
    /// exported diagram mirrors the on-screen one (matches ChordDiagramView).
    private var frets: [Int] {
        let raw = voicing.fretInts
        return leftHanded ? raw.reversed() : raw
    }

    /// Open-string pitch classes in the same draw order as `frets`, so the note
    /// names below the grid stay aligned with each mirrored string.
    private var orderedPitchClasses: [Int] {
        leftHanded ? Array(openPitchClasses.reversed()) : openPitchClasses
    }

    /// Open-string names in the same draw order as `frets`, used for the
    /// accessibility description so it reflects the mirrored order (#638).
    private var orderedStringNames: [String] {
        leftHanded ? Array(stringNames.reversed()) : stringNames
    }

    private var startFret: Int {
        let maxFret = Int(voicing.maxFret)
        let minFret = Int(voicing.minFret)
        if maxFret == 0 || maxFret <= minFretRows {
            return 0
        }
        return max(1, minFret)
    }

    private var fretRowCount: Int {
        let maxFret = Int(voicing.maxFret)
        if maxFret <= minFretRows {
            return minFretRows
        }
        return max(minFretRows, maxFret - startFret + 1)
    }

    private var isAtNut: Bool { startFret == 0 }

    private var fingering: [Int] {
        let kotlinFrets = frets.map { KotlinInt(int: Int32($0)) }
        let result = ChordInfo.shared.suggestFingering(frets: kotlinFrets)
        return result.asInts
    }

    private var diagramWidth: CGFloat { CGFloat(stringCount - 1) * stringSpacing }

    private var accessibilityDescription: String {
        // Name each string in the (possibly mirrored) draw order so a blind
        // left-handed user hears the same layout the sighted image shows (#638).
        let perString = zip(orderedStringNames, frets).map { name, fret -> String in
            if fret < 0 { return "\(name) string muted" }
            if fret == 0 { return "\(name) string open" }
            return "\(name) string fret \(fret)"
        }.joined(separator: ", ")
        var desc = "\(chordName) chord diagram, \(perString)"
        if let label = inversionLabel { desc += ", \(label)" }
        return desc
    }

    var body: some View {
        VStack(spacing: 0) {
            Text(chordName)
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(.black)
                .padding(.bottom, 8)

            chordCanvas
                .frame(
                    width: 48 + diagramWidth + 24,
                    height: 24 + (isAtNut ? nutThickness : 0) + CGFloat(fretRowCount) * fretSpacing + 40
                )

            if let label = inversionLabel {
                Text(label)
                    .font(.system(size: 13).italic())
                    .foregroundStyle(.gray)
                    .padding(.top, 8)
            }

            Text("Ukulele Companion")
                .font(.system(size: 10))
                .foregroundStyle(Color(white: 0.75))
                .padding(.top, 4)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 20)
        .background(Color.white)
        .accessibilityCombined(label: accessibilityDescription)
    }

    // MARK: - Canvas

    private var chordCanvas: some View {
        Canvas { context, size in
            let posLabelWidth: CGFloat = 48
            let openArea: CGFloat = 24
            let gridTop = openArea + (isAtNut ? nutThickness : 0)

            let stringXPositions = (0..<stringCount).map { i in
                posLabelWidth + CGFloat(i) * stringSpacing
            }

            // Draw nut
            if isAtNut {
                var nutPath = Path()
                nutPath.move(to: CGPoint(x: stringXPositions.first!, y: gridTop))
                nutPath.addLine(to: CGPoint(x: stringXPositions.last!, y: gridTop))
                context.stroke(nutPath, with: .color(.black), lineWidth: nutThickness)
            }

            // Horizontal fret lines
            for row in 0...fretRowCount {
                let y = gridTop + CGFloat(row) * fretSpacing
                var path = Path()
                path.move(to: CGPoint(x: stringXPositions.first!, y: y))
                path.addLine(to: CGPoint(x: stringXPositions.last!, y: y))
                context.stroke(path, with: .color(.gray.opacity(0.5)), lineWidth: 1.5)
            }

            // Vertical string lines
            for x in stringXPositions {
                var path = Path()
                path.move(to: CGPoint(x: x, y: gridTop))
                path.addLine(to: CGPoint(x: x, y: gridTop + CGFloat(fretRowCount) * fretSpacing))
                context.stroke(path, with: .color(.black.opacity(0.7)), lineWidth: 1.5)
            }

            let currentFrets = frets
            let currentFingering = fingering

            // Draw open circles, muted X marks, and fretted dots
            for s in 0..<stringCount {
                let fret = currentFrets[s]
                let x = stringXPositions[s]

                if fret < 0 {
                    // Muted: draw X
                    let centerY = openArea / 2
                    let xSize = openCircleRadius * 0.7
                    var line1 = Path()
                    line1.move(to: CGPoint(x: x - xSize, y: centerY - xSize))
                    line1.addLine(to: CGPoint(x: x + xSize, y: centerY + xSize))
                    context.stroke(line1, with: .color(.black), lineWidth: 2)
                    var line2 = Path()
                    line2.move(to: CGPoint(x: x - xSize, y: centerY + xSize))
                    line2.addLine(to: CGPoint(x: x + xSize, y: centerY - xSize))
                    context.stroke(line2, with: .color(.black), lineWidth: 2)
                } else if fret == 0 {
                    // Open string circle
                    let circle = Path(ellipseIn: CGRect(
                        x: x - openCircleRadius, y: openArea / 2 - openCircleRadius,
                        width: openCircleRadius * 2, height: openCircleRadius * 2
                    ))
                    context.stroke(circle, with: .color(.black), lineWidth: 2)
                } else {
                    // Fretted dot
                    let relFret = fret - startFret
                    let y: CGFloat
                    if isAtNut {
                        y = gridTop + (CGFloat(relFret) - 0.5) * fretSpacing
                    } else {
                        y = gridTop + (CGFloat(relFret) + 0.5) * fretSpacing
                    }

                    // Green dot
                    let dotPath = Path(ellipseIn: CGRect(
                        x: x - dotRadius, y: y - dotRadius,
                        width: dotRadius * 2, height: dotRadius * 2
                    ))
                    context.fill(dotPath, with: .color(dotColor))

                    // White finger number
                    let finger = currentFingering[s]
                    if finger > 0 {
                        let text = Text("\(finger)")
                            .font(.system(size: dotRadius * 1.2, weight: .bold))
                            .foregroundColor(.white)
                        context.draw(context.resolve(text), at: CGPoint(x: x, y: y))
                    }
                }
            }

            // Position indicator (e.g., "7fr")
            if !isAtNut {
                let posText = Text("\(startFret)fr")
                    .font(.system(size: 12))
                    .foregroundColor(Color(white: 0.3))
                context.draw(
                    context.resolve(posText),
                    at: CGPoint(x: posLabelWidth / 2, y: gridTop + fretSpacing / 2)
                )
            }

            // Note names and fret numbers below grid
            let gridBottom = gridTop + CGFloat(fretRowCount) * fretSpacing
            let noteNameY = gridBottom + 14
            let fretNumberY = noteNameY + 14

            for s in 0..<stringCount {
                let fret = currentFrets[s]
                let x = stringXPositions[s]
                let openPitch = orderedPitchClasses[s]

                let noteName: String
                if fret < 0 {
                    noteName = "x"
                } else {
                    let pitchClass = (openPitch + fret) % 12
                    noteName = Notes.shared.pitchClassToName(pitchClass: Int32(pitchClass))
                }

                let noteText = Text(noteName)
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.3))
                context.draw(context.resolve(noteText), at: CGPoint(x: x, y: noteNameY))

                let fretLabel = fret < 0 ? "x" : "\(fret)"
                let fretText = Text(fretLabel)
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
                context.draw(context.resolve(fretText), at: CGPoint(x: x, y: fretNumberY))
            }
        }
    }
}

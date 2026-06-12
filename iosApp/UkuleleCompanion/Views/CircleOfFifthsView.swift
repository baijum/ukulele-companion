import SwiftUI
import shared

struct CircleOfFifthsView: View {
    @State private var selectedKeyIndex: Int? = 0
    @State private var showMinor = false

    private let circleOrder: [Int32] = KeySignatures.shared.CIRCLE_ORDER.asInt32s

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Major/Minor toggle
                Picker("Mode", selection: $showMinor) {
                    Text("Major").tag(false)
                    Text("Minor").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                circleCanvas
                    .aspectRatio(1, contentMode: .fit)
                    .frame(maxHeight: 500)
                    .padding(.horizontal)

                // Detail panel
                if let index = selectedKeyIndex {
                    detailPanel(for: circleOrder[index])
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Circle of Fifths")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Circle Canvas

    private var circleCanvas: some View {
        GeometryReader { geo in
            let center = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)
            let outerRadius = min(geo.size.width, geo.size.height) / 2 - 20
            let innerRadius = outerRadius * 0.65
            let labelOuterRadius = outerRadius - 18
            let labelInnerRadius = innerRadius + 14

            Canvas { context, size in
                let c = CGPoint(x: size.width / 2, y: size.height / 2)

                // Draw highlight arc for selected key
                if let index = selectedKeyIndex {
                    let startAngle = Angle.degrees(Double(index) * 30 - 105)
                    let endAngle = Angle.degrees(Double(index) * 30 - 75)
                    var path = Path()
                    path.addArc(center: c, radius: outerRadius, startAngle: startAngle, endAngle: endAngle, clockwise: false)
                    path.addArc(center: c, radius: innerRadius * 0.85, startAngle: endAngle, endAngle: startAngle, clockwise: true)
                    path.closeSubpath()
                    context.fill(path, with: .color(.accentColor.opacity(0.2)))
                }

                // Draw segment lines (offset by 15° so they fall between labels)
                for i in 0..<12 {
                    let angle = Angle.degrees(Double(i) * 30 - 90 + 15)
                    var line = Path()
                    let x1 = c.x + cos(angle.radians) * innerRadius * 0.85
                    let y1 = c.y + sin(angle.radians) * innerRadius * 0.85
                    let x2 = c.x + cos(angle.radians) * outerRadius
                    let y2 = c.y + sin(angle.radians) * outerRadius
                    line.move(to: CGPoint(x: x1, y: y1))
                    line.addLine(to: CGPoint(x: x2, y: y2))
                    context.stroke(line, with: .color(.gray.opacity(0.3)), lineWidth: 0.5)
                }

                // Draw outer ring
                var outerCircle = Path()
                outerCircle.addEllipse(in: CGRect(
                    x: c.x - outerRadius, y: c.y - outerRadius,
                    width: outerRadius * 2, height: outerRadius * 2
                ))
                context.stroke(outerCircle, with: .color(.gray.opacity(0.4)), lineWidth: 1)

                // Draw inner ring
                var innerCircle = Path()
                innerCircle.addEllipse(in: CGRect(
                    x: c.x - innerRadius * 0.85, y: c.y - innerRadius * 0.85,
                    width: innerRadius * 0.85 * 2, height: innerRadius * 0.85 * 2
                ))
                context.stroke(innerCircle, with: .color(.gray.opacity(0.3)), lineWidth: 1)
            }
            .accessibilityHidden(true)

            // Major key labels (outer ring)
            ForEach(0..<12, id: \.self) { i in
                let angle = Angle.degrees(Double(i) * 30 - 90)
                let x = center.x + cos(angle.radians) * labelOuterRadius
                let y = center.y + sin(angle.radians) * labelOuterRadius
                let pc = circleOrder[i]
                let name = Notes.shared.pitchClassToName(pitchClass: pc)
                let isSelected = selectedKeyIndex == i

                Text(name)
                    .font(isSelected ? .callout.bold() : .callout)
                    .foregroundStyle(isSelected ? Color.accentColor : Color.primary)
                    .position(x: x, y: y)
                    .accessibilityLabel("\(name) major")
                    .accessibilityAddTraits(.isButton)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                    .accessibilityHint("Double tap to select key")
                    .onTapGesture { selectedKeyIndex = i }
            }

            // Minor key labels (inner ring)
            ForEach(0..<12, id: \.self) { i in
                let angle = Angle.degrees(Double(i) * 30 - 90)
                let x = center.x + cos(angle.radians) * labelInnerRadius
                let y = center.y + sin(angle.radians) * labelInnerRadius
                let pc = circleOrder[i]
                let keySig = KeySignatures.shared.forKey(pitchClass: pc)
                let minorPC = keySig?.relativeMinorPitchClass ?? 0
                let minorName = Notes.shared.pitchClassToName(pitchClass: minorPC) + "m"
                let isSelected = selectedKeyIndex == i

                Text(minorName)
                    .font(.caption2)
                    .foregroundStyle(isSelected && showMinor ? Color.accentColor : Color.secondary)
                    .position(x: x, y: y)
                    .accessibilityLabel("\(Notes.shared.pitchClassToName(pitchClass: minorPC)) minor")
                    .accessibilityAddTraits(.isButton)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                    .accessibilityHint("Double tap to select key")
                    .onTapGesture { selectedKeyIndex = i }
            }

            // Tap gesture overlay
            Color.clear
                .contentShape(Rectangle())
                .accessibilityHidden(true)
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onEnded { value in
                            let dx = value.location.x - center.x
                            let dy = value.location.y - center.y
                            let dist = sqrt(dx * dx + dy * dy)
                            guard dist <= outerRadius else { return }
                            let angle = atan2(dy, dx)
                            var degrees = angle * 180 / .pi + 90
                            if degrees < 0 { degrees += 360 }
                            let segment = Int((degrees + 15) / 30) % 12
                            selectedKeyIndex = segment
                        }
                )
        }
    }

    // MARK: - Detail Panel

    @ViewBuilder
    private func detailPanel(for pitchClass: Int32) -> some View {
        let keySig = KeySignatures.shared.forKey(pitchClass: pitchClass)
        let majorName = Notes.shared.pitchClassToName(pitchClass: pitchClass)

        VStack(alignment: .leading, spacing: 12) {
            if let keySig = keySig {
                let minorName = Notes.shared.pitchClassToName(
                    pitchClass: keySig.relativeMinorPitchClass
                )
                let sigText = KeySignatures.shared.formatSignature(keySig: keySig)

                // Key name and signature
                HStack {
                    VStack(alignment: .leading) {
                        Text(showMinor ? "\(minorName) Minor" : "\(majorName) Major")
                            .font(.title2.bold())
                        Text(sigText)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text(showMinor ? "Relative Major" : "Relative Minor")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(showMinor ? "\(majorName) Major" : "\(minorName) Minor")
                            .font(.subheadline.bold())
                    }
                }

                // Closely related keys
                let related = KeySignatures.shared.closelyRelatedKeys(pitchClass: pitchClass)
                let ccwName = Notes.shared.pitchClassToName(pitchClass: Int32(related.first!.intValue))
                let cwName = Notes.shared.pitchClassToName(pitchClass: Int32(related.second!.intValue))

                HStack {
                    Text("Nearby keys:")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(ccwName) Major")
                        .font(.caption.bold())
                    Text("and")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(cwName) Major")
                        .font(.caption.bold())
                }

                // Diatonic chords
                Text(showMinor ? "Diatonic Chords (Minor)" : "Diatonic Chords (Major)")
                    .font(.subheadline.bold())
                    .padding(.top, 4)
                    .accessibilityAddTraits(.isHeader)

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 70), spacing: 8)], spacing: 8) {
                    let chords: [KotlinPair<NSString, NSString>] = showMinor
                        ? KeySignatures.shared.diatonicChordsForMinor(
                            pitchClass: keySig.relativeMinorPitchClass
                        ).asArray(of: KotlinPair<NSString, NSString>.self)
                        : KeySignatures.shared.diatonicChordsForMajor(
                            pitchClass: pitchClass
                        ).asArray(of: KotlinPair<NSString, NSString>.self)
                    ForEach(Array(chords.enumerated()), id: \.offset) { _, pair in
                        let numeral = pair.first! as String
                        let chordName = pair.second! as String
                        VStack(spacing: 2) {
                            Text(numeral)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Text(chordName)
                                .font(.caption.bold())
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color(.systemGray6))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel("\(numeral), \(chordName)")
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }
}

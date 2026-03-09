import SwiftUI
import shared

struct CapoGuideView: View {
    @EnvironmentObject var settings: SettingsViewModel
    @State private var selectedCapoFret: Int = 0

    private var tuning: UkuleleTuning {
        UkuleleTuning.entries.first { $0.label == settings.selectedTuning } ?? .highG
    }

    private var pitchClasses: [Int32] {
        (0..<Int(tuning.pitchClasses.count)).map { (tuning.pitchClasses[$0] as! NSNumber).int32Value }
    }

    private var stringNames: [String] {
        (0..<Int(tuning.stringNames.count)).map { tuning.stringNames[$0] as! String }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Section 1: What is a Capo?
                sectionCard(number: 1, title: "What is a Capo?") {
                    Text(CapoReference.shared.WHAT_IS_A_CAPO)
                        .font(.body)
                }

                // Section 2: Interactive Pitch Demo
                sectionCard(number: 2, title: "How It Changes Pitch") {
                    Text(CapoReference.shared.HOW_IT_CHANGES_PITCH)
                        .font(.body)
                        .padding(.bottom, 8)

                    Text("Tap a fret to see the effective pitches:")
                        .font(.subheadline.bold())

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(0..<8, id: \.self) { fret in
                                Button {
                                    selectedCapoFret = fret
                                } label: {
                                    Text(fret == 0 ? "Open" : "Fret \(fret)")
                                        .font(.caption.bold())
                                        .frame(minWidth: 52, minHeight: 36)
                                        .background(
                                            selectedCapoFret == fret
                                                ? Color.accentColor
                                                : Color(.systemGray5)
                                        )
                                        .foregroundStyle(
                                            selectedCapoFret == fret ? .white : .primary
                                        )
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                                .accessibilityAddTraits(selectedCapoFret == fret ? .isSelected : [])
                            }
                        }
                    }

                    // Effective pitches display
                    HStack(spacing: 16) {
                        ForEach(0..<pitchClasses.count, id: \.self) { i in
                            let openPC = pitchClasses[i]
                            let effectivePC = (openPC + Int32(selectedCapoFret)) % 12
                            let effectiveName = Notes.shared.pitchClassToName(pitchClass: effectivePC)
                            let originalName = stringNames[i]
                            VStack(spacing: 4) {
                                Text("String \(i + 1)")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text(effectiveName)
                                    .font(.title2.bold())
                                    .foregroundStyle(.primary)
                                if selectedCapoFret > 0 {
                                    Text("(was \(originalName))")
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .accessibilityCombined(
                                label: "String \(i + 1)",
                                value: selectedCapoFret > 0 ? "\(effectiveName), was \(originalName)" : effectiveName
                            )
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                // Section 3: Common Positions
                sectionCard(number: 3, title: "When to Use a Capo") {
                    Text(CapoReference.shared.WHEN_TO_USE)
                        .font(.body)
                }

                // Section 4: Common Positions Reference Table
                sectionCard(number: 4, title: "Common Capo Positions") {
                    let positions = CapoReference.shared.COMMON_POSITIONS as! [CapoReference.CapoPosition]
                    VStack(spacing: 0) {
                        // Header
                        HStack {
                            Text("Fret").font(.caption.bold()).frame(width: 36, alignment: .leading)
                            Text("Shape").font(.caption.bold()).frame(maxWidth: .infinity, alignment: .leading)
                            Text("Sounds").font(.caption.bold()).frame(maxWidth: .infinity, alignment: .leading)
                            Text("Chords").font(.caption.bold()).frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(.vertical, 6)
                        .padding(.horizontal, 8)
                        .background(Color(.systemGray5))
                        .accessibilityAddTraits(.isHeader)

                        ForEach(Array(positions.enumerated()), id: \.offset) { _, pos in
                            HStack {
                                Text("\(pos.capoFret)").font(.caption).frame(width: 36, alignment: .leading)
                                Text(pos.shapeKey).font(.caption).frame(maxWidth: .infinity, alignment: .leading)
                                Text(pos.soundingKey).font(.caption).frame(maxWidth: .infinity, alignment: .leading)
                                Text(pos.exampleChords).font(.caption).frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding(.vertical, 4)
                            .padding(.horizontal, 8)
                            .accessibilityElement(children: .combine)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color(.systemGray4), lineWidth: 1)
                    )
                }

                // Section 5: Try the Calculator
                sectionCard(number: 5, title: "Try It") {
                    Text("Use the Capo Calculator to find the easiest capo position for any chord or progression.")
                        .font(.body)
                    NavigationLink {
                        CapoCalculatorView(mode: .singleChord(
                            rootPitchClass: 0,
                            formula: (ChordFormulas.shared.ALL as! [ChordFormula]).first!
                        ))
                    } label: {
                        Label("Open Capo Calculator", systemImage: "guitars")
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .padding(.top, 4)
                }

                // Section 6: Scenarios
                sectionCard(number: 6, title: "Practical Scenarios") {
                    let scenarios = CapoReference.shared.SCENARIOS as! [CapoReference.Scenario]
                    VStack(spacing: 12) {
                        ForEach(Array(scenarios.enumerated()), id: \.offset) { _, scenario in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(alignment: .top) {
                                    Image(systemName: "questionmark.circle")
                                        .foregroundStyle(.orange)
                                    Text(scenario.problem)
                                        .font(.subheadline)
                                }
                                HStack(alignment: .top) {
                                    Image(systemName: "checkmark.circle")
                                        .foregroundStyle(.green)
                                    Text(scenario.solution)
                                        .font(.subheadline)
                                }
                            }
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(.systemGray6))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .accessibilityCombined(label: "Problem: \(scenario.problem). Solution: \(scenario.solution)")
                        }
                    }

                    // Friendly shapes
                    Text("Beginner-Friendly Shapes")
                        .font(.subheadline.bold())
                        .padding(.top, 8)

                    let shapes = CapoReference.shared.FRIENDLY_SHAPES as! [KotlinPair<NSString, NSString>]
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 80), spacing: 8)], spacing: 8) {
                        ForEach(Array(shapes.enumerated()), id: \.offset) { _, pair in
                            let name = pair.first! as String
                            let tab = pair.second! as String
                            VStack(spacing: 2) {
                                Text(name).font(.subheadline.bold())
                                Text(tab).font(.caption.monospaced()).foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(Color(.systemGray6))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Capo Guide")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func sectionCard<Content: View>(
        number: Int,
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("\(number)")
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .frame(width: 24, height: 24)
                    .background(Color.accentColor)
                    .clipShape(Circle())
                Text(title)
                    .font(.title3.bold())
            }
            .accessibilityCombined(label: "Section \(number): \(title)")
            .accessibilityAddTraits(.isHeader)
            content()
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

import SwiftUI
import shared

struct PracticeRoutineView: View {
    @State private var durationMinutes: Double = 15
    @State private var skillLevel: PracticeRoutineGenerator.SkillLevel = .beginner
    @State private var focusAreas: Set<PracticeRoutineGenerator.FocusArea> = Set(PracticeRoutineGenerator.FocusArea.entries)
    @State private var routine: PracticeRoutine? = nil

    // Guided flow state
    @State private var isActiveRoutine = false
    @State private var activeStep: Int = 0
    @State private var completedSteps: Set<Int> = []

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if isActiveRoutine, let routine {
                    activeRoutineContent(routine)
                } else {
                    setupContent
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Practice Routine")
    }

    // MARK: - Setup Phase

    private var setupContent: some View {
        VStack(spacing: 16) {
            VStack(spacing: 4) {
                Text("Duration: \(Int(durationMinutes)) min")
                    .font(.headline)
                Slider(value: $durationMinutes, in: 5...60, step: 5)
            }
            .padding(.horizontal)

            HStack {
                Text("Level")
                    .font(.headline)
                Picker("Level", selection: $skillLevel) {
                    Text("Beginner").tag(PracticeRoutineGenerator.SkillLevel.beginner)
                    Text("Intermediate").tag(PracticeRoutineGenerator.SkillLevel.intermediate)
                    Text("Advanced").tag(PracticeRoutineGenerator.SkillLevel.advanced)
                }
                .pickerStyle(.segmented)
            }
            .padding(.horizontal)

            VStack(alignment: .leading, spacing: 8) {
                Text("Focus Areas")
                    .font(.headline)
                let areas: [PracticeRoutineGenerator.FocusArea] = [.chords, .scales, .ear, .theory, .songs]
                ForEach(areas, id: \.name) { area in
                    Toggle(area.label, isOn: Binding(
                        get: { focusAreas.contains(area) },
                        set: { isOn in
                            if isOn { focusAreas.insert(area) }
                            else { focusAreas.remove(area) }
                        }
                    ))
                }
            }
            .padding(.horizontal)

            Button("Generate Routine") {
                generateRoutine()
            }
            .buttonStyle(.borderedProminent)
            .disabled(focusAreas.isEmpty)

            if let routine {
                Divider()
                routineSummary(routine)

                Button("Start Routine") {
                    activeStep = 0
                    completedSteps = []
                    isActiveRoutine = true
                }
                .buttonStyle(.borderedProminent)
                .tint(.green)
            }
        }
    }

    private func routineSummary(_ routine: PracticeRoutine) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(routine.title)
                .font(.title3.weight(.bold))
            Text("\(routine.totalMinutes) minutes, \(routine.steps.asArray(of: PracticeStep.self).count) steps")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal)
    }

    // MARK: - Active Routine Phase

    @ViewBuilder
    private func activeRoutineContent(_ routine: PracticeRoutine) -> some View {
        let steps = routine.steps.asArray(of: PracticeStep.self)

        VStack(spacing: 12) {
            Text(routine.title)
                .font(.title3.weight(.bold))
                .padding(.horizontal)

            ProgressView(value: Double(completedSteps.count), total: Double(steps.count))
                .padding(.horizontal)
                .accessibilityLabel("Progress")
                .accessibilityValue("\(completedSteps.count) of \(steps.count) steps completed")

            Text("\(completedSteps.count) / \(steps.count) steps completed")
                .font(.caption)
                .foregroundStyle(.secondary)

            if completedSteps.count == steps.count {
                completionCard
            } else {
                ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                    stepCard(step, index: index, totalSteps: steps.count)
                }
            }
        }
    }

    private func stepCard(_ step: PracticeStep, index: Int, totalSteps: Int) -> some View {
        let isDone = completedSteps.contains(index)
        let isActive = index == activeStep && !isDone

        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(index + 1)")
                    .font(.caption.weight(.bold))
                    .frame(width: 24, height: 24)
                    .background(isDone ? Color.green : (isActive ? Color.accentColor : Color(.systemGray4)))
                    .foregroundStyle(.white)
                    .cornerRadius(12)

                VStack(alignment: .leading, spacing: 2) {
                    Text(step.title)
                        .font(.subheadline.weight(.medium))
                    Text("\(step.durationMinutes) min")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                if isDone {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                        .accessibilityLabel("completed")
                }
            }

            if isActive {
                Text(step.description_)
                    .font(.caption)
                    .foregroundStyle(.secondary)

                HStack {
                    Spacer()
                    Button("Done") {
                        completedSteps.insert(index)
                        if index + 1 < totalSteps {
                            activeStep = index + 1
                            while completedSteps.contains(activeStep) && activeStep + 1 < totalSteps {
                                activeStep += 1
                            }
                        }
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
        }
        .padding()
        .background(isActive ? Color.accentColor.opacity(0.08) : Color(.systemGray6))
        .cornerRadius(8)
        .opacity(isDone ? 0.6 : 1.0)
        .padding(.horizontal)
        .onTapGesture {
            if !isDone { activeStep = index }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Step \(index + 1): \(step.title), \(step.durationMinutes) minutes")
        .accessibilityValue(isDone ? "completed" : (isActive ? "current step" : ""))
    }

    private var completionCard: some View {
        VStack(spacing: 12) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 48))
                .foregroundStyle(.green)
            Text("Routine Complete!")
                .font(.title2.bold())
            Text("Great job finishing your practice session.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Button("Back to Setup") {
                isActiveRoutine = false
                routine = nil
            }
            .buttonStyle(.bordered)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }

    private func generateRoutine() {
        routine = PracticeRoutineGenerator.shared.generate(
            durationMinutes: Int32(durationMinutes),
            skillLevel: skillLevel,
            focusAreas: focusAreas
        )
    }
}

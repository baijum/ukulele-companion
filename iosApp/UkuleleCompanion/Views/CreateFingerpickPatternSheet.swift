import SwiftUI

struct CreateFingerpickPatternSheet: View {
    @ObservedObject var viewModel: CustomPatternsViewModel
    var initialPattern: CustomFingerpickingData? = nil
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var timeSignature = "4/4"
    @State private var steps: [FingerpickStepData] = []

    private let timeSignatures = ["4/4", "3/4", "6/8"]
    private let fingers = ["T", "I", "M", "R"]
    private let stringNames = ["G", "C", "E", "A"]

    private var isEditing: Bool { initialPattern != nil }

    var body: some View {
        NavigationStack {
            Form {
                Section("Pattern Name") {
                    TextField("Name", text: $name)
                }
                Section("Time Signature") {
                    Picker("Time Signature", selection: $timeSignature) {
                        ForEach(timeSignatures, id: \.self) { Text($0).tag($0) }
                    }
                    .pickerStyle(.segmented)
                }
                Section("Steps") {
                    ForEach(0..<steps.count, id: \.self) { i in
                        HStack {
                            Picker("Finger", selection: $steps[i].finger) {
                                ForEach(fingers, id: \.self) { Text($0).tag($0) }
                            }
                            .labelsHidden()
                            .frame(width: 60)
                            Picker("String", selection: $steps[i].stringIndex) {
                                ForEach(0..<stringNames.count, id: \.self) { j in
                                    Text(stringNames[j]).tag(j)
                                }
                            }
                            .labelsHidden()
                            .frame(width: 60)
                            Toggle("Accent", isOn: $steps[i].emphasis)
                        }
                    }
                    .onDelete { steps.remove(atOffsets: $0) }
                    Button("Add Step") {
                        steps.append(FingerpickStepData(finger: "T", stringIndex: 0, emphasis: false))
                    }
                }
            }
            .navigationTitle(isEditing ? "Edit Fingerpicking Pattern" : "New Fingerpicking Pattern")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let existing = initialPattern {
                            viewModel.deleteFingerpickingPattern(id: existing.id)
                        }
                        let pattern = CustomFingerpickingData(
                            id: initialPattern?.id ?? UUID().uuidString,
                            name: name,
                            steps: steps,
                            createdAt: initialPattern?.createdAt ?? Date().timeIntervalSince1970,
                            timeSignature: timeSignature
                        )
                        viewModel.saveFingerpickingPattern(pattern)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || steps.isEmpty)
                }
            }
            .onAppear {
                if let p = initialPattern {
                    name = p.name
                    timeSignature = p.timeSignature
                    steps = p.steps
                }
            }
        }
    }
}

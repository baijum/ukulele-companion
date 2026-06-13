import SwiftUI

struct CreateStrumPatternSheet: View {
    @ObservedObject var viewModel: CustomPatternsViewModel
    var initialPattern: CustomStrumPatternData? = nil
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var timeSignature = "4/4"
    @State private var beats: [StrumBeatData] = []

    private let timeSignatures = ["4/4", "3/4", "6/8"]
    private let directions = ["DOWN", "UP", "CHUCK", "MISS", "PAUSE"]

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
                Section("Beats") {
                    ForEach(0..<beats.count, id: \.self) { i in
                        HStack {
                            Picker("Direction", selection: $beats[i].direction) {
                                ForEach(directions, id: \.self) { Text($0).tag($0) }
                            }
                            .labelsHidden()
                            Toggle("Accent", isOn: $beats[i].emphasis)
                        }
                    }
                    .onDelete { beats.remove(atOffsets: $0) }
                    Button("Add Beat") {
                        beats.append(StrumBeatData(direction: "DOWN", emphasis: false))
                    }
                }
            }
            .navigationTitle(isEditing ? "Edit Strum Pattern" : "New Strum Pattern")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let existing = initialPattern {
                            viewModel.deleteStrumPattern(id: existing.id)
                        }
                        let pattern = CustomStrumPatternData(
                            id: initialPattern?.id ?? UUID().uuidString,
                            name: name,
                            beats: beats,
                            createdAt: initialPattern?.createdAt ?? Date().timeIntervalSince1970,
                            timeSignature: timeSignature
                        )
                        viewModel.saveStrumPattern(pattern)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || beats.isEmpty)
                }
            }
            .onAppear {
                if let p = initialPattern {
                    name = p.name
                    timeSignature = p.timeSignature
                    beats = p.beats
                }
            }
        }
    }
}

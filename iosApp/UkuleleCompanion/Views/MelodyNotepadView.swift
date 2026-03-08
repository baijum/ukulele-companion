import SwiftUI
import shared

struct MelodyNotepadView: View {
    @StateObject private var viewModel = MelodyViewModel()
    @State private var showingSaveDialog = false
    @State private var saveName = ""
    @State private var showingLoadSheet = false
    @State private var showingRenameDialog = false
    @State private var renameTarget: MelodyData? = nil
    @State private var renameName = ""
    @State private var showDiscardAlert = false
    @State private var pendingAction: (() -> Void)?

    var body: some View {
        VStack(spacing: 12) {
            modeToggle
            if viewModel.inputMode == .tap {
                tapInput
            } else {
                recordInput
            }
            notesList
            playbackControls
            persistenceControls
        }
        .padding()
        .navigationTitle("Melody Notepad")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Save Melody", isPresented: $showingSaveDialog) {
            TextField("Melody name", text: $saveName)
            Button("Save") {
                if !saveName.isEmpty {
                    viewModel.save(name: saveName)
                    saveName = ""
                }
            }
            Button("Cancel", role: .cancel) { saveName = "" }
        }
        .sheet(isPresented: $showingLoadSheet) {
            loadSheet
        }
        .alert("Unsaved Changes", isPresented: $showDiscardAlert) {
            Button("Discard", role: .destructive) {
                viewModel.hasUnsavedChanges = false
                pendingAction?()
                pendingAction = nil
            }
            Button("Cancel", role: .cancel) { pendingAction = nil }
        } message: {
            Text("You have unsaved changes. Discard them?")
        }
        .alert("Rename Melody", isPresented: $showingRenameDialog) {
            TextField("New name", text: $renameName)
            Button("Rename") {
                if let target = renameTarget, !renameName.isEmpty {
                    viewModel.renameMelody(id: target.id, newName: renameName)
                }
                renameTarget = nil
                renameName = ""
            }
            Button("Cancel", role: .cancel) {
                renameTarget = nil
                renameName = ""
            }
        }
    }

    private var modeToggle: some View {
        Picker("Input Mode", selection: $viewModel.inputMode) {
            Text("TAP").tag(MelodyInputMode.tap)
            Text("RECORD").tag(MelodyInputMode.record)
        }
        .pickerStyle(.segmented)
        .onChange(of: viewModel.inputMode) { _, newValue in
            if newValue == .tap && viewModel.isRecording {
                viewModel.stopRecording()
            }
        }
    }

    private var tapInput: some View {
        VStack(spacing: 8) {
            // Note buttons
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 6), spacing: 6) {
                ForEach(0..<12, id: \.self) { pc in
                    Button {
                        viewModel.addNote(pitchClass: pc, octave: viewModel.currentOctave)
                    } label: {
                        Text(viewModel.noteNames[pc])
                            .font(.caption.bold())
                            .frame(maxWidth: .infinity)
                            .frame(height: 36)
                            .background(Color(.systemGray5))
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(viewModel.noteNames[pc]), octave \(viewModel.currentOctave)")
                }
            }

            HStack {
                // Octave selector
                HStack(spacing: 4) {
                    Text("Octave:")
                        .font(.caption)
                    Stepper("\(viewModel.currentOctave)", value: $viewModel.currentOctave, in: 3...6)
                        .font(.caption)
                }

                Spacer()

                Button {
                    viewModel.addRest()
                } label: {
                    Label("Rest", systemImage: "pause.fill")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
            }

            // Duration selector
            HStack(spacing: 6) {
                ForEach(NoteDuration.allCases, id: \.self) { duration in
                    Button {
                        viewModel.selectedDuration = duration
                    } label: {
                        VStack(spacing: 2) {
                            Text(duration.rawValue)
                                .font(.title3)
                            Text(duration.label)
                                .font(.caption2)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                        .background(
                            viewModel.selectedDuration == duration
                                ? Color.accentColor.opacity(0.2)
                                : Color(.systemGray6)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(viewModel.selectedDuration == duration ? .isSelected : [])
                }
            }
        }
    }

    private var recordInput: some View {
        VStack(spacing: 12) {
            Button {
                if viewModel.isRecording {
                    viewModel.stopRecording()
                } else {
                    viewModel.startRecording()
                }
            } label: {
                Label(
                    viewModel.isRecording ? "Stop" : "Record",
                    systemImage: viewModel.isRecording ? "stop.circle.fill" : "mic.circle.fill"
                )
                .font(.title2)
                .foregroundStyle(viewModel.isRecording ? .red : .accentColor)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(viewModel.isRecording ? "Stop recording" : "Start recording")

            if viewModel.isRecording && viewModel.stabilizationProgress > 0 {
                ProgressView(value: viewModel.stabilizationProgress)
                    .tint(.accentColor)
                    .accessibilityValue("\(Int(viewModel.stabilizationProgress * 100))% stabilized")
            }

            if let note = viewModel.detectedNote {
                Text("Detected: \(note)")
                    .font(.headline)
                    .foregroundStyle(Color.accentColor)
                    .accessibilityAddTraits(.updatesFrequently)
            } else if viewModel.isRecording {
                Text("Listening...")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            // Duration selector for recording too
            HStack(spacing: 6) {
                ForEach(NoteDuration.allCases, id: \.self) { duration in
                    Button {
                        viewModel.selectedDuration = duration
                    } label: {
                        Text(duration.label)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(
                                viewModel.selectedDuration == duration
                                    ? Color.accentColor.opacity(0.2)
                                    : Color(.systemGray6)
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(viewModel.selectedDuration == duration ? .isSelected : [])
                }
            }
        }
        .padding(.vertical, 8)
    }

    private var notesList: some View {
        Group {
            if viewModel.notes.isEmpty {
                Text("No notes added yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, minHeight: 80)
            } else {
                noteChipsView
            }
        }
        .overlay(alignment: .top) {
            if let feedback = viewModel.lastAddedFeedback {
                Text(feedback)
                    .font(.caption.bold())
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.accentColor.opacity(0.9))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .animation(.easeInOut(duration: 0.3), value: viewModel.lastAddedFeedback)
            }
        }
    }

    private var noteChipsView: some View {
        ScrollView {
            let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 8)
            LazyVGrid(columns: columns, spacing: 4) {
                ForEach(0..<viewModel.notes.count, id: \.self) { i in
                    let note = viewModel.notes[i]
                    let isPlaying = viewModel.playingIndex == i
                    let isSelected = viewModel.selectedNoteIndex == i

                    ZStack(alignment: .topTrailing) {
                        VStack(spacing: 1) {
                            Text(note.displayName)
                                .font(.caption2.bold())
                            Text(note.duration.rawValue)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                        .background(
                            isPlaying ? Color.accentColor :
                            isSelected ? Color.accentColor.opacity(0.3) :
                            Color(.systemGray6)
                        )
                        .foregroundStyle(isPlaying ? .white : .primary)
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                        .onTapGesture { viewModel.selectNote(index: i) }
                        .accessibilityLabel("\(note.displayName), \(note.duration.label)")
                        .accessibilityAddTraits(isSelected ? .isSelected : [])

                        if isSelected {
                            Button { viewModel.deleteNoteAt(index: i) } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.caption2)
                                    .foregroundStyle(.white, .red)
                            }
                            .offset(x: 4, y: -4)
                            .accessibilityLabel("Delete \(note.displayName)")
                        }
                    }
                }
            }
        }
        .frame(minHeight: 80, maxHeight: 160)
    }

    private var playbackControls: some View {
        HStack {
            Button {
                if viewModel.isPlaying {
                    viewModel.stopPlayback()
                } else {
                    viewModel.playMelody()
                }
            } label: {
                Label(
                    viewModel.isPlaying ? "Stop" : "Play",
                    systemImage: viewModel.isPlaying ? "stop.fill" : "play.fill"
                )
            }
            .buttonStyle(.bordered)
            .disabled(viewModel.notes.isEmpty)

            Spacer()

            HStack(spacing: 4) {
                Text("BPM:")
                    .font(.caption)
                Slider(value: Binding(
                    get: { Double(viewModel.bpm) },
                    set: { viewModel.bpm = Int($0) }
                ), in: 40...200, step: 1)
                .frame(width: 100)
                Text("\(viewModel.bpm)")
                    .font(.caption.monospacedDigit())
                    .frame(width: 30)
            }
        }
    }

    private var persistenceControls: some View {
        HStack {
            Button("New") {
                guardUnsaved { viewModel.clearAll() }
            }
            .buttonStyle(.bordered)

            Button("Save") { showingSaveDialog = true }
                .buttonStyle(.bordered)
                .disabled(viewModel.notes.isEmpty)

            Button("Load") {
                guardUnsaved { showingLoadSheet = true }
            }
            .buttonStyle(.bordered)
            .disabled(viewModel.savedMelodies.isEmpty)

            Spacer()

            if let name = viewModel.loadedMelodyName {
                Text(name)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func guardUnsaved(_ action: @escaping () -> Void) {
        if viewModel.hasUnsavedChanges && !viewModel.notes.isEmpty {
            pendingAction = action
            showDiscardAlert = true
        } else {
            action()
        }
    }

    private var loadSheet: some View {
        NavigationStack {
            List {
                ForEach(viewModel.savedMelodies) { melody in
                    Button {
                        viewModel.load(melody: melody)
                        showingLoadSheet = false
                    } label: {
                        VStack(alignment: .leading) {
                            Text(melody.name)
                                .font(.subheadline.bold())
                            Text("\(melody.notes.count) notes, \(melody.bpm) BPM")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .buttonStyle(.plain)
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            viewModel.deleteMelody(id: melody.id)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        Button {
                            renameName = melody.name
                            renameTarget = melody
                            showingRenameDialog = true
                        } label: {
                            Label("Rename", systemImage: "pencil")
                        }
                        .tint(.orange)
                    }
                }
            }
            .navigationTitle("Load Melody")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showingLoadSheet = false }
                }
            }
        }
    }
}

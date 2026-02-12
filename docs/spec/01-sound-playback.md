# Feature: Sound Playback

**Status: IMPLEMENTED (v1.4)**

## Summary

Allow users to hear the notes of a chord by tapping a play button. This provides immediate auditory feedback and reinforces the connection between fret positions and the sounds they produce.

## Motivation

- Beginners often learn chords visually but struggle to connect shapes to sounds
- Hearing a chord helps users verify they selected the right voicing
- Audio feedback makes the app more engaging and educational
- No existing dependency on audio — this is a new capability

## Scope

### In Scope

- Play all selected notes as a strummed chord (notes staggered ~50ms apart)
- A play button next to the chord name in the result area
- Synthesized tones using sine wave generation (no audio sample files)
- Only selected strings are played — unselected strings are implicitly muted

### Out of Scope

- Recording or saving audio
- Microphone input / pitch detection
- Realistic instrument modeling (steel vs nylon, body resonance)
- Metronome or tempo features (could be a separate feature)
- Playing individual string notes on fret tap (future enhancement)
- Volume control or mute toggle (future enhancement)

## Technical Approach

### Option A: Sine Wave Synthesis (Recommended)

Use Android's `AudioTrack` API to generate sine waves at the correct frequencies for each note.

- **Pros**: Zero asset size, mathematically precise, fully offline
- **Cons**: Sounds synthetic, not like a real ukulele

**Key frequencies:**
- A4 = 440 Hz, then `frequency = 440 * 2^((pitchClass - 9) / 12)` adjusted for octave
- Each string's octave is known from the tuning (G4, C4, E4, A4)

### Option B: Short Audio Samples

Bundle short `.ogg` samples for each of the 12 pitch classes (or per-fret per-string for realism).

- **Pros**: Sounds more natural
- **Cons**: Increases APK size, need to source/record samples

### Recommended: Start with Option A

Sine wave synthesis keeps the app lightweight and offline. Can upgrade to samples later.

### Implementation Details

1. **New file**: `audio/ToneGenerator.kt`
   - Generate PCM audio data for a given frequency and duration
   - Mix multiple frequencies for chord playback
   - Handle `AudioTrack` lifecycle (create, play, release)

2. **ViewModel changes**: Add `playChord()` and `playNote(stringIndex, fret)` methods to `FretboardViewModel`

3. **UI changes**:
   - Add a play button (speaker icon) next to the chord result
   - Optionally play notes on fret tap (toggleable in settings)

4. **Threading**: Audio generation and playback must run on a background thread / coroutine

## Architecture

```
audio/
  └── ToneGenerator.kt    -- PCM generation & AudioTrack management

viewmodel/
  └── FretboardViewModel.kt  -- playChord(), playNote() methods

ui/
  └── ChordResultView.kt  -- play button
  └── FretboardView.kt    -- optional tap-to-play
```

## UX Design

- Play button: `Icons.Filled.PlayArrow` from Material Icons, placed next to chord name
- Brief tone duration: ~500ms per note, slight stagger for strum effect (~50ms between strings)
- Visual feedback: brief highlight animation on strings as they "play"
- Mute state persists across app sessions (DataStore preference)

## Muting Behavior

Strings are **implicitly muted** based on fret selection:
- Only strings with a selected fret are included in playback
- Unselected strings (shown as "x" in finger positions) produce no sound
- There is no explicit per-string mute toggle; muting is controlled entirely by whether a fret is selected on that string
- The strum timing adapts to the number of selected strings (e.g., 2 selected strings = 1 strum gap instead of 3)

## Edge Cases

- No notes selected → play button hidden (NoSelection state shows no play control)
- Rapid repeated taps → mutex ensures only one sound plays at a time (previous stops before new starts)
- App backgrounded during playback → coroutine cancellation stops audio
- Device on silent/vibrate → respects system volume settings (uses USAGE_MEDIA audio attributes)

## Effort Estimate

- **Complexity**: Medium
- **Estimated time**: 2–3 days
- **New files**: 1–2
- **Modified files**: 3–4
- **APK size impact**: Negligible (synthesis) or ~200KB (samples)

## Dependencies

- None (Android `AudioTrack` is part of the standard SDK)

## Testing

- Unit test: verify frequency calculation for all 12 pitch classes across octaves
- Unit test: PCM buffer generation produces correct sample count for duration
- Manual test: play each note and chord, verify correct pitch
- Manual test: rapid tapping doesn't crash or produce distortion
- Manual test: respects system volume and silent mode

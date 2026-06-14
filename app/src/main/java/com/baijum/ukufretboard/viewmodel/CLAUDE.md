# ViewModels (Android)

See canonical rule: `.cursor/rules/android-viewmodel.mdc`

Critical constraints:
- Expose UI state via `StateFlow`, never `LiveData`
- Repository access only — no direct SharedPreferences or UserDefaults calls
- Use `viewModelScope` for coroutines; cancel-safe by default

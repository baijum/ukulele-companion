---
name: add-translations
description: Add translated strings to Android and iOS locale files. Use when adding new user-facing strings, fixing MissingTranslation lint errors, or when the user asks to translate, localize, or add strings to all languages.
---

# Add Translations

Add new string resources to all supported locales for Android (`strings.xml`) and iOS (`Localizable.xcstrings`).

## Supported Locales

| Code | Language | Android path | iOS |
|------|----------|-------------|-----|
| en | English (source) | `app/src/main/res/values/strings.xml` | `Localizable.xcstrings` (sourceLanguage) |
| ar | Arabic | `values-ar/strings.xml` | `Localizable.xcstrings` → `ar` |
| zh-Hans | Chinese (Simplified) | `values-b+zh+Hans/strings.xml` | `Localizable.xcstrings` → `zh-Hans` |
| de | German | `values-de/strings.xml` | `Localizable.xcstrings` → `de` |
| es | Spanish | `values-es/strings.xml` | `Localizable.xcstrings` → `es` |
| fr | French | `values-fr/strings.xml` | `Localizable.xcstrings` → `fr` |
| hi | Hindi | `values-hi/strings.xml` | `Localizable.xcstrings` → `hi` |
| in | Indonesian | `values-in/strings.xml` | `Localizable.xcstrings` → `id` |
| it | Italian | `values-it/strings.xml` | `Localizable.xcstrings` → `it` |
| ja | Japanese | `values-ja/strings.xml` | `Localizable.xcstrings` → `ja` |
| ko | Korean | `values-ko/strings.xml` | `Localizable.xcstrings` → `ko` |
| nl | Dutch | `values-nl/strings.xml` | `Localizable.xcstrings` → `nl` |
| pl | Polish | `values-pl/strings.xml` | `Localizable.xcstrings` → `pl` |
| pt | Portuguese | `values-pt/strings.xml` | `Localizable.xcstrings` → `pt-BR` |
| ru | Russian | `values-ru/strings.xml` | `Localizable.xcstrings` → `ru` |
| tr | Turkish | `values-tr/strings.xml` | `Localizable.xcstrings` → `tr` |

All Android paths are relative to `app/src/main/res/`.

## Workflow

### Step 1: Identify strings to translate

Common triggers:
- Adding new UI text (buttons, labels, accessibility descriptions)
- `MissingTranslation` lint errors from `./gradlew lintDebug`
- New strings added to `values/strings.xml` without locale counterparts

To find missing strings, run:

```bash
./gradlew lintDebug 2>&1 | grep "MissingTranslation"
```

### Step 2: Add to English source (if not already present)

**Android:** Add `<string name="key">English text</string>` to `app/src/main/res/values/strings.xml`, grouped with related strings.

**iOS:** Strings in SwiftUI are auto-extracted into `Localizable.xcstrings` at build time. No manual English entry needed — just use `Text("English text")` or `LocalizedStringKey` in code.

### Step 3: Add translations to all 15 locale files

**Android:** For each locale `strings.xml`, add a `<string name="key">Translated text</string>` entry before `</resources>`.

Use a subagent (`model: fast`) to update all 15 files in parallel when adding multiple strings. Provide the subagent with:
1. The exact string key(s)
2. The translated text for each locale
3. An anchor string near the insertion point (a unique existing `<string>` near the end of each file)

Example subagent prompt pattern:

```
Add these strings to all 15 locale strings.xml files in
app/src/main/res/. Find the anchor string "existing_key" near
the end of each file and add the new strings after it, before
</resources>.

Translations:
- values-ar: <string name="my_key">Arabic text</string>
- values-de: <string name="my_key">German text</string>
...
```

**iOS:** `Localizable.xcstrings` is a large JSON file (100k+ lines). Do NOT edit it manually. iOS translations are managed via Xcode's String Catalog or by running `xcodebuild build` which auto-extracts new strings. Translations for iOS can be added by editing `Localizable.xcstrings` programmatically or through Xcode, but the recommended approach is:
1. Add the English string in SwiftUI code
2. Build the project to extract the key
3. Add translations via the xcstrings JSON structure if needed

### Step 4: Verify

```bash
./gradlew lintDebug
```

Lint must pass with 0 errors. Warnings are acceptable.

## Translation Guidelines

- Keep translations concise — UI space is limited, especially on mobile
- Preserve format specifiers exactly: `%1$d`, `%s`, `%1$@`, `%lld`, etc.
- Preserve XML entities: `&amp;`, `&lt;`, `\'`, etc.
- Do not translate brand names (e.g., "ChordPro", "Ukulele Companion")
- Accessibility strings (`cd_*` prefix) should be natural spoken language
- For technical terms with no good translation, keep the English term (e.g., "PDF", "BPM")

## Format Specifier Mapping (Android ↔ iOS)

| Android | iOS | Type |
|---------|-----|------|
| `%d`, `%1$d` | `%lld`, `%1$lld` | Integer |
| `%s`, `%1$s` | `%@`, `%1$@` | String |
| `%f`, `%1$f` | `%f`, `%1$f` | Float |
| `%%` | `%%` | Literal percent |

## Quick Reference

| Task | Command |
|------|---------|
| Check missing translations | `./gradlew lintDebug 2>&1 \| grep MissingTranslation` |
| Run full lint | `./gradlew lintDebug` |
| List all string keys | `grep 'name="' app/src/main/res/values/strings.xml` |
| Count keys per locale | `grep -c '<string' app/src/main/res/values-de/strings.xml` |

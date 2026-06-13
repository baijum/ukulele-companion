---
description: Add a user-facing string/plural to all 16 locales (Android + iOS)
argument-hint: [string name + English text, e.g. share_chord_hint "Share chord diagram"]
---

Add the following user-facing string(s) to **all** supported locales: **$ARGUMENTS**

Use the **add-translations** skill. Key points it covers:

- Android `strings.xml` (`app/src/main/res/values*/`) is the **source of truth**; the
  iOS `Localizable.xcstrings` is generated from it by `scripts/convert_strings.py`.
- Add the string to the English source plus all 16 locale variants
  (ar, zh-Hans, de, es, fr, hi, in, it, ja, ko, nl, pl, pt, ru, tr).
- Regenerate the iOS catalog with `scripts/convert_strings.py`.
- For UI-facing strings, also ensure any accessibility label/hint that needs it is
  added (accessibility is treated as seriously as functionality).
- Verify there are no `MissingTranslation` lint errors afterward (`./gradlew lintDebug`).

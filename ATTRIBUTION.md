# Attribution

## Audio Samples

**Ukulele single notes, close-mic**
- **Author:** [stomachache](https://freesound.org/people/stomachache/)
- **Source:** [Freesound.org — Pack #8545](https://freesound.org/people/stomachache/packs/8545/)
- **License:** [Creative Commons Attribution 3.0 Unported (CC BY 3.0)](https://creativecommons.org/licenses/by/3.0/)
- **Description:** Single ukulele notes recorded close-miked with Sony PCM-D50.
- **Files used:** All 12 chromatic notes (C through B) from the pack, converted to OGG preview format.

Per the CC BY 3.0 license, the original sounds were created by Freesound user
"stomachache" and are used with attribution as required.

## Machine Learning Model

**SwiftF0**
- **Author:** [lars76](https://github.com/lars76)
- **Source:** [github.com/lars76/swift-f0](https://github.com/lars76/swift-f0)
- **License:** [MIT License](https://opensource.org/licenses/MIT)
- **Description:** Lightweight monophonic pitch detection model used for neural pitch estimation in the chromatic tuner.
- **Files used:** `swift_f0_model.onnx` bundled in both Android assets and iOS resources.

## Runtime Dependencies

**ONNX Runtime**
- **Author:** [Microsoft](https://github.com/microsoft/onnxruntime)
- **Source:** [github.com/microsoft/onnxruntime](https://github.com/microsoft/onnxruntime)
- **License:** [MIT License](https://opensource.org/licenses/MIT)
- **Description:** Cross-platform inference engine used to run the SwiftF0 neural pitch detection model. Used as an Android library and via the C API on iOS.

**Reorderable**
- **Author:** [Calvin Liang](https://github.com/Calvin-LL)
- **Source:** [github.com/Calvin-LL/Reorderable](https://github.com/Calvin-LL/Reorderable)
- **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Description:** Compose Multiplatform library for drag-and-drop list reordering.

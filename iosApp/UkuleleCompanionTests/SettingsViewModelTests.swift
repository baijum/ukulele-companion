import XCTest
@testable import UkuleleCompanion

final class SettingsViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        let defaults = UserDefaults(suiteName: "app_settings")!
        defaults.removePersistentDomain(forName: "app_settings")
    }

    @MainActor
    func testDefaultValues() {
        let vm = SettingsViewModel()
        XCTAssertTrue(vm.soundEnabled)
        XCTAssertEqual(vm.volume, 1.0, accuracy: 0.01)
        XCTAssertEqual(vm.noteDurationMs, 600)
        XCTAssertEqual(vm.strumDelayMs, 50)
        XCTAssertFalse(vm.playOnTap)
        XCTAssertEqual(vm.themeMode, "System")
        XCTAssertTrue(vm.showTips)
        XCTAssertEqual(vm.selectedTuning, "High-G (Standard)")
        XCTAssertFalse(vm.precisionMode)
        XCTAssertFalse(vm.autoAdvance)
        XCTAssertEqual(vm.a4Reference, 440.0, accuracy: 0.1)
        XCTAssertFalse(vm.leftHanded)
        XCTAssertTrue(vm.showNoteNames)
        XCTAssertFalse(vm.allowMuted)
        XCTAssertEqual(vm.lastFret, 12)
        XCTAssertFalse(vm.onboardingCompleted)
    }

    @MainActor
    func testSaveAndLoad() {
        let vm = SettingsViewModel()
        vm.leftHanded = true
        vm.selectedTuning = "Low-G"
        vm.a4Reference = 442.0
        vm.lastFret = 15
        vm.save()

        let vm2 = SettingsViewModel()
        XCTAssertTrue(vm2.leftHanded)
        XCTAssertEqual(vm2.selectedTuning, "Low-G")
        XCTAssertEqual(vm2.a4Reference, 442.0, accuracy: 0.1)
        XCTAssertEqual(vm2.lastFret, 15)
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = SettingsViewModel()
        vm.soundEnabled = false
        vm.volume = 0.5
        vm.themeMode = "Dark"
        vm.precisionMode = true
        vm.save()

        let exported = vm.exportSettings()

        let defaults = UserDefaults(suiteName: "app_settings")!
        defaults.removePersistentDomain(forName: "app_settings")

        let vm2 = SettingsViewModel()
        vm2.importSettings(exported)

        XCTAssertFalse(vm2.soundEnabled)
        XCTAssertEqual(vm2.volume, 0.5, accuracy: 0.01)
        XCTAssertEqual(vm2.themeMode, "Dark")
        XCTAssertTrue(vm2.precisionMode)
    }

    @MainActor
    func testOnboardingFlag() {
        let defaults = UserDefaults(suiteName: "app_settings")!
        XCTAssertFalse(defaults.bool(forKey: "onboarding_completed"))

        defaults.set(true, forKey: "onboarding_completed")
        let vm = SettingsViewModel()
        XCTAssertTrue(vm.onboardingCompleted)
    }
}

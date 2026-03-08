import XCTest
@testable import UkuleleCompanion

final class PracticeTimerViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "practice_timer")
    }

    @MainActor
    func testDefaultValues() {
        let vm = PracticeTimerViewModel()
        XCTAssertEqual(vm.data.totalMinutes, 0)
        XCTAssertEqual(vm.data.totalSessions, 0)
        XCTAssertEqual(vm.data.longestSession, 0)
        XCTAssertEqual(vm.data.dailyGoal, 15)
        XCTAssertTrue(vm.data.dailyMinutes.isEmpty)
        XCTAssertEqual(vm.todayMinutes, 0)
        XCTAssertEqual(vm.dailyProgress, 0)
    }

    @MainActor
    func testRecordSession() {
        let vm = PracticeTimerViewModel()
        vm.recordSession(durationSeconds: 300)

        XCTAssertEqual(vm.data.totalMinutes, 5)
        XCTAssertEqual(vm.data.totalSessions, 1)
        XCTAssertEqual(vm.todayMinutes, 5)

        vm.recordSession(durationSeconds: 600)

        XCTAssertEqual(vm.data.totalMinutes, 15)
        XCTAssertEqual(vm.data.totalSessions, 2)
        XCTAssertEqual(vm.todayMinutes, 15)
    }

    @MainActor
    func testRecordSessionMinimumOneMinute() {
        let vm = PracticeTimerViewModel()
        vm.recordSession(durationSeconds: 10)

        XCTAssertEqual(vm.data.totalMinutes, 1)
        XCTAssertEqual(vm.data.totalSessions, 1)
    }

    @MainActor
    func testDailyProgress() {
        let vm = PracticeTimerViewModel()
        XCTAssertEqual(vm.dailyProgress, 0)

        vm.setDailyGoal(10)
        vm.recordSession(durationSeconds: 300)

        XCTAssertEqual(vm.dailyProgress, 0.5, accuracy: 0.01)

        vm.recordSession(durationSeconds: 300)

        XCTAssertEqual(vm.dailyProgress, 1.0, accuracy: 0.01)
    }

    @MainActor
    func testSetDailyGoalClamping() {
        let vm = PracticeTimerViewModel()

        vm.setDailyGoal(3)
        XCTAssertEqual(vm.data.dailyGoal, 5)

        vm.setDailyGoal(200)
        XCTAssertEqual(vm.data.dailyGoal, 120)

        vm.setDailyGoal(30)
        XCTAssertEqual(vm.data.dailyGoal, 30)
    }

    @MainActor
    func testTotalTimeFormatted() {
        let vm = PracticeTimerViewModel()

        XCTAssertEqual(vm.totalTimeFormatted, "0m")

        vm.recordSession(durationSeconds: 2700)
        XCTAssertEqual(vm.totalTimeFormatted, "45m")

        vm.recordSession(durationSeconds: 1200)
        XCTAssertEqual(vm.totalTimeFormatted, "1h 5m")
    }

    @MainActor
    func testLongestSession() {
        let vm = PracticeTimerViewModel()
        vm.recordSession(durationSeconds: 300)
        XCTAssertEqual(vm.data.longestSession, 5)

        vm.recordSession(durationSeconds: 900)
        XCTAssertEqual(vm.data.longestSession, 15)

        vm.recordSession(durationSeconds: 120)
        XCTAssertEqual(vm.data.longestSession, 15)
    }

    @MainActor
    func testPersistence() {
        let vm = PracticeTimerViewModel()
        vm.recordSession(durationSeconds: 600)
        vm.setDailyGoal(20)

        let vm2 = PracticeTimerViewModel()
        XCTAssertEqual(vm2.data.totalMinutes, 10)
        XCTAssertEqual(vm2.data.totalSessions, 1)
        XCTAssertEqual(vm2.data.dailyGoal, 20)
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = PracticeTimerViewModel()
        vm.recordSession(durationSeconds: 1200)
        vm.setDailyGoal(25)
        let exported = vm.exportData()

        UserDefaults.standard.removeObject(forKey: "practice_timer")

        let vm2 = PracticeTimerViewModel()
        XCTAssertEqual(vm2.data.totalMinutes, 0)

        vm2.importData(exported)
        XCTAssertEqual(vm2.data.totalMinutes, 20)
        XCTAssertEqual(vm2.data.totalSessions, 1)
        XCTAssertEqual(vm2.data.longestSession, 20)
        XCTAssertEqual(vm2.data.dailyGoal, 25)
    }
}

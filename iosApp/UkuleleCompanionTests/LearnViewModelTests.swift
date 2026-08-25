import XCTest
@testable import UkuleleCompanion
import shared

final class LearnViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        let defaults = UserDefaults(suiteName: "learn_progress")!
        defaults.removePersistentDomain(forName: "learn_progress")
    }

    @MainActor
    func testDefaultProgress() {
        let vm = LearnViewModel()
        XCTAssertEqual(vm.completedLessonCount(), 0)
        XCTAssertEqual(vm.passedQuizCount(), 0)
        XCTAssertEqual(vm.currentDayStreak(), 0)
        XCTAssertEqual(vm.bestDayStreak(), 0)
    }

    @MainActor
    func testMarkLessonCompleted() {
        let vm = LearnViewModel()
        XCTAssertFalse(vm.isLessonCompleted("lesson_1"))

        vm.markLessonCompleted("lesson_1")
        XCTAssertTrue(vm.isLessonCompleted("lesson_1"))
        XCTAssertFalse(vm.isLessonCompleted("lesson_2"))
    }

    @MainActor
    func testMarkLessonQuizPassed() {
        let vm = LearnViewModel()
        XCTAssertFalse(vm.isLessonQuizPassed("lesson_1"))

        vm.markLessonQuizPassed("lesson_1")
        XCTAssertTrue(vm.isLessonQuizPassed("lesson_1"))
    }

    @MainActor
    func testRecordQuizAnswerCorrect() {
        let vm = LearnViewModel()
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordQuizAnswer(category: .intervals, correct: false)

        let stats = vm.quizStats(category: .intervals)
        XCTAssertEqual(stats.total, 3)
        XCTAssertEqual(stats.correct, 2)
        XCTAssertEqual(stats.accuracyPercent, 66)
    }

    @MainActor
    func testQuizStatsOverall() {
        let vm = LearnViewModel()
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordQuizAnswer(category: .chords, correct: true)
        vm.recordQuizAnswer(category: .chords, correct: false)

        let overall = vm.quizStats()
        XCTAssertEqual(overall.total, 3)
        XCTAssertEqual(overall.correct, 2)

        let intervalsOnly = vm.quizStats(category: .intervals)
        XCTAssertEqual(intervalsOnly.total, 1)
        XCTAssertEqual(intervalsOnly.correct, 1)
    }

    @MainActor
    func testQuizBestStreak() {
        let vm = LearnViewModel()
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordQuizAnswer(category: .intervals, correct: false)
        vm.recordQuizAnswer(category: .intervals, correct: true)

        let stats = vm.quizStats(category: .intervals)
        XCTAssertEqual(stats.bestStreak, 3)
    }

    @MainActor
    func testRecordNoteQuizAnswer() {
        let vm = LearnViewModel()
        vm.recordNoteQuizAnswer(correct: true)
        vm.recordNoteQuizAnswer(correct: true)
        vm.recordNoteQuizAnswer(correct: false)

        let stats = vm.noteQuizStats()
        XCTAssertEqual(stats.total, 3)
        XCTAssertEqual(stats.correct, 2)
        XCTAssertEqual(stats.bestStreak, 2)
    }

    @MainActor
    func testRecordIntervalAnswer() {
        let vm = LearnViewModel()
        vm.recordIntervalAnswer(level: 1, correct: true)
        vm.recordIntervalAnswer(level: 1, correct: false)

        let stats = vm.intervalStats(level: 1)
        XCTAssertEqual(stats.total, 2)
        XCTAssertEqual(stats.correct, 1)
    }

    @MainActor
    func testRecordActivityStreakToday() {
        let vm = LearnViewModel()
        vm.recordActivity()

        XCTAssertEqual(vm.currentDayStreak(), 1)
        XCTAssertEqual(vm.bestDayStreak(), 1)

        vm.recordActivity()
        XCTAssertEqual(vm.currentDayStreak(), 1)
    }

    @MainActor
    func testUnlockAchievement() {
        let vm = LearnViewModel()
        XCTAssertTrue(vm.unlockedAchievementIds.isEmpty)

        vm.unlockAchievement("first_chord")
        XCTAssertTrue(vm.unlockedAchievementIds.contains("first_chord"))

        vm.unlockAchievement("first_chord")
        XCTAssertEqual(vm.unlockedAchievementIds.count, 1)
    }

    @MainActor
    func testClearAllProgress() {
        let vm = LearnViewModel()
        vm.markLessonCompleted("lesson_1")
        vm.recordQuizAnswer(category: .intervals, correct: true)

        vm.clearAllProgress()

        XCTAssertFalse(vm.isLessonCompleted("lesson_1"))
        XCTAssertEqual(vm.quizStats().total, 0)
    }

    // Reset All Progress must clear learning progress but keep unlocked
    // achievements, matching Android (where trophies live in a separate
    // store). See issue #607.
    @MainActor
    func testClearAllProgressKeepsUnlockedAchievements() {
        let vm = LearnViewModel()
        vm.markLessonCompleted("lesson_1")
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.unlockAchievement("first_chord")
        vm.unlockAchievement("test")

        vm.clearAllProgress()

        XCTAssertFalse(vm.isLessonCompleted("lesson_1"))
        XCTAssertEqual(vm.quizStats().total, 0)
        XCTAssertTrue(vm.unlockedAchievementIds.contains("first_chord"))
        XCTAssertTrue(vm.unlockedAchievementIds.contains("test"))
        XCTAssertEqual(vm.unlockedAchievementIds.count, 2)
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = LearnViewModel()
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordQuizAnswer(category: .intervals, correct: true)
        vm.recordNoteQuizAnswer(correct: true)
        vm.unlockAchievement("first_chord")
        let exported = vm.exportProgress()

        let defaults = UserDefaults(suiteName: "learn_progress")!
        defaults.removePersistentDomain(forName: "learn_progress")

        let vm2 = LearnViewModel()
        XCTAssertEqual(vm2.quizStats(category: .intervals).total, 0)

        vm2.importProgress(exported)
        XCTAssertEqual(vm2.quizStats(category: .intervals).total, 2)
        XCTAssertEqual(vm2.quizStats(category: .intervals).correct, 2)
        XCTAssertEqual(vm2.noteQuizStats().total, 1)
        XCTAssertTrue(vm2.unlockedAchievementIds.contains("first_chord"))
    }
}

import XCTest
import shared
@testable import UkuleleCompanion

/// Tests the app-wide achievement check that `ContentView` drives.
///
/// The point of moving this off the Achievements screen (issue #543) is that
/// progress made anywhere unlocks achievements, so these exercise the watcher
/// directly rather than through any view.
final class AchievementWatcherTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removePersistentDomain(forName: "learn_progress")
    }

    override func tearDown() {
        UserDefaults.standard.removePersistentDomain(forName: "learn_progress")
        super.tearDown()
    }

    @MainActor
    func testNothingUnlockedFromAStandingStart() {
        let learnVM = LearnViewModel()
        XCTAssertFalse(
            AchievementWatcher.unlockNewlyEarned(learnVM: learnVM, songsCount: 0, favoritesCount: 0)
        )
        XCTAssertTrue(learnVM.unlockedAchievementIds.isEmpty)
    }

    @MainActor
    func testCompletingALessonUnlocksWithoutTheAchievementsScreen() {
        let learnVM = LearnViewModel()
        learnVM.markLessonCompleted("notes_12")

        XCTAssertTrue(
            AchievementWatcher.unlockNewlyEarned(learnVM: learnVM, songsCount: 0, favoritesCount: 0)
        )
        XCTAssertTrue(learnVM.unlockedAchievementIds.contains("first_lesson"))
    }

    /// `unlockAchievement` bumps `stateVersion`, which re-fires the trigger in
    /// `ContentView`. The second pass has to come back empty or the view would
    /// loop rather than settle.
    @MainActor
    func testASecondPassFindsNothingAndSettles() {
        let learnVM = LearnViewModel()
        learnVM.markLessonCompleted("notes_12")

        XCTAssertTrue(
            AchievementWatcher.unlockNewlyEarned(learnVM: learnVM, songsCount: 0, favoritesCount: 0)
        )
        XCTAssertFalse(
            AchievementWatcher.unlockNewlyEarned(learnVM: learnVM, songsCount: 0, favoritesCount: 0)
        )
    }

    @MainActor
    func testSongsCountReachesTheSongbookAchievement() {
        let learnVM = LearnViewModel()

        XCTAssertTrue(
            AchievementWatcher.unlockNewlyEarned(learnVM: learnVM, songsCount: 1, favoritesCount: 0)
        )
        XCTAssertTrue(learnVM.unlockedAchievementIds.contains("first_song"))
    }

    @MainActor
    func testFavoritesCountReachesTheFavoritesAchievement() {
        let learnVM = LearnViewModel()

        XCTAssertTrue(
            AchievementWatcher.unlockNewlyEarned(learnVM: learnVM, songsCount: 0, favoritesCount: 5)
        )
        XCTAssertTrue(learnVM.unlockedAchievementIds.contains("fav_5"))
    }

    @MainActor
    func testContextCarriesTheCountsItIsGiven() {
        let learnVM = LearnViewModel()
        let context = AchievementWatcher.context(learnVM: learnVM, songsCount: 3, favoritesCount: 7)

        XCTAssertEqual(context.songsCount, 3)
        XCTAssertEqual(context.favoritesCount, 7)
    }
}

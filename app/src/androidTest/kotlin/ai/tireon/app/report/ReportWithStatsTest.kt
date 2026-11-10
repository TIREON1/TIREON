package ai.tireon.app.report

import ai.tireon.app.stats.StatsDisplay
import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.EventLine
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 리포트 화면에 통계를 넣었을 때 레이아웃이 성립하는지 확인한다.
 *
 * 리포트는 LazyColumn이고 통계 화면은 자체 세로 스크롤을 가진다. 둘을 그대로
 * 겹치면 LazyColumn이 자식에게 무한 높이를 주고, 그 안의 세로 스크롤이 측정에
 * 실패해 예외가 난다. 통계 기록이 있는 사용자만 겪는 문제라 빈 기록 테스트로는
 * 드러나지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class ReportWithStatsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun record() = DrivingSessionRecord(
        id = "r1",
        startedAtMs = 1_700_000_000_000L,
        durationSec = 120,
        distanceKm = 1.5f,
        score = 85,
        insufficientReason = null,
        eventCounts = mapOf("HARSH_BRAKE" to 1),
    )

    private fun statsDisplay() = StatsDisplay(
        hasStats = true,
        emptyMessage = "",
        averageText = "85",
        changeText = "지난주보다 5점 올랐어요",
        sessionText = "3회",
        eventText = "4회",
        distanceText = "12.5km",
        topEvents = listOf(EventLine(label = "급제동", count = 4)),
    )

    @Test
    fun 통계가_있어도_리포트_화면이_그려진다() {
        composeRule.setContent {
            ReportScreen(
                state = ReportUiState(records = listOf(record()), loading = false),
                statsDisplay = statsDisplay(),
            )
        }

        composeRule.onNodeWithText("주간 통계").assertIsDisplayed()
    }

    @Test
    fun 통계와_기록을_함께_스크롤한다() {
        composeRule.setContent {
            ReportScreen(
                state = ReportUiState(records = listOf(record()), loading = false),
                statsDisplay = statsDisplay(),
            )
        }

        composeRule.onNodeWithText("85점").assertIsDisplayed()
    }
}

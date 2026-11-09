package ai.tireon.app.report

import ai.tireon.core.report.EventLine
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 리포트 상세 화면이 근거를 제대로 보여주는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class ReportDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun detail(
        scoreText: String? = "85",
        scoreReason: String? = null,
        events: List<EventLine> = emptyList(),
        missionLabel: String? = null,
        missionDetail: String? = null,
    ) = ReportDetail(
        scoreText = scoreText,
        hasScore = scoreText != null,
        scoreReason = scoreReason,
        dateText = "2026-11-09 14:30",
        durationText = "2분 5초",
        distanceText = "1.5km",
        events = events,
        missionLabel = missionLabel,
        missionDetail = missionDetail,
    )

    @Test
    fun 점수와_기본_정보를_보여준다() {
        composeRule.setContent {
            ReportDetailScreen(detail = detail(), onBack = {})
        }

        composeRule.onNodeWithText("85").assertIsDisplayed()
        composeRule.onNodeWithText("2026-11-09 14:30").assertIsDisplayed()
        composeRule.onNodeWithText("2분 5초").assertIsDisplayed()
        composeRule.onNodeWithText("1.5km").assertIsDisplayed()
    }

    @Test
    fun 점수가_없으면_사유를_보여준다() {
        composeRule.setContent {
            ReportDetailScreen(
                detail = detail(
                    scoreText = null,
                    scoreReason = "1분 이상 주행하면 점수를 낼 수 있어요",
                ),
                onBack = {},
            )
        }

        composeRule.onNodeWithText("1분 이상 주행하면 점수를 낼 수 있어요").assertIsDisplayed()
    }

    @Test
    fun 이벤트_횟수를_보여준다() {
        composeRule.setContent {
            ReportDetailScreen(
                detail = detail(
                    events = listOf(
                        EventLine("급제동", 3),
                        EventLine("급가속", 1),
                    ),
                ),
                onBack = {},
            )
        }

        composeRule.onNodeWithText("급제동").assertIsDisplayed()
        composeRule.onNodeWithText("3회").assertIsDisplayed()
        composeRule.onNodeWithText("급가속").assertIsDisplayed()
        composeRule.onNodeWithText("1회").assertIsDisplayed()
    }

    @Test
    fun 이벤트가_없으면_안내를_보여준다() {
        composeRule.setContent {
            ReportDetailScreen(detail = detail(), onBack = {})
        }

        composeRule.onNodeWithText("기록된 이벤트가 없어요").assertIsDisplayed()
    }

    @Test
    fun 미션_결과를_보여준다() {
        composeRule.setContent {
            ReportDetailScreen(
                detail = detail(
                    missionLabel = "목표 달성",
                    missionDetail = "급가속 없이 출발했어요",
                ),
                onBack = {},
            )
        }

        composeRule.onNodeWithText("목표 달성").assertIsDisplayed()
        composeRule.onNodeWithText("급가속 없이 출발했어요").assertIsDisplayed()
    }

    @Test
    fun 뒤로_가기_버튼이_동작한다() {
        var backed = false
        composeRule.setContent {
            ReportDetailScreen(detail = detail(), onBack = { backed = true })
        }

        composeRule.onNodeWithText("← 목록으로").performClick()

        assertEquals(true, backed)
    }
}

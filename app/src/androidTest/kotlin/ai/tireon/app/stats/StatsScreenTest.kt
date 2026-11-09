package ai.tireon.app.stats

import ai.tireon.core.report.EventLine
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 통계 화면이 지표를 제대로 그리는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class StatsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun display(
        averageText: String? = "85",
        changeText: String? = "지난주보다 5점 올랐어요",
        sessionText: String? = "3회",
        eventText: String? = "5회",
        distanceText: String? = "12.5km",
        topEvents: List<EventLine> = emptyList(),
    ) = StatsDisplay(
        hasStats = true,
        emptyMessage = "",
        averageText = averageText,
        changeText = changeText,
        sessionText = sessionText,
        eventText = eventText,
        distanceText = distanceText,
        topEvents = topEvents,
    )

    @Test
    fun 평균_점수와_변화를_보여준다() {
        composeRule.setContent { StatsScreen(display = display()) }

        composeRule.onNodeWithText("85").assertIsDisplayed()
        composeRule.onNodeWithText("지난주보다 5점 올랐어요").assertIsDisplayed()
    }

    @Test
    fun 요약_지표를_보여준다() {
        composeRule.setContent { StatsScreen(display = display()) }

        composeRule.onNodeWithText("3회").assertIsDisplayed()
        composeRule.onNodeWithText("12.5km").assertIsDisplayed()
        composeRule.onNodeWithText("5회").assertIsDisplayed()
    }

    @Test
    fun 이벤트_종류별_횟수를_보여준다() {
        composeRule.setContent {
            StatsScreen(
                display = display(
                    topEvents = listOf(
                        EventLine("급제동", 3),
                        EventLine("급가속", 1),
                    ),
                ),
            )
        }

        composeRule.onNodeWithText("급제동").assertIsDisplayed()
        composeRule.onNodeWithText("급가속").assertIsDisplayed()
    }

    @Test
    fun 이벤트가_없으면_안내를_보여준다() {
        composeRule.setContent { StatsScreen(display = display(topEvents = emptyList())) }

        composeRule.onNodeWithText("기록된 이벤트가 없어요").assertIsDisplayed()
    }

    @Test
    fun 기록이_없으면_안내만_보여준다() {
        composeRule.setContent {
            StatsScreen(
                display = StatsDisplay.from(null, periodDays = 7),
            )
        }

        composeRule.onNodeWithText("최근 7일 기록이 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("주간 통계").assertIsDisplayed()
    }
}

package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertCountEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 리포트 화면의 미션별 성적 구역을 확인한다. */
@RunWith(AndroidJUnit4::class)
class MissionStatsSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun record(
        id: String,
        mission: String?,
        success: Boolean?,
    ) = DrivingSessionRecord(
        id = id,
        startedAtMs = 1_700_000_000_000L,
        durationSec = 60,
        distanceKm = 1f,
        score = 85,
        insufficientReason = null,
        eventCounts = emptyMap(),
        mission = mission,
        missionSuccess = success,
    )

    @Test
    fun 미션_기록이_없으면_구역을_그리지_않는다() {
        composeRule.setContent {
            MissionStatsSection(display = MissionStatsDisplay.from(emptyList()))
        }

        composeRule.onAllNodesWithText("미션별 성적").assertCountEquals(0)
    }

    @Test
    fun 미션_성적과_성공률을_보여준다() {
        composeRule.setContent {
            MissionStatsSection(
                display = MissionStatsDisplay.from(
                    listOf(
                        record("a", "SMOOTH_START", true),
                        record("b", "SMOOTH_START", false),
                    ),
                ),
            )
        }

        composeRule.onNodeWithText("미션별 성적").assertIsDisplayed()
        composeRule.onNodeWithText("부드러운 출발").assertIsDisplayed()
        composeRule.onNodeWithText("50%").assertIsDisplayed()
        composeRule.onNodeWithText("1번 성공 / 2번 시도").assertIsDisplayed()
    }

    @Test
    fun 어려운_미션을_안내한다() {
        composeRule.setContent {
            MissionStatsSection(
                display = MissionStatsDisplay.from(
                    listOf(
                        record("a", "SMOOTH_START", true),
                        record("b", "SMOOTH_STOP", false),
                        record("c", "SMOOTH_STOP", false),
                    ),
                ),
            )
        }

        composeRule.onNodeWithText("부드러운 정지 연습이 필요해요").assertIsDisplayed()
    }
}

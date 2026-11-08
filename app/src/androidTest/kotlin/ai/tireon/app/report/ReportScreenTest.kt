package ai.tireon.app.report

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.tireon.core.report.DrivingSessionRecord
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 리포트 화면이 미션 결과와 점수 없음 상태를 제대로 그리는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class ReportScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun record(
        mission: String? = null,
        success: Boolean? = null,
        detail: String? = null,
        score: Int? = 85,
    ) = DrivingSessionRecord(
        id = "r1",
        startedAtMs = 1_700_000_000_000L,
        durationSec = 120,
        distanceKm = 1.5f,
        score = score,
        insufficientReason = if (score == null) "DURATION_TOO_SHORT" else null,
        eventCounts = mapOf("HARSH_BRAKE" to 1),
        mission = mission,
        missionSuccess = success,
        missionDetail = detail,
    )

    @Test
    fun 기록이_없으면_안내_문구를_보여준다() {
        composeRule.setContent {
            ReportScreen(state = ReportUiState(records = emptyList(), loading = false))
        }

        composeRule.onNodeWithText("아직 주행 기록이 없어요").assertIsDisplayed()
    }

    @Test
    fun 점수와_이벤트_수를_보여준다() {
        composeRule.setContent {
            ReportScreen(
                state = ReportUiState(records = listOf(record()), loading = false),
            )
        }

        composeRule.onNodeWithText("85점").assertIsDisplayed()
        composeRule.onNodeWithText("기록된 이벤트 1건").assertIsDisplayed()
    }

    @Test
    fun 미션_성공을_표시한다() {
        composeRule.setContent {
            ReportScreen(
                state = ReportUiState(
                    records = listOf(
                        record(
                            mission = "SMOOTH_START",
                            success = true,
                            detail = "급가속 없이 출발했어요",
                        ),
                    ),
                    loading = false,
                ),
            )
        }

        composeRule.onNodeWithText("목표 달성").assertIsDisplayed()
        composeRule.onNodeWithText("급가속 없이 출발했어요").assertIsDisplayed()
    }

    @Test
    fun 미션_실패를_표시한다() {
        composeRule.setContent {
            ReportScreen(
                state = ReportUiState(
                    records = listOf(
                        record(
                            mission = "SMOOTH_STOP",
                            success = false,
                            detail = "급제동이 있었어요 (2 번)",
                        ),
                    ),
                    loading = false,
                ),
            )
        }

        composeRule.onNodeWithText("다시 해볼까요").assertIsDisplayed()
    }

    @Test
    fun 판정_불가는_달성_여부_없이_사유만_보여준다() {
        composeRule.setContent {
            ReportScreen(
                state = ReportUiState(
                    records = listOf(
                        record(
                            mission = "STEADY_SPEED",
                            success = null,
                            detail = "속도 신호를 받지 못했어요",
                        ),
                    ),
                    loading = false,
                ),
            )
        }

        composeRule.onAllNodesWithText("목표 달성").assertCountEquals(0)
        composeRule.onAllNodesWithText("다시 해볼까요").assertCountEquals(0)
        composeRule.onNodeWithText("속도 신호를 받지 못했어요").assertIsDisplayed()
    }

    @Test
    fun 점수가_없으면_안내_문구를_보여준다() {
        composeRule.setContent {
            ReportScreen(
                state = ReportUiState(records = listOf(record(score = null)), loading = false),
            )
        }

        composeRule.onNodeWithText("점수를 내지 못했어요").assertIsDisplayed()
    }
}

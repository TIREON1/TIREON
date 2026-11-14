package ai.tireon.app.report

import ai.tireon.app.ui.theme.TireonTheme
import ai.tireon.core.report.DrivingSessionRecord
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrivingMissionReportScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun drivingSummaryIsVisibleWithoutSuccessFlag() {
        show("SMOOTH_DRIVING", "급가속 1회, 급제동 0회", true)
        composeRule.onNodeWithText("주행 미션 요약").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("급가속 1회, 급제동 0회").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun missingBrakeReasonIsVisibleWithScore() {
        show("SMOOTH_DRIVING", "브레이크 신호를 받지 못했어요", false)
        composeRule.onNodeWithText("판정 보류").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("브레이크 신호를 받지 못했어요").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("수신한 신호에서 감지된 이벤트만 표시해요. 신호가 없었던 항목은 0회나 안전한 주행을 뜻하지 않아요.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun legacyPracticeInsufficientReasonIsVisible() {
        show("STEADY_SPEED", "속도 신호를 받지 못했어요", null)
        composeRule.onNodeWithText("판정 보류").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("속도 신호를 받지 못했어요").performScrollTo().assertIsDisplayed()
    }

    private fun show(mission: String, reason: String, evaluated: Boolean?) {
        val record = DrivingSessionRecord(
            id = "report-fixture", startedAtMs = 1000L, durationSec = 120, distanceKm = 1f,
            score = 100, insufficientReason = null, eventCounts = emptyMap(),
            mission = mission, missionDetail = reason, missionEvaluated = evaluated,
        )
        composeRule.setContent {
            TireonTheme { ReportDetailScreen(detail = ReportDetail.from(record), onBack = {}) }
        }
    }
}

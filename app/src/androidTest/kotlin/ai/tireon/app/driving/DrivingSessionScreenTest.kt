package ai.tireon.app.driving

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.tireon.core.mission.DrivingMission
import ai.tireon.pleos.restriction.DrivingRestriction
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 주행 세션 화면이 상태별로 올바르게 그려지는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class DrivingSessionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        state: DrivingSessionState,
        result: DrivingSessionResult? = null,
        onStart: () -> Unit = {},
        onStop: () -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            DrivingSessionScreen(
                state = state,
                result = result,
                onStart = onStart,
                onStop = onStop,
                onClose = onClose,
            )
        }
    }

    @Test
    fun 준비_상태는_설명과_시작_버튼을_보여준다() {
        setScreen(DrivingSessionState.initial())

        composeRule.onNodeWithText("주행 기록").assertIsDisplayed()
        composeRule.onNodeWithText("주행 기록 시작").assertIsDisplayed()
    }

    @Test
    fun 시작_버튼을_누르면_콜백이_불린다() {
        var started = false
        setScreen(DrivingSessionState.initial(), onStart = { started = true })

        composeRule.onNodeWithText("주행 기록 시작").performClick()

        assertTrue(started)
    }

    @Test
    fun 수집_중에는_아무것도_그리지_않는다() {
        val collecting = DrivingSessionState.initial()
            .start(DrivingRestriction.MINIMAL_UI)

        setScreen(collecting)

        composeRule.onAllNodesWithText("주행 기록").assertCountEquals(0)
        composeRule.onAllNodesWithText("주행 기록 시작").assertCountEquals(0)
    }

    @Test
    fun 완료_상태는_점수와_지표를_보여준다() {
        val finished = DrivingSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 100)
            .stop(DrivingRestriction.NONE)

        val result = DrivingSessionResult(
            scoreText = "88",
            message = "주행이 끝났어요",
            mission = DrivingMission.OVERALL,
            missionDetail = "급가속 1회, 급제동 2회, 급조향 0회, 방향지시등 0회",
            eventCount = 3,
        )

        setScreen(finished, result = result)

        composeRule.onNodeWithText("88").assertIsDisplayed()
        composeRule.onNodeWithText("주행이 끝났어요").assertIsDisplayed()
        composeRule.onNodeWithText("급가속 1회, 급제동 2회, 급조향 0회, 방향지시등 0회")
            .assertIsDisplayed()
        composeRule.onNodeWithText("기록된 이벤트 3건").assertIsDisplayed()
    }

    @Test
    fun 점수가_없는_완료_상태는_사유를_보여준다() {
        val finished = DrivingSessionState.initial()
            .start(DrivingRestriction.NONE)
            .stop(DrivingRestriction.NONE)

        val result = DrivingSessionResult(
            scoreText = null,
            message = "1분 이상 주행하면 점수를 낼 수 있어요",
            mission = null,
            missionDetail = null,
            eventCount = 0,
        )

        setScreen(finished, result = result)

        composeRule.onNodeWithText("1분 이상 주행하면 점수를 낼 수 있어요").assertIsDisplayed()
    }

    @Test
    fun 완료_화면에서_닫기_버튼이_동작한다() {
        var closed = false
        val finished = DrivingSessionState.initial()
            .start(DrivingRestriction.NONE)
            .stop(DrivingRestriction.NONE)

        setScreen(finished, onClose = { closed = true })

        composeRule.onNodeWithText("닫기").performClick()

        assertTrue(closed)
    }
}

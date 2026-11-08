package ai.tireon.app.home

import ai.tireon.app.ui.WindowLayout
import ai.tireon.core.mission.PracticeMission
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.tireon.app.driving.DrivingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 홈 화면의 추천 카드가 제대로 동작하는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class HomeRecommendTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        state: HomeUiState,
        onStartPractice: (PracticeMission) -> Unit = {},
    ) {
        composeRule.setContent {
            HomeScreen(
                state = state,
                driving = DrivingUiState(),
                layout = WindowLayout.COMPACT,
                onStartPractice = onStartPractice,
                onStartDriving = {},
                onRefresh = {},
            )
        }
    }

    @Test
    fun 추천이_없으면_카드를_보여주지_않는다() {
        setScreen(HomeUiState.empty())

        composeRule.onAllNodesWithText("추천 연습").assertCountEquals(0)
    }

    @Test
    fun 추천_미션과_이유를_보여준다() {
        setScreen(
            HomeUiState(
                scoreText = "85",
                statusText = "최근 운전 점수",
                hasScore = true,
                recommendedMission = PracticeMission.SMOOTH_STOP,
                recommendReason = "최근 급제동이(가) 3번 있었어요",
            ),
        )

        composeRule.onNodeWithText("추천 연습").assertIsDisplayed()
        composeRule.onNodeWithText("부드러운 정지").assertIsDisplayed()
        composeRule.onNodeWithText("최근 급제동이(가) 3번 있었어요").assertIsDisplayed()
    }

    @Test
    fun 추천_버튼을_누르면_해당_미션으로_시작한다() {
        var started: PracticeMission? = null
        setScreen(
            state = HomeUiState(
                scoreText = null,
                statusText = "아직 주행 기록이 없어요",
                hasScore = false,
                recommendedMission = PracticeMission.SMOOTH_STOP,
                recommendReason = "최근 급제동이(가) 3번 있었어요",
            ),
            onStartPractice = { started = it },
        )

        composeRule.onNodeWithText("추천 연습").assertIsDisplayed()

        // 추천 카드의 시작 버튼이 먼저 나온다
        composeRule.onAllNodesWithText("연습 시작")[0].performClick()

        assertEquals(PracticeMission.SMOOTH_STOP, started)
    }
}

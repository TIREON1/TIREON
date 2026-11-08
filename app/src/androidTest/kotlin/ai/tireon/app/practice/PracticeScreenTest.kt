package ai.tireon.app.practice

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.tireon.app.R
import ai.tireon.core.mission.PracticeMission
import ai.tireon.pleos.restriction.DrivingRestriction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 연습 화면을 실제 Compose 런타임에서 검증한다.
 *
 * 단위 테스트로는 상태 전이만 확인할 수 있고, 화면이 그 상태를 제대로 그리는지는
 * 알 수 없다. 터치 주입이 막힌 에뮬레이터에서도 이 방식은 동작한다.
 */
@RunWith(AndroidJUnit4::class)
class PracticeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val mission = MissionDisplay(
        mission = PracticeMission.SMOOTH_START,
        nameResId = R.string.mission_smooth_start_name,
        descriptionResId = R.string.mission_smooth_start_description,
    )

    private fun setScreen(
        state: PracticeSessionState,
        summary: PracticeSummary? = null,
        speedText: String? = null,
        onStart: () -> Unit = {},
        onStop: () -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            PracticeScreen(
                mission = mission,
                state = state,
                summary = summary,
                speedText = speedText,
                onStart = onStart,
                onStop = onStop,
                onClose = onClose,
            )
        }
    }

    @Test
    fun 준비_상태는_미션과_시작_버튼을_보여준다() {
        setScreen(PracticeSessionState.initial())

        composeRule.onNodeWithText("부드러운 출발").assertIsDisplayed()
        composeRule.onNodeWithText("연습 시작").assertIsDisplayed()
    }

    @Test
    fun 제한_상태에서는_속도만_보이고_버튼은_없다() {
        val running = PracticeSessionState.initial()
            .start(DrivingRestriction.MINIMAL_UI)
            .tick(DrivingRestriction.MINIMAL_UI, sampleCount = 3)

        setScreen(running, speedText = "42")

        composeRule.onNodeWithText("42").assertIsDisplayed()
        composeRule.onAllNodesWithText("연습 종료").assertCountEquals(0)
    }

    @Test
    fun 제한_상태에서_속도가_없으면_안내_문구를_보여준다() {
        val running = PracticeSessionState.initial()
            .start(DrivingRestriction.MINIMAL_UI)

        setScreen(running, speedText = null)

        composeRule.onNodeWithText("사용할 수 없음").assertIsDisplayed()
    }

    @Test
    fun 제한이_없으면_경과_시간과_종료_버튼을_보여준다() {
        val running = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 5)

        setScreen(running)

        composeRule.onNodeWithText("연습 종료").assertIsDisplayed()
    }

    @Test
    fun 완료_상태는_점수와_닫기_버튼을_보여준다() {
        val finished = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 30)
            .finish(DrivingRestriction.NONE)

        val summary = PracticeSummary(scoreText = "88", message = "연습이 끝났어요", eventCount = 1)

        setScreen(finished, summary = summary)

        composeRule.onNodeWithText("88").assertIsDisplayed()
        composeRule.onNodeWithText("연습이 끝났어요").assertIsDisplayed()
        composeRule.onNodeWithText("닫기").assertIsDisplayed()
    }

    @Test
    fun 미션을_달성하면_목표_달성을_보여준다() {
        val finished = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 30)
            .finish(DrivingRestriction.NONE)

        val summary = PracticeSummary(
            scoreText = "92",
            message = "연습이 끝났어요",
            eventCount = 0,
            missionSuccess = true,
            missionDetail = "급가속 없이 출발했어요",
        )

        setScreen(finished, summary = summary)

        composeRule.onNodeWithText("목표 달성").assertIsDisplayed()
        composeRule.onNodeWithText("급가속 없이 출발했어요").assertIsDisplayed()
    }

    @Test
    fun 미션을_달성하지_못하면_다시_하기를_권한다() {
        val finished = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 30)
            .finish(DrivingRestriction.NONE)

        val summary = PracticeSummary(
            scoreText = "70",
            message = "연습이 끝났어요",
            eventCount = 2,
            missionSuccess = false,
            missionDetail = "급가속이 있었어요 (2 번)",
        )

        setScreen(finished, summary = summary)

        composeRule.onNodeWithText("다시 해볼까요").assertIsDisplayed()
    }

    @Test
    fun 판정_불가면_달성_여부를_표시하지_않는다() {
        val finished = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .finish(DrivingRestriction.NONE)

        val summary = PracticeSummary(
            scoreText = null,
            message = "주행 데이터가 부족해요",
            eventCount = 0,
            missionSuccess = null,
            missionDetail = "속도 신호를 받지 못했어요",
        )

        setScreen(finished, summary = summary)

        composeRule.onAllNodesWithText("목표 달성").assertCountEquals(0)
        composeRule.onAllNodesWithText("다시 해볼까요").assertCountEquals(0)
        composeRule.onNodeWithText("속도 신호를 받지 못했어요").assertIsDisplayed()
    }

    @Test
    fun 저장에_실패하면_안내를_보여준다() {
        val finished = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .finish(DrivingRestriction.NONE)

        val summary = PracticeSummary(scoreText = "88", message = "연습이 끝났어요", eventCount = 0)

        composeRule.setContent {
            PracticeScreen(
                mission = mission,
                state = finished,
                summary = summary,
                saveStatus = ai.tireon.app.report.SaveStatus.from(
                    Result.failure(IllegalStateException("disk full")),
                ),
                speedText = null,
                onStart = {},
                onStop = {},
                onClose = {},
            )
        }

        composeRule.onNodeWithText("기록을 저장하지 못했어요").assertIsDisplayed()
    }

    @Test
    fun 점수가_없는_완료_상태는_사유를_보여준다() {
        val finished = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .finish(DrivingRestriction.NONE)

        val summary = PracticeSummary(
            scoreText = null,
            message = "1분 이상 연습하면 점수를 낼 수 있어요",
            eventCount = 0,
        )

        setScreen(finished, summary = summary)

        composeRule.onNodeWithText("1분 이상 연습하면 점수를 낼 수 있어요").assertIsDisplayed()
    }
}

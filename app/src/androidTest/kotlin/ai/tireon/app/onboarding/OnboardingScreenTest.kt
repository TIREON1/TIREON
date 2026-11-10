package ai.tireon.app.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 온보딩 화면을 넘기고 마치는 흐름을 확인한다. */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 첫_장을_보여준다() {
        composeRule.setContent {
            OnboardingScreen(onFinish = {})
        }

        composeRule.onNodeWithText("TIREON").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 4").assertIsDisplayed()
    }

    @Test
    fun 다음을_누르면_두_번째_장으로_간다() {
        composeRule.setContent {
            OnboardingScreen(onFinish = {})
        }

        composeRule.onNodeWithText("다음").performClick()

        composeRule.onNodeWithText("연습과 주행").assertIsDisplayed()
        composeRule.onNodeWithText("2 / 4").assertIsDisplayed()
    }

    @Test
    fun 마지막_장에서는_시작하기가_나온다() {
        composeRule.setContent {
            OnboardingScreen(onFinish = {})
        }

        repeat(3) { composeRule.onNodeWithText("다음").performClick() }

        composeRule.onNodeWithText("안전 고지").assertIsDisplayed()
        composeRule.onNodeWithText("시작하기").assertIsDisplayed()
    }

    @Test
    fun 시작하기를_누르면_마친다() {
        var finished = false
        composeRule.setContent {
            OnboardingScreen(onFinish = { finished = true })
        }

        repeat(3) { composeRule.onNodeWithText("다음").performClick() }
        composeRule.onNodeWithText("시작하기").performClick()

        assertTrue(finished)
    }

    @Test
    fun 건너뛰기를_누르면_마친다() {
        var finished = false
        composeRule.setContent {
            OnboardingScreen(onFinish = { finished = true })
        }

        composeRule.onNodeWithText("건너뛰기").performClick()

        assertTrue(finished)
    }

    @Test
    fun 다시_보기에서는_건너뛰기_대신_닫기가_나온다() {
        composeRule.setContent {
            OnboardingScreen(onFinish = {}, onClose = {})
        }

        repeat(3) { composeRule.onNodeWithText("다음").performClick() }

        composeRule.onNodeWithText("닫기").assertIsDisplayed()
        composeRule.onNodeWithText("건너뛰기").assertDoesNotExist()
    }
}

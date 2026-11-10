package ai.tireon.app.ui

import ai.tireon.core.mission.DrivingMission
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 주행 미션 선택 화면이 미션을 제대로 전달하는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class DrivingMissionEntryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 미션_없이_주행_선택지를_보여준다() {
        composeRule.setContent {
            DrivingMissionEntryScreen(onSelect = {})
        }

        composeRule.onNodeWithText("미션 없이 주행").assertIsDisplayed()
    }

    @Test
    fun 모든_주행_미션을_보여준다() {
        composeRule.setContent {
            DrivingMissionEntryScreen(onSelect = {})
        }

        composeRule.onNodeWithText("부드러운 주행").assertIsDisplayed()
        composeRule.onNodeWithText("방향지시등 습관").assertIsDisplayed()
        composeRule.onNodeWithText("안정적인 조향").assertIsDisplayed()
        composeRule.onNodeWithText("전체 점검").assertIsDisplayed()
    }

    @Test
    fun 미션을_누르면_그_미션을_돌려준다() {
        var selected: DrivingMission? = null
        composeRule.setContent {
            DrivingMissionEntryScreen(onSelect = { selected = it })
        }

        composeRule.onNodeWithText("방향지시등 습관").performClick()

        assertEquals(DrivingMission.SIGNAL_HABIT, selected)
    }

    @Test
    fun 미션_없이_주행을_누르면_미션이_없다() {
        var called = false
        var selected: DrivingMission? = DrivingMission.OVERALL
        composeRule.setContent {
            DrivingMissionEntryScreen(
                onSelect = {
                    called = true
                    selected = it
                },
            )
        }

        composeRule.onNodeWithText("미션 없이 주행").performClick()

        assertEquals(true, called)
        assertNull(selected)
    }
}

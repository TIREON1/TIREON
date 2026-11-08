package ai.tireon.app.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 설정 화면이 정보와 안전 고지를 제대로 그리는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 앱_버전을_보여준다() {
        composeRule.setContent {
            SettingsScreen(
                info = SettingsInfo(
                    appVersion = "0.1.0",
                    vin = null,
                    vehicleAvailable = false,
                ),
            )
        }

        composeRule.onNodeWithText("버전: 0.1.0").assertIsDisplayed()
    }

    @Test
    fun 차량이_연결되면_상태와_VIN을_보여준다() {
        composeRule.setContent {
            SettingsScreen(
                info = SettingsInfo(
                    appVersion = "0.1.0",
                    vin = "KMHXX00XXXX000000",
                    vehicleAvailable = true,
                ),
            )
        }

        composeRule.onNodeWithText("연결 상태: 연결됨").assertIsDisplayed()
        composeRule.onNodeWithText("차대번호 (VIN): KMHXX00XXXX000000").assertIsDisplayed()
    }

    @Test
    fun 차량이_연결되지_않으면_안내를_보여준다() {
        composeRule.setContent {
            SettingsScreen(
                info = SettingsInfo(
                    appVersion = "0.1.0",
                    vin = null,
                    vehicleAvailable = false,
                ),
            )
        }

        composeRule.onNodeWithText("연결 상태: 연결되지 않음").assertIsDisplayed()
        composeRule.onNodeWithText("차대번호 (VIN): 사용할 수 없음").assertIsDisplayed()
    }

    @Test
    fun 안전_고지를_보여준다() {
        composeRule.setContent {
            SettingsScreen(
                info = SettingsInfo(appVersion = "0.1.0", vin = null, vehicleAvailable = false),
            )
        }

        composeRule.onNodeWithText("안전 고지").assertIsDisplayed()
        composeRule.onNodeWithText(
            "• TIREON의 코칭은 참고용 정보이며, 차량의 안전 시스템을 대체하지 않습니다.",
        ).assertIsDisplayed()
    }
}

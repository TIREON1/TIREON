package ai.tireon.app.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 설정 화면에 표시할 정보.
 *
 * 차량 정보는 SDK에서 오지만, 설정 화면은 값이 없을 때도 안내를 보여줘야 한다.
 */
class SettingsInfoTest {

    @Test
    fun `값이 있으면 그대로 표시한다`() {
        val info = SettingsInfo(
            appVersion = "0.1.0",
            vin = "KMHXX00XXXX000000",
            vehicleAvailable = true,
        )

        assertEquals("0.1.0", info.appVersion)
        assertEquals("KMHXX00XXXX000000", info.vin)
        assertTrue(info.vehicleAvailable)
    }

    @Test
    fun `차량 정보가 없으면 안내 문구를 쓴다`() {
        val info = SettingsInfo(appVersion = "0.1.0", vin = null, vehicleAvailable = false)

        assertEquals("사용할 수 없음", info.vinText)
    }

    @Test
    fun `VIN이 비어 있으면 안내 문구를 쓴다`() {
        val info = SettingsInfo(appVersion = "0.1.0", vin = "  ", vehicleAvailable = true)

        assertEquals("사용할 수 없음", info.vinText)
    }

    @Test
    fun `차량 연결 상태를 문구로 만든다`() {
        val connected = SettingsInfo(appVersion = "0.1.0", vin = null, vehicleAvailable = true)
        val disconnected = SettingsInfo(appVersion = "0.1.0", vin = null, vehicleAvailable = false)

        assertEquals("연결됨", connected.connectionText)
        assertEquals("연결되지 않음", disconnected.connectionText)
    }

    @Test
    fun `안전 고지 항목이 비어 있지 않다`() {
        val notices = SettingsInfo.safetyNotices()

        assertTrue(notices.isNotEmpty())
        assertTrue(notices.all { it.isNotBlank() })
    }

    @Test
    fun `안전 고지에 면책 문구가 포함된다`() {
        val notices = SettingsInfo.safetyNotices().joinToString(" ")

        assertTrue(notices.contains("참고"))
        assertTrue(notices.contains("안전"))
    }
}

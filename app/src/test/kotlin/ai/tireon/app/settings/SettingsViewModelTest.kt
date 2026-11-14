package ai.tireon.app.settings

import ai.tireon.pleos.vehicle.VehicleInfoSource
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 화면 재진입 시 차량 정보를 다시 읽고 늦은 응답을 버린다.
 *
 * Pleos SDK 콜백은 비동기이므로, 생성 직후 요청과 재진입 요청이 겹칠 수 있다.
 */
class SettingsViewModelTest {

    private class FakeSource : VehicleInfoSource {
        val pending = mutableListOf<(Boolean, String?) -> Unit>()

        override fun readInfo(onResult: (Boolean, String?) -> Unit) {
            pending += onResult
        }

        override fun release() = Unit
    }

    @Test
    fun `생성 직후 요청 응답을 반영한다`() {
        val source = FakeSource()
        val viewModel = SettingsViewModel(appVersion = "1.1.1", infoSource = source)

        source.pending[0](true, "VIN-1")

        assertEquals(true, viewModel.info.value.vehicleAvailable)
        assertEquals("VIN-1", viewModel.info.value.vin)
    }

    @Test
    fun `재진입 전 요청의 늦은 응답은 버린다`() {
        val source = FakeSource()
        val viewModel = SettingsViewModel(appVersion = "1.1.1", infoSource = source)
        val firstRequest = source.pending[0]

        viewModel.load()
        source.pending[1](false, null)
        // 첫 요청이 뒤늦게 도착해도 최신 상태를 덮지 않는다.
        firstRequest(true, "VIN-OLD")

        assertEquals(false, viewModel.info.value.vehicleAvailable)
        assertEquals(null, viewModel.info.value.vin)
    }

    @Test
    fun `가장 최근 응답만 반영한다`() {
        val source = FakeSource()
        val viewModel = SettingsViewModel(appVersion = "1.1.1", infoSource = source)

        viewModel.load()
        viewModel.load()
        source.pending[1](true, "VIN-2")
        source.pending[2](true, "VIN-3")

        assertEquals("VIN-3", viewModel.info.value.vin)
    }
}

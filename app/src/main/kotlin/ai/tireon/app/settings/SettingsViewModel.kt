package ai.tireon.app.settings

import ai.tireon.pleos.vehicle.VehicleInfoSource
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 설정 화면 ViewModel. SDK는 [VehicleInfoSource] 뒤에 숨어 있다. */
class SettingsViewModel(
    private val appVersion: String,
    private val infoSource: VehicleInfoSource,
) : ViewModel() {

    private val _info = MutableStateFlow(
        SettingsInfo(appVersion = appVersion, vin = null, vehicleAvailable = false),
    )
    val info: StateFlow<SettingsInfo> = _info.asStateFlow()

    init {
        load()
    }

    private var requestId = 0

    /** 화면에 다시 들어올 때 차량 정보를 갱신한다. 늦게 온 이전 응답은 버린다. */
    fun load() {
        val id = ++requestId
        infoSource.readInfo { connected, vin ->
            if (id != requestId) return@readInfo
            _info.value = _info.value.copy(
                vehicleAvailable = connected,
                vin = vin,
            )
        }
    }

    override fun onCleared() {
        infoSource.release()
        super.onCleared()
    }

    /** @param infoSource ViewModel을 만들 때만 호출해 차량 세션을 연다. */
    class Factory(
        private val appVersion: String,
        private val infoSource: () -> VehicleInfoSource,
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(appVersion, infoSource()) as T
    }
}

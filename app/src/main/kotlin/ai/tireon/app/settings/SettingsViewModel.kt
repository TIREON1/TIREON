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

    /** 이전 응답을 버리는 기준. 선언을 init보다 앞에 두어 초기화 순서에 기대지 않는다. */
    private var requestId = 0

    /** [requestId]는 콜백 스레드에서도 읽으므로 증감과 확인을 같은 락으로 묶는다. */
    private val requestLock = Any()

    init {
        load()
    }

    /** 화면에 다시 들어올 때 차량 정보를 갱신한다. 늦게 온 이전 응답은 버린다. */
    fun load() {
        val id = synchronized(requestLock) { ++requestId }
        infoSource.readInfo { connected, vin ->
            val isLatest = synchronized(requestLock) { id == requestId }
            if (!isLatest) return@readInfo
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

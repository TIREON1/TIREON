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

    fun load() {
        infoSource.readInfo { connected, vin ->
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

    class Factory(
        private val appVersion: String,
        private val infoSource: VehicleInfoSource,
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(appVersion, infoSource) as T
    }
}

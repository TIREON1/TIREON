package com.tireon.pleos

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tireon.pleos.vehicle.OdometerClient
import com.tireon.pleos.vehicle.VehicleResult
import com.tireon.pleos.vehicle.VehicleSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 주행 데이터 화면 상태.
 *
 * SDK 값과 오류를 구분해 보관하며, 값이 없을 때 0으로 대체하지 않는다.
 * 거리·시간의 단위는 Pleos SDK가 제공하지 않으므로 숫자만 보관한다.
 */
data class DrivingUiState(
    val initialized: Boolean = false,
    val distanceDriven: Float? = null,
    val tripDrivingTime: Int? = null,
    val message: String? = null,
)

/**
 * Pleos Vehicle SDK의 주행 데이터를 화면에 전달하는 ViewModel.
 *
 * SDK 세션은 화면 생명주기와 함께 생성·해제된다.
 * 비동기 콜백이 끝나기 전에 release를 호출하지 않도록 [onCleared]에서 해제한다.
 */
class DrivingViewModel(context: Context) : ViewModel() {

    private val session = VehicleSession(context)
    private val _uiState = MutableStateFlow(DrivingUiState())
    val uiState: StateFlow<DrivingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        when (val initialized = session.initialize()) {
            is VehicleResult.Success -> {
                _uiState.value = _uiState.value.copy(initialized = true, message = null)
                readDrivingData()
            }

            is VehicleResult.Failure -> {
                Log.e(TAG, "Vehicle SDK 초기화 실패: ${initialized.operation}", initialized.cause)
                _uiState.value = _uiState.value.copy(
                    initialized = false,
                    message = "차량 서비스에 연결하지 못했습니다.",
                )
            }

            is VehicleResult.Unsupported -> {
                _uiState.value = _uiState.value.copy(
                    initialized = false,
                    message = "차량 서비스를 사용할 수 없습니다.",
                )
            }
        }
    }

    private fun readDrivingData() {
        when (val odometer = session.odometer()) {
            is VehicleResult.Success -> {
                val client = OdometerClient(odometer.value)

                client.readCurrentDistanceDriven { result ->
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            distanceDriven = (result as? VehicleResult.Success)?.value,
                            message = result.failureMessage() ?: _uiState.value.message,
                        )
                        result.logIfFailed()
                    }
                }

                client.readTripCurrentDrivingTime { result ->
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            tripDrivingTime = (result as? VehicleResult.Success)?.value,
                            message = result.failureMessage() ?: _uiState.value.message,
                        )
                        result.logIfFailed()
                    }
                }
            }

            is VehicleResult.Failure -> {
                Log.e(TAG, "Odometer 획득 실패: ${odometer.operation}", odometer.cause)
                _uiState.value = _uiState.value.copy(message = "주행 데이터를 사용할 수 없습니다.")
            }

            is VehicleResult.Unsupported -> {
                _uiState.value = _uiState.value.copy(message = "주행 데이터를 사용할 수 없습니다.")
            }
        }
    }

    private fun VehicleResult<*>.logIfFailed() {
        when (this) {
            is VehicleResult.Failure -> Log.e(TAG, "Vehicle API 실패: $operation", cause)
            is VehicleResult.Unsupported -> Log.i(TAG, "Vehicle API 미지원: $operation ($detail)")
            is VehicleResult.Success -> Unit
        }
    }

    /** 미지원은 오류가 아니므로 값만 비워 두고, 실제 실패만 안내한다. */
    private fun VehicleResult<*>.failureMessage(): String? = when (this) {
        is VehicleResult.Success, is VehicleResult.Unsupported -> null
        is VehicleResult.Failure -> "주행 데이터를 불러오지 못했습니다."
    }

    override fun onCleared() {
        session.release()
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DrivingViewModel(context.applicationContext) as T
    }

    private companion object {
        const val TAG = "DrivingViewModel"
    }
}

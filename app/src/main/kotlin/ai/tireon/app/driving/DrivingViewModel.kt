package ai.tireon.app.driving

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ai.tireon.pleos.driving.DrivingData
import ai.tireon.pleos.driving.DrivingDataSource
import ai.tireon.pleos.driving.PleosDrivingDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 주행 데이터 화면 상태.
 *
 * 값이 없을 때 0으로 대체하지 않는다. 거리와 시간의 단위는 SDK가 제공하지 않아
 * 숫자만 보관한다.
 */
data class DrivingUiState(
    val distanceDriven: Float? = null,
    val tripDrivingTime: Int? = null,
    val message: String? = null,
)

/**
 * 주행 데이터를 화면에 전달하는 ViewModel.
 *
 * SDK는 [DrivingDataSource] 뒤에 숨어 있어 이 클래스는 SDK 타입을 모른다.
 */
class DrivingViewModel(
    private val dataSource: DrivingDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrivingUiState())
    val uiState: StateFlow<DrivingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        dataSource.read { data -> _uiState.value = data.toUiState() }
    }

    override fun onCleared() {
        dataSource.release()
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DrivingViewModel(PleosDrivingDataSource(context.applicationContext)) as T
    }

    private fun DrivingData.toUiState() = DrivingUiState(
        distanceDriven = distanceDriven,
        tripDrivingTime = tripDrivingTime,
        message = message,
    )
}

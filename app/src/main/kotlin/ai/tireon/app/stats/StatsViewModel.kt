package ai.tireon.app.stats

import ai.tireon.core.report.DrivingStats
import ai.tireon.core.report.SessionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 통계 화면 상태. */
data class StatsUiState(
    val display: StatsDisplay = StatsDisplay.from(null, periodDays = 7),
    val loading: Boolean = true,
)

class StatsViewModel(
    private val repository: SessionRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val records = repository.recent(FETCH_LIMIT)
            val stats = DrivingStats.from(records, nowMs = clock(), periodDays = DEFAULT_PERIOD_DAYS)
            _state.value = StatsUiState(
                display = StatsDisplay.from(stats, DEFAULT_PERIOD_DAYS),
                loading = false,
            )
        }
    }

    class Factory(private val repository: SessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(repository) as T
    }

    private companion object {
        /** 기간 비교에 필요한 만큼 넉넉히 읽는다. */
        const val FETCH_LIMIT = 100
        const val DEFAULT_PERIOD_DAYS = 7
    }
}

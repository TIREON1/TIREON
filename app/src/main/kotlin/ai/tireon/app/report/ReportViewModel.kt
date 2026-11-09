package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 리포트 화면 상태. */
data class ReportUiState(
    val records: List<DrivingSessionRecord> = emptyList(),
    val loading: Boolean = true,
    /** 선택한 기록. null이면 목록 화면이다. */
    val selected: DrivingSessionRecord? = null,
) {
    /** 상세로 볼 기록을 [ReportDetail]로 변환한 값. */
    val selectedDetail: ReportDetail?
        get() = selected?.let { ReportDetail.from(it) }
}

class ReportViewModel(
    private val repository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = ReportUiState(
                records = repository.recent(LIMIT),
                loading = false,
                selected = null,
            )
        }
    }

    fun select(record: DrivingSessionRecord) {
        _state.value = _state.value.copy(selected = record)
    }

    /** 목록으로 돌아간다. 목록은 그대로 유지한다. */
    fun clearSelection() {
        _state.value = _state.value.copy(selected = null)
    }

    class Factory(private val repository: SessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReportViewModel(repository) as T
    }

    private companion object {
        const val LIMIT = 50
    }
}

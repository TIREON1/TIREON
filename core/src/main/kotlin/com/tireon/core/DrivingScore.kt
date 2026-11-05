package com.tireon.core

/**
 * 운전 코칭 도메인 모델의 시작점.
 * 실제 지표와 규칙은 Pleos SDK 지원 범위가 확인된 뒤 추가한다.
 */
data class DrivingScore(
    val value: Int,
) {
    init {
        require(value in MIN..MAX) { "DrivingScore는 $MIN..$MAX 범위여야 합니다: $value" }
    }

    companion object {
        const val MIN = 0
        const val MAX = 100
    }
}

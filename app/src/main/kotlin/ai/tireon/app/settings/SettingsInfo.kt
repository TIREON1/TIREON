package ai.tireon.app.settings

/**
 * 설정 화면에 표시할 정보.
 *
 * 차량 정보는 SDK에서 오지만 값이 없을 수 있다. 없다고 0이나 빈 문자열을
 * 보여주지 않고 "사용할 수 없음"으로 안내한다.
 */
data class SettingsInfo(
    val appVersion: String,
    val vin: String?,
    val vehicleAvailable: Boolean,
) {
    /** VIN 표시 문구. 값이 없으면 안내 문구를 쓴다. */
    val vinText: String
        get() = vin?.takeIf { it.isNotBlank() } ?: UNAVAILABLE

    /** 차량 연결 상태 문구. */
    val connectionText: String
        get() = if (vehicleAvailable) "연결됨" else "연결되지 않음"

    companion object {
        private const val UNAVAILABLE = "사용할 수 없음"

        /**
         * 안전 고지.
         *
         * AI 코칭이 차량 안전 시스템을 대체하지 않는다는 점을 명시한다.
         * 이 문구는 심사와 사용자 신뢰 모두에 필요하다.
         */
        fun safetyNotices(): List<String> = listOf(
            "TIREON의 코칭은 참고용 정보이며, 차량의 안전 시스템을 대체하지 않습니다.",
            "주행 중에는 화면을 조작하지 마세요. 모든 조작은 정차 후에 하세요.",
            "측정된 값과 AI가 해석한 내용을 구분해 표시합니다.",
            "주행 데이터는 기기 내부에만 저장되며 외부로 전송하지 않습니다.",
            "안전 운전의 최종 판단과 책임은 운전자에게 있습니다.",
        )
    }
}

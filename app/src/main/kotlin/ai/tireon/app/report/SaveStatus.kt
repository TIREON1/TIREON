package ai.tireon.app.report

/**
 * 세션 저장 결과를 화면에 알리기 위한 상태.
 *
 * 저장 실패를 조용히 삼키면 사용자가 기록이 사라진 것을 모른다. 실패만 안내한다.
 */
data class SaveStatus(
    val message: String?,
) {
    companion object {
        private const val FAILED_MESSAGE = "기록을 저장하지 못했어요"

        fun idle(): SaveStatus = SaveStatus(message = null)

        fun from(result: Result<Unit>): SaveStatus = SaveStatus(
            message = if (result.isSuccess) null else FAILED_MESSAGE,
        )
    }
}

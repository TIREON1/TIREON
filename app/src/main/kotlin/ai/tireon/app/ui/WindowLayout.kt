package ai.tireon.app.ui

/** 창 폭에 따른 레이아웃 형태. */
enum class WindowLayout { COMPACT, EXPANDED }

/**
 * 창 폭으로 레이아웃을 정한다.
 *
 * `WindowSizeClass`를 직접 쓰지 않고 폭만 받는 이유는 JVM에서 시험하기 위함이다.
 * 600dp 이상이면 두 열을 쓸 수 있는 것으로 본다.
 */
object WindowLayoutPolicy {
    private const val EXPANDED_MIN_WIDTH_DP = 600

    fun of(widthDp: Int): WindowLayout =
        if (widthDp >= EXPANDED_MIN_WIDTH_DP) WindowLayout.EXPANDED else WindowLayout.COMPACT
}

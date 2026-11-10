package ai.tireon.app.onboarding

import ai.tireon.app.R
import androidx.annotation.StringRes

/** 온보딩 한 장. */
data class OnboardingPage(
    @StringRes val titleResId: Int,
    @StringRes val bodyResId: Int,
    /** 안전 고지 장인지. 마지막 장에만 참이다. */
    val isSafetyNotice: Boolean = false,
)

/** 온보딩을 마친 결과. */
data class OnboardingExit(val needsOnboarding: Boolean)

/**
 * 온보딩 구성과 진행.
 *
 * 처음 쓰는 사람이 앱을 이해하도록 돕는다. 4장을 넘기지 않는다.
 * 길면 읽지 않는다.
 *
 * 화면 문구는 리소스에 있고, 진행 규칙만 여기 둔다. JVM에서 시험하기 위함이다.
 */
object Onboarding {

    val pages: List<OnboardingPage> = listOf(
        OnboardingPage(
            titleResId = R.string.onboarding_intro_title,
            bodyResId = R.string.onboarding_intro_body,
        ),
        OnboardingPage(
            titleResId = R.string.onboarding_modes_title,
            bodyResId = R.string.onboarding_modes_body,
        ),
        OnboardingPage(
            titleResId = R.string.onboarding_driving_title,
            bodyResId = R.string.onboarding_driving_body,
        ),
        OnboardingPage(
            titleResId = R.string.onboarding_safety_title,
            bodyResId = R.string.onboarding_safety_body,
            isSafetyNotice = true,
        ),
    )

    /** 첫 장에서는 뒤로 갈 곳이 없다. */
    fun canGoBack(index: Int): Boolean = index > 0

    /** 마지막 장이면 "다음" 대신 "시작하기"를 보여준다. */
    fun isLast(index: Int): Boolean = index >= pages.lastIndex

    /** 범위를 벗어난 위치를 목록 안으로 당긴다. */
    fun clamp(index: Int): Int = index.coerceIn(0, pages.lastIndex)

    /**
     * 온보딩을 마칠 때의 결과.
     *
     * 건너뛰기도 완료로 본다. 강제로 읽게 하면 두 번째 실행부터 짜증이 난다.
     */
    fun exit(completed: Boolean, skipped: Boolean): OnboardingExit =
        OnboardingExit(needsOnboarding = !(completed || skipped))
}

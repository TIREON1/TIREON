package ai.tireon.app.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 온보딩 장 구성과 진행.
 *
 * 처음 쓰는 사람이 앱을 이해하도록 돕는 화면이다. 4장을 넘기지 않는다.
 * 길면 읽지 않는다.
 */
class OnboardingTest {

    @Test
    fun `장이 네 개다`() {
        assertEquals(4, Onboarding.pages.size)
    }

    @Test
    fun `모든 장에 제목과 내용이 있다`() {
        Onboarding.pages.forEach { page ->
            assertTrue(page.titleResId != 0, "제목이 없는 장: $page")
            assertTrue(page.bodyResId != 0, "내용이 없는 장: $page")
        }
    }

    @Test
    fun `장이 중복되지 않는다`() {
        val ids = Onboarding.pages.map { it.titleResId }

        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `마지막 장은 안전 고지다`() {
        // 안전 고지는 가장 중요한 내용이라 마지막에 둔다. 마지막 장을 읽고
        // 시작하기를 누르므로 기억에 남는다.
        val last = Onboarding.pages.last()

        assertEquals(true, last.isSafetyNotice)
    }

    @Test
    fun `첫 장에서는 이전으로 갈 수 없다`() {
        assertTrue(!Onboarding.canGoBack(0))
    }

    @Test
    fun `마지막 장에서는 다음 대신 시작하기다`() {
        val lastIndex = Onboarding.pages.lastIndex

        assertTrue(Onboarding.isLast(lastIndex))
        assertTrue(!Onboarding.isLast(0))
    }

    @Test
    fun `장 범위를 벗어난 위치는 마지막으로 당긴다`() {
        assertEquals(0, Onboarding.clamp(-1))
        assertEquals(Onboarding.pages.lastIndex, Onboarding.clamp(99))
        assertEquals(2, Onboarding.clamp(2))
    }

    @Test
    fun `건너뛰기는 완료로 본다`() {
        // 강제로 읽게 하면 두 번째 실행부터 짜증이 난다.
        assertTrue(!Onboarding.exit(completed = false, skipped = true).needsOnboarding)
    }

    @Test
    fun `시작하기를 누르면 완료된다`() {
        // 시작하기는 완료로 저장된 뒤에 판정한다.
        assertTrue(!Onboarding.exit(completed = true, skipped = false).needsOnboarding)
    }

    @Test
    fun `이미 완료했으면 온보딩을 보여주지 않는다`() {
        assertTrue(!Onboarding.exit(completed = true, skipped = false).needsOnboarding)
    }

    @Test
    fun `처음 실행하면 온보딩을 보여준다`() {
        assertTrue(Onboarding.exit(completed = false, skipped = false).needsOnboarding)
    }
}

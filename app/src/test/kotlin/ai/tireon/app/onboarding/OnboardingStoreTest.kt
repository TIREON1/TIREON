package ai.tireon.app.onboarding

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 온보딩 완료 저장.
 *
 * 불리언 하나를 저장하는 데 DataStore를 끌어오지 않는다. SharedPreferences로
 * 충분하고, 저장 실패가 앱을 막지 않는다.
 */
class OnboardingStoreTest {

    private class FakeStore(private var completed: Boolean = false) : OnboardingStore {
        var writeCount = 0
            private set

        override fun isCompleted(): Boolean = completed

        override fun markCompleted() {
            completed = true
            writeCount++
        }
    }

    @Test
    fun `처음에는 완료되지 않은 상태다`() {
        assertFalse(FakeStore().isCompleted())
    }

    @Test
    fun `완료를 저장하면 다시 읽을 때 유지된다`() {
        val store = FakeStore()

        store.markCompleted()

        assertTrue(store.isCompleted())
    }

    @Test
    fun `완료 저장은 한 번만 하면 된다`() {
        val store = FakeStore()

        store.markCompleted()
        store.markCompleted()

        assertTrue(store.isCompleted())
    }
}

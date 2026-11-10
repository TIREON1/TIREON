package ai.tireon.app.onboarding

import android.content.Context

/**
 * 온보딩을 마쳤는지 저장한다.
 *
 * 불리언 하나를 저장하는 데 DataStore를 끌어오지 않는다. SharedPreferences로
 * 충분하다.
 */
interface OnboardingStore {

    fun isCompleted(): Boolean

    fun markCompleted()
}

/** SharedPreferences에 저장한다. */
class PrefsOnboardingStore(context: Context) : OnboardingStore {

    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    override fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    override fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private companion object {
        const val NAME = "tireon_onboarding"
        const val KEY_COMPLETED = "completed"
    }
}

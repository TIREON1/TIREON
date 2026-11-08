package ai.tireon.app.ui

import androidx.annotation.StringRes
import ai.tireon.app.R

/** 하단 또는 측면 내비게이션에 표시하는 목적지. */
enum class TireonDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    HOME("home", R.string.nav_home),
    PRACTICE("practice", R.string.nav_practice),
    REPORT("report", R.string.nav_report),
    SETTINGS("settings", R.string.nav_settings),
}

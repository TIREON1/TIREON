package ai.tireon.app.ui

import androidx.annotation.StringRes
import ai.tireon.app.R

/** 하단 또는 측면 내비게이션에 표시하는 목적지. */
enum class TireonDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val glyph: String,
) {
    HOME("home", R.string.nav_home, "H"),
    PRACTICE("practice", R.string.nav_practice, "P"),
    REPORT("report", R.string.nav_report, "R"),
    SETTINGS("settings", R.string.nav_settings, "S"),
}

package ai.tireon.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ai.tireon.app.R

/** 하단 또는 측면 내비게이션에 표시하는 목적지. */
enum class TireonDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME("home", R.string.nav_home, R.drawable.ic_home),
    PRACTICE("practice", R.string.nav_practice, R.drawable.ic_practice),
    REPORT("report", R.string.nav_report, R.drawable.ic_report),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_settings),
}

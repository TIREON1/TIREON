package ai.tireon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.tireon.app.R

/*
 * Pleos Connect UI Design Guide(document.pleos.ai, docs-design)의 토큰을 옮긴다.
 *
 * - 회색조: basic_00~900. 라이트와 다크에서 값이 서로 뒤집힌다.
 * - 정보 색: informative_active(초록), informative_positive(파랑), informative_negative(빨강)
 * - 주의 색: regulation_orange
 * - 텍스트: 라이트는 #131417, 다크는 #FFFFFF에 투명도 100/84/64/32%
 */

private object Basic {
    const val L00 = 0xFFFFFFFF
    const val L50 = 0xFFF7F8FA
    const val L100 = 0xFFEDEEF2
    const val L200 = 0xFFE0E1E6
    const val L300 = 0xFFC7C9CE
    const val L600 = 0xFF44464E

    const val D00 = 0xFF131417
    const val D50 = 0xFF1D1E21
    const val D100 = 0xFF313236
    const val D200 = 0xFF44464E
    const val D600 = 0xFFE0E1E6
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0082FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D2B4A),
    onPrimaryContainer = Color.White,
    // 채움 버튼(button_filled = basic_600)
    secondary = Color(Basic.D600),
    onSecondary = Color(Basic.D00),
    secondaryContainer = Color(Basic.D200),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFFF8A00),
    onTertiary = Color(Basic.D00),
    tertiaryContainer = Color(0xFF3D2A12),
    onTertiaryContainer = Color.White,
    error = Color(0xFFFF4C28),
    onError = Color.White,
    errorContainer = Color(0xFF3D1A14),
    onErrorContainer = Color.White,
    background = Color(Basic.D00),
    onBackground = Color.White,
    surface = Color(Basic.D00),
    onSurface = Color.White,
    surfaceVariant = Color(Basic.D100),
    // text_tertiary: 흰색 64%
    onSurfaceVariant = Color(0xA3FFFFFF),
    surfaceContainerLowest = Color(Basic.D00),
    surfaceContainerLow = Color(Basic.D50),
    surfaceContainer = Color(Basic.D50),
    surfaceContainerHigh = Color(Basic.D100),
    surfaceContainerHighest = Color(Basic.D200),
    outline = Color(Basic.D200),
    outlineVariant = Color(Basic.D100),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0064FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0ECFF),
    onPrimaryContainer = Color(0xFF0050CC),
    secondary = Color(Basic.L600),
    onSecondary = Color.White,
    secondaryContainer = Color(Basic.L200),
    onSecondaryContainer = Color(Basic.D00),
    tertiary = Color(0xFFFF8A00),
    onTertiary = Color(Basic.D00),
    tertiaryContainer = Color(0xFFFFEBD1),
    onTertiaryContainer = Color(0xFF5C3200),
    error = Color(0xFFFE3D16),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5DF),
    onErrorContainer = Color(0xFF8A1B05),
    background = Color(Basic.L50),
    onBackground = Color(Basic.D00),
    surface = Color(Basic.L50),
    onSurface = Color(Basic.D00),
    surfaceVariant = Color(Basic.L100),
    // text_tertiary: #131417 64%
    onSurfaceVariant = Color(0xA3131417),
    surfaceContainerLowest = Color(Basic.L00),
    surfaceContainerLow = Color(Basic.L00),
    surfaceContainer = Color(Basic.L00),
    surfaceContainerHigh = Color(Basic.L100),
    surfaceContainerHighest = Color(Basic.L200),
    outline = Color(Basic.L300),
    outlineVariant = Color(Basic.L200),
)

/** Material 색 체계에 없는 상태 색. 점수 구간과 판정 결과에 쓴다. */
@Immutable
data class TireonStatusColors(
    /** informative_active */
    val good: Color,
    /** regulation_orange */
    val caution: Color,
    /** informative_negative */
    val bad: Color,
)

private val DarkStatus = TireonStatusColors(
    good = Color(0xFF00E074),
    caution = Color(0xFFFF8A00),
    bad = Color(0xFFFF4C28),
)

private val LightStatus = TireonStatusColors(
    good = Color(0xFF02C265),
    caution = Color(0xFFFF8A00),
    bad = Color(0xFFFE3D16),
)

val LocalStatusColors = staticCompositionLocalOf { DarkStatus }

/** 가이드 권장 서체. 42dot이 만들어 SIL OFL로 공개했다(assets/licenses). */
@OptIn(ExperimentalTextApi::class)
private fun astaSans(weight: FontWeight) = Font(
    resId = R.font.asta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val AstaSans = FontFamily(
    astaSans(FontWeight.Normal),
    astaSans(FontWeight.Medium),
    astaSans(FontWeight.SemiBold),
    astaSans(FontWeight.Bold),
    astaSans(FontWeight.ExtraBold),
)

/**
 * 가이드의 글자 크기는 차량 디스플레이 px 기준이다(Headline 60/56/48, Title 40/36/32,
 * Body 30/28/26, Label 24/20). Pleos Connect 에뮬레이터 밀도에 맞춰 sp로 바꾼다.
 */
internal const val PLEOS_PX_TO_DP = 1f

private fun px(value: Int): TextUnit = (value * PLEOS_PX_TO_DP).sp

private fun style(size: Int, weight: FontWeight) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = weight,
    fontSize = px(size),
    lineHeight = px((size * 1.35f).toInt()),
)

private val TireonTypography = Typography(
    displayLarge = style(60, FontWeight.ExtraBold),
    displayMedium = style(48, FontWeight.Bold),
    displaySmall = style(40, FontWeight.Bold),
    headlineLarge = style(40, FontWeight.Bold),
    headlineMedium = style(36, FontWeight.Bold),
    headlineSmall = style(32, FontWeight.Bold),
    titleLarge = style(30, FontWeight.Bold),
    titleMedium = style(28, FontWeight.SemiBold),
    titleSmall = style(26, FontWeight.SemiBold),
    bodyLarge = style(26, FontWeight.Normal),
    bodyMedium = style(24, FontWeight.Normal),
    bodySmall = style(20, FontWeight.Normal),
    labelLarge = style(26, FontWeight.SemiBold),
    labelMedium = style(24, FontWeight.Medium),
    labelSmall = style(20, FontWeight.Medium),
)

// 버튼 규격: Large 100 / r24, Medium 80 / r16, Small 64 / r12 (px)
private val TireonShapes = Shapes(
    small = RoundedCornerShape((12 * PLEOS_PX_TO_DP).dp),
    medium = RoundedCornerShape((16 * PLEOS_PX_TO_DP).dp),
    large = RoundedCornerShape((24 * PLEOS_PX_TO_DP).dp),
)

@Composable
fun TireonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = TireonTypography,
            shapes = TireonShapes,
            content = content,
        )
    }
}

/**
 * 액티비티 최상위 배경.
 *
 * edge-to-edge로 그리므로 시스템 바 영역만큼 내용을 안쪽으로 민다.
 */
@Composable
fun TireonSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.safeDrawingPadding()) {
            content()
        }
    }
}

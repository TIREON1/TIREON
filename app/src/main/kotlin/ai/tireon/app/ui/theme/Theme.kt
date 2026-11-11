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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 브랜드 색은 런처 아이콘(#4ADE80, 배경 #101418)에서 가져온다.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF12391F),
    onPrimaryContainer = Color(0xFFA7F3C0),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF002F5C),
    secondaryContainer = Color(0xFF15304F),
    onSecondaryContainer = Color(0xFFD3E5FD),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF43360F),
    onTertiaryContainer = Color(0xFFFDE7A9),
    error = Color(0xFFF87171),
    onError = Color(0xFF490006),
    errorContainer = Color(0xFF4A1C1C),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0C1014),
    onBackground = Color(0xFFECF0F3),
    surface = Color(0xFF0C1014),
    onSurface = Color(0xFFECF0F3),
    surfaceVariant = Color(0xFF1D242B),
    onSurfaceVariant = Color(0xFF94A1AE),
    surfaceContainerLowest = Color(0xFF080B0E),
    surfaceContainerLow = Color(0xFF12171C),
    surfaceContainer = Color(0xFF161C22),
    surfaceContainerHigh = Color(0xFF1D242B),
    surfaceContainerHighest = Color(0xFF252D35),
    outline = Color(0xFF36414C),
    outlineVariant = Color(0xFF262F38),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF0B2A66),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF0F1418),
    surface = Color(0xFFF4F6F8),
    onSurface = Color(0xFF0F1418),
    surfaceVariant = Color(0xFFE6EAEE),
    onSurfaceVariant = Color(0xFF5A6672),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEF1F4),
    surfaceContainerHighest = Color(0xFFE6EAEE),
    outline = Color(0xFFC5CDD5),
    outlineVariant = Color(0xFFDDE2E7),
)

// 차량 화면은 팔 길이 거리에서 읽으므로 본문 크기를 기본값보다 키운다.
private val TireonTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontSize = 64.sp, lineHeight = 68.sp, fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 18.sp, lineHeight = 27.sp),
        bodyMedium = bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodySmall = bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    )
}

private val TireonShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TireonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TireonTypography,
        shapes = TireonShapes,
        content = content,
    )
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

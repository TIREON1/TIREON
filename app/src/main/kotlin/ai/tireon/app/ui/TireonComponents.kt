package ai.tireon.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.ui.theme.LocalStatusColors
import ai.tireon.core.analysis.Thresholds

/**
 * 화면 공통 치수.
 *
 * Pleos Connect 디스플레이는 2560x1440 mdpi(160dpi)라 가이드의 px 값이 곧 dp다.
 * 버튼은 가이드의 Medium 규격(높이 80, 모서리 16)을 쓴다.
 */
internal object TireonDimens {
    val ScreenPadding = 40.dp
    val CardPadding = 32.dp
    val Gap = 24.dp
    val ActionHeight = 80.dp

    /** 넓은 화면에서 한 열이 지나치게 늘어나지 않게 막는 폭. */
    val ReadableWidth = 960.dp
}

/** 점수 구간. 색만으로 구간을 전하지 않도록 화면은 숫자를 항상 함께 보여준다. */
internal enum class ScoreTone { GOOD, CAUTION, LOW, NONE }

private const val CAUTION_SCORE = 60

internal fun scoreToneOf(score: Int?): ScoreTone = when {
    score == null -> ScoreTone.NONE
    score >= Thresholds.GOOD_SCORE -> ScoreTone.GOOD
    score >= CAUTION_SCORE -> ScoreTone.CAUTION
    else -> ScoreTone.LOW
}

@Composable
internal fun ScoreTone.color(): Color = when (this) {
    ScoreTone.GOOD -> LocalStatusColors.current.good
    ScoreTone.CAUTION -> LocalStatusColors.current.caution
    ScoreTone.LOW -> LocalStatusColors.current.bad
    ScoreTone.NONE -> MaterialTheme.colorScheme.outline
}

/** 둥근 카드. [onClick]이 있으면 카드 전체가 터치 영역이다. */
@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    @DrawableRes titleIcon: Int? = null,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(TireonDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (titleIcon != null) {
                        Icon(
                            painter = painterResource(titleIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                }
            }
            content()
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = color,
            content = body,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = color,
            content = body,
        )
    }
}

/** 가운데 정렬된 한 열. 넓은 화면에서도 읽기 좋은 폭을 넘지 않는다. */
@Composable
internal fun CenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 내용이 짧으면 세로 가운데에, 길면 스크롤되게 한다.
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(TireonDimens.ScreenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TireonDimens.ReadableWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()
            }
        }
    }
}

/** 화면 제목과 보조 설명. */
@Composable
internal fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 점수 게이지.
 *
 * 점수가 없으면 숫자 대신 아이콘만 그린다. 0점과 "점수 없음"을 구분하기 위함이다.
 */
@Composable
internal fun ScoreRing(
    scoreText: String?,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    strokeWidth: Dp = 20.dp,
) {
    val score = scoreText?.toIntOrNull()
    val toneColor = scoreToneOf(score).color()
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            drawArc(
                color = trackColor,
                startAngle = RING_START,
                sweepAngle = RING_SWEEP,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            if (score != null) {
                drawArc(
                    color = toneColor,
                    startAngle = RING_START,
                    sweepAngle = RING_SWEEP * score.coerceIn(0, 100) / 100f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        if (scoreText != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = scoreText,
                    style = if (size >= LARGE_RING) {
                        MaterialTheme.typography.displayLarge
                    } else {
                        MaterialTheme.typography.displayMedium
                    },
                )
                Text(
                    text = "점",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_speed),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.28f),
            )
        }
    }
}

private val LARGE_RING = 200.dp
private const val RING_START = 135f
private const val RING_SWEEP = 270f

/** 라벨과 큰 숫자 한 쌍. */
@Composable
internal fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
    }
}

/** 왼쪽 라벨, 오른쪽 값. */
@Composable
internal fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

/** 0~1 비율 막대. 끝을 둥글게 그리고 트랙은 배경보다 한 단계 밝게 둔다. */
@Composable
internal fun MeterBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/**
 * 이벤트 종류별 횟수 막대. 가장 많은 항목을 기준으로 길이를 맞춘다.
 *
 * @param lines (라벨, 횟수) 목록. 이미 정렬된 순서로 그린다.
 */
@Composable
internal fun EventBars(
    lines: List<Pair<String, Int>>,
    countText: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val max = lines.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        lines.forEach { (label, count) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = countText(count), style = MaterialTheme.typography.bodyMedium)
                }
                MeterBar(
                    fraction = count.toFloat() / max,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

internal enum class ActionStyle { PRIMARY, SECONDARY, DANGER }

/** 큰 동작 버튼. 운전 중 오조작을 줄이기 위해 높이를 크게 둔다. */
@Composable
internal fun TireonActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    style: ActionStyle = ActionStyle.PRIMARY,
) {
    // Pleos 버튼 위계: 채움(button_filled = basic_600)을 주 동작에, 기본(basic_200)을 보조 동작에 쓴다.
    val colors = when (style) {
        ActionStyle.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        )
        ActionStyle.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        ActionStyle.DANGER -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = TireonDimens.ActionHeight),
        shape = MaterialTheme.shapes.medium,
        colors = colors,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 둥근 사각형 안의 아이콘. 미션과 메뉴의 얼굴 역할을 한다. */
@Composable
internal fun IconBadge(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** 상태 표시. 색만으로 뜻을 전하지 않도록 아이콘과 문구를 함께 쓴다. */
@Composable
internal fun StatusChip(
    text: String,
    @DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 내비게이션 상단의 브랜드 표시. */
@Composable
internal fun BrandMark(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "T",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * 최근 점수 막대 그래프. 한 계열이라 범례 없이 제목이 대상을 밝힌다.
 *
 * 글자는 캔버스에 직접 그린다. 접근성 트리에는 요약 문장 하나만 남겨
 * 막대마다 읽히지 않게 한다.
 *
 * @param scores 오래된 순서의 점수.
 */
@Composable
internal fun ScoreTrendChart(
    scores: List<Int>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val mutedBar = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val valueStyle = MaterialTheme.typography.labelLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val measurer = rememberTextMeasurer()
    val summary = "최근 점수 " + scores.joinToString(", ") + "점"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .semantics { contentDescription = summary },
    ) {
        val axisWidth = 48.dp.toPx()
        val topPad = 36.dp.toPx()
        val chartHeight = size.height - topPad
        val chartWidth = size.width - axisWidth

        listOf(0, 50, 100).forEach { mark ->
            val y = topPad + chartHeight * (1f - mark / 100f)
            drawLine(
                color = gridColor,
                start = Offset(axisWidth, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            val label = measurer.measure(mark.toString(), labelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(0f, y - label.size.height / 2f),
            )
        }

        if (scores.isEmpty()) return@Canvas
        val slot = chartWidth / scores.size
        val barWidth = minOf(slot * 0.56f, 40.dp.toPx())
        val radius = 6.dp.toPx()

        scores.forEachIndexed { index, score ->
            val value = score.coerceIn(0, 100)
            val barHeight = chartHeight * value / 100f
            val left = axisWidth + slot * index + (slot - barWidth) / 2f
            val top = topPad + chartHeight - barHeight
            val isLatest = index == scores.lastIndex
            val color = if (isLatest) barColor else mutedBar
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(radius, radius),
            )
            // 바닥 쪽은 기준선에 붙도록 각지게 덮는다.
            if (barHeight > radius) {
                drawRect(
                    color = color,
                    topLeft = Offset(left, top + barHeight - radius),
                    size = Size(barWidth, radius),
                )
            }
            if (isLatest) {
                val text = measurer.measure(score.toString(), valueStyle)
                drawText(
                    textLayoutResult = text,
                    topLeft = Offset(
                        left + barWidth / 2f - text.size.width / 2f,
                        top - text.size.height - 2.dp.toPx(),
                    ),
                )
            }
        }
    }
}

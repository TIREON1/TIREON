package ai.tireon.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.ui.ActionStyle
import ai.tireon.app.ui.BrandMark
import ai.tireon.app.ui.IconBadge
import ai.tireon.app.ui.TireonActionButton
import ai.tireon.app.ui.TireonDimens

/**
 * 온보딩 화면.
 *
 * 4장을 한 장씩 넘긴다. 마지막 장이 안전 고지이고, 거기서 "시작하기"를 누른다.
 * 설정에서 다시 열면 "닫기"로 바뀐다.
 *
 * @param onFinish 온보딩을 마쳤다. 건너뛰기도 여기로 온다.
 * @param onClose 다시 보기로 열렸을 때 닫는다. 완료 상태를 다시 쓰지 않는다.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val page = Onboarding.pages[Onboarding.clamp(index)]
    val isLast = Onboarding.isLast(index)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TireonDimens.ScreenPadding),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (onClose == null) {
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (index == 0) {
                BrandMark(size = 88.dp)
            } else {
                IconBadge(icon = pageIcons[Onboarding.clamp(index)], size = 88.dp)
            }
            Text(
                text = stringResource(page.titleResId),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(page.bodyResId),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = TireonDimens.ReadableWidth),
            )
            PageDots(current = index, total = Onboarding.pages.size)
            Text(
                text = stringResource(
                    R.string.onboarding_page_indicator,
                    index + 1,
                    Onboarding.pages.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .widthIn(max = TireonDimens.ReadableWidth)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (Onboarding.canGoBack(index)) {
                TireonActionButton(
                    text = stringResource(R.string.onboarding_back),
                    onClick = { index -= 1 },
                    style = ActionStyle.SECONDARY,
                )
            }

            TireonActionButton(
                text = stringResource(
                    when {
                        onClose != null && isLast -> R.string.onboarding_close
                        isLast -> R.string.onboarding_start
                        else -> R.string.onboarding_next
                    },
                ),
                onClick = {
                    when {
                        onClose != null && isLast -> onClose()
                        isLast -> onFinish()
                        else -> index += 1
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 장마다 내용을 대표하는 아이콘. 첫 장은 브랜드 표시를 쓴다. */
private val pageIcons = listOf(
    R.drawable.ic_car,
    R.drawable.ic_practice,
    R.drawable.ic_car,
    R.drawable.ic_shield,
)

@Composable
private fun PageDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { page ->
            Box(
                modifier = Modifier
                    .size(width = if (page == current) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (page == current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
    }
}

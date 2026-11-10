package ai.tireon.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R

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
            .padding(24.dp),
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
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
            )
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (Onboarding.canGoBack(index)) {
                TextButton(onClick = { index -= 1 }) {
                    Text(stringResource(R.string.onboarding_back))
                }
            }

            Button(
                onClick = {
                    when {
                        onClose != null && isLast -> onClose()
                        isLast -> onFinish()
                        else -> index += 1
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    stringResource(
                        when {
                            onClose != null && isLast -> R.string.onboarding_close
                            isLast -> R.string.onboarding_start
                            else -> R.string.onboarding_next
                        },
                    ),
                )
            }
        }
    }
}

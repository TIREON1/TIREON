package ai.tireon.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ai.tireon.app.R

/**
 * 네 목적지를 가진 내비게이션 그래프.
 *
 * 탭 전환은 백스택을 쌓지 않는다. 같은 목적지를 반복 선택해도 뒤로 가기가
 * 쌓이지 않도록 `launchSingleTop`과 `popUpTo`를 쓴다.
 */
@Composable
fun TireonNavHost(
    modifier: Modifier = Modifier,
    onStartPractice: () -> Unit = {},
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = TireonDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(TireonDestination.HOME.route) {
            HomePlaceholder(onStartPractice = onStartPractice)
        }
        composable(TireonDestination.PRACTICE.route) {
            PlaceholderScreen(
                title = stringResource(R.string.practice_title),
                description = stringResource(R.string.practice_description),
            )
        }
        composable(TireonDestination.REPORT.route) {
            PlaceholderScreen(
                title = stringResource(R.string.report_title),
                description = stringResource(R.string.report_description),
            )
        }
        composable(TireonDestination.SETTINGS.route) {
            PlaceholderScreen(
                title = stringResource(R.string.settings_title),
                description = stringResource(R.string.settings_description),
            )
        }
    }
}

@Composable
private fun HomePlaceholder(onStartPractice: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.app_name),
        description = stringResource(R.string.app_placeholder),
        onAction = onStartPractice,
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onAction != null) {
            androidx.compose.material3.Button(onClick = onAction) {
                Text(text = stringResource(R.string.action_start))
            }
        }
    }
}

package ai.tireon.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.tireon.app.R

/** 설정 화면. 앱 정보, 차량 정보, 안전 고지를 보여준다. */
@Composable
fun SettingsScreen(
    info: SettingsInfo,
    modifier: Modifier = Modifier,
    onShowOnboarding: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InfoCard(title = stringResource(R.string.settings_app_section)) {
            InfoRow(
                label = stringResource(R.string.settings_app_version),
                value = info.appVersion,
            )
        }

        InfoCard(title = stringResource(R.string.settings_vehicle_section)) {
            InfoRow(
                label = stringResource(R.string.settings_connection),
                value = info.connectionText,
            )
            InfoRow(
                label = stringResource(R.string.settings_vin),
                value = info.vinText,
            )
        }

        if (onShowOnboarding != null) {
            InfoCard(title = stringResource(R.string.settings_guide_section)) {
                Card(
                    onClick = onShowOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.settings_onboarding),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        InfoCard(title = stringResource(R.string.settings_safety_section)) {
            SettingsInfo.safetyNotices().forEach { notice ->
                Text(
                    text = "• $notice",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
}

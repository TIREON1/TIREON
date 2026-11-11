package ai.tireon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.onboarding.OnboardingScreen
import ai.tireon.app.onboarding.OnboardingStore
import ai.tireon.app.onboarding.PrefsOnboardingStore
import ai.tireon.app.permission.SpeedPermission
import ai.tireon.app.permission.rememberSpeedPermissionState
import ai.tireon.app.ui.IconBadge
import ai.tireon.app.ui.TireonApp
import ai.tireon.app.ui.theme.TireonSurface
import ai.tireon.app.ui.theme.TireonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TireonTheme {
                TireonSurface {
                    TireonRoot()
                }
            }
        }
    }
}

/**
 * 권한과 온보딩 상태에 따라 무엇을 보여줄지 고른다.
 *
 * 권한이 먼저다. 권한이 없으면 앱 자체를 쓸 수 없으므로 온보딩을 읽을 이유도 없다.
 * 온보딩을 마쳤거나 건너뛰면 다시 보여주지 않는다.
 */
@Composable
private fun TireonRoot() {
    val context = LocalContext.current
    val store: OnboardingStore = remember { PrefsOnboardingStore(context) }
    var onboarded by remember { mutableStateOf(store.isCompleted()) }
    val speedGranted = rememberSpeedPermissionState()

    when {
        speedGranted == false -> PermissionRequired()
        speedGranted == null -> Unit
        !onboarded -> OnboardingScreen(
            onFinish = {
                store.markCompleted()
                onboarded = true
            },
        )
        else -> TireonApp()
    }
}

@Composable
private fun PermissionRequired() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            icon = R.drawable.ic_speed,
            size = 80.dp,
            tint = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer,
        )
        Text(
            text = "권한이 필요해요",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = SpeedPermission.DENIED_MESSAGE,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "설정에서 차량 속도 권한을 허용한 뒤 앱을 다시 실행해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

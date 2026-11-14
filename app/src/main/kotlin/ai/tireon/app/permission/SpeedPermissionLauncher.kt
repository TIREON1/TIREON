package ai.tireon.app.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import ai.tireon.app.R

/**
 * 차속 권한을 요청하고 결과를 상태로 알려준다.
 *
 * 승인 없이는 속도가 null이 되어 급가속·급제동 분석이 불가능하다. 화면은 이 상태를
 * 보고 안내 문구를 표시한다.
 *
 * @return 승인 여부. 확인 중이면 null.
 */
@Composable
fun rememberSpeedPermissionState(): Boolean? {
    val context = LocalContext.current
    var granted by rememberSaveable { mutableStateOf(context.hasSpeedPermission()) }
    var previouslyGranted by rememberSaveable {
        mutableStateOf(context.hasSpeedPermission())
    }
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    var requestInFlight by remember { mutableStateOf(false) }
    var rationale by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { result ->
        requestInFlight = false
        granted = result
        if (result) {
            previouslyGranted = true
            rationale = null
        } else {
            Log.i(TAG, "차속 권한이 거부되었습니다. 속도 신호를 사용할 수 없습니다.")
            rationale = SpeedPermission.DENIED_MESSAGE
        }
    }

    fun request() {
        if (!SpeedPermission.shouldRequest(SpeedPermission.Decision.REQUEST, requestInFlight)) return
        requestInFlight = true
        askedBefore = true
        launcher.launch(SpeedPermission.PERMISSION)
    }

    // 복귀할 때마다 시스템 값으로 다시 판정한다. 사용자가 설정에서 권한을 켜고
    // 돌아오는 경우, 앱을 다시 실행하지 않아도 들어올 수 있어야 한다.
    LifecycleResumeEffect(Unit) {
        val grantedNow = context.hasSpeedPermission()
        val decision = SpeedPermission.decideOnResume(
            grantedNow = grantedNow,
            shouldShowRationale = context.shouldShowRationale(),
            askedBefore = askedBefore,
            previouslyGranted = previouslyGranted,
        )
        granted = grantedNow

        when (decision) {
            SpeedPermission.Decision.NONE -> rationale = null
            SpeedPermission.Decision.REQUEST_WITH_RATIONALE -> {
                rationale = SpeedPermission.rationaleFor(decision)
            }
            SpeedPermission.Decision.OPEN_SETTINGS -> {
                rationale = SpeedPermission.rationaleFor(decision)
            }
            SpeedPermission.Decision.REQUEST -> request()
            SpeedPermission.Decision.DENIED -> Unit
        }

        onPauseOrDispose { }
    }

    rationale?.let { message ->
        val openSettings = SpeedPermission.decideOnResume(
            grantedNow = false,
            shouldShowRationale = context.shouldShowRationale(),
            askedBefore = askedBefore,
            previouslyGranted = previouslyGranted,
        ) == SpeedPermission.Decision.OPEN_SETTINGS

        AlertDialog(
            onDismissRequest = { rationale = null },
            title = { Text(stringResource(R.string.permission_rationale_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        rationale = null
                        if (openSettings) {
                            context.openAppSettings()
                        } else {
                            request()
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (openSettings) {
                                R.string.permission_open_settings
                            } else {
                                R.string.permission_continue
                            },
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { rationale = null }) {
                    Text(stringResource(R.string.permission_later))
                }
            },
        )
    }

    return granted
}

private fun Context.hasSpeedPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, SpeedPermission.PERMISSION) == PackageManager.PERMISSION_GRANTED

private fun Context.shouldShowRationale(): Boolean {
    val activity = this as? android.app.Activity ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, SpeedPermission.PERMISSION)
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private const val TAG = "SpeedPermission"

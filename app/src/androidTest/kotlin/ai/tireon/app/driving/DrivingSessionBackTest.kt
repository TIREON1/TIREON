package ai.tireon.app.driving

import ai.tireon.app.MainActivity
import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 런처 화면 위에 열린 실제 주행 액티비티의 뒤로가기를 검증한다. */
@RunWith(AndroidJUnit4::class)
class DrivingSessionBackTest {
    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        "android.car.permission.CAR_SPEED",
        "android.permission.POST_NOTIFICATIONS",
    )

    @Test
    fun backWhileRecordingKeepsActivityAndCollectionAlive() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val parent = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        try {
            // 루트 액티비티의 기본 뒤로가기는 이미 배경 이동이므로 실제 앱과 같은 스택이 필요하다.
            instrumentation.runOnMainSync { parent.startActivity(DrivingSessionActivity.intent(parent, null)) }
            instrumentation.waitForIdleSync()
            var activity: DrivingSessionActivity? = null
            lateinit var viewModel: DrivingSessionViewModel
            try {
                instrumentation.runOnMainSync {
                    activity = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<DrivingSessionActivity>().single()
                    viewModel = ViewModelProvider(requireNotNull(activity))[DrivingSessionViewModel::class.java]
                    viewModel.start()
                    DrivingRecordService.start(requireNotNull(activity))
                }
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync { requireNotNull(activity).onBackPressedDispatcher.onBackPressed() }
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync {
                    assertFalse("뒤로가기로 미저장 세션을 닫으면 안 된다", requireNotNull(activity).isFinishing)
                    assertFalse(requireNotNull(activity).isDestroyed)
                    assertTrue(viewModel.state.value.collecting)
                }
            } finally {
                // 세션을 저장하지 않고 테스트가 만든 화면과 서비스만 정리한다.
                instrumentation.runOnMainSync {
                    activity?.finish()
                    DrivingRecordService.stop(instrumentation.targetContext)
                }
            }
        } finally {
            instrumentation.runOnMainSync { parent.finish() }
            instrumentation.waitForIdleSync()
        }
    }
}

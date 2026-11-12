package ai.tireon.app.driving

import ai.tireon.app.R
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * 주행 기록 중 앱 프로세스를 포그라운드로 유지하는 서비스.
 *
 * 수집은 [DrivingSessionViewModel]이 한다. 이 서비스는 운전자가 내비게이션으로
 * 넘어가도 프로세스가 캐시 상태로 내려가지 않게 하는 역할만 한다. Android 14는
 * 캐시 상태 프로세스를 약 10초 뒤 얼리므로, 서비스 없이는 그때부터 수집이 멈춘다.
 *
 * 프로세스가 종료되면 세션도 사라지므로 다시 시작하지 않는다.
 */
class DrivingRecordService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(
            // 운전 중 소리나 진동으로 주의를 끌지 않도록 낮은 중요도를 쓴다.
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.driving_record_channel))
                .build(),
        )

        // 런처와 같은 인텐트로 열어야 기존 태스크의 주행 기록 화면으로 돌아간다.
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle(getString(R.string.driving_session_recording_title))
            .setContentText(getString(R.string.driving_record_notification_body))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {

        private const val CHANNEL_ID = "driving_record"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DrivingRecordService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DrivingRecordService::class.java))
        }
    }
}

package ai.tireon.app.report

import ai.tireon.core.report.SessionRepository
import android.content.Context
import java.io.File

/**
 * 앱에서 쓰는 저장소 인스턴스를 제공한다.
 *
 * 파일은 앱 전용 디렉터리에 두며 다른 앱이 읽을 수 없다.
 */
object SessionRepositoryProvider {

    @Volatile
    private var instance: SessionRepository? = null

    fun get(context: Context): SessionRepository =
        instance ?: synchronized(this) {
            instance ?: FileSessionRepository(
                File(context.applicationContext.filesDir, "sessions"),
            ).also { instance = it }
        }
}

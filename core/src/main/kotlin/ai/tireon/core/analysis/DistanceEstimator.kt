package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingSample
import kotlin.math.abs

/**
 * 속도 적분으로 주행 거리를 추정한다.
 *
 * Pleos Odometer는 누적 거리만 주고 트립 거리를 주지 않는다. 세션 거리는 속도를
 * 적분해 구해야 한다.
 *
 * 속도가 없는 구간과 시간이 뒤로 가는 구간은 건너뛴다. 추정할 수 없으면 0을
 * 돌려주므로, 호출 측은 `ScoreCalculator`가 데이터 부족으로 처리하게 둔다.
 */
object DistanceEstimator {

    /** @return 킬로미터 단위 거리. 추정 불가면 0. */
    fun estimateKm(samples: List<DrivingSample>): Float {
        if (samples.size < 2) return 0f

        var meters = 0.0
        var counted = 0

        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            // 후진 중 차속은 음수로 온다. 이동 거리는 방향과 무관하게 더한다.
            val speed = current.speedMps?.let { abs(it) } ?: continue
            val dtMs = current.timestampMs - previous.timestampMs
            if (dtMs <= 0) continue

            meters += speed * (dtMs / 1000.0)
            counted++
        }

        return if (counted == 0) 0f else (meters / 1000.0).toFloat()
    }
}

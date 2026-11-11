package ai.tireon.pleos.signal

import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.analysis.Thresholds
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertTrue

class SignalIntervalTest {

    @Test
    fun `샘플 간격은 이벤트 판정 상한의 절반 이하다`() {
        // 간격이 상한을 넘으면 EventDetector가 가속도를 계산하지 않아 이벤트가 0건이 된다.
        assertTrue(DefaultPleosSignalSource.DEFAULT_INTERVAL_MS * 2 <= Thresholds.MAX_SAMPLE_GAP_MS)
    }

    @Test
    fun `샘플 간격은 이벤트 판정 하한보다 길다`() {
        assertTrue(DefaultPleosSignalSource.DEFAULT_INTERVAL_MS > Thresholds.MIN_SAMPLE_GAP_MS)
    }

    @Test
    fun `실제 수집 주기로 들어온 급제동을 잡는다`() {
        // 50km/h에서 3초 동안 정지(약 -4.6m/s²)하는 장면을 수집 주기대로 만든다.
        val interval = DefaultPleosSignalSource.DEFAULT_INTERVAL_MS
        val steps = (BRAKING_MS / interval).toInt()
        val cruise = (0 until 10).map { sample(it * interval, 13.9f, brake = false) }
        val braking = (1..steps).map { step ->
            val speed = (13.9f - 13.9f * step / steps).coerceAtLeast(0f)
            sample(cruise.last().timestampMs + step * interval, speed, brake = true)
        }

        val events = EventDetector().detect(cruise + braking)

        assertTrue(events.any { it.type == DrivingEventType.HARSH_BRAKE })
    }

    private companion object {
        const val BRAKING_MS = 3_000L
    }

    private fun sample(atMs: Long, speed: Float, brake: Boolean) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = 0f,
        brakePressed = brake,
        turnSignal = TurnSignal.OFF,
        gear = Gear.DRIVE,
    )
}

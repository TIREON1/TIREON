package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals

class EventDetectorSignaledTurnTest {

    private val detector = EventDetector()

    private fun sample(
        atMs: Long,
        steering: Float = 0f,
        signal: TurnSignal? = TurnSignal.OFF,
        speed: Float = 5f,
        gear: Gear = Gear.DRIVE,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = false,
        turnSignal = signal,
        gear = gear,
    )

    @Test
    fun `방향지시등을 켜고 회전하면 1회로 센다`() {
        val samples = (0 until 10).map { sample(it * 100L, signal = TurnSignal.RIGHT) } +
            (10 until 30).map { sample(it * 100L, steering = -45f, signal = TurnSignal.RIGHT) } +
            (30 until 40).map { sample(it * 100L) }

        assertEquals(1, detector.countSignaledTurns(samples))
        assertEquals(emptyList(), detector.detect(samples))
    }

    @Test
    fun `방향지시등 없이 회전하면 세지 않는다`() {
        val samples = (0 until 10).map { sample(it * 100L) } +
            (10 until 30).map { sample(it * 100L, steering = 45f) } +
            (30 until 40).map { sample(it * 100L) }

        assertEquals(0, detector.countSignaledTurns(samples))
        assertEquals(1, detector.detect(samples).size)
    }

    @Test
    fun `방향지시등만 켜고 회전하지 않으면 세지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, signal = TurnSignal.LEFT) }

        assertEquals(0, detector.countSignaledTurns(samples))
    }

    @Test
    fun `정차 중 조향은 회전으로 세지 않는다`() {
        val samples = (0 until 10).map { sample(it * 100L, speed = 0f, signal = TurnSignal.LEFT) } +
            (10 until 30).map {
                sample(it * 100L, steering = 45f, speed = 0f, signal = TurnSignal.LEFT)
            }

        assertEquals(0, detector.countSignaledTurns(samples))
    }

    @Test
    fun `한참 전에 켰던 방향지시등은 다음 회전의 신호로 보지 않는다`() {
        // 왼쪽 신호를 켜고 회전 → 신호를 끄고 15초 직진 → 신호 없이 회전.
        val samples = (0 until 10).map { sample(it * 100L, signal = TurnSignal.LEFT) } +
            (10 until 30).map { sample(it * 100L, steering = 45f, signal = TurnSignal.LEFT) } +
            (30 until 40).map { sample(it * 100L, signal = TurnSignal.LEFT) } +
            (40 until 190).map { sample(it * 100L) } +
            (190 until 210).map { sample(it * 100L, steering = 45f) } +
            (210 until 220).map { sample(it * 100L) }

        assertEquals(1, detector.countSignaledTurns(samples))
        assertEquals(1, detector.detect(samples).size)
    }

    @Test
    fun `반대 방향 방향지시등은 회전 신호로 보지 않는다`() {
        // 좌회전인데 오른쪽 신호만 켠 경우. 누락으로 세야 한다.
        val samples = (0 until 10).map { sample(it * 100L, signal = TurnSignal.RIGHT) } +
            (10 until 30).map { sample(it * 100L, steering = 45f, signal = TurnSignal.RIGHT) } +
            (30 until 40).map { sample(it * 100L) }

        assertEquals(0, detector.countSignaledTurns(samples))
        assertEquals(1, detector.detect(samples).size)
    }

    @Test
    fun `회전 직전에 켠 방향지시등은 그 회전의 신호로 본다`() {
        // 신호를 켰다가 회전 3초 전에 꺼졌어도 누락이 아니다.
        val samples = (0 until 20).map { sample(it * 100L, signal = TurnSignal.RIGHT) } +
            (20 until 50).map { sample(it * 100L) } +
            (50 until 70).map { sample(it * 100L, steering = -45f) } +
            (70 until 80).map { sample(it * 100L) }

        assertEquals(emptyList(), detector.detect(samples))
    }
}

package ai.tireon.core.mission

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertIs

class MissionEvaluatorTest {

    private val evaluator = MissionEvaluator()

    private fun sample(
        atMs: Long,
        speed: Float? = 5f,
        steering: Float? = 0f,
        signal: TurnSignal? = TurnSignal.OFF,
        gear: Gear? = Gear.DRIVE,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = false,
        turnSignal = signal,
        gear = gear,
    )

    @Test
    fun `샘플이 없으면 판정하지 않는다`() {
        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_START,
            samples = emptyList(),
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `부드러운 출발은 급가속이 없으면 성공이다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 5f + it * 0.1f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_START,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `부드러운 출발은 급가속이 있으면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L) }
        val events = listOf(
            DrivingEvent(DrivingEventType.HARSH_ACCEL, 0L, 3.5f, Confidence.MEASURED),
        )

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_START,
            samples = samples,
            events = events,
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `부드러운 정지는 급제동이 있으면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L) }
        val events = listOf(
            DrivingEvent(DrivingEventType.HARSH_BRAKE, 0L, 3.5f, Confidence.MEASURED),
        )

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_STOP,
            samples = samples,
            events = events,
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `후진 감각은 기어 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, gear = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `후진 감각은 후진으로 3초 이상 주행하면 성공이다`() {
        val samples = (0 until 40).map {
            sample(it * 100L, speed = 2f, steering = 10f, gear = Gear.REVERSE)
        }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `후진 감각은 드라이브 기어만으로는 성공하지 못한다`() {
        val samples = (0 until 40).map { sample(it * 100L, gear = Gear.DRIVE) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `깜빡이 습관은 누락이 없으면 성공이다`() {
        val samples = (0 until 30).map { sample(it * 100L, signal = TurnSignal.LEFT) }

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `깜빡이 습관은 누락이 있으면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L) }
        val events = listOf(
            DrivingEvent(DrivingEventType.MISSING_TURN_SIGNAL, 0L, 45f, Confidence.MEASURED),
        )

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = events,
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `정속 유지는 속도 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `정속 유지는 속도가 일정하면 성공이다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 5.5f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `정속 유지는 속도가 크게 흔들리면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = if (it % 2 == 0) 1f else 9f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }
}

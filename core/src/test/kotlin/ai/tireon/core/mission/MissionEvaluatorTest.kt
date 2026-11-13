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
        brake: Boolean? = false,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = brake,
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
    fun `부드러운 출발은 정지에서 급가속 없이 출발하면 성공이다`() {
        val samples = (0 until 10).map { sample(it * 100L, speed = 0f) } +
            (10 until 50).map { sample(it * 100L, speed = (it - 10) * 0.1f) }

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
    fun `부드러운 출발은 차를 움직이지 않으면 성공하지 못한다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 0f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_START,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `부드러운 정지는 달리다가 급제동 없이 멈추면 성공이다`() {
        val samples = (0 until 20).map { sample(it * 100L, speed = 4f) } +
            (20 until 60).map { sample(it * 100L, speed = 4f - (it - 19) * 0.1f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `부드러운 정지는 브레이크 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 20).map { sample(it * 100L, speed = 4f, brake = null) } +
            (20 until 60).map {
                sample(it * 100L, speed = 4f - (it - 19) * 0.1f, brake = null)
            }

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
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
    fun `부드러운 정지는 멈추지 않으면 성공하지 못한다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 4f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `천천히 후진은 기어 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, gear = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `천천히 후진은 후진으로 3초 이상 천천히 움직이면 성공이다`() {
        val samples = (0 until 40).map {
            sample(it * 100L, speed = 2f, gear = Gear.REVERSE)
        }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `천천히 후진은 드라이브 기어만으로는 성공하지 못한다`() {
        val samples = (0 until 40).map { sample(it * 100L, gear = Gear.DRIVE) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `천천히 후진은 기어만 넣고 서 있으면 성공하지 못한다`() {
        val samples = (0 until 40).map {
            sample(it * 100L, speed = 0f, gear = Gear.REVERSE)
        }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `천천히 후진은 시속 10km를 넘으면 실패다`() {
        val samples = (0 until 40).map {
            sample(it * 100L, speed = 4f, gear = Gear.REVERSE)
        }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `천천히 후진은 후진 중 속도를 모르면 판정하지 않는다`() {
        val samples = (0 until 10).map { sample(it * 100L, speed = 0f) } +
            (10 until 50).map { sample(it * 100L, speed = null, gear = Gear.REVERSE) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `깜빡이 습관은 방향지시등을 켜고 회전하면 성공이다`() {
        val samples = (0 until 10).map { sample(it * 100L, signal = TurnSignal.LEFT) } +
            (10 until 30).map { sample(it * 100L, steering = 45f, signal = TurnSignal.LEFT) } +
            (30 until 40).map { sample(it * 100L) }

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `깜빡이 습관은 회전이 없으면 성공하지 못한다`() {
        val samples = (0 until 30).map { sample(it * 100L, signal = TurnSignal.LEFT) }

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
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
    fun `정속 유지는 목표 속도를 5초 이상 일정하게 유지하면 성공이다`() {
        val samples = (0 until 60).map { sample(it * 100L, speed = 5.5f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `정속 유지는 출발과 정지 구간이 섞여도 유지 구간만 본다`() {
        // 10초 가속, 30초 유지, 10초 감속. 세션 전체 편차는 3km/h를 넘는다.
        val cruise = 20f / 3.6f
        val samples = (0 until 100).map { sample(it * 100L, speed = cruise * it / 100f) } +
            (100 until 400).map { sample(it * 100L, speed = cruise) } +
            (400 until 500).map { sample(it * 100L, speed = cruise * (500 - it) / 100f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `정속 유지는 목표 속도를 5초 못 채우면 성공하지 못한다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 5.5f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `정속 유지는 속도가 크게 흔들리면 실패다`() {
        val samples = (0 until 60).map {
            sample(it * 100L, speed = if (it % 2 == 0) 4.3f else 6.8f)
        }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    /** 접근(2초) → 정차(pauseSamples × 100ms) → 출발하며 조향. */
    private fun rightTurn(
        pauseSamples: Int,
        signal: TurnSignal = TurnSignal.RIGHT,
        turnSteering: Float = 60f,
    ): List<DrivingSample> {
        val turnStart = 20 + pauseSamples
        return (0 until 20).map { sample(it * 100L, speed = 4f, signal = signal) } +
            (20 until turnStart).map { sample(it * 100L, speed = 0f, signal = signal) } +
            (turnStart until turnStart + 20).map {
                sample(it * 100L, speed = 2f, steering = turnSteering, signal = signal)
            }
    }

    @Test
    fun `우회전 일시정지는 2초 이상 멈춘 뒤 우회전하면 성공이다`() {
        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = rightTurn(pauseSamples = 25),
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `우회전 일시정지는 멈춘 시간이 2초보다 짧으면 실패다`() {
        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = rightTurn(pauseSamples = 10),
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `우회전 일시정지는 오른쪽 방향지시등이 없으면 실패다`() {
        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = rightTurn(pauseSamples = 25, signal = TurnSignal.OFF),
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `우회전 일시정지는 멈추기만 하고 회전하지 않으면 실패다`() {
        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = rightTurn(pauseSamples = 25, turnSteering = 0f),
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `우회전 일시정지는 출발 전부터 서 있던 것을 일시정지로 보지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 0f, signal = TurnSignal.RIGHT) } +
            (30 until 50).map {
                sample(it * 100L, speed = 2f, steering = 60f, signal = TurnSignal.RIGHT)
            }

        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `우회전 일시정지는 정지 중 핸들을 미리 돌려둬도 성공이다`() {
        val samples = (0 until 20).map { sample(it * 100L, speed = 4f, signal = TurnSignal.RIGHT) } +
            (20 until 30).map { sample(it * 100L, speed = 0f, signal = TurnSignal.RIGHT) } +
            (30 until 45).map {
                sample(it * 100L, speed = 0f, steering = 60f, signal = TurnSignal.RIGHT)
            } +
            (45 until 65).map {
                sample(it * 100L, speed = 2f, steering = 60f, signal = TurnSignal.RIGHT)
            }

        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `우회전 일시정지는 방향지시등 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, signal = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }
}

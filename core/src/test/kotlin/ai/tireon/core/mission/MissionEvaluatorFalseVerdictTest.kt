package ai.tireon.core.mission

import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import ai.tireon.core.analysis.Thresholds
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 리뷰에서 확인한 판정 오판 재현 테스트.
 *
 * 세 가지 모두 사용자가 올바르게 했는데 실패하거나, 잘못했는데 성공하는 경우다.
 */
class MissionEvaluatorFalseVerdictTest {

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
    fun `깜빡이 습관 - 속도 신호가 없으면 성공이 아니라 판정 불가다`() {
        // 회전은 했고 방향지시등도 켰지만 속도 신호가 전혀 없다.
        // 회전 여부를 판정할 수 없으므로 "회전하지 않았다"고 단정하면 안 된다.
        val samples = (0 until 10).map { sample(it * 100L, speed = null, signal = TurnSignal.LEFT) } +
            (10 until 30).map {
                sample(it * 100L, speed = null, steering = 45f, signal = TurnSignal.LEFT)
            } +
            (30 until 40).map { sample(it * 100L, speed = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `깜빡이 습관 - 속도 신호가 회전 중에만 끊겨도 판정 불가다`() {
        // 정지·저속 판정에 속도가 필요하다. 일부 구간만 있어도 회전 검출이 불가능하다.
        val samples = (0 until 10).map { sample(it * 100L, signal = TurnSignal.LEFT) } +
            (10 until 30).map {
                sample(it * 100L, speed = null, steering = 45f, signal = TurnSignal.LEFT)
            } +
            (30 until 40).map { sample(it * 100L, speed = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `천천히 후진 - 기어 신호가 한두 샘플 끊겨도 연속 후진을 인정한다`() {
        // 4초 후진 중 200ms만 기어 신호가 null이다. 실제로는 계속 후진 중이다.
        val samples = (0 until 10).map { sample(it * 100L, speed = 1.5f, gear = Gear.REVERSE) } +
            (10 until 12).map { sample(it * 100L, speed = 1.5f, gear = null) } +
            (12 until 40).map { sample(it * 100L, speed = 1.5f, gear = Gear.REVERSE) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `천천히 후진 - 기어 신호가 길게 끊기면 연속으로 보지 않는다`() {
        // 2초 가까이 끊기면 앞뒤를 이어 붙이지 않는다.
        val gapMs = 2_000L
        val before = (0 until 10).map { sample(it * 100L, speed = 1.5f, gear = Gear.REVERSE) }
        val during = (0 until (gapMs / 100).toInt()).map {
            sample(1_000L + it * 100L, speed = 1.5f, gear = null)
        }
        val after = (0 until 10).map {
            sample(1_000L + gapMs + it * 100L, speed = 1.5f, gear = Gear.REVERSE)
        }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = before + during + after,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `우회전 일시정지 - 멈춘 뒤 후진으로 조향하면 성공이 아니다`() {
        val samples = (0 until 20).map { sample(it * 100L, speed = 4f, signal = TurnSignal.RIGHT) } +
            (20 until 50).map { sample(it * 100L, speed = 0f, signal = TurnSignal.RIGHT) } +
            (50 until 70).map {
                sample(
                    it * 100L,
                    speed = -2f,
                    steering = 60f,
                    gear = Gear.REVERSE,
                    signal = TurnSignal.RIGHT,
                )
            }

        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `우회전 일시정지 - 기어는 드라이브인데 차속이 음수여도 성공이 아니다`() {
        // 후진 중 기어 콜백이 늦게 오면 드라이브로 남을 수 있다. 차속 부호로도 막는다.
        val samples = (0 until 20).map { sample(it * 100L, speed = 4f, signal = TurnSignal.RIGHT) } +
            (20 until 50).map { sample(it * 100L, speed = 0f, signal = TurnSignal.RIGHT) } +
            (50 until 70).map {
                sample(it * 100L, speed = -2f, steering = 60f, signal = TurnSignal.RIGHT)
            }

        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `우회전 일시정지 - 정지 후 출발 전에 핸들을 돌려도 성공이다`() {
        // 핸들을 정지 중에 미리 돌려두는 것은 실제 운전에서 정상이다.
        val samples = (0 until 20).map { sample(it * 100L, speed = 4f, signal = TurnSignal.RIGHT) } +
            (20 until 50).map { sample(it * 100L, speed = 0f, signal = TurnSignal.RIGHT) } +
            (50 until 70).map { sample(it * 100L, speed = 2f, steering = 60f, signal = TurnSignal.RIGHT) }

        val result = evaluator.evaluate(
            mission = PracticeMission.RIGHT_TURN_STOP,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }
}

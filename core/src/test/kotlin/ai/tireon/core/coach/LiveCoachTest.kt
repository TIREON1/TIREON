package ai.tireon.core.coach

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 세션 중 새로 발생한 이벤트만 골라낸다.
 *
 * 같은 이벤트를 반복해서 안내하면 운전자가 피로해진다. 이벤트 식별자(종류 + 시각)로
 * 중복을 걸러야 한다.
 */
class LiveCoachTest {

    private fun event(type: DrivingEventType, atMs: Long) = DrivingEvent(
        type = type,
        atMs = atMs,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    private fun sample(atMs: Long, speed: Float = 5f) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = 0f,
        brakePressed = false,
        turnSignal = TurnSignal.OFF,
        gear = Gear.DRIVE,
    )

    @Test
    fun `처음에는 새 이벤트가 없다`() {
        val coach = LiveCoach()

        assertTrue(coach.newEvents(emptyList()).isEmpty())
    }

    @Test
    fun `처음 본 이벤트를 새 이벤트로 돌려준다`() {
        val coach = LiveCoach()
        val events = listOf(event(DrivingEventType.HARSH_BRAKE, 1_000L))

        assertEquals(events, coach.newEvents(events))
    }

    @Test
    fun `같은 이벤트를 두 번 돌려주지 않는다`() {
        val coach = LiveCoach()
        val events = listOf(event(DrivingEventType.HARSH_BRAKE, 1_000L))

        coach.newEvents(events)
        val second = coach.newEvents(events)

        assertTrue(second.isEmpty())
    }

    @Test
    fun `새로 추가된 이벤트만 돌려준다`() {
        val coach = LiveCoach()
        val first = listOf(event(DrivingEventType.HARSH_BRAKE, 1_000L))
        coach.newEvents(first)

        val second = coach.newEvents(
            first + event(DrivingEventType.HARSH_STEER, 2_000L),
        )

        assertEquals(1, second.size)
        assertEquals(DrivingEventType.HARSH_STEER, second.first().type)
    }

    @Test
    fun `시각이 달라도 종류가 같으면 다른 이벤트다`() {
        val coach = LiveCoach()
        coach.newEvents(listOf(event(DrivingEventType.HARSH_BRAKE, 1_000L)))

        val second = coach.newEvents(
            listOf(
                event(DrivingEventType.HARSH_BRAKE, 1_000L),
                event(DrivingEventType.HARSH_BRAKE, 2_000L),
            ),
        )

        assertEquals(1, second.size)
        assertEquals(2_000L, second.first().atMs)
    }

    @Test
    fun `안내 문장을 만든다`() {
        val coach = LiveCoach()

        val message = coach.messageFor(event(DrivingEventType.HARSH_BRAKE, 1_000L))

        assertTrue(message.contains("급제동"))
    }

    @Test
    fun `급가속 안내 문장을 만든다`() {
        val coach = LiveCoach()

        val message = coach.messageFor(event(DrivingEventType.HARSH_ACCEL, 1_000L))

        assertTrue(message.contains("급가속"))
    }

    @Test
    fun `깜빡이 누락 안내 문장을 만든다`() {
        val coach = LiveCoach()

        val message = coach.messageFor(event(DrivingEventType.MISSING_TURN_SIGNAL, 1_000L))

        assertTrue(message.contains("방향지시등"))
    }

    @Test
    fun `급조향 안내 문장을 만든다`() {
        val coach = LiveCoach()

        val message = coach.messageFor(event(DrivingEventType.HARSH_STEER, 1_000L))

        assertTrue(message.contains("핸들"))
    }

    @Test
    fun `샘플 창에서 새 이벤트를 찾아낸다`() {
        val coach = LiveCoach()
        // 3.5 m/s²로 1초 가속: 급가속 이벤트가 생긴다
        val samples = (0 until 20).map { index ->
            sample(atMs = index * 100L, speed = 10f + 3.5f * 0.1f * index)
        }

        val first = coach.observe(samples)
        val second = coach.observe(samples)

        assertTrue(first.isNotEmpty())
        assertTrue(second.isEmpty())
    }
}

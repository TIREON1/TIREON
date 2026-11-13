package ai.tireon.core.coach

import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample

/**
 * 연습 중 새로 발생한 이벤트를 골라 안내 문장을 만든다.
 *
 * 세션 전체를 매번 다시 분석하면 이미 안내한 이벤트가 또 나온다. 이벤트의
 * 종류와 시각을 식별자로 삼아 한 번만 돌려준다.
 *
 * 이 클래스는 상태를 가진다. 세션이 바뀌면 새 인스턴스를 쓴다.
 */
class LiveCoach {

    private val detector = EventDetector()
    private val announced = mutableSetOf<String>()

    /** 누적 샘플에서 아직 안내하지 않은 이벤트만 골라 돌려준다. */
    fun observe(samples: List<DrivingSample>): List<DrivingEvent> =
        newEvents(detector.detect(samples, includeOpenTurn = false))

    /** 아직 안내하지 않은 이벤트만 골라 돌려준다. */
    fun newEvents(events: List<DrivingEvent>): List<DrivingEvent> {
        val fresh = events.filter { announced.add(it.id()) }
        return fresh
    }

    /** 이벤트 하나에 대응하는 짧은 안내 문장. */
    fun messageFor(event: DrivingEvent): String = when (event.type) {
        DrivingEventType.HARSH_BRAKE -> "급제동이 있었어요. 조금 더 미리 밟아보세요."
        DrivingEventType.HARSH_ACCEL -> "급가속이 있었어요. 천천히 밟아보세요."
        DrivingEventType.HARSH_STEER -> "핸들을 급하게 돌렸어요."
        DrivingEventType.MISSING_TURN_SIGNAL -> "방향지시등을 켜고 핸들을 돌려보세요."
    }

    private fun DrivingEvent.id(): String = "$type@$atMs"
}

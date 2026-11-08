package ai.tireon.core.coach

import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.analysis.Thresholds
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import java.util.Locale
import kotlin.math.abs

/**
 * 측정된 이벤트와 점수에서 코칭 문장을 만든다.
 *
 * 점수는 여기서 만들지 않는다. 이미 계산된 측정 결과를 문장으로 옮기기만 한다.
 * 문장에는 측정값을 넣지 않고 [CoachMessage.evidence]로 분리해,
 * 사용자가 "무엇을 측정했는지"와 "무엇을 하라는 말인지"를 구분할 수 있게 한다.
 */
class CoachMessages {

    fun build(events: List<DrivingEvent>, result: ScoreResult): List<CoachMessage> {
        val messages = mutableListOf<CoachMessage>()

        if (result is ScoreResult.Insufficient) {
            messages += insufficientMessage(result.reason)
            return messages
        }

        val scored = result as ScoreResult.Scored
        val counts = events.groupingBy { it.type }.eachCount()

        issueMessage(DrivingEventType.HARSH_BRAKE, counts[DrivingEventType.HARSH_BRAKE], events)?.let {
            messages += it
        }
        issueMessage(DrivingEventType.HARSH_ACCEL, counts[DrivingEventType.HARSH_ACCEL], events)?.let {
            messages += it
        }
        issueMessage(DrivingEventType.HARSH_STEER, counts[DrivingEventType.HARSH_STEER], events)?.let {
            messages += it
        }
        issueMessage(
            DrivingEventType.MISSING_TURN_SIGNAL,
            counts[DrivingEventType.MISSING_TURN_SIGNAL],
            events,
        )?.let { messages += it }

        if (messages.isEmpty()) {
            messages += CoachMessage(
                text = "이번 주행은 아주 부드러웠어요.",
                severity = Severity.GOOD,
                evidence = null,
            )
        }

        messages += scoreMessage(scored)
        return messages
    }

    private fun insufficientMessage(reason: InsufficientReason): CoachMessage = when (reason) {
        InsufficientReason.DISTANCE_TOO_SHORT -> CoachMessage(
            text = "조금 더 길게 주행하면 점수를 낼 수 있어요.",
            severity = Severity.WARN,
            evidence = null,
        )

        InsufficientReason.DURATION_TOO_SHORT -> CoachMessage(
            text = "1분 이상 주행하면 점수를 낼 수 있어요.",
            severity = Severity.WARN,
            evidence = null,
        )

        InsufficientReason.NOT_ENOUGH_SAMPLES -> CoachMessage(
            text = "차량 신호를 충분히 받지 못했어요.",
            severity = Severity.WARN,
            evidence = null,
        )
    }

    private fun issueMessage(
        type: DrivingEventType,
        count: Int?,
        events: List<DrivingEvent>,
    ): CoachMessage? {
        if (count == null || count <= 0) return null

        val extreme = events.filter { it.type == type }
            .maxOfOrNull { abs(it.value) } ?: 0f

        return when (type) {
            DrivingEventType.HARSH_BRAKE -> CoachMessage(
                text = "급제동이 ${count}번 있었어요. 브레이크를 조금 더 미리 밟아보세요.",
                severity = Severity.CRITICAL,
                evidence = String.format(Locale.KOREAN, "감속도 -%.1f m/s² (측정)", extreme),
            )

            DrivingEventType.HARSH_ACCEL -> CoachMessage(
                text = "급가속이 ${count}번 있었어요. 출발할 때 가속 페달을 천천히 밟아보세요.",
                severity = Severity.WARN,
                evidence = String.format(Locale.KOREAN, "가속도 +%.1f m/s² (측정)", extreme),
            )

            DrivingEventType.HARSH_STEER -> CoachMessage(
                text = "핸들을 급하게 돌린 구간이 ${count}번 있었어요.",
                severity = Severity.WARN,
                evidence = String.format(Locale.KOREAN, "조향 속도 %.1f °/s (측정)", extreme),
            )

            DrivingEventType.MISSING_TURN_SIGNAL -> CoachMessage(
                text = "방향지시등 없이 핸들을 돌린 구간이 ${count}번 있었어요.",
                severity = Severity.WARN,
                evidence = String.format(Locale.KOREAN, "조향 %.1f° 변화 (측정)", extreme),
            )
        }
    }

    private fun scoreMessage(scored: ScoreResult.Scored): CoachMessage {
        val value = scored.score.value

        // 감점이 0으로 반올림된 항목은 개선 제안에서 제외한다.
        // 100점인데 "줄이면 더 좋아져요"라고 하면 신뢰를 잃는다.
        val top = scored.breakdown.firstOrNull { it.penalty > 0 }

        val text = if (top == null) {
            "오늘은 ${value}점이에요."
        } else {
            "오늘은 ${value}점이에요. ${labelOf(top.type)}만 줄이면 더 좋아져요."
        }

        return CoachMessage(
            text = text,
            severity = if (value >= Thresholds.GOOD_SCORE) Severity.GOOD else Severity.WARN,
            evidence = null,
        )
    }

    private fun labelOf(type: DrivingEventType): String = when (type) {
        DrivingEventType.HARSH_BRAKE -> "급제동"
        DrivingEventType.HARSH_ACCEL -> "급가속"
        DrivingEventType.HARSH_STEER -> "급조향"
        DrivingEventType.MISSING_TURN_SIGNAL -> "방향지시등 누락"
    }
}

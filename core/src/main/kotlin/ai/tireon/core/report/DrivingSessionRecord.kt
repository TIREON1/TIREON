package ai.tireon.core.report

import kotlinx.serialization.Serializable

/**
 * 한 번의 주행 또는 연습 세션 기록.
 *
 * @param schemaVersion 저장 형식 버전. 형식이 바뀌면 올리고 읽기 쪽에서 분기한다.
 * @param score 점수. 데이터가 부족해 점수를 내지 못했으면 null이다.
 * @param insufficientReason 점수를 내지 못한 사유 이름. 점수가 있으면 null이다.
 * @param distanceKm 추정 거리. 추정할 수 없으면 null이다. 0으로 저장하지 않는다.
 * @param eventCounts 이벤트 종류별 발생 횟수. 발생하지 않은 종류는 담지 않는다.
 * @param mission 수행한 연습 미션 이름. 미션 없이 진행한 세션이면 null이다.
 * @param missionSuccess 미션 달성 여부. 판정하지 못했으면 null이다.
 * @param missionDetail 판정 상세 문구. 판정하지 못했으면 사유가 들어간다.
 *
 * 개인정보는 저장하지 않는다. 좌표, 위치, 사용자 식별자를 넣지 않는다.
 *
 * 버전 이력:
 * - 1: 최초 형식
 * - 2: 미션 결과 필드 추가 (mission, missionSuccess, missionDetail)
 */
@Serializable
data class DrivingSessionRecord(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val id: String,
    val startedAtMs: Long,
    val durationSec: Int,
    val distanceKm: Float?,
    val score: Int?,
    val insufficientReason: String?,
    val eventCounts: Map<String, Int>,
    val mission: String? = null,
    val missionSuccess: Boolean? = null,
    val missionDetail: String? = null,
) {
    companion object {
        /** 미션 결과를 담기 시작한 버전. */
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

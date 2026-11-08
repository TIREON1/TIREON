package ai.tireon.core.mission

/** 미션 판정 결과. */
sealed interface MissionResult {
    /** 목표를 달성했다. */
    data class Success(val detail: String) : MissionResult

    /** 목표에 도달하지 못했다. */
    data class Failed(val detail: String) : MissionResult

    /**
     * 판정에 필요한 신호가 없어 판정하지 못했다.
     *
     * 성공도 실패도 아니다. 사유를 사용자에게 안내해야 한다.
     */
    data class Insufficient(val reason: String) : MissionResult
}

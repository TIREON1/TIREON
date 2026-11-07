package ai.tireon.core.analysis

/**
 * 주행 판정 임계값.
 *
 * 모든 판정 숫자를 이 파일에만 둔다. 실측 데이터가 쌓이면 여기만 조정하면 된다.
 * 미션별 임계값 분리는 데이터가 확보된 뒤에 판단한다.
 */
object Thresholds {
    /** 가속 판정. 0.3g를 m/s²로 환산한 값. */
    const val HARSH_ACCEL_MPS2 = 2.94f
    const val HARSH_BRAKE_MPS2 = -2.94f

    /** 조향각 변화 속도 판정. */
    const val HARSH_STEER_DEG_PER_SEC = 200f
    const val HARSH_STEER_MIN_SPEED_MPS = 5.56f

    /** 조향 변화가 이 값 이상일 때 방향지시등 점등 여부를 본다. */
    const val MISSING_SIGNAL_STEER_DEG = 30f

    /** 이벤트로 인정하는 최소 지속 시간. */
    const val EVENT_MIN_DURATION_MS = 500L

    /**
     * 급조향으로 인정하는 최소 연속 쌍 수.
     *
     * 한 샘플짜리 스파이크(0→60→0)는 노이즈일 가능성이 높아 이벤트로 세지 않는다.
     */
    const val STEER_MIN_CONFIRMING_PAIRS = 2

    /** 가속도 계산에 쓰는 샘플 간격 허용 범위. 이 밖이면 계산하지 않는다. */
    const val MIN_SAMPLE_GAP_MS = 50L
    const val MAX_SAMPLE_GAP_MS = 500L

    /** 가속도 이동평균 창 크기. */
    const val SMOOTHING_WINDOW = 5

    /** 점수를 낼 수 있는 최소 조건. */
    const val VALID_MIN_DISTANCE_KM = 0.5f
    const val VALID_MIN_DURATION_SEC = 60
    const val VALID_MIN_SAMPLES = 20

    /**
     * 감점 비율을 계산할 때 쓰는 거리 하한.
     *
     * 짧은 주행에서 이벤트 하나가 과도하게 반영되는 것을 막는다.
     */
    const val SCORE_DISTANCE_FLOOR_KM = 1f

    /** 이 점수 이상이면 칭찬 문장으로 안내한다. */
    const val GOOD_SCORE = 80

    /** 이벤트 종류별 감점 가중치. */
    const val W_HARSH_BRAKE = 8
    const val W_HARSH_ACCEL = 6
    const val W_HARSH_STEER = 5
    const val W_MISSING_SIGNAL = 4
}

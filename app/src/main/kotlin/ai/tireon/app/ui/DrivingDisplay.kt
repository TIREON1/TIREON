package ai.tireon.app.ui

/**
 * 주행 데이터 화면에 표시할 값을 결정한다.
 *
 * Pleos SDK는 거리 단위를 제공하지 않으므로 단위를 붙이지 않고,
 * 값이 없을 때 0으로 대체하지 않는다. 0은 유효한 측정값과 구분되지 않기 때문이다.
 */
internal object DrivingDisplay {

    /** 측정값이 없으면 null을 반환해 호출 측이 "사용할 수 없음"을 표시하게 한다. */
    fun distance(value: Float?): String? = value?.toString()

    fun drivingTime(value: Int?): String? = value?.toString()
}

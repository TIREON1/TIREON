package ai.tireon.pleos.driving

/**
 * 주행 데이터 조회 결과.
 *
 * SDK 타입을 앱 계층으로 노출하지 않기 위한 경계 타입이다.
 */
data class DrivingData(
    val distanceDriven: Float? = null,
    val tripDrivingTime: Int? = null,
    val message: String? = null,
)

/** 주행 데이터를 조회하는 경계. SDK는 이 인터페이스 뒤에 숨는다. */
interface DrivingDataSource {
    /**
     * 값을 조회해 [onResult]로 전달한다. 콜백은 메인 스레드에서 불린다.
     *
     * 값이 없는 항목은 null로 남는다. 0으로 대체하지 않는다.
     */
    fun read(onResult: (DrivingData) -> Unit)

    /** SDK 리소스를 해제한다. */
    fun release()
}

package ai.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.ErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleResultTest {

    @Test
    fun `READ와 READ_WRITE만 읽기 가능으로 판단한다`() {
        assertTrue(AccessType.READ.canRead())
        assertTrue(AccessType.READ_WRITE.canRead())
        assertFalse(AccessType.WRITE.canRead())
        assertFalse(AccessType.NONE.canRead())
    }

    @Test
    fun `미지원은 실패와 구분된다`() {
        val unsupported = VehicleResult.unsupported("check", AccessType.NONE)

        assertIs<VehicleResult.Unsupported>(unsupported)
        assertEquals("check", unsupported.operation)
        assertEquals("accessType=NONE", unsupported.detail)
    }

    @Test
    fun `오류 코드를 확인할 수 없는 예외는 실패로 처리한다`() {
        val cause = IllegalStateException("unknown")

        val result = failed("check", cause)

        assertIs<VehicleResult.Failure>(result)
        assertEquals("check", result.operation)
        assertNull(result.code)
    }

    @Test
    fun `난독화된 SDK 예외에서 오류 코드를 추출한다`() {
        val code = VehicleErrorCodes.from(sdkException(ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY))

        assertEquals(ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY, code)
    }

    @Test
    fun `미지원 오류 코드는 Unsupported로 변환한다`() {
        val result = failed("check", sdkException(ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY))

        assertIs<VehicleResult.Unsupported>(result)
        assertEquals("ERROR_NOT_SUPPORTED_PROPERTY", result.detail)
    }

    @Test
    fun `권한 오류 코드는 실패로 유지된다`() {
        val result = failed("check", sdkException(ErrorCode.ERROR_SECURITY_PERMISSION))

        assertIs<VehicleResult.Failure>(result)
        assertEquals(ErrorCode.ERROR_SECURITY_PERMISSION, result.code)
    }

    /**
     * 배포된 Vehicle 2.3.2의 실제 예외 타입을 만든다.
     *
     * 문서상 `VehicleException`이 이 타입에 해당하며, 생성자는 오류 코드 id를
     * 문자열로 받아 `ErrorCode.findById`로 변환한다.
     */
    private fun sdkException(code: ErrorCode): Exception =
        Class
            .forName("ai.pleos.playground.vehicle.callback.exception.a")
            .getConstructor(String::class.java)
            .newInstance(code.id.toString()) as Exception
}

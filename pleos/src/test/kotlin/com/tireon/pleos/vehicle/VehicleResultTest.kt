package com.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.api.model.capability.AccessType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

        assertTrue(unsupported is VehicleResult.Unsupported)
        assertEquals("check", unsupported.operation)
        assertEquals("accessType=NONE", unsupported.detail)
    }

    @Test
    fun `Capability 조회 실패는 미지원으로 분류하고 메시지를 보존한다`() {
        val cause = IllegalStateException("ERROR_NOT_SUPPORTED_PROPERTY")

        val result = VehicleResult.capabilityUnavailable("check", cause)

        assertTrue(result is VehicleResult.Unsupported)
        assertEquals("ERROR_NOT_SUPPORTED_PROPERTY", result.detail)
    }
}

package com.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Odometer
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import android.content.Context

/**
 * Pleos Vehicle SDK 진입점을 감싸는 세션.
 *
 * 공식 가이드(Obtaining a domain object)에 따라
 * `Vehicle(context)` 생성 → `initialize()` → `getXxx()` 순서로 사용하고,
 * 사용이 끝나면 `release()`로 해제한다.
 *
 * 문서 기준으로 비동기 콜백이 끝나기 전에 `release()`를 호출하면 안 되므로,
 * 호출 측은 [release] 시점을 화면 생명주기 종료에 맞춰 관리해야 한다.
 */
class VehicleSession(context: Context) {

    private val vehicle = Vehicle(context.applicationContext)
    private var initialized = false

    val isInitialized: Boolean
        get() = initialized

    /** 차량 서비스 연결을 시작한다. 이미 초기화된 경우 아무 작업도 하지 않는다. */
    fun initialize(): VehicleResult<Unit> {
        if (initialized) return VehicleResult.Success(Unit)

        return try {
            vehicle.initialize()
            initialized = true
            VehicleResult.Success(Unit)
        } catch (exception: Exception) {
            VehicleResult.Failure(OPERATION_INITIALIZE, exception)
        }
    }

    /** 세션이 초기화된 경우에만 odometer 도메인 객체를 반환한다. */
    fun odometer(): VehicleResult<Odometer> {
        if (!initialized) {
            return VehicleResult.Failure(
                OPERATION_ODOMETER,
                IllegalStateException("VehicleSession이 초기화되지 않았습니다."),
            )
        }
        return VehicleResult.Success(vehicle.getOdometer())
    }

    /** 차량 서비스 연결을 해제한다. */
    fun release() {
        if (!initialized) return
        vehicle.release()
        initialized = false
    }

    companion object {
        const val OPERATION_INITIALIZE = "initialize"
        const val OPERATION_ODOMETER = "odometer"
    }
}

/** Capability 결과에서 읽기 가능 여부만 판단한다. */
internal fun AccessType.canRead(): Boolean =
    this == AccessType.READ || this == AccessType.READ_WRITE

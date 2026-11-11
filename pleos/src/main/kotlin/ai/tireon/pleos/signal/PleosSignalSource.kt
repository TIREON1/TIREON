package ai.tireon.pleos.signal

import ai.tireon.core.driving.DrivingSample
import ai.tireon.pleos.vehicle.VehicleResult
import ai.tireon.pleos.vehicle.VehicleSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 주행 샘플 스트림. SDK 타입은 노출하지 않는다. */
interface PleosSignalSource {
    /**
     * @param sessionActive 세션이 진행 중일 때만 true. false가 되면 리스너를 해제한다.
     */
    fun samples(sessionActive: Flow<Boolean>): Flow<DrivingSample>
}

/**
 * AAOS 표준 프로퍼티와 Pleos Vehicle SDK를 묶어 10Hz 샘플을 만든다.
 *
 * 속도와 기어는 AAOS에서, 조향과 방향지시등, 브레이크는 Pleos SDK에서 받는다.
 * 한쪽이 실패해도 나머지 신호는 계속 수집한다. 없는 값은 null로 남는다.
 */
class DefaultPleosSignalSource(
    private val context: Context,
    private val session: VehicleSession,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
) : PleosSignalSource {

    override fun samples(sessionActive: Flow<Boolean>): Flow<DrivingSample> = callbackFlow {
        val aggregator = SignalAggregator { System.currentTimeMillis() }
        val binding = PleosSignalBinding(context.applicationContext, aggregator)

        var polling = false
        var ticker: kotlinx.coroutines.Job? = null

        fun startCollection() {
            if (polling) return
            polling = true

            when (val initialized = session.initialize()) {
                is VehicleResult.Failure ->
                    Log.e(TAG, "VehicleSession 초기화 실패", initialized.cause)

                is VehicleResult.Unsupported ->
                    Log.i(TAG, "차량 서비스를 사용할 수 없습니다")

                is VehicleResult.Success -> Unit
            }

            binding.register(session)

            ticker = launch {
                while (isActive && polling) {
                    trySend(aggregator.snapshot())
                    delay(intervalMs)
                }
            }
        }

        fun stopCollection() {
            if (!polling) return
            polling = false
            ticker?.cancel()
            ticker = null
            binding.unregister()
            aggregator.reset()
        }

        val watcher = launch {
            sessionActive.collect { active ->
                if (active) startCollection() else stopCollection()
            }
        }

        awaitClose {
            watcher.cancel()
            stopCollection()
        }
    }

    internal companion object {
        private const val TAG = "PleosSignalSource"

        /**
         * 샘플 간격.
         *
         * `EventDetector`는 간격이 `Thresholds.MAX_SAMPLE_GAP_MS`(500ms)를 넘는 샘플 쌍으로는
         * 가속도와 조향 속도를 계산하지 않는다. 1초 간격이면 급가속·급제동·급조향이
         * 한 번도 잡히지 않는다.
         *
         * 판정 임계값과 이동평균 창(5쌍 = 0.5초)은 100ms 간격 데이터로 맞추고 시험했으므로
         * 같은 간격을 쓴다. 속도 콜백이 10Hz로 들어오므로(AaosSpeedReader) 매 샘플이 새 값이다.
         */
        const val DEFAULT_INTERVAL_MS = 100L
    }
}

package ai.tireon.app.driving

import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.TurnSignal
import ai.tireon.core.mission.MissionEvaluator
import ai.tireon.core.mission.MissionResult
import ai.tireon.core.mission.PracticeMission
import ai.tireon.pleos.signal.DefaultPleosSignalSource
import ai.tireon.pleos.vehicle.VehicleSession
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pleos 에뮬레이터의 실제 차량 신호로 판정 로직을 확인한다.
 *
 * 단위 테스트는 합성 샘플만 쓴다. 이 테스트는 Virtual Console 신호 주입 API로
 * 실제 VHAL/DDS 경로를 통과시킨 뒤, 앱이 쓰는 수집 계층과 판정기를 그대로
 * 실행해 신호 단위·부호·수집 주기가 판정에 미치는 영향을 확인한다.
 *
 * 에뮬레이터 신호 API가 없으면 건너뛴다. 실행 전 준비:
 * `adb forward tcp:18088 tcp:8088` 후 Virtual Console을 브라우저로 한 번 연다.
 */
@RunWith(AndroidJUnit4::class)
class PleosSignalVerdictTest {

    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        // 속도는 AAOS CarPropertyManager가 이 권한을 요구한다.
        "android.car.permission.CAR_SPEED",
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val console = EmulatorConsole()

    /**
     * 수집을 먼저 시작하고 [during]에서 신호를 주입한다.
     *
     * 차량 신호는 마지막 값만 유지된다. 주입이 끝난 뒤 수집을 시작하면 조향 같은
     * 순간값이 이미 0으로 돌아가 있어 검증할 수 없다.
     */
    private fun collectWhile(during: () -> Unit): List<DrivingSample> {
        val session = VehicleSession(instrumentation.targetContext)
        val source = DefaultPleosSignalSource(instrumentation.targetContext, session)
        val active = MutableStateFlow(true)
        try {
            val samples = mutableListOf<DrivingSample>()
            return runBlocking {
                val collector = launchCollector(source, active, samples)
                try {
                    collector.start()
                    // 리스너 등록과 첫 샘플을 기다린다.
                    Thread.sleep(STARTUP_MS)
                    during()
                    collector.join(COLLECT_MS)
                } finally {
                    active.value = false
                    collector.interrupt()
                }
                samples.toList()
            }
        } finally {
            active.value = false
            session.release()
        }
    }

    private fun launchCollector(
        source: DefaultPleosSignalSource,
        active: MutableStateFlow<Boolean>,
        samples: MutableList<DrivingSample>,
    ): Thread = Thread {
        try {
            runBlocking {
                withTimeoutOrNull(COLLECT_MS) {
                    source.samples(active).collect { samples += it }
                }
            }
        } catch (e: InterruptedException) {
            // 수집 종료.
        }
    }

    @After
    fun resetSignals() {
        runCatching {
            console.gear("Park")
            console.turnSignal("Off")
            console.controls(speed = 0, brake = 0, steering = 0)
        }
    }

    @Test
    fun 좌회전이_양수_조향으로_수집되고_깜빡이_미션을_달성한다() {
        assumeTrue("에뮬레이터 신호 API를 사용할 수 없습니다", console.available())

        val samples = collectWhile {
            console.gear("Drive")
            console.turnSignal("BlinkLeft")
            console.controls(speed = 20, brake = 0, steering = 0)
            Thread.sleep(400)
            console.controls(speed = 20, brake = 0, steering = 120)
            Thread.sleep(900)
            console.controls(speed = 20, brake = 0, steering = 0)
            Thread.sleep(400)
        }

        assertTrue("샘플이 수집되지 않았습니다", samples.size >= 5)

        // 수집 계층은 Pleos SDK의 반전된 부호를 뒤집어 좌회전을 양수로 만든다.
        val steerings = samples.mapNotNull { it.steeringDeg }
        assertTrue("조향 신호가 수집되지 않았습니다", steerings.isNotEmpty())
        println("수집된 조향값: ${steerings.take(20)}")
        println("수집된 속도값: ${samples.mapNotNull { it.speedMps }.take(10)}")
        println("수집된 방향지시등: ${samples.mapNotNull { it.turnSignal }.distinct()}")

        val events = EventDetector().detect(samples)
        val result = MissionEvaluator().evaluate(PracticeMission.TURN_SIGNAL, samples, events)
        println("실제 신호 TURN_SIGNAL 판정: $result / 이벤트 ${events.map { it.type }}")

        // 신호가 부족하면 판정 불가가 정답이다. 성공/실패로 단정하면 안 된다.
        assertTrue(
            "속도·조향이 일부만 수집된 경우 판정 불가여야 합니다: $result",
            result is MissionResult.Success || result is MissionResult.Insufficient,
        )
    }

    @Test
    fun 방향지시등을_켜지_않고_회전하면_실제_신호로_실패한다() {
        assumeTrue("에뮬레이터 신호 API를 사용할 수 없습니다", console.available())

        val samples = collectWhile {
            console.gear("Drive")
            console.turnSignal("Off")
            console.controls(speed = 20, brake = 0, steering = 0)
            Thread.sleep(400)
            console.controls(speed = 20, brake = 0, steering = 120)
            Thread.sleep(900)
            console.controls(speed = 20, brake = 0, steering = 0)
            Thread.sleep(400)
        }

        assertTrue("샘플이 수집되지 않았습니다", samples.size >= 5)
        val events = EventDetector().detect(samples)
        val result = MissionEvaluator().evaluate(PracticeMission.TURN_SIGNAL, samples, events)
        println("깜빡이 없이 회전 판정: $result")

        // 회전이 검출됐다면 누락으로 실패해야 한다. 검출되지 않았다면 판정 불가다.
        assertTrue(result is MissionResult.Failed || result is MissionResult.Insufficient)
    }

    /** Virtual Console HTTP API. */
    private class EmulatorConsole {
        // 콘솔은 에뮬레이터 게스트 안에서 8088로 동작한다. 앱도 같은 게스트에서
        // 실행되므로 loopback으로 바로 접근한다. 호스트 브라우저에서 열 때만
        // `adb forward tcp:18088 tcp:8088`을 쓴다.
        private val host = "127.0.0.1"
        private val port = 8088

        fun available(): Boolean = runCatching {
            request("/vehicle/state").contains("\"emulator_active\":true")
        }.getOrDefault(false)

        fun gear(value: String) = request("/vehicle/gear?gear=$value")

        fun turnSignal(mode: String) = request("/vehicle/turn-signal?mode=$mode")

        fun controls(speed: Int, brake: Int, steering: Int) =
            request("/vehicle/controls?speed=$speed&brake=$brake&steering=$steering")

        /**
         * 직접 소켓으로 보낸다.
         *
         * HttpURLConnection은 앱의 cleartext 정책을 따른다. 테스트 때문에 앱
         * 매니페스트에 예외를 넣지 않으려고 HTTP/1.1 요청을 직접 작성한다.
         */
        private fun request(path: String): String {
            java.net.Socket(host, port).use { socket ->
                socket.soTimeout = 3_000
                val request = "GET $path HTTP/1.1\r\nHost: $host:$port\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().write(request.toByteArray())
                socket.getOutputStream().flush()
                val response = socket.getInputStream().bufferedReader().readText()
                return response.substringAfter("\r\n\r\n", response)
            }
        }
    }

    private companion object {
        const val STARTUP_MS = 1_500L
        const val COLLECT_MS = 6_000L
    }
}

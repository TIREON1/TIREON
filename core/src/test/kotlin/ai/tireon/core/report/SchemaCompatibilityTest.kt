package ai.tireon.core.report

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 이전 버전으로 저장된 파일을 읽을 수 있는지 확인한다.
 *
 * 필드를 추가할 때 기본값을 두면 기존 파일이 그대로 읽힌다. 이 성질이 깨지면
 * 업데이트 후 사용자의 기존 기록이 사라진다.
 */
class SchemaCompatibilityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `미션 필드가 없는 버전 1 기록을 읽는다`() {
        val legacy = """
            {
              "schemaVersion": 1,
              "id": "legacy-1",
              "startedAtMs": 1000,
              "durationSec": 120,
              "distanceKm": 1.5,
              "score": 88,
              "insufficientReason": null,
              "eventCounts": {"HARSH_BRAKE": 1}
            }
        """.trimIndent()

        val record = json.decodeFromString(DrivingSessionRecord.serializer(), legacy)

        assertEquals(1, record.schemaVersion)
        assertEquals("legacy-1", record.id)
        assertEquals(88, record.score)
        assertNull(record.mission)
        assertNull(record.missionSuccess)
        assertNull(record.missionDetail)
    }

    @Test
    fun `알 수 없는 필드가 있어도 읽는다`() {
        val future = """
            {
              "schemaVersion": 99,
              "id": "future-1",
              "startedAtMs": 2000,
              "durationSec": 60,
              "distanceKm": null,
              "score": null,
              "insufficientReason": "DURATION_TOO_SHORT",
              "eventCounts": {},
              "unknownField": "ignored"
            }
        """.trimIndent()

        val record = json.decodeFromString(DrivingSessionRecord.serializer(), future)

        assertEquals("future-1", record.id)
        assertEquals(99, record.schemaVersion)
    }

    @Test
    fun `버전 2 기록은 미션 정보를 유지한다`() {
        val current = DrivingSessionRecord(
            schemaVersion = 2,
            id = "current-1",
            startedAtMs = 3000,
            durationSec = 60,
            distanceKm = 1f,
            score = 90,
            insufficientReason = null,
            eventCounts = emptyMap(),
            mission = "SMOOTH_START",
            missionSuccess = true,
            missionDetail = "급가속 없이 출발했어요",
        )

        val encoded = json.encodeToString(DrivingSessionRecord.serializer(), current)
        val decoded = json.decodeFromString(DrivingSessionRecord.serializer(), encoded)

        assertEquals(current, decoded)
        assertEquals(2, decoded.schemaVersion)
    }

    @Test
    fun `버전 2 주행 미션에 없던 판정 상태를 추측하지 않는다`() {
        val legacy = """{"schemaVersion":2,"id":"driving-old","startedAtMs":1000,
            "durationSec":120,"distanceKm":1.0,"score":100,"insufficientReason":null,
            "eventCounts":{},"mission":"SMOOTH_DRIVING"}"""
        val record = json.decodeFromString<DrivingSessionRecord>(legacy)
        assertNull(record.missionEvaluated)
        assertNull(record.missionDetail)
    }

    @Test
    fun `버전 3은 정상 판정과 보류를 왕복 보존한다`() {
        for (evaluated in listOf(true, false, null)) {
            val record = DrivingSessionRecord(
                id = "driving-new", startedAtMs = 1000L, durationSec = 120,
                distanceKm = 1f, score = 90, insufficientReason = null, eventCounts = emptyMap(),
                mission = "SMOOTH_DRIVING", missionEvaluated = evaluated,
                missionDetail = "저장할 판정 결과",
            )
            val encoded = json.encodeToString(DrivingSessionRecord.serializer(), record)
            assertEquals(record, json.decodeFromString<DrivingSessionRecord>(encoded))
            assertEquals(3, record.schemaVersion)
        }
    }
}

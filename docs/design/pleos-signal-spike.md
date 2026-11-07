# Pleos 신호 스파이크 결과 (개정판)

날짜: 2026-11-07 (개정: 문서 확인 후)
대상: Pleos Connect Emulator v2.1 (AAOS android-34, arm64)
SDK: Vehicle 2.3.2 / vehicle-data 1.1.11 / FusedLocation 1.1.3

> **개정 사유**: 최초 조사는 SDK 바이너리만 확인해 "속도 불가"로 결론지었다.
> 이후 공식 문서 확인 결과 **속도는 AAOS 표준 API로 사용 가능**하다.
> 아래는 문서 + 실측을 모두 반영한 최종 결과다.

## 문서 근거

| 문서 | 핵심 내용 |
|---|---|
| [SDK API Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api) | "표준 데이터 → **AAOS Car API** → **속도**, 기어 상태, 외기 온도, 연료 레벨" |
| [API Compatibility & Availability](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability) | `PERF_VEHICLE_SPEED` **O (사용 가능)**, `PERF_STEERING_ANGLE` X, `PERF_ODOMETER` X |
| [Hardware Access Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture) | "Pleos Connect SDK가 제공하는 **표준 플랫폼 API**로 차량 상태 획득" |
| [TextToSpeech SDK](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro) | **ON_DEVICE 모드** — 네트워크·앱 등록 없이 오프라인 합성 |
| [SpeechToText SDK](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro) | **ON_DEVICE 모드** — 오프라인 인식 |
| [Emulator Release Note](https://document.pleos.ai/en/docs/pleos-only/release-note/emulator-release-note) | 에뮬레이터 v2.1에서 **LLM SDK 지원 종료** |

즉 Pleos는 **AAOS Alignment 정책**에 따라 속도 같은 표준 데이터는
`CarPropertyManager`로 읽도록 설계되어 있다. Pleos SDK에 없는 것이 정상이다.

### ADAS 주의

Pleos 웹사이트의 Develop 페이지에는 ADAS SDK가 "objects, lane markings,
parking spaces"를 제공한다고 소개되어 있다. 그러나:

- 공식 문서 트리에 ADAS 항목이 **없다** (`/ADAS/intro` → 안내 페이지)
- Maven 저장소에서 `ADAS` 아티팩트를 받을 수 없다 (404)
- [API Compatibility](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)에서
  "Pleos Connect SDK – ADAS | **Not supported**"

**결론: ADAS는 사용하지 않는다.** 문서와 배포 채널이 모두 부재하므로
근거 없는 구현이 된다. 전방 차량·차선 인식 기반 코칭은 제외한다.

### Gleo AI 주의

- Gleo AI SDK는 문서상 "Fully compatible"로 표기되어 있다.
- 그러나 **Maven 저장소에 아티팩트가 없다**(404).
- 에뮬레이터 v2.1 릴리스 노트: **"End of support for LLM SDK"**.
- 남아 있는 것은 **Gleo Interface**(음성 명령 function calling 연동)이지
  LLM 추론 SDK가 아니다.

**결론: AI 코칭은 앱 자체 구현 또는 외부 API로 처리한다.**
Gleo Interface는 추후 음성 명령 연동 옵션으로만 검토한다.

## 방법

1. `:pleos`에 `SignalProbe`, `LocationProbe`를 두어 Vehicle SDK 신호 검증
2. `:app`에 `AaosSpeedActivity`를 두어 AAOS `CarPropertyManager` 검증
3. 에뮬레이터 Virtual Console(`localhost:8088`) HTTP API + UI 조작으로 신호 주입
4. logcat으로 리스너 반응 확인

주입 명령:

```bash
adb forward tcp:18088 tcp:8088
curl "http://localhost:18088/vehicle/turn-signal?mode=BlinkLeft"
curl "http://localhost:18088/vehicle/gear?gear=Drive"
curl "http://localhost:18088/vehicle/odometer?traveled_distance_km=1234&usage_time_min=567"
curl "http://localhost:18088/vehicle/controls?brake=90&steering=120"
adb emu geo fix -122.084 37.4252
```

## 검증 결과

### 속도 — 사용 가능 (문서 + 실측)

- AAOS `Car.PROPERTY_SERVICE` → `CarPropertyManager` 정상 획득
- `PERF_VEHICLE_SPEED`(0x11600207) **읽기 성공**
- 단위: **m/s** (AAOS 표준). `val mps = 2.5f` → 9 km/h
- 리스너로 **실시간 갱신 확인** (2.5 m/s → 0.556 m/s 변화 수신)
- 필요 권한: `android.car.permission.CAR_POWERTRAIN` (또는 `CAR_SPEED`)
- 에뮬레이터 노출 프로퍼티 9개 중 speed 2종 포함

```
I/SpikeAaos: getProperty speed=CarPropertyValue{mPropertyId=0x11600207,
  propertyName=PERF_VEHICLE_SPEED, mValue=2.5}
I/SpikeAaos: SPEED prop=0x11600207 value=0.5555556
```

#### 샘플레이트 실측

`registerCallback(hz=1/10/50)` 모두 등록 성공. 실제 갱신 타임스탬프 간격:

```
intervals(ms): 200.5, 101.0, 199.6, 100.2, 202.7, 110.3,
               185.2, 100.8, 103.1, 102.2, 193.9, 101.7
median: 110.3 ms   mean: 141.8 ms   => 약 5~10 Hz
```

- **최소 단위 100ms**. 급가속/급제동 판정에 충분하다.
- 100ms 간격에서 27.8 m/s(100km/h) → 0 m/s 변화 시 평균 감가속도 계산 가능.
- 주의: `getCarPropertyConfig()`는 이 에뮬레이터에서 **null 반환**.
  `minSampleRate`/`maxSampleRate`는 조회할 수 없다. 요청 주파수는
  1Hz로 두고 타임스탬프 기반으로 실제 dt를 계산하는 방식이 안전하다.

### Pleos Vehicle SDK — 사용 가능 (실측)

| 신호 | API | 결과 |
|---|---|---|
| 누적 주행거리 | `Odometer.registerDistanceDriven` | ✅ 1234.0 |
| 브레이크 페달 | `Brake.registerBrakePedalStatus` | ✅ true/false |
| 조향각 | `SteeringWheel.registerSteeringWheelAngle` | ✅ -120/+200 (**주입값 부호 반전**) |
| 방향지시등 | `TurnSignal.registerTurnSignalState` | ✅ LEFT/RIGHT/OFF |
| 기어 | `Gear.registerGearSelection` | ✅ D/R/P/N |
| EV 배터리 SoC | `EvBattery.registerEvBatteryStateOfCharge` | ✅ 42/100 |
| GPS | `FusedLocation.registerFusedLocationCallback` | ✅ 1Hz, 좌표 이동 반영 |

### 사용 불가

| 항목 | 사유 |
|---|---|
| 트립 거리/시간 | `ERROR_NOT_SUPPORTED_PROPERTY` (SDK 경로 없음) |
| 연료 | 프로퍼티 미지원 (EV 차량) |
| 텔테일·안전벨트·타이어 | `PRIVATE_CAR_SAFETY` 필요 — 일반 앱 부여 불가 |
| `getCapabilityInfo` | `PRIVATE_CAR_INFO` 필요 — 일반 앱 부여 불가 |
| `PERF_STEERING_ANGLE` (AAOS) | 문서상 X. SDK의 SteeringWheel 도메인을 쓴다 |
| `PERF_ODOMETER` (AAOS) | 문서상 X. SDK의 Odometer 도메인을 쓴다 |

## 최종 신호 소스

```
속도          → AAOS CarPropertyManager (PERF_VEHICLE_SPEED, m/s, ~5-10Hz)
기어          → Pleos Gear
거리          → Pleos Odometer (누적)
브레이크 페달 → Pleos Brake
조향각        → Pleos SteeringWheel (부호 반전 주의)
방향지시등    → Pleos TurnSignal
배터리        → Pleos EvBattery
GPS           → FusedLocation (보조)
음성 코칭     → TextToSpeech ON_DEVICE (오프라인)
```

## 설계 결정

1. **속도는 AAOS에서 m/s로 받아 앱에서 km/h로 변환**한다. 화면 표기는 실측 근거가 있다.
2. **AAOS 접근 코드는 `:pleos` 모듈 안에** 두어 SDK 격리 원칙을 유지한다.
   `android.car.jar`는 `compileOnly`로 컴파일하고 런타임은 차량 프레임워크가 제공한다.
3. **샘플레이트는 1Hz로 요청하되, 실제 dt는 타임스탬프로 계산**한다.
   `getCarPropertyConfig`가 null이므로 선언된 레이트를 신뢰하지 않는다.
   최소 100ms 간격이 확보되므로 급가속 판정이 가능하다.
4. **급가속/급제동은 속도 미분 + 브레이크 페달** 두 신호로 판정한다.
   노이즈 억제를 위해 3~5샘플 이동 평균을 적용한다.
5. **급조향은 조향각 변화율**. 부호 반전을 흡수하려 절댓값과 부호 정규화를 쓴다.
6. **깜빡이 누락** = `TurnSignal == OFF` + 조향각 임계 초과.
7. **주행 시간은 앱 자체 계측**한다(SDK 트립 시간 미지원).
8. **Safety/Tire 계열은 데모·기능에서 제외**한다.
9. **ADAS는 사용하지 않는다** (문서·아티팩트 부재).
10. **AI 코칭 문장은 TTS ON_DEVICE로 읽어준다** (오프라인 보장).
    문장 생성은 룰 기반 우선, 외부 LLM은 선택적 확장.

## 남은 확인 사항

- `PERF_VEHICLE_SPEED`가 실차에서도 O인지 (문서상 O, 실차 미검증)
- `CAR_POWERTRAIN` 권한이 앱 심사에서 승인되는지 (Pleos App Market 사전 협의 필요)
- 조향각 부호 반전이 실차에서도 동일한지
- TTS ON_DEVICE 한국어 품질 (에뮬레이터에서 미검증)
- 에뮬레이터에서 `emulator_active`가 포커스를 잃으면 false로 리셋됨
  → 속도 주입 테스트 시 Virtual Console을 전면에 유지해야 한다

## 산출물

- 스파이크 코드는 검증 후 제거했다.
- 재현은 이 문서의 주입 명령과 신호 소스 표로 가능하다.

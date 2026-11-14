# Pleos 신호 스파이크 결과 (개정판)

날짜: 2026-11-07 (개정: 공식 문서 확인 후)
대상: Pleos Connect Emulator v2.1 (AAOS android-34, arm64)
SDK: Vehicle 2.3.2, vehicle-data 1.1.11, FusedLocation 1.1.3

> 개정 사유: 최초 조사는 SDK 바이너리만 확인하고 "속도 불가"로 결론지었습니다.
> 이후 공식 문서를 확인한 결과 속도는 AAOS 표준 API로 사용할 수 있습니다.
> 아래는 문서와 실측을 모두 반영한 최종 결과입니다.

## 문서 근거

| 문서 | 핵심 내용 |
|---|---|
| [SDK API Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api) | 표준 데이터(속도, 기어 상태, 외기 온도, 연료 레벨)는 AAOS Car API로 접근 |
| [API Compatibility & Availability](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability) | `PERF_VEHICLE_SPEED` O (사용 가능), `PERF_STEERING_ANGLE` X, `PERF_ODOMETER` X |
| [Hardware Access Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture) | Pleos Connect SDK가 제공하는 표준 플랫폼 API로 차량 상태 획득 |
| [TextToSpeech SDK](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro) | ON_DEVICE 모드는 네트워크와 앱 등록 없이 오프라인 합성 |
| [SpeechToText SDK](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro) | ON_DEVICE 모드로 오프라인 인식 |
| [Emulator Release Note](https://document.pleos.ai/en/docs/pleos-only/release-note/emulator-release-note) | 에뮬레이터 v2.1에서 LLM SDK 지원 종료 |

Pleos는 AAOS Alignment 정책을 따릅니다. 그래서 속도 같은 표준 데이터는
`CarPropertyManager`로 읽도록 설계되어 있습니다. Pleos SDK에 속도 API가
없는 것이 정상입니다.

### ADAS 주의

Pleos 웹사이트의 Develop 페이지에는 ADAS SDK가 objects, lane markings,
parking spaces를 제공한다고 소개되어 있습니다. 하지만 확인해 보니:

- 공식 문서 트리에 ADAS 항목이 없습니다 (`/ADAS/intro`는 안내 페이지).
- Maven 저장소에서 ADAS 아티팩트를 받을 수 없습니다 (404).
- [API Compatibility](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)에
  "Pleos Connect SDK - ADAS | Not supported"로 표기되어 있습니다.

결론은 ADAS를 사용하지 않는 것입니다. 문서와 배포 채널이 모두 없으므로
근거 없는 구현이 됩니다. 전방 차량과 차선 인식 기반 코칭은 제외합니다.

### Gleo AI 주의

- Gleo AI SDK는 문서상 Fully compatible로 표기되어 있습니다.
- 그러나 Maven 저장소에 아티팩트가 없습니다 (404).
- 에뮬레이터 v2.1 릴리스 노트에 "End of support for LLM SDK"가 있습니다.
- 남아 있는 것은 Gleo Interface(음성 명령 function calling 연동)이고
  LLM 추론 SDK가 아닙니다.

결론은 AI 코칭을 앱 자체 구현이나 외부 API로 처리하는 것입니다.
Gleo Interface는 추후 음성 명령 연동 옵션으로만 검토합니다.

### Gleo Interface 조사 결과

Gleo AI 연동 방법을 문서로 확인했습니다. 에뮬레이터에서는 검증할 수 없어
구현은 보류합니다.

**구조**

```
[운전자 음성] → [Gleo AI (LLM 내장)] → [Intent] → [TIREON 앱]
                                            ↓
                                    [액션 처리 후 결과]
                                            ↓
                              [Broadcast] → [Gleo AI가 자연어 응답 생성]
```

LLM은 Gleo AI 쪽에 있습니다. 앱은 함수만 노출하므로 API 키가 필요 없고,
숫자 불변 가드도 필요 없습니다. LLM이 숫자를 만들지 않고 앱이 준 값을 말하기
때문입니다.

**App Schema** (`assets/schema.json`)

```json
{
  "packageName": "ai.tireon.app",
  "appDescription": "Driving coach that reports driving scores and practice missions.",
  "invokeWord": "티레온",
  "version": "0.1.0",
  "actionCalls": [
    {
      "type": "function",
      "function": {
        "name": "GET_DRIVING_SCORE",
        "description": "Returns the most recent driving score and the top issue.",
        "parameters": { "type": "object", "properties": {}, "required": [] },
        "component": "service"
      }
    }
  ]
}
```

**Callback**

```kotlin
Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
    putExtra("packageName", packageName)
    putExtra("response", responseJson)
    sendBroadcast(this)
}
```

`responseJson`은 다음 형식입니다.

```json
{
  "functionName": "GET_DRIVING_SCORE",
  "status": "success",
  "message": "The latest driving score was retrieved.",
  "data": "[{\"score\":85,\"topIssue\":\"harsh braking\"}]"
}
```

**제약**

| 항목 | 제약 |
|---|---|
| 함수 개수 | 최대 3개 |
| 설명 언어 | 영문 필수 |
| Callback 타임아웃 | 7초 |
| Schema 위치 | `assets/schema.json` (파일명 고정) |
| Schema 검토 | Pleos 측 사전 검토 필요 |
| 상태 값 | success, asking, fail |
| 응답 모드 | voice, beep, silent |

**에뮬레이터 확인 결과: 구현 불가**

| 확인 항목 | 결과 |
|---|---|
| Gleo AI 앱 본체 | 없음 |
| `APP_ACTION_RESPONSE` 수신자 | 없음 (쿼리 0건) |
| 있는 것 | `LLMModelProviderSerivce.apk` (모델 제공자 서비스만) |

`ai.pleos.gleoai.model.provider`는 LLM 모델을 제공하는 서비스이고, 사용자 음성을
받아 function calling을 수행하는 Gleo AI 앱이 아닙니다. 에뮬레이터 릴리스 노트의
"End of support for LLM SDK"와 일치합니다.

**결론:** App Schema와 Callback 코드는 준비할 수 있지만, 대화 상대가 없어 동작을
검증할 수 없습니다. 실차에서만 확인 가능합니다.


## 신호 수집 계층 통합 검증

`:pleos`에 신호 수집 계층을 구현한 뒤 에뮬레이터에서 실제 수집을 확인했습니다.

### 결과

1Hz 샘플이 정상 생성되고, 주입한 값이 그대로 반영됩니다.

| 샘플 | gear | speed | steer | brake | signal |
|---|---|---|---|---|---|
| n=4 | DRIVE | 0.0 | 0.0 | false | OFF |
| n=5 | DRIVE | 0.0 | 0.0 | false | LEFT |
| n=7 | DRIVE | 12.5 | 120.0 | false | LEFT |
| n=9 | DRIVE | 12.5 | 0.0 | true | LEFT |

- 속도 12.5 m/s는 주입값 45 km/h에 해당합니다.
- 조향 120.0은 주입값 120과 부호까지 일치합니다.
- 기어, 브레이크, 방향지시등 모두 주입대로 바뀝니다.
- 세션 종료 신호를 보내자 샘플이 정확히 멈췄습니다(n=20이 마지막). 리스너 해제가 동작합니다.

### 검증 중 발견한 것

구현 단계에서 두 가지를 실측으로 바로잡았습니다.

1. **차속 권한은 `CAR_SPEED`다.** `CAR_POWERTRAIN`만 선언하면 `getProperty`가
   `SecurityException: requires android.car.permission.CAR_SPEED`로 실패합니다.
   스파이크에서 성공했던 것은 두 권한을 함께 선언했기 때문입니다.
2. **기어는 Pleos SDK 대신 AAOS `GEAR_SELECTION`을 쓴다.** Pleos SDK의
   `Gear` 도메인은 deprecated이고, 공식 안내가 "Google GEAR_SELECTION property를
   쓰라"고 지시합니다. Pleos 문서의 API 선택 기준도 기어를 AAOS 표준 데이터로
   분류합니다. 문서상 `GEAR_SELECTION`은 O(사용 가능)입니다.

### 참고: 프로퍼티 지원 범위

`dumpsys car_service`에서 확인한 차속 프로퍼티 설정입니다.

```
Property:0x11600207, PERF_VEHICLE_SPEED, access:0x1, changeMode:0x2
fs min:10.0, fs max:20.0
```

권한이 있으면 요청 주파수 1Hz, 10Hz, 20Hz 모두 등록됩니다. 선언된 최소
주파수는 10Hz지만 더 낮은 요청을 거부하지는 않습니다. 집계는 별도 주기로 하므로
콜백 주기가 높아도 샘플 수는 늘지 않습니다.

## 연습 세션 화면 검증

`PracticeActivity`를 구현한 뒤 에뮬레이터에서 확인한 결과입니다.

### 확인된 것

| 항목 | 결과 |
|---|---|
| 앱 셸 렌더링 | 정상. landscape로 인식돼 측면 레일(H, P, R, S) 표시 |
| 홈 화면 | 점수 카드("아직 주행 기록이 없어요"), 주행 요약, 연습 시작 버튼 |
| 연습 세션 진입 | 홈의 버튼으로 `PracticeActivity` 실행 |
| READY 화면 | 미션 이름("부드러운 출발"), 설명, 시작 버튼 정상 표시 |
| DO 선언 인식 | 홈스크린 로그에 `distractionOptimized: true` |

### 확인하지 못한 것

| 항목 | 사유 |
|---|---|
| 시작 버튼 동작 | `adb shell input tap`이 앱에 전달되지 않음 |
| RUNNING 상태 화면 | 위와 같은 이유로 상태 전이를 유발하지 못함 |
| 제한 활성 시 숫자 하나 표시 | 상태 전이를 유발하지 못해 확인 불가 |

터치 주입이 앱에 닿지 않는 원인은 Pleos 멀티윈도우의 터치 영역 처리로 보입니다.
홈스크린 로그의 터치 가능 영역 계산에서 앱 영역이 제외되는 것을 확인했습니다.

`PracticeActivity`가 `exported=false`라 adb로 직접 실행할 수 없는 점도 있습니다.
검증 중에는 임시로 `exported=true`로 바꿔 실행했고, 확인 후 되돌렸습니다.

### 다음 검증 방법

에뮬레이터 UI를 직접 조작하거나, 계측 테스트(`androidTest`)로 상태 전이를
검증하는 편이 낫습니다. `adb input`은 멀티윈도우 환경에서 신뢰할 수 없습니다.

## 계측 테스트 환경 특성

계측 테스트를 도입하면서 확인한 에뮬레이터 특성입니다.

### 근본 원인: GPU 모드 선택 실수

계측 테스트의 간헐 실패는 **에뮬레이터 GPU 모드** 때문이었습니다.

처음에는 `-gpu swiftshader_indirect`(소프트웨어 렌더링)로 실행했습니다. 호스트에
Metal GPU가 있는데도 쓰지 않은 것이 원인이었습니다. 소프트웨어 렌더링에서는
Pleos의 Driving View 3D 렌더링이 전부 CPU로 돌아 GPU 컴포저가 96%를 점유하고,
테스트용 액티비티가 RESUMED로 전환되지 못합니다.

측정 비교:

| GPU 모드 | GPU 컴포저 | idle | 계측 테스트 |
|---|---|---|---|
| `swiftshader_indirect` | 96% | 64% | 2~6개 산발 실패 |
| `host` | 7.6% | 212% | 24개 전부 통과 |

`host` 모드에서 실제 렌더러:

```
GLES: Android Emulator OpenGL ES Translator (Apple M5)
      OpenGL ES 3.0 (4.1 Metal - 91.7)
```

**에뮬레이터 실행 명령:**

```bash
emulator -avd Pleos_Connect_v2.1 -gpu host
```

`swiftshader_indirect`는 쓰지 않습니다. `pm disable-user`로 Driving View를 끄는
우회도 필요 없습니다. GPU가 병목이 아니게 되면 3D 씬이 돌아도 테스트가 통과합니다.

화면 자체를 끄면(`KEYCODE_SLEEP`) 액티비티가 RESUMED가 될 수 없어 테스트가 전부
실패합니다. 화면은 켜 두어야 합니다.

### 실패 메시지 두 종류

GPU 모드를 잘못 선택했을 때 나타나는 메시지입니다. `host` 모드로 바꾸면 발생하지
않습니다.

| 실패 | 메시지 |
|---|---|
| 액티비티 미전환 | `Activity never becomes requested state` |
| Compose 계층 없음 | `No compose hierarchies found in the app` |

`host` 모드에서 24개 전체를 실행해 전부 통과하는 것을 확인했습니다. 재시도 설정이
필요하지 않습니다.

### 터치 주입 대신 계측 테스트를 쓰는 이유

`adb shell input tap`은 Pleos 멀티윈도우 환경에서 앱에 전달되지 않습니다. 계측
테스트는 Compose 런타임 안에서 직접 상호작용하므로 이 제약을 받지 않습니다.

## 방법

1. `:pleos`에 `SignalProbe`와 `LocationProbe`를 두고 Vehicle SDK 신호 검증
2. `:app`에 `AaosSpeedActivity`를 두고 AAOS `CarPropertyManager` 검증
3. 에뮬레이터 Virtual Console(`localhost:8088`)의 HTTP API와 UI 조작으로 신호 주입
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

## DO와 CarUxRestrictions 검증 결과

DO(Distraction Optimization) 차단 동작을 실제 주행 상태에서 검증했습니다.

### 검증 방법

Virtual Console로 기어를 Drive, 속도를 45km/h로 만들어 주행 상태를 만든 뒤
DO 선언 액티비티와 미선언 액티비티를 각각 실행했습니다. 별도 명령 없이
`emulator_active`가 켜져 있으면 주행 상태가 유지됩니다.

### 결과

DO는 정상 동작합니다.

| 액티비티 | DO 선언 | 주행 중 결과 |
|---|---|---|
| `DoProbeActivity` | `true` | 정상 실행 (`onCreate` -> `onResume`) |
| `NoDoProbeActivity` | 없음 | 차단됨 (빈 화면) |
| 기존 `MainActivity` | 없음 | 차단됨 ("주행 중에는 사용할 수 없습니다") |

### 확인된 것

| 항목 | 결과 |
|---|---|
| 매니페스트 메타데이터 인식 | 홈스크린 로그에 `distractionOptimized: true` |
| DO 선언 액티비티 생존 | `onCreate`와 `onResume` 정상 호출 |
| DO 미선언 액티비티 차단 | 포커스는 잡지만 빈 화면으로 대체됨 |
| `CarUxRestrictionsManager` 획득 | 성공 |
| UX 제한 상태 | 처음부터 활성. `Requires DO? true` |

### 처음에 잘못 판단한 부분

처음에는 `cmd car_service enable-uxr`가 `requires non-user build`로 차단되는
것을 보고 "UX 제한을 켤 수 없으니 DO를 검증할 수 없다"고 결론지었습니다.

이것이 틀렸습니다. `enable-uxr`는 제한을 강제로 켜는 명령이고, 에뮬레이터에서는
제한이 이미 켜진 상태로 부팅됩니다. `dumpsys car_service`에 그 증거가 처음부터
있었습니다.

```
baseline mode UXR:
  State: unknown   num restrictions: 1
  Requires DO?  true
```

주행 상태만 만들면 DO 동작을 그대로 시험할 수 있었습니다.

### 검증이 불가능한 것

`get-do-activities`와 `inject-vhal-event`는 여전히 `requires non-user build`로
차단됩니다. 다만 DO 동작 확인에는 필요하지 않았습니다.

### 대응: DO 선언과 자체 축소를 함께 둔다

DO가 동작하는 것을 확인했지만, 앱이 `CarUxRestrictions`를 구독해 스스로 화면을
축소하는 구조는 그대로 유지합니다. 이유는 두 가지입니다.

1. 지역과 차량에 따라 제한 범위가 달라집니다. 플랫폼이 막아주더라도 연습
   화면이 숫자 하나만 남기는 편이 안전합니다.
2. 제한이 걸린 상태에서 어떤 UI를 보여줄지는 앱이 결정해야 합니다. 플랫폼은
   액티비티 단위로 허용 여부만 판단합니다.

이 로직은 순수 함수로 분리해 JVM에서 검증합니다.
### 남는 위험

실차에서 DO가 동작하지 않으면 주행 중 연습 화면이 플랫폼에 의해 가려집니다.
이 경우 연습 기능은 주차 상태에서만 쓸 수 있습니다. 앱이 멈추거나 안전 규정을
위반하지는 않습니다.

## 검증 결과

### 속도: 사용 가능 (문서와 실측)

- AAOS `Car.PROPERTY_SERVICE`에서 `CarPropertyManager`를 정상적으로 얻었습니다.
- `PERF_VEHICLE_SPEED`(0x11600207) 읽기에 성공했습니다.
- 단위는 m/s입니다. 값 2.5는 9 km/h입니다.
- 리스너로 실시간 갱신을 확인했습니다 (2.5 m/s에서 0.556 m/s로 변하는 것을 수신).
- 필요 권한은 `android.car.permission.CAR_POWERTRAIN`입니다 (`CAR_SPEED`도 가능).
- 에뮬레이터에 노출된 프로퍼티 9개 중 속도 관련 2종이 포함되어 있습니다.

```
I/SpikeAaos: getProperty speed=CarPropertyValue{mPropertyId=0x11600207,
  propertyName=PERF_VEHICLE_SPEED, mValue=2.5}
I/SpikeAaos: SPEED prop=0x11600207 value=0.5555556
```

#### 샘플레이트 실측

`registerCallback(hz=1/10/50)` 모두 등록에 성공했습니다. 실제 갱신
타임스탬프 간격은 다음과 같습니다.

```
intervals(ms): 200.5, 101.0, 199.6, 100.2, 202.7, 110.3,
               185.2, 100.8, 103.1, 102.2, 193.9, 101.7
median: 110.3 ms   mean: 141.8 ms   약 5~10 Hz
```

- 최소 간격이 100ms라서 급가속과 급제동 판정에 충분합니다.
- 100ms 간격이면 27.8 m/s(100km/h)에서 0 m/s로 바뀔 때의 평균 감가속도를
  계산할 수 있습니다.
- 주의할 점이 있습니다. `getCarPropertyConfig()`가 이 에뮬레이터에서 null을
  반환합니다. `minSampleRate`와 `maxSampleRate`를 조회할 수 없습니다.
  그래서 요청 주파수는 1Hz로 두고, 실제 dt는 타임스탬프 차분으로 계산하는
  방식이 안전합니다.

### Pleos Vehicle SDK: 사용 가능 (실측)

| 신호 | API | 결과 |
|---|---|---|
| 누적 주행거리 | `Odometer.registerDistanceDriven` | 1234.0 수신 |
| 브레이크 페달 | `Brake.registerBrakePedalStatus` | true, false 수신 |
| 조향각 | `SteeringWheel.registerSteeringWheelAngle` | -120, +200 수신 (주입값의 부호가 반전됨) |
| 방향지시등 | `TurnSignal.registerTurnSignalState` | LEFT, RIGHT, OFF 수신 |
| 기어 | `Gear.registerGearSelection` | D, R, P, N 수신 |
| EV 배터리 SoC | `EvBattery.registerEvBatteryStateOfCharge` | 42, 100 수신 |
| GPS | `FusedLocation.registerFusedLocationCallback` | 1Hz로 수신, 좌표 이동 반영 |

### 사용 불가

| 항목 | 사유 |
|---|---|
| 트립 거리와 시간 | `ERROR_NOT_SUPPORTED_PROPERTY`. SDK 경로가 없음 |
| 연료 | 프로퍼티 미지원 (EV 차량) |
| 텔테일, 안전벨트, 타이어 | `PRIVATE_CAR_SAFETY` 필요. 일반 앱에 부여 불가 |
| `getCapabilityInfo` | `PRIVATE_CAR_INFO` 필요. 일반 앱에 부여 불가 |
| `PERF_STEERING_ANGLE` (AAOS) | 문서상 X. SDK의 SteeringWheel 도메인을 사용 |
| `PERF_ODOMETER` (AAOS) | 문서상 X. SDK의 Odometer 도메인을 사용 |

## 최종 신호 소스

```
속도          AAOS CarPropertyManager (PERF_VEHICLE_SPEED, m/s, 5~10Hz)
기어          AAOS GEAR_SELECTION (AaosGearReader)
거리          Pleos Odometer (누적)
브레이크 페달 Pleos Brake
조향각        Pleos SteeringWheel (부호 반전 주의)
방향지시등    Pleos TurnSignal
배터리        Pleos EvBattery
GPS           FusedLocation (보조)
음성 코칭     TextToSpeech ON_DEVICE (오프라인)
```

## 설계 결정

1. 속도는 AAOS에서 m/s로 받고 앱에서 km/h로 변환합니다. 화면 표기의 근거가
   실측으로 확보되어 있습니다.
2. AAOS 접근 코드는 `:pleos` 모듈 안에 두어 SDK 격리 원칙을 지킵니다.
   `android.car.jar`는 `compileOnly`로 컴파일하고, 런타임에는 차량
   프레임워크가 제공합니다.
3. 샘플레이트는 1Hz로 요청하되 실제 dt는 타임스탬프로 계산합니다.
   `getCarPropertyConfig`가 null이라 선언된 레이트를 신뢰할 수 없습니다.
   최소 100ms 간격이 확보되므로 급가속 판정은 가능합니다.
4. 급가속과 급제동은 속도 미분과 브레이크 페달 두 신호로 판정합니다.
   노이즈 억제를 위해 5샘플 이동평균을 적용합니다.
5. 급조향은 조향각 변화율로 판정합니다. 부호 반전을 흡수하려고 절댓값과
   방향 정규화를 사용합니다.
6. 깜빡이 누락은 `TurnSignal`이 OFF인 상태에서 조향이 임계값을 넘는지로
   판정합니다. 회전 동작 한 번당 1회만 셉니다.
7. 주행 시간은 앱이 직접 계측합니다. SDK 트립 시간을 쓸 수 없습니다.
8. Safety와 Tire 계열은 데모와 기능에서 제외합니다.
9. ADAS는 사용하지 않습니다. 문서와 아티팩트가 모두 없습니다.
10. AI 코칭 문장은 TTS ON_DEVICE로 읽어줍니다. 오프라인에서도 동작합니다.
    문장 생성은 룰 기반을 우선하고 외부 LLM은 선택적 확장으로 둡니다.

## 남은 확인 사항

- `PERF_VEHICLE_SPEED`가 실차에서도 O인지 확인해야 합니다. 문서상으로는 O이고
  에뮬레이터에서는 확인했지만 실차 검증은 하지 못했습니다.
- `CAR_POWERTRAIN` 권한이 앱 심사에서 승인되는지 확인해야 합니다.
  Pleos App Market과 사전 협의가 필요합니다.
- 조향각 부호 반전이 실차에서도 같은지 확인해야 합니다.
- TTS ON_DEVICE의 한국어 품질은 에뮬레이터에서 검증하지 못했습니다.
- 에뮬레이터에서 `emulator_active`는 포커스를 잃으면 false로 돌아갑니다.
  속도를 주입해 테스트할 때는 Virtual Console을 전면에 유지해야 합니다.

## 산출물

- 스파이크 코드는 검증 후 제거했습니다.
- 재현은 이 문서의 주입 명령과 신호 소스 표로 가능합니다.

## 실차 검증이 필요할 때

에뮬레이터로 확인할 수 없는 항목이 남아 있습니다. 실차에서만 검증 가능한 항목과
그에 필요한 차량을 정리합니다.

### 실차에서만 확인 가능한 항목

| 항목 | 에뮬레이터 결과 | 필요한 것 |
|---|---|---|
| `PERF_VEHICLE_SPEED` 실제 값 | 읽기 성공 | Pleos 탑재 차량 |
| `GEAR_SELECTION` 실제 값 | 읽기 성공 | Pleos 탑재 차량 |
| DO 태그 주행 차단 | 차단 확인됨 | 실차 재확인 권장 |
| `CAR_SPEED` 등 권한 승인 | 에뮬레이터에서 부여 | 앱 마켓 사전 협의 |
| 조향각 부호 | 부호 반전 확인 | 실차 동일성 확인 |
| TTS 한국어 품질 | 미확인 | 실차 또는 에뮬레이터 청취 |
| Gleo AI 연동 | 앱 없음 | Gleo AI 탑재 차량 |

### Pleos Connect 탑재 차량 (2026년 기준)

Pleos Connect는 2026년 5월 더 뉴 그랜저(GN7 PE)부터 적용되었습니다.

| 브랜드 | 차종 | 시기 | 비고 |
|---|---|---|---|
| 현대 | 더 뉴 그랜저 (GN7 PE) | 2026.05 ~ | 최초 적용 |
| 현대 | 아이오닉 3 | 2026 ~ | 현대 최초 공개 차량 |
| 현대 | 디 올 뉴 팰리세이드 | 2026 ~ | |
| 현대 | 8세대 아반떼 (CN8) | 2026 상반기 | 예정 |
| 기아 | PV5 | 2025 ~ | 시범 적용 |
| 기아 | 4세대 K5 | 2026 상반기 | 예정 |
| 기아 | PV7 | 2027 ~ | 예정 |
| 제네시스 | GV90 | 2026.09 | 예정 |

**렌트 가능성이 가장 높은 차종은 더 뉴 그랜저(GN7 PE)입니다.** 2026년 5월부터
양산되었고 국내 렌트 시장에 가장 빨리 공급됩니다. 아이오닉 3도 2026년 출시라
시간이 지나면 렌트 가능성이 있습니다.

### 실차 테스트 준비물

| 항목 | 용도 |
|---|---|
| 노트북 + USB 케이블 | adb 연결 |
| 앱 디버그 APK | `./gradlew assembleDebug` |
| 권한 승인 | `adb shell pm grant` (또는 앱 마켓 승인) |
| 안전한 장소 | 공터 또는 주차장 |

**주의:** 실차에서 신호를 검증할 때는 반드시 안전한 장소에서 합니다. 주행 중
조작은 금지하고, 동승자가 조작합니다.

### 실차 검증 절차

1. 앱 설치 및 권한 부여
2. `adb logcat -s PleosSignalSource AaosSpeedReader AaosGearReader`
3. 엔진 시동 후 기어 P에서 신호 확인
4. 공터에서 기어 D, 저속 주행하며 속도·조향·방향지시등 확인
5. 급제동·급가속을 의도적으로 만들어 이벤트 검출 확인
6. TTS 재생 품질 청취
7. Gleo AI 탑재 차량이면 "티레온 운전 점수 알려줘"로 연동 확인

### 렌트 시 확인할 것

렌트 예약 전에 다음을 확인하면 헛걸음을 줄일 수 있습니다.

- 트림에 Pleos Connect가 포함되는지 (하위 트림은 ccNC일 수 있음)
- Gleo AI가 활성화되어 있는지 (구독 또는 설정 필요 여부)
- 앱 설치가 가능한지 (ADB 사이드로드 또는 마켓 접근)

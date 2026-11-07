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
기어          Pleos Gear
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

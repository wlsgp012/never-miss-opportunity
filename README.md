# Never Miss Opportunity

경제/주식 지표를 정해진 시간에 조회하고, 최신 값과 등락폭을 알림 및 앱 화면으로 보여주는 Kotlin Android 앱입니다.

현재 버전은 `1.0.2`이며, Android `minSdk 26`, `targetSdk 35` 기준으로 구성되어 있습니다.

## 주요 기능

- 주요 환율, 주가지수, 변동성, 금, 비트코인 지표를 주기적으로 조회
- 지표별 현재값, 등락폭, 등락률, 업데이트 시각 표시
- CNN Fear & Greed Index의 점수와 단계 표시
  - 예: `Fear`, `Extreme Fear`, `Greed`
- S&P 500, Nasdaq 100, KOSPI, BTC/USD의 RSI 14 표시
- Android 알림으로 조회 결과 전달
- 알림 권한이 꺼져 있을 때 앱 화면에서 권한 요청 가능
- 마지막 조회값을 로컬 Room DB에 저장하고, 오래된 값은 stale 상태로 표시

## 표시 지표

앱 기본 화면에는 아래 순서로 지표가 표시됩니다.

1. USD/KRW
2. CNN Fear & Greed
3. S&P 500
4. Nasdaq 100
5. VIX
6. KRX gold spot 1g
7. KOSPI
8. BTC/USD
9. Bitcoin MVRV

## 알림 스케줄

모든 시간은 `Asia/Seoul` 기준입니다.

| 지표 | 조회 시간 |
| --- | --- |
| USD/KRW | 09:00부터 21:00까지 매시간 |
| CNN Fear & Greed | 09:00, 23:00 |
| S&P 500 | 08:30, 22:40, 23:00, 23:40 |
| Nasdaq 100 | 22:40, 23:00, 23:40 |
| VIX | 08:30, 22:40, 23:00, 23:40 |
| KRX gold spot 1g | 12:00 |
| KOSPI | 09:00부터 15:00까지 매시간 |
| BTC/USD | 12:00 |
| Bitcoin MVRV | 12:00 |

## 데이터 소스

| 지표 | 소스 |
| --- | --- |
| USD/KRW, S&P 500, Nasdaq 100, VIX, KOSPI, BTC/USD | Yahoo Finance chart API |
| CNN Fear & Greed | CNN Fear & Greed graph data |
| Bitcoin MVRV | CoinMetrics `CapMVRVCur` |
| KRX gold spot 1g | KRX public web fallback provider |

Yahoo Finance, CNN, CoinMetrics 데이터는 공개 또는 비공식 엔드포인트를 사용하므로 응답 형식이나 접근 정책이 바뀔 수 있습니다.

## 동작 구조

- `AlarmManager`가 각 스케줄 슬롯을 등록합니다.
- 알람이 실행되면 `MarketAlarmReceiver`가 `WorkManager` 작업을 큐에 넣습니다.
- `MarketFetchWorker`가 해당 슬롯의 지표를 조회합니다.
- 조회 결과는 Room DB에 저장되고, 성공한 fresh quote는 알림으로 표시됩니다.
- 앱 기본 화면은 저장된 최신 quote를 구독해 목록으로 표시합니다.

## 빌드 및 테스트

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## 참고 사항

- Android 13 이상에서는 알림 표시 전에 notification permission이 필요합니다.
- 기본 알람은 inexact alarm으로 등록됩니다.
- RSI는 Yahoo Finance의 1개월 일봉 종가를 기준으로 14-period RSI를 계산합니다.
- 기존 설치본에서 `1.0.2`로 업데이트할 때 Room DB는 `1 -> 2` 마이그레이션으로 보조 신호 필드를 추가합니다.

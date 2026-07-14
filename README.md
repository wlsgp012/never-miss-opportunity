# Never Miss Opportunity

Kotlin Android app for scheduled market indicator notifications.

## Indicators

1. USD/KRW exchange rate
2. CNN Fear & Greed Index
3. S&P 500 index
4. Nasdaq 100 index
5. KRX gold spot 1g price
6. KOSPI index

## Build

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Notes

- Schedules use Asia/Seoul time.
- Inexact alarms are used by default.
- Android 13+ requires notification permission before alerts can appear.
- Yahoo Finance and CNN endpoints are public/unofficial data sources and may change.
- KRX gold uses a fallback provider until a stable public endpoint is verified.

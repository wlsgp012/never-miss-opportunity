# Market Alert Android App Design

Date: 2026-07-14
Status: Approved for implementation planning

## Goal

Build a Kotlin Android app that periodically checks selected market and economic indicators, stores the latest observed values, and shows scheduled notifications with the current value and movement. The main screen shows the same indicators in the user-requested order.

The app should start with a solid application skeleton and use public web data sources where practical. Data providers must be replaceable because public or unofficial endpoints can change without notice.

## Target Indicators

The main screen and notification payloads use this fixed display order:

1. USD/KRW exchange rate
2. CNN Fear & Greed Index
3. S&P 500 index
4. Nasdaq 100 index
5. KRX gold spot 1g price
6. KOSPI index

Each indicator displays:

- Current value
- Absolute change
- Percent change when available
- Last successful update time
- Data state: loading, fresh, stale, or unavailable
- Data source label

## Schedule

All schedules are based on Asia/Seoul local time.

| Indicator | Schedule |
| --- | --- |
| USD/KRW exchange rate | Every hour from 09:00 through 21:00 |
| CNN Fear & Greed Index | 09:00 and 23:00 |
| S&P 500 index | 22:40, 23:00, and 23:40 |
| Nasdaq 100 index | 22:40, 23:00, and 23:40 |
| KRX gold spot 1g price | 12:00 |
| KOSPI index | Every hour from 09:00 through 15:00 |

For overlapping times, the app fetches all indicators assigned to that schedule slot in one worker run and sends one grouped notification for that slot. This avoids notification spam while preserving all requested update times.

## Architecture

Use a single Android app module with Kotlin and Jetpack Compose.

Core layers:

- `ui`: Compose screen for the dashboard, notification permission prompt, refresh action, and status rendering.
- `domain`: indicator definitions, schedule definitions, quote models, movement formatting, and repository interfaces.
- `data`: public web data providers, JSON parsing, fallback mock provider, and local persistence.
- `notifications`: notification channel creation and market update notification rendering.
- `scheduler`: boot receiver, alarm receiver, and schedule registration.
- `worker`: background fetch worker that resolves a schedule slot, refreshes required indicators, persists results, and asks the notification layer to display results.

Dependencies should stay conservative:

- Jetpack Compose Material 3 for UI.
- Kotlin coroutines for asynchronous work.
- WorkManager for deferrable background fetch work.
- AlarmManager for scheduled time triggers.
- Room for local state because quote history and fetch logs are useful for debugging public endpoints.
- Retrofit, OkHttp, and kotlinx.serialization for HTTP JSON parsing.

## Scheduling Design

Use `AlarmManager` to trigger schedule slots. The alarm receiver enqueues a `OneTimeWorkRequest` for `MarketFetchWorker` with the schedule slot id.

Use inexact alarms by default. Android documentation recommends inexact alarms for most scheduled background work and warns that exact alarms require special handling on recent Android versions. The user-facing value is "around this market update time", not a medical or alarm-clock precision requirement.

If a future requirement demands exact minute-level delivery, add an exact-alarm mode behind an explicit settings toggle and explain why `SCHEDULE_EXACT_ALARM` may be needed.

Register schedules:

- On app start after first launch setup.
- On device boot with `BOOT_COMPLETED`.
- After `TIMEZONE_CHANGED` and `DATE_CHANGED` broadcasts.

WorkManager is not used for periodic schedules because its periodic repeat interval has a 15-minute minimum and the exact run time can be shifted by system constraints. WorkManager is still useful for the actual fetch work once an alarm fires.

## Notification Design

Create a notification channel named `Market Alerts`.

Android 13 and later require runtime `POST_NOTIFICATIONS` permission. The main screen should show a compact enable-notifications action when permission is not granted. Background workers should still refresh data even when notification permission is denied, but they must skip posting notifications.

Notification content:

- Title: `Market update`
- Body: compact summary such as `USD/KRW 1,382.40 (+2.10, +0.15%)`
- Grouped body for slots that contain multiple indicators.
- Tapping a notification opens the dashboard.

## Public Data Sources

Initial public source candidates:

| Indicator | Candidate source | Notes |
| --- | --- | --- |
| USD/KRW | Yahoo Finance chart endpoint, symbol `USDKRW=X` | Unofficial endpoint; parse defensively. |
| S&P 500 | Yahoo Finance chart endpoint, symbol `^GSPC` | Unofficial endpoint; market hours and delays apply. |
| Nasdaq 100 | Yahoo Finance chart endpoint, symbol `^NDX` | Unofficial endpoint; market hours and delays apply. |
| KOSPI | Yahoo Finance chart endpoint, symbol `^KS11` | Unofficial endpoint; market hours and delays apply. |
| CNN Fear & Greed | CNN dataviz JSON endpoint | Public JSON endpoint observed at `https://production.dataviz.cnn.io/index/fearandgreed/graphdata`. |
| KRX gold spot 1g | Dedicated `GoldSpotProvider` interface with public web implementation and mock fallback | KRX/Naver-style public data can be brittle; keep this adapter isolated. |

Provider contract:

```kotlin
interface MarketDataProvider {
    suspend fun fetch(indicator: Indicator): MarketQuote
}
```

`MarketQuote` must include current value, prior comparison value when available, absolute change, percent change, observed timestamp, source name, and freshness state.

If a provider fails:

- Preserve the last successful quote.
- Mark the quote as stale if it exists.
- Mark as unavailable if no prior quote exists.
- Log a fetch result row with the error category.
- Notification should mention only successfully refreshed values; if every value in a slot fails, send no notification and update in-app status.

## Main Screen

The first screen is the actual dashboard, not a landing page.

Layout:

- Top app bar with app name and manual refresh action.
- Permission/status strip only when action is needed, such as notification permission missing.
- Six indicator rows in fixed order.
- Each row shows name, value, movement, last update, and source/state.
- Positive movement uses a clear up treatment; negative movement uses a clear down treatment; neutral movement remains subdued.
- Pull-to-refresh or toolbar refresh triggers immediate fetch for all indicators.

The UI should be calm and information-dense, suitable for repeated checking. Avoid decorative hero sections or marketing copy.

## Local Persistence

Use Room tables:

- `indicator_quotes`: latest and historical quote rows keyed by indicator id and observed timestamp.
- `fetch_logs`: schedule slot id, started time, completed time, success count, failure count, and error summaries.

The dashboard reads the latest quote per indicator. A small repository API should hide database details from UI and workers.

## Error Handling

Network failures, provider parse failures, and empty market data responses are expected.

Rules:

- Never crash the worker because one indicator failed.
- Use per-indicator result objects.
- Retry transient network failures with WorkManager backoff.
- Do not retry parse failures aggressively; they probably mean the public endpoint changed.
- Expose stale/unavailable states in UI.
- Keep public-source parsing code small and well-tested.

## Testing

Unit tests:

- Schedule expansion for all requested times.
- Movement calculation and formatting.
- Provider JSON parsing with sample fixtures.
- Repository stale/unavailable behavior.

Worker tests:

- Slot with all successful fetches posts a grouped notification.
- Slot with partial failures persists successes and skips failed indicators in the notification.
- Slot with all failures does not post an empty notification.

UI tests:

- Dashboard renders six indicators in fixed order.
- Permission prompt appears only when notifications are not granted.
- Stale and unavailable states are visible.

Manual verification:

- Build debug APK.
- Launch dashboard.
- Trigger manual refresh.
- Trigger a worker or receiver with a test slot.
- Confirm notification permission behavior on Android 13+ emulator or device.

## Implementation Boundaries

In scope for the initial app skeleton:

- Kotlin Android project scaffold.
- Compose dashboard.
- Domain models and schedule definitions.
- Local Room persistence.
- Alarm receiver and WorkManager worker path.
- Notification channel and notification rendering.
- Public provider interfaces and initial Yahoo/CNN provider implementations where endpoints are straightforward.
- Isolated gold provider with mock fallback if a reliable public endpoint cannot be verified during implementation.

Out of scope for the initial skeleton:

- User-editable schedules.
- Watchlists beyond the six requested indicators.
- Login, paid APIs, or API-key storage.
- Charts and historical analytics.
- Exact alarm permission flow unless minute-perfect delivery becomes a hard requirement.

## Source Notes

- Android WorkManager supports one-time and periodic work, but periodic work has a 15-minute minimum repeat interval and exact execution can shift based on constraints and system optimization.
- Android AlarmManager supports time-based triggers outside the app lifetime. Android recommends inexact alarms for most apps; exact alarms require special permissions or policy-sensitive declarations on recent Android versions.
- Android 13 and later require declaring and requesting `POST_NOTIFICATIONS` before posting normal app notifications.
- Jetpack Compose is the modern Android UI toolkit and is appropriate for this new Kotlin app.

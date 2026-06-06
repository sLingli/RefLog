# AGENTS.md - RefLog (Soccer Referee Timer)

## Project Overview
RefLog is an Android app for soccer referees featuring a dual-timer system (main + stoppage time), match event logging (cards, goals, substitutions), and match history. Built entirely with **Kotlin** and **Jetpack Compose**.

## Architecture

### MVVM + Single-Activity
- `MainActivity.kt` (~350 LOC): NavHost 路由注册、个人资料状态、弹窗组合
- `TimerViewModel.kt` (~300 LOC): 计时器状态机、协程定时循环、事件记录、比赛保存
- `DashboardViewModel.kt`: 仪表盘统计数据（SQL 聚合查询）
- State machine uses enums: `TimerState.READY` → `TimerState.RUNNING` ↔ `TimerState.PAUSED` → `TimerState.HALFTIME` → `TimerState.FINISHED`
- Half tracking: `HalfState.FIRST` → `HalfState.BREAK` → `HalfState.SECOND`

### Data Flow
```
User Action → TimerViewModel.toggleTimer() → State Change → StateFlow<TimerUiState> → Composable recompose
Timer Loop → viewModelScope.launch { tick(); delay(100) } → mainTime++ / stoppageTime++ → checkTimeAlerts()
Event → UnifiedEventBottomSheet → TimerViewModel.handleEventConfirmed()
Match End → saveMatchRecord() → MatchRecordRepository → Room Database
```

### Key Files
| File | Purpose |
|------|---------|
| `MainActivity.kt` | NavHost 路由、个人资料、弹窗组合 |
| `TimerViewModel.kt` | 计时器状态机、协程定时器、事件记录、比赛保存 |
| `TimerUiState.kt` | 计时器 UI 状态数据类 |
| `MatchEventEnums.kt` | TimerState, HalfState, EventType, TeamSelection 枚举 |
| `MatchRecord.kt` | Data classes: `MatchEvent`, `MatchRecord` |
| `MatchRecordRepository.kt` | Room persistence, Entity ↔ data class mapping |
| `TimerPage.kt` | 计时器 UI Composable |
| `DashboardScreen.kt` | 仪表盘首页 UI |
| `MatchTemplateScreen.kt` | 赛事预设库 UI |

### Database
- Room with `RefLogDatabase` (version 1)
- Tables: `match_records`, `match_events` (FK cascade), `match_templates`
- `DatabaseModule` provides singleton Repository instances

## Conventions

### UI Patterns
- **All Compose**: No XML layouts. Dialogs use `Dialog()` or `ModalBottomSheet()`
- **Color scheme**: Dark theme (`#121212` bg), Material 3 with custom accent colors
- **String resources**: Localized in `values/strings.xml` and `values-zh/strings.xml` - always use `stringResource(R.string.*)`, never hardcode text
- **Icons**: Vector drawables in `res/drawable/`, tinted programmatically

### State Management
- Timer state lives in `TimerViewModel`, exposed as `StateFlow<TimerUiState>`
- Composable 层从 ViewModel collect 状态，自行派生显示文字（statusText/mainTimeColor 等）
- 个人资料、主题、语言等设置状态留在 MainActivity（SharedPreferences）
- Team colors stored as Int with white-detection for text contrast

### Timer Implementation
- `viewModelScope.launch` 协程 + `delay(100)` 替代旧的 `Handler.postDelayed`
- `mainTime` increments in RUNNING and PAUSED states; `stoppageTime` only in PAUSED
- Time alerts at `halfTimeSeconds` (configurable 5-45 min) trigger color change

## Build & Run
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install to connected device
```
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Kotlin**: 2.1.20
- **Compose BOM**: 2026.05.01
- **Room**: 2.7.1

## Testing Notes
- No unit tests currently implemented (only template ExampleUnitTest)
- Manual testing requires Android device/emulator
- Test timer transitions: Ready → Running → Paused (opens event dialog) → Running → half time alert → end half

## Common Modifications

### Adding New Event Type
1. Add string to `strings.xml` (both default and zh)
2. Add event type to `EventType` enum in `MatchEventEnums.kt`
3. Add button UI and click handler in `UnifiedEventBottomSheet.kt`
4. Add case in `TimerViewModel.handleEventConfirmed()`
5. Add icon drawable and color mapping in `MatchEventEnums.kt` `getEventIconInfo()`

### Changing Timer Behavior
- Timer state machine: `TimerViewModel.kt` — toggleTimer/startTimer/pauseTimer etc.
- Timer tick loop: `TimerViewModel.tick()` method
- Alert thresholds: `TimerViewModel.checkTimeAlerts()` method
- UI display: `TimerPage.kt` (button styles, layout) + `MainActivity.FullscreenTimerContent()` (status text/color derivation)

### Adding Persistence Fields
1. Update `MatchRecord` data class + `MatchRecordEntity` + mapping in `MatchRecordRepository`
2. Bump Room database version and add migration
3. Update `TimerViewModel.saveMatchRecord()` to populate new field
4. Update `MatchSummaryDialog` to display it

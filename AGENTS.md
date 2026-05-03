# AGENTS.md - RefLog (Soccer Referee Timer)

## Project Overview
RefLog is an Android app for soccer referees featuring a dual-timer system (main + stoppage time), match event logging (cards, goals, substitutions), and match history. Built with **Kotlin**, **Jetpack Compose** (dialogs), and **XML layouts** (main UI).

## Architecture

### Single-Activity Pattern
- `MainActivity.kt` (1500+ LOC) is the core: contains **state machine**, **timer logic**, all dialogs, and UI updates
- State machine uses constants: `STATE_READY` → `STATE_RUNNING` ↔ `STATE_PAUSED` → `STATE_HALFTIME` → `STATE_FINISHED`
- Half tracking: `HALF_FIRST` → `HALF_BREAK` → `HALF_SECOND`

### Data Flow
```
User Action → toggleTimer() → State Change → updateButtonStyle() / updateStatusLabel()
Timer Loop → updateTimer() (100ms) → mainTime++ / stoppageTime++ → checkTimeAlerts()
Event → showEventDialog() → showTeamSelectionDialog() → showNumberSelectionDialog() → recordEventWithDetails()
Match End → saveMatchRecord() → MatchRecordManager → SharedPreferences (JSON via Gson)
```

### Key Files
| File | Purpose |
|------|---------|
| `MainActivity.kt` | State machine, timers, all dialog orchestration |
| `MatchRecord.kt` | Data classes: `MatchEvent`, `MatchRecord` |
| `MatchRecordManager.kt` | SharedPreferences persistence with Gson |
| `CenterScaleLayoutManager.kt` | Custom RecyclerView LayoutManager for physics wheel (3D scale effect) |
| `ColorWheelAdapter.kt` | Infinite-scroll color picker adapter |
| `HistoryDialog.kt` / `EventSelectionDialog.kt` | Compose-based dialogs |

## Conventions

### UI Patterns
- **Dialogs**: All dialogs are Jetpack Compose-based (`HistoryDialog.kt`, `EventSelectionDialog.kt`, `TeamSelectionDialog.kt`, etc.), managed via `ComposeView` in MainActivity
- **Color scheme**: Dark theme (`#121212` bg), Material 3 with custom accent colors
- **String resources**: Localized in `values/strings.xml` and `values-zh/strings.xml` - always use `getString(R.string.*)`, never hardcode text
- **Icons**: Vector drawables in `res/drawable/`, tinted programmatically

### State Management
- All state lives in MainActivity properties (no ViewModel)
- `matchEvents: MutableList<MatchEvent>` accumulates during match, saved on end
- Team colors stored as Int (`homeTeamColor`, `awayTeamColor`) with white-detection for text contrast

### Timer Implementation
- `Handler` + `Runnable` loop at 100ms interval, updates only when 1000ms elapsed
- `mainTime` increments in RUNNING and PAUSED states; `stoppageTime` only in PAUSED
- Time alerts at `halfTimeSeconds` (configurable 5-45 min) trigger color change + status update

## Build & Run
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install to connected device
```
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Compose BOM**: 2024.02.00

## Testing Notes
- No unit tests currently implemented
- Manual testing requires Android device/emulator with vibration permission
- Test timer transitions: Ready → Running → Paused (opens event dialog) → Running → half time alert → end half

## Common Modifications

### Adding New Event Type
1. Add string to `strings.xml` (both default and zh)
2. Add event type to `EventType` enum in `EventSelectionDialog.kt`
3. Add button UI and click handler in `EventSelectionDialog`
4. Add case in `initializeComposeDialogs()` in MainActivity
5. Add icon drawable and color mapping in `showMatchSummary()` icon switch
6. Update `MatchRecord` counts if needed

### Changing Timer Behavior
- Timer core: `updateTimer()` method (~line 420)
- Alert thresholds: `checkTimeAlerts()` method
- Visual states: `updateButtonStyle()` for button appearance

### Adding Persistence Fields
1. Update `MatchRecord` data class
2. Update `saveMatchRecord()` to populate new field
3. Update `showMatchSummary()` to display it


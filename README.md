# WordWise

An Android flashcard app that helps university students build academic English vocabulary through short daily sessions and spaced repetition. Built for CP3406 Assessment 3 with Kotlin, Jetpack Compose and Material Design 3.

> Work in progress. This README grows with each development step.

## Core screens

| Screen | Purpose | Status |
|---|---|---|
| Home (landing page) | What the app does, today’s progress, shortcuts | Shows deck and today’s progress |
| Study (activity screen) | Flashcard session with spaced repetition | Placeholder |
| Stats (user statistics) | Progress, accuracy, words mastered | Placeholder |
| Settings | Daily goal, difficulty, sound | Placeholder |

## Architecture

- **Single activity**, all screens are Compose destinations.
- **Navigation:** Navigation Compose 2.8 with type-safe `@Serializable` routes and a Material 3 bottom navigation bar that saves and restores each tab’s state.
- **Dependency injection:** Hilt modules for the database, repository bindings and pure model classes.
- **Layers:** Compose UI → ViewModel (`StateFlow`) → `StudyRepository` interface → Room DAOs. ViewModels never touch Room directly.
- **Model package** (`model/`): pure Kotlin with no Android dependencies, so all learning logic is covered by JVM unit tests.
- **Planned:** the Free Dictionary API as a network data source (Step 3).

## Learning design

- **Leitner spaced repetition** (`LeitnerScheduler`): five boxes reviewed after 1, 2, 4, 8 and 16 days. A correct answer moves a word up one box; a wrong answer sends it back to box 1.
- **Daily goal** (`SessionPlanner`): due reviews first, then new words, up to the goal the learner sets. When the goal is met the session is empty and the app shows “Done for today”.
- **Forgiving streaks** (`StreakCalculator`): one rest day in a row does not break a streak, and today never counts as missed.
- **Word deck:** the 60 headwords of Sublist 1 of the Academic Word List [1], with definitions and example sentences written for this app, at three difficulty levels.

## Data storage (Room)

| Table | Purpose |
|---|---|
| `words` | Deck and learner-added words; unique, case-insensitive terms |
| `card_progress` | Leitner box and next review day per studied word (foreign key to `words`) |
| `review_log` | One row per answer; source of accuracy, history and streak |

Answers are saved with a single `@Transaction` so progress and history never disagree. Statistics come from aggregate queries exposed as `Flow`, so the UI updates automatically.

## Ethical design commitments

Informed by my Assessment 2 presentation on dark patterns and addictive design:

- The learner sets the daily goal, and the app shows “Done for today” when it is reached.
- Streaks are forgiving and progress is personal; there are no public leaderboards.
- Buttons for competing choices have equal visual weight; no countdowns or fake scarcity.
- Learning content comes from the network; personal progress stays on the device.
- No ads, analytics or tracking SDKs.

## Testing

Model unit tests live in `app/src/test` and run on the JVM:

```
./gradlew testDebugUnitTest
```

They cover the Leitner scheduler, session planning against the daily goal, the forgiving streak, statistics arithmetic and parsing of the bundled deck (including a check that the shipped deck file is valid).

## Build

Open the project in Android Studio (Ladybug or newer), let Gradle sync, then run the `app` configuration on an emulator or device with Android 8.0 (API 26) or higher.

## Development log

1. Project skeleton: Gradle version catalog, Material 3 theme, Hilt, type-safe navigation between the four core screens.
2. Learning model and local database: Leitner scheduler, session planner, forgiving streak and statistics calculator with unit tests; Room database, DAOs and repository; academic word deck seeded on first launch and shown on the Home screen.

## References

[1] A. Coxhead, “A new academic word list,” *TESOL Quarterly*, vol. 34, no. 2, pp. 213–238, 2000.

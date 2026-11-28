# WordWise

An Android flashcard app that helps university students build academic English vocabulary through short daily sessions and spaced repetition. Built for CP3406 Assessment 3 with Kotlin, Jetpack Compose and Material Design 3.

> Work in progress. This README grows with each development step.

## Core screens

| Screen | Purpose | Status |
|---|---|---|
| Home (landing page) | What the app does, progress toward today’s goal, shortcuts | Done |
| Study (activity screen) | Flashcards with a 3D flip, honest self-check and spaced repetition | Done |
| Stats (user statistics) | Mastered words, accuracy, forgiving streak, 7-day chart, words per memory box | Done |
| Settings | Daily goal, difficulty, read-aloud, reset my progress | Done |
| Add a word (extra) | Look up a word online and add a chosen meaning | Done |

## Architecture

- **Single activity**, all screens are Compose destinations.
- **Navigation:** Navigation Compose 2.8 with type-safe `@Serializable` routes and a Material 3 bottom navigation bar that saves and restores each tab’s state.
- **Dependency injection:** Hilt modules for the database, repository bindings and pure model classes.
- **Layers:** Compose UI → ViewModel (`StateFlow`) → `StudyRepository` interface → Room DAOs. ViewModels never touch Room directly.
- **Model package** (`model/`): pure Kotlin with no Android dependencies, so all learning logic is covered by JVM unit tests.
- **Network:** Retrofit + OkHttp call the Free Dictionary API; a pure Kotlin parser and mapper turn every response and HTTP status into a `LookupResult`.

## Learning design

- **Leitner spaced repetition** (`LeitnerScheduler`): five boxes reviewed after 1, 2, 4, 8 and 16 days. A correct answer moves a word up one box; a wrong answer sends it back to box 1.
- **Daily goal** (`SessionPlanner`): due reviews first, then new words, up to the goal the learner sets. When the goal is met the session is empty and the app shows “Done for today”.
- **Forgiving streaks** (`StreakCalculator`): one rest day in a row does not break a streak, and today never counts as missed.
- **Word deck:** the 60 headwords of Sublist 1 of the Academic Word List [1], with definitions and example sentences written for this app, at three difficulty levels.

## Online dictionary (Free Dictionary API)

The **Add a word** screen (opened from Home) looks up a word at `https://api.dictionaryapi.dev/api/v2/entries/en/{word}` and shows its pronunciation, parts of speech, definitions and examples. The learner picks the meaning they need and it joins their collection; it appears first in the next session.

- **Input check before sending** (`WordInputValidator`): only a single word or short phrase of letters is sent, so a mistyped sentence or personal detail never leaves the device.
- **Every outcome is explained:** found, not found, invalid input, offline and service errors each have their own message. The built-in deck keeps working offline.
- **Pronunciation:** the play button uses the dictionary’s recording when it exists: the clip is downloaded with the app’s OkHttp client, cached and played from the local file, so it replays offline. The API sometimes lists recordings that no longer exist (HTTP 404); then the device’s own text-to-speech voice reads the word, and the screen says so. Nothing plays automatically. The phonetic spelling shown is the one recorded with the audio, and pronunciation data fetched for a word is cached in Room (`withPronunciation`).
- **Privacy:** requests carry only the word. There is no account, no identifier and no logging interceptor.
- **Attribution:** definitions come from Wiktionary through the Free Dictionary API, and the Wiktionary source link is shown with each result.

## Study session

- `StudySession` is an immutable state machine (show → turn over → answer → next), unit tested on the JVM.
- A card must be turned over before it can be answered, so every answer is an honest self-check.
- **“Still learning”** and **“I knew it”** have identical styling; neither is pushed on the learner.
- After each answer the screen says where the card went (“moved to box 3, back in 4 days”), so the schedule is transparent.
- Progress is shown against the learner’s own goal (“Card 3 of 10”). When the goal is met the screen says **Done for today** and offers progress and settings, not more cards.
- The flip animation respects the system “remove animations” setting.

## Statistics

The Stats screen updates live from Room `Flow` queries: words mastered, accuracy, a forgiving streak, reviews over the last 7 days against the learner’s goal, and words in each Leitner box. Progress is personal only, with no rankings. Every chart has a text equivalent for TalkBack, and goal days are marked by bar shade and explained in a caption, not by colour alone.

## Settings (DataStore)

| Setting | Default | Notes |
|---|---|---|
| Daily goal | 10 cards | 5–30 in steps of 5; stored values are sanitised |
| Difficulty | Medium | Easy = level 1, Medium = levels 1–2, Hard = all |
| Read each word aloud | Off | Audio is opt-in |
| Reset my learning progress | — | Deletes answer history and Leitner state after confirmation; words stay |

Settings are stored with Jetpack DataStore; a corrupted file falls back to the defaults.

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

**Unit tests** (`app/src/test`, JVM, 92 tests):

```
./gradlew testDebugUnitTest
```

- *Model*: the Leitner scheduler, session planning against the daily goal, the forgiving streak, statistics arithmetic, parsing of the bundled deck (including a check that the shipped deck file is valid), input validation, and parsing and status mapping of dictionary responses, including the API’s quirks (merged entries, empty audio links, protocol-relative URLs, malformed JSON).
- *ViewModels*: Study, Add a word, Settings, Home and Stats, run against hand-written fakes of the repository interfaces with `kotlinx-coroutines-test`. They check, for example, that an answer is ignored until the card is turned, that the last answer ends the session instead of offering more, that “nothing due” is told apart from “done for today”, that every lookup failure has its own state, and that the learner is told when the device voice replaces a missing recording.

**GUI tests** (`app/src/androidTest`, Compose UI test, needs an emulator or device):

```
./gradlew connectedDebugAndroidTest
```

They render the stateless screens and check what the learner sees and which action each control triggers, including the ethical rules: Home offers progress, not more cards, once the goal is met; the answers appear only after the card is turned and both are offered; Done for today has no “keep going” button; TalkBack descriptions are present.

## Build

Open the project in Android Studio (Ladybug or newer), let Gradle sync, then run the `app` configuration on an emulator or device with Android 8.0 (API 26) or higher.

## Development log

1. Project skeleton: Gradle version catalog, Material 3 theme, Hilt, type-safe navigation between the four core screens.
2. Learning model and local database: Leitner scheduler, session planner, forgiving streak and statistics calculator with unit tests; Room database, DAOs and repository; academic word deck seeded on first launch and shown on the Home screen.
3. Online dictionary: Retrofit service for the Free Dictionary API, response parser and input validator with unit tests, Add a word screen with pronunciation playback, and pronunciation caching in Room.
4. Study and Settings: flashcard session with flip animation, transparent scheduling feedback and Done for today; DataStore settings for goal, difficulty and read-aloud; progress reset; Home shows progress toward the goal.
5. Statistics: live Stats screen with summary tiles, 7-day chart and memory-box chart, each with TalkBack text.
6. Visual design: "Calm Academic" theme (forest green on ivory, serif headings, checked contrast, dark mode), a goal-first Home screen, and a hand-drawn sprout on the Done for today screen instead of a reward animation.
7. Tests: ViewModel tests with fakes and Compose GUI tests for Home, Study and Stats.

## References

[1] A. Coxhead, “A new academic word list,” *TESOL Quarterly*, vol. 34, no. 2, pp. 213–238, 2000.

[2] Free Dictionary API. [Online]. Available: https://dictionaryapi.dev/

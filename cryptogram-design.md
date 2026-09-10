# Cryptogram Infinite — Design & Build Document

Date: 2026-09-09
Platform: Android only · Language: English only · Working title: **Cryptogram Infinite**
Package: `dev.milan.cryptogram` · Content repo: `https://github.com/IpadApe/Cryptogram-Infinite` (public)

This document is the single source of truth. Every open question from the grill session is resolved here. Section 9 contains the agent briefs in build order.

---

## 1. Fixed Decisions (do not reopen)

| Area | Decision |
|---|---|
| Distribution | Google Play, free, ad-supported, one IAP `remove_ads` (non-consumable) |
| Content | Bundled offline corpus (~2,000 quotes, ≥300 per band) + one daily per difficulty fetched from GitHub |
| Provenance | Every quote is a verbatim row in the verified quote DB with `author`, `source`, `sourceUrl`. Never AI-composed. |
| Difficulty bands (chars) | Easy 20–30 · Medium 31–45 · Hard 46–70 · Extreme 71–100 |
| Reveals (% of distinct letters, floor) | Easy 50% · Medium 30% · Hard 15% · Extreme 5% (min 0) |
| Feedback | Easy/Medium: wrong letter turns red on entry · Hard: `Check` button · Extreme: nothing until grid fully correct |
| Lives per puzzle | 5 / 4 / 3 / 3 · zero lives = restart same level with new key · no revive · no global lives |
| Free hints per puzzle | 3 / 2 / 1 / 0 · hint reveals one chosen cipher number · extra hints via rewarded ad, max 3 per puzzle |
| Ads | Banner on Home, Level Select, Results only. No interstitials, ever. No ads on Play screen. |
| Remove Ads | Removes banners only. Rewarded hint ads remain available to everyone. |
| Levels | Numbered per difficulty, seeded shuffle, append-only corpus, cycle re-keys. Nothing gated. |
| Daily | Keyed to device local date. Repo publishes ≥2 days ahead. Fetch on launch/resume + WorkManager best-effort. 7-day cache. Bundled fallback. |
| Daily generator | GitHub Actions cron, deterministic Python script, no LLM at publish time |
| Identity | None. All state local. Entitlement restored via Play Billing `queryPurchasesAsync`. |
| Stack | Kotlin 2.x, Jetpack Compose (Material 3), Room, DataStore Preferences, WorkManager, Google Mobile Ads SDK, Play Billing 7.x, OkHttp, kotlinx.serialization, minSdk 26, targetSdk 35, single `:app` module, manual DI via `AppContainer` |

---

## 2. Architecture

Single Gradle module `:app`. Packages under `dev.milan.cryptogram`:

```
di/            AppContainer.kt
data/db/       AppDatabase.kt, entities/, dao/
data/corpus/   CorpusLoader.kt (reads assets/corpus.json into Room on first run)
data/daily/    DailyRepository.kt, DailyRemoteSource.kt, DailySyncWorker.kt
data/prefs/    SettingsStore.kt (DataStore)
engine/        Cipher.kt, RevealPolicy.kt, LevelIndex.kt, PuzzleSession.kt, Difficulty.kt
ads/           AdManager.kt, BannerAd.kt, RewardedHintAd.kt, ConsentManager.kt
billing/       BillingManager.kt
ui/            theme/, navigation/NavGraph.kt, components/
ui/home/       HomeScreen.kt, HomeViewModel.kt
ui/levels/     LevelSelectScreen.kt, LevelSelectViewModel.kt
ui/play/       PlayScreen.kt, PlayViewModel.kt, PuzzleGrid.kt, CipherKeyboard.kt
ui/results/    ResultsScreen.kt
ui/daily/      DailyScreen.kt, DailyViewModel.kt
ui/stats/      StatsScreen.kt, StatsViewModel.kt
ui/settings/   SettingsScreen.kt, SettingsViewModel.kt
ui/sources/    SourcesScreen.kt
```

Pattern: `Screen(viewModel)` collects `StateFlow<UiState>`; ViewModel owns a `PuzzleSession` for play; repositories wrap DAOs; no Hilt/Koin.

`AppContainer` (created in `CryptogramApp : Application`) exposes: `database`, `corpusRepository`, `progressRepository`, `dailyRepository`, `settingsStore`, `adManager`, `billingManager`.

---

## 3. Content Repository (`cryptogram-content`)

### 3.1 Layout

```
corpus/quotes.json          master verified quote DB (append-only)
corpus/profanity.txt        one lowercase word per line
daily/index.json            list of published dates
daily/YYYY-MM-DD.json       one file per local date
scripts/validate.py         validator (CI gate for every PR and every daily)
scripts/publish_daily.py    cron generator
scripts/build_assets.py     writes app/src/main/assets/corpus.json (used at app build time)
.github/workflows/daily.yml
.github/workflows/validate.yml
```

### 3.2 Quote row schema (`corpus/quotes.json` = JSON array)

```json
{
  "id": 1042,
  "text": "Imagination is more important than knowledge.",
  "author": "Albert Einstein",
  "source": "Saturday Evening Post interview, 1929",
  "sourceUrl": "https://en.wikiquote.org/wiki/Albert_Einstein",
  "band": "MEDIUM",
  "addedOn": "2026-09-10"
}
```

- `id: Int` — strictly increasing, never reused, never reordered.
- `text: String` — plaintext. Allowed chars: `A–Z a–z space , . ! ? ' ; : - "`. No digits, no unicode quotes (normalize `’`→`'`, `“”`→`"`, `—`→`-`).
- `band` — derived from `len(text)` by the validator; stored for fast filtering. Bands: `EASY 20–30`, `MEDIUM 31–45`, `HARD 46–70`, `EXTREME 71–100`.
- `author` — required, non-empty. Fictional characters formatted `"Gandalf (The Lord of the Rings)"`.

### 3.3 Daily file schema (`daily/2026-09-12.json`)

```json
{
  "date": "2026-09-12",
  "puzzles": {
    "EASY":    { "quoteId": 233,  "seed": 719104233 },
    "MEDIUM":  { "quoteId": 1042, "seed": 719101042 },
    "HARD":    { "quoteId": 877,  "seed": 719100877 },
    "EXTREME": { "quoteId": 1503, "seed": 719101503 }
  }
}
```

`seed = int(date.strftime("%Y%m%d")) * 10_000 + quoteId` (fits in Long). The app derives the cipher key from `seed`; the daily file contains **no plaintext** — the app looks the quote up by `quoteId` in its bundled corpus. Consequence: a daily may only reference quotes with `id <= maxBundledId` of the **oldest supported app build**. `publish_daily.py` reads `corpus/min_app_corpus_id.txt` and only picks `id <= that`. When a new app version ships with a larger corpus, bump that file.

`daily/index.json`: `{ "dates": ["2026-09-10", "2026-09-11", "2026-09-12"] }` — last 14 entries, ascending.

### 3.4 Validator (`scripts/validate.py`) — hard rules, exit 1 on any failure

For every row in `quotes.json`:
1. `id` unique, array sorted ascending by `id`, no gaps required.
2. `text` length 20–100 after `strip()`, matches `^[A-Za-z ,.!?';:\-"]+$`.
3. `text` contains ≥10 distinct letters (case-insensitive).
4. `text` has ≥3 words.
5. `band` equals band computed from length.
6. `author` non-empty; `sourceUrl` starts with `https://`.
7. No profanity: lowercase tokenized `text` ∩ `profanity.txt` = ∅.
8. No duplicate `text` (case- and punctuation-insensitive compare).
9. ≤10 rows per `author` where `sourceUrl` domain is not `en.wikiquote.org` — soft cap on non-Wikiquote-sourced living authors; enforced as ≤10 per author overall.
10. Per band count ≥300 (warning only below 300; error below 100).

For every `daily/*.json`:
1. Filename matches `date` field.
2. Each of the four bands present; `quoteId` exists in corpus and its `band` matches the key.
3. `quoteId` not used by any other daily in the last 365 days.
4. `quoteId <= min_app_corpus_id`.
5. `index.json` lists the file.

### 3.5 Daily generator (`scripts/publish_daily.py`)

Runs via `daily.yml` cron `0 3 * * *` UTC. For `target = today_utc + 2 days` (also backfills any missing date in `[today_utc, target]`):

```
used = set(quoteId for f in daily/*.json within 365 days)
rng  = random.Random(int(target.strftime("%Y%m%d")))
for band in [EASY, MEDIUM, HARD, EXTREME]:
    pool = [q for q in corpus if q.band == band and q.id <= min_app_corpus_id and q.id not in used]
    if not pool: pool = [q for q in corpus if q.band == band and q.id <= min_app_corpus_id]  # ran out, allow repeats
    pick = rng.choice(pool)
write daily/{target}.json, update index.json (keep last 14), run validate.py, git commit + push
```

If validation fails, the workflow fails and does **not** push; the 2-day buffer absorbs it.

### 3.6 Corpus curation pipeline (offline, one-off, AI-assisted)

1. Pull candidate quotes from Wikiquote pages of authors/works in `curation/authors.txt` (scripted, keep page URL).
2. LLM pass (local, via Forge or Claude) scores each candidate 1–5 on: memorable, self-contained, no proper-noun-heavy, no dated/offensive content. Keep ≥4.
3. Normalize punctuation, compute band, drop out-of-band.
4. Manual skim of the Extreme band first (scarcest), then a 10% random sample of the rest.
5. Append with new `id`s, run `validate.py`, commit.

Launch target: 2,000 rows, ≥300 per band. App displays a **Sources** screen: "Quotes sourced from Wikiquote, licensed CC BY-SA 3.0" + link.

---

## 4. Game Engine (`engine/`)

### 4.1 Difficulty

```kotlin
enum class Difficulty(
    val minLen: Int, val maxLen: Int,
    val revealRatio: Float, val lives: Int, val freeHints: Int,
    val feedback: FeedbackMode
) {
    EASY(20, 30, 0.50f, 5, 3, FeedbackMode.IMMEDIATE),
    MEDIUM(31, 45, 0.30f, 4, 2, FeedbackMode.IMMEDIATE),
    HARD(46, 70, 0.15f, 3, 1, FeedbackMode.ON_CHECK),
    EXTREME(71, 100, 0.05f, 3, 0, FeedbackMode.ON_COMPLETE)
}
enum class FeedbackMode { IMMEDIATE, ON_CHECK, ON_COMPLETE }
const val MAX_AD_HINTS_PER_PUZZLE = 3
```

### 4.2 Cipher key (`Cipher.kt`)

Number-substitution cipher: each distinct plaintext letter is replaced by a number 1..26. `key[i]` is the cipher number for plaintext letter `'A' + i`.

```kotlin
object Cipher {
    /** A permutation of 1..26, deterministic for seed. key[i] = cipher number for 'A'+i. */
    fun key(seed: Long): IntArray {
        val rng = kotlin.random.Random(seed)
        return (1..26).toMutableList().also { it.shuffle(rng) }.toIntArray()
    }
    /** cipher number (1..26) -> plaintext letter. Index 0 unused. */
    fun invert(key: IntArray): CharArray {
        val inv = CharArray(27)
        for (i in 0 until 26) inv[key[i]] = 'A' + i
        return inv
    }
    /** Tokenises plaintext: a number per letter, the literal char for anything else. */
    fun encrypt(plain: String, key: IntArray): List<CipherToken> =
        plain.uppercase().map { c ->
            if (c in 'A'..'Z') CipherToken.Num(key[c - 'A']) else CipherToken.Sym(c)
        }
}
sealed interface CipherToken {
    data class Num(val n: Int) : CipherToken   // encoded letter
    data class Sym(val c: Char) : CipherToken  // passed-through space / punctuation
}
```

Seed rules:
- Level play: `seed = difficulty.ordinal * 1_000_000_000L + level * 1_000L + cycle` — recomputed on restart with `cycle + 1` (restart after zero lives uses a fresh key; `cycle` for restarts is stored in `PuzzleSession`, not persisted; first play cycle = level / bandSize).
- Daily: `seed` from the daily JSON.

### 4.3 Reveal policy (`RevealPolicy.kt`)

```kotlin
fun revealedLetters(plain: String, d: Difficulty, seed: Long): Set<Char> {
    val distinct = plain.uppercase().filter { it in 'A'..'Z' }.toSet().toList().sorted()
    val n = kotlin.math.floor(distinct.size * d.revealRatio).toInt()
    val rng = kotlin.random.Random(seed xor 0x5EEDL)
    // prefer revealing letters with mid frequency: sort by count desc, take from the middle third first
    return distinct.shuffled(rng).take(n).toSet()
}
```

Revealed letters are pre-filled in the grid, locked (not editable), and shown on the keyboard as used. A letter is "revealed" as a plaintext letter; the cipher number whose correct answer is that letter is the locked cell.

### 4.4 Level index (`LevelIndex.kt`)

Requirement: level N of a band must map to the same quote on every device and must never change when the corpus grows.

```kotlin
class LevelIndex(
    quotesByBand: Map<Difficulty, List<Int>>,   // all ids in the local corpus, per band
    frozenMaxId: Map<Difficulty, Int>           // from DataStore, set once at first run
) {
    private val frozen: Map<Difficulty, List<Int>> = quotesByBand.mapValues { (d, ids) ->
        ids.filter { it <= frozenMaxId.getValue(d) }.sortedBy { splitmix64(BAND_SEED.getValue(d) * 31L + it) }
    }
    private val appended: Map<Difficulty, List<Int>> = quotesByBand.mapValues { (d, ids) ->
        ids.filter { it > frozenMaxId.getValue(d) }.sorted()
    }
    fun bandSize(d: Difficulty) = frozen.getValue(d).size + appended.getValue(d).size
    fun quoteIdFor(d: Difficulty, level: Int): Int {          // level is 1-based
        val f = frozen.getValue(d); val a = appended.getValue(d)
        val idx = (level - 1) % (f.size + a.size)
        return if (idx < f.size) f[idx] else a[idx - f.size]
    }
    fun cycleFor(d: Difficulty, level: Int) = (level - 1) / bandSize(d)
    companion object { val BAND_SEED = mapOf(Difficulty.EASY to 11L, Difficulty.MEDIUM to 22L, Difficulty.HARD to 33L, Difficulty.EXTREME to 44L) }
}
```

`splitmix64(x: Long): Long` is the standard SplitMix64 finalizer. `frozenMaxId_<BAND>` is written to DataStore on first run only (= max id in the bundled corpus for that band) and is **never** updated on app upgrade, so a bigger corpus only extends the sequence with appended ids in ascending id order. Level numbers never shift.

### 4.5 Puzzle session (`PuzzleSession.kt`) — pure Kotlin, no Android deps

```kotlin
data class PuzzleState(
    val plain: String,                // uppercase original with punctuation
    val difficulty: Difficulty,
    val key: IntArray,                // plain index -> cipher number (1..26)
    val mapping: Map<Int, Char>,      // cipher number -> guessed plainChar (player entries)
    val revealed: Set<Char>,          // plain letters pre-revealed (locked)
    val livesLeft: Int,
    val hintsLeft: Int,               // free hints remaining
    val adHintsUsed: Int,
    val mistakes: Int,
    val wrongCipherNums: Set<Int>,    // cipher numbers currently marked wrong (IMMEDIATE / after CHECK)
    val selectedCipherNum: Int?,
    val elapsedMs: Long,
    val status: PuzzleStatus          // IN_PROGRESS, SOLVED, FAILED
) {
    fun tokens(): List<CipherToken> = Cipher.encrypt(plain, key)   // ciphertext, derived
}
enum class PuzzleStatus { IN_PROGRESS, SOLVED, FAILED }
```

The ciphertext is not stored — it is `Cipher.encrypt(plain, key)`. The player taps a tile (or a frequency bar) and types the plaintext letter they think its number stands for; the A–Z keyboard is unchanged. The number is always shown as a caption under the tile; the tile box holds only the guessed letter.

`autofill` (Settings toggle, default on): assigning a number fills every tile of that number at once. Off: the player fills one tile at a time — `filledPositions` is the set of letter positions (0-based, left to right) already placed, `selectedPosition` the tile the next keystroke lands on. In manual mode the grid is complete only when every letter position is filled.

Rules ("cipher number" = a value 1..26; the player types a plaintext letter for it):
- `select(cipherNum: Int)` / `enter(plainGuess: Char)`: `enter` requires `selectedCipherNum != null` and that number not locked (its correct letter is revealed). If `plainGuess` is already mapped from a different cipher number, that other mapping is cleared first (one plain letter can only back one number). Sets `mapping[selected] = plainGuess`, then:
  - `IMMEDIATE`: if the number's correct letter `!= plainGuess` → `mistakes++`, `livesLeft--`, and the guess is **not placed** — the cell stays empty, flashes red and shakes (`lastWrongNum = target`), selection stays on it so the player can retry. Else the guess is placed and any `lastWrongNum` clears.
  - `ON_CHECK`: no evaluation.
  - `ON_COMPLETE`: no evaluation.
  - After any entry, if all cipher numbers are mapped: evaluate full grid. If all correct → `SOLVED`. If `ON_COMPLETE` and not correct → `mistakes++`, `livesLeft--`, do **not** reveal which numbers are wrong.
  - If `livesLeft == 0` → `FAILED`.
  - Auto-advance selection to the next unmapped cipher number (left-to-right in ciphertext).
- `clear()`: removes mapping for the selected cipher number; removes it from `wrongCipherNums`.
- `check()` (`ON_CHECK` only): compute wrong set among mapped numbers; if non-empty → `wrongCipherNums = set`, `mistakes++`, `livesLeft--`; if empty and grid complete → `SOLVED`.
- `hint()`: requires `hintsLeft > 0` or a rewarded ad grant; reveals `selectedCipherNum` (or first unmapped if none selected): sets mapping to the correct letter, adds that letter to `revealed` (locked), removes the number from `wrongCipherNums`, `hintsLeft--` or `adHintsUsed++`. Then run completion check.
- `tick(deltaMs)`: adds to `elapsedMs` only while `IN_PROGRESS` and the screen is resumed.
- Serializable to JSON for autosave (`InProgressEntity.stateJson`).

Star rating on solve (displayed on Results): 3 stars = 0 mistakes and 0 ad hints; 2 = ≤1 mistake and ≤1 ad hint; 1 otherwise.

---

## 5. Data Model

### 5.1 Room (`AppDatabase`, version 1)

```kotlin
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: Int, val text: String, val author: String,
    val source: String, val sourceUrl: String, val band: String   // Difficulty.name
)

@Entity(tableName = "progress", primaryKeys = ["difficulty", "level"])
data class ProgressEntity(
    val difficulty: String, val level: Int,
    val solvedAt: Long, val bestTimeMs: Long, val mistakes: Int, val stars: Int, val hintsUsed: Int
)

@Entity(tableName = "in_progress", primaryKeys = ["kind", "difficulty"])
data class InProgressEntity(   // kind = "LEVEL" or "DAILY"; one autosave slot per (kind, difficulty)
    val kind: String, val difficulty: String, val level: Int, val date: String?, val stateJson: String, val updatedAt: Long
)

@Entity(tableName = "daily_cache")
data class DailyCacheEntity(@PrimaryKey val date: String, val json: String, val fetchedAt: Long, val isFallback: Boolean)

@Entity(tableName = "daily_result", primaryKeys = ["date", "difficulty"])
data class DailyResultEntity(val date: String, val difficulty: String, val solvedAt: Long, val timeMs: Long, val mistakes: Int, val stars: Int)
```

DAOs:
- `QuoteDao`: `count()`, `getById(id)`, `idsForBand(band)`, `insertAll(list)`.
- `ProgressDao`: `get(difficulty, level)`, `upsert(entity)`, `highestSolved(difficulty): Int?`, `countSolved(difficulty)`, `statsForBand(difficulty): BandStats` (count, min bestTimeMs, avg bestTimeMs, sum stars).
- `InProgressDao`: `get(kind, difficulty)`, `upsert`, `delete(kind, difficulty)`.
- `DailyCacheDao`: `get(date)`, `upsert`, `deleteOlderThan(date)`.
- `DailyResultDao`: `get(date, difficulty)`, `upsert`, `allDatesSolvedAllFour(): List<String>` (for streak).

### 5.2 DataStore keys (`SettingsStore`)

`corpusLoadedVersion: Int`, `frozenMaxId_EASY..EXTREME: Int`, `removeAdsOwned: Boolean`, `soundEnabled: Boolean`, `hapticsEnabled: Boolean`, `themeMode: String (SYSTEM/LIGHT/DARK)`, `currentLevel_EASY..EXTREME: Int` (next unsolved, default 1), `lastDailyDateSeen: String`, `consentObtained: Boolean`.

### 5.3 Corpus load

On first launch (or when `assets/corpus.json` version > `corpusLoadedVersion`): parse `assets/corpus.json` (`{"version": 3, "quotes": [...]}`), `insertAll` with `OnConflictStrategy.IGNORE`, set `frozenMaxId_*` **only if unset**, set `corpusLoadedVersion`. Runs in `CorpusLoader` on `Dispatchers.IO` behind a splash state on Home.

---

## 6. Daily Sync

`DailyRepository.getToday(localDate: LocalDate): DailyPuzzles`:

1. If `daily_cache[localDate]` exists → return it.
2. Else fetch `https://raw.githubusercontent.com/<owner>/cryptogram-content/main/daily/<date>.json` (OkHttp, 5 s connect / 5 s read, no retries here).
3. Validate: `date` matches; four bands present; each `quoteId` exists in local `quotes` table and its `band` matches. On success → cache (`isFallback=false`), return.
4. On any failure → build fallback: for each band pick `quoteIdFor(band, level = hash(date) % bandSize + 1)` from the level index with `seed = hash(date, band)`, cache with `isFallback=true`, return. Fallback entries are re-fetched on the next `getToday` call (step 1 skips `isFallback=true` rows if online).
5. Prefetch: after step 3 succeeds, also fetch `date+1` and `date+2` into cache if missing (best-effort, ignore errors).
6. Prune cache rows older than `localDate - 7`.

Triggers: `Home` on `onResume` (via `LifecycleEventObserver`) calls `getToday`; `DailySyncWorker` (`PeriodicWorkRequest`, 6 h, `NetworkType.CONNECTED`) calls `getToday(today)` + prefetch. No exact-alarm at midnight.

Daily UI shows the four dailies for the device's local date; solved ones show stars/time; "Yesterday" tab lists the previous 6 dates from cache with their solved state (playable). Streak = consecutive local dates ending today or yesterday on which all four dailies were solved.

---

## 7. Ads & Billing

- `ConsentManager`: Google UMP `ConsentInformation.requestConsentInfoUpdate` on Home first composition; show form if required; only then `MobileAds.initialize`. Store `consentObtained`.
- `BannerAd` composable (`AdSize.BANNER` anchored bottom) rendered on Home, LevelSelect, Results **only when `removeAdsOwned == false`**. Never in `PlayScreen`.
- `RewardedHintAd`: preload one `RewardedAd` when `PlayScreen` opens; on "Get a hint (watch ad)" show it; on `onUserEarnedReward` call `session.hint(fromAd = true)`. Button hidden when `adHintsUsed >= 3` or ad not loaded. Available regardless of `removeAdsOwned`.
- `BillingManager`: product `remove_ads` (INAPP). On start: `queryPurchasesAsync` → set `removeAdsOwned`. Purchase flow from Settings and from a small "Remove ads" text button on Home. Acknowledge purchases. Handle `ITEM_ALREADY_OWNED` as owned.
- Ad unit IDs and the raw GitHub base URL live in `BuildConfig` fields set from `gradle.properties`; debug builds use Google test ad units.

---

## 8. Screens

Navigation routes (`NavGraph.kt`): `home`, `levels/{difficulty}`, `play/level/{difficulty}/{level}`, `play/daily/{date}/{difficulty}`, `results` (args via `SavedStateHandle` from Play), `daily`, `stats`, `settings`, `sources`.

**Home** — title, four difficulty cards (each shows "Level N" = next unsolved and solved count), "Daily" card (shows date + 4 small band chips solved/unsolved + streak), Stats and Settings icons, "Remove ads" text button (hidden if owned), banner at bottom.

**LevelSelect** — grid of level numbers for the chosen difficulty, paged 100 per page (`LazyVerticalGrid`, 5 columns), solved = filled with star count, in-progress = outlined with dot, unsolved = plain. Tapping any level opens it. "Continue" FAB → `currentLevel`. Banner at bottom.

**Play** — top bar: difficulty + level (or "Daily · Easy"), timer, lives as hearts (`livesLeft/lives`), hints counter. Body: `PuzzleGrid` — words wrapped as units; each letter is a cell with the cipher **number** zero-padded to two digits (`01`–`26`) below (small) and the guessed letter above (large); punctuation/space cells are non-interactive; revealed cells filled and locked; wrong cells tinted `errorContainer` with a red border and shake; the selected number highlighted in every occurrence. Bottom: `CipherKeyboard` A–Z in 3 rows, letters already used greyed but tappable (re-assigns), backspace, and a row with `Hint` (shows `hintsLeft` or "Watch ad" state), `Check` (Hard only). No system IME. Autosave to `in_progress` on every state change (debounced 300 ms). Back navigates out without losing state. On `SOLVED` → navigate to Results; on `FAILED` → dialog "Out of lives" with single action "Try again" → new session same level, `cycle+1`.

**Results** — quote in full with author and source (tappable → opens `sourceUrl`), time, mistakes, hints used, stars, "Next level" (or "Back to Daily"), "Share" (text: `Cryptogram Infinite · Hard 42 · 3:21 · ★★★`). Banner at bottom.

**Daily** — today's four cards + streak; "Previous days" list (6). Cards navigate to `play/daily/...`.

**Stats** — per difficulty: solved, best time, average time, total stars. Daily: current streak, best streak, dailies solved.

**Settings** — sound, haptics, theme, "Remove ads" purchase / "Purchased ✓", "Restore purchases", Sources link, privacy policy link, app version.

**Sources** — Wikiquote attribution + CC BY-SA 3.0 link, note that quotes are reproduced verbatim with attribution, link to content repo.

---

## 9. Build Sequence — Agent Briefs

Two-phase model: Brief 0 creates the skeleton; Briefs 1–9 are applied strictly in order. Each brief names every file it creates or modifies. Database version bumps are explicit. No open questions remain in any brief.

### Brief 0 — Foundation

Create project `CryptogramInfinite`, package `dev.milan.cryptogram`, single module `:app`, Kotlin 2.0.x, AGP 8.6+, compileSdk 35, minSdk 26, targetSdk 35, Compose BOM 2024.09+, Material 3.

Dependencies: `androidx.room:room-runtime/ktx` + KSP, `androidx.datastore:datastore-preferences`, `androidx.work:work-runtime-ktx`, `androidx.navigation:navigation-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `org.jetbrains.kotlinx:kotlinx-serialization-json`, `com.squareup.okhttp3:okhttp`, `com.google.android.gms:play-services-ads`, `com.google.android.ump:user-messaging-platform`, `com.android.billingclient:billing-ktx`.

Files:
- `CryptogramApp.kt` — `Application`, holds `lateinit var container: AppContainer`, created in `onCreate`.
- `di/AppContainer.kt` — lazy `database: AppDatabase`, `settingsStore: SettingsStore`, `quoteRepository`, `progressRepository`, `dailyRepository`, `adManager`, `billingManager` (repositories stubbed until their briefs).
- `engine/Difficulty.kt` — exactly as §4.1.
- `data/db/AppDatabase.kt` version 1 with all five entities from §5.1 and all DAOs from §5.1 (full method signatures, `suspend` for one-shots, `Flow` for `countSolved`, `highestSolved`, `allDatesSolvedAllFour`).
- `data/prefs/SettingsStore.kt` — all keys from §5.2 as `Flow<T>` getters + `suspend set` functions.
- `ui/theme/` — Material 3 theme, dynamic color off, two color schemes, font: default; `LocalCellSize` composition local.
- `ui/navigation/NavGraph.kt` — all routes from §8 registered with placeholder composables showing the route name.
- `MainActivity.kt` — `enableEdgeToEdge`, sets `NavGraph`.
- `AndroidManifest.xml` — `INTERNET`, `com.google.android.gms.ads.APPLICATION_ID` meta-data from `BuildConfig`.
- `app/build.gradle.kts` — `buildConfigField`s: `ADMOB_APP_ID`, `BANNER_UNIT_ID`, `REWARDED_UNIT_ID`, `CONTENT_BASE_URL` (release from `gradle.properties`, debug = Google test IDs).
- `assets/corpus.json` — placeholder `{"version":1,"quotes":[]}` (replaced by content repo build script).

### Brief 1 — Corpus load + engine

Files: `data/corpus/CorpusLoader.kt`, `data/corpus/QuoteRepository.kt`, `engine/Cipher.kt`, `engine/RevealPolicy.kt`, `engine/LevelIndex.kt`, `engine/PuzzleSession.kt`, `engine/PuzzleState.kt`, unit tests `engine/*Test.kt`.

- `CorpusLoader.load()` exactly as §5.3; called from `HomeViewModel.init` (Brief 2) — until done, Home shows `HomeUiState.Loading`.
- `QuoteRepository`: `suspend fun byId(id: Int): QuoteEntity?`, `suspend fun idsByBand(): Map<Difficulty, List<Int>>`, `suspend fun levelIndex(): LevelIndex` (cached in memory after first build).
- `Cipher`, `RevealPolicy`, `LevelIndex` exactly as §4.2–4.4.
- `PuzzleSession(plain: String, difficulty: Difficulty, seed: Long)` exposes `state: StateFlow<PuzzleState>` and functions `select(cipherChar)`, `enter(plainChar)`, `clear()`, `check()`, `hint(fromAd: Boolean)`, `tick(deltaMs)`, `toJson()`, `companion fromJson(json)`; behaviour exactly as §4.5.
- Tests: key is a derangement for 1,000 seeds; reveal count = floor(distinct × ratio); level mapping stable when appended ids are added; IMMEDIATE loses a life on wrong entry; ON_COMPLETE loses a life on wrong full grid without exposing wrong chars; ON_CHECK loses a life only on `check()` with ≥1 wrong; hint on the last unmapped letter solves the puzzle.

### Brief 2 — Home + LevelSelect + Play (level mode)

Files: `ui/home/HomeScreen.kt`, `HomeViewModel.kt`, `ui/levels/LevelSelectScreen.kt`, `LevelSelectViewModel.kt`, `ui/play/PlayScreen.kt`, `PlayViewModel.kt`, `PuzzleGrid.kt`, `CipherKeyboard.kt`, `data/progress/ProgressRepository.kt`; modify `NavGraph.kt`.

- `HomeUiState` sealed: `Loading`, `Ready(bands: List<BandSummary>, daily: DailySummary?, streak: Int, removeAdsOwned: Boolean)`; `BandSummary(difficulty, nextLevel: Int, solvedCount: Int)`. `daily` is `null` until Brief 4.
- `LevelSelectUiState(difficulty, page: Int, levels: List<LevelCell>)`; `LevelCell(level, status: UNSOLVED/IN_PROGRESS/SOLVED, stars)`; page size 100; total pages = `ceil(max(bandSize, highestSolved + 100) / 100)` so the grid is always at least one page past progress.
- `PlayViewModel(difficulty, level)`: loads `in_progress` slot for `(LEVEL, difficulty)` if `level` matches, else builds a new `PuzzleSession` with `quoteId = levelIndex.quoteIdFor(difficulty, level)`, `seed` per §4.2 with `cycle = levelIndex.cycleFor(...)`, restartCount = 0. Exposes `PlayUiState(session state, quoteAuthor, difficulty, level, canCheck, canHint, canAdHint, adHintLoaded)`. Timer via `viewModelScope` loop 1 s while resumed. Autosave debounced 300 ms to `InProgressDao`. On `SOLVED`: `ProgressDao.upsert` (bestTime = min with existing), delete in-progress slot, set `currentLevel_<BAND> = max(current, level+1)`, navigate `results`. On `FAILED`: show dialog; "Try again" → new session with `seed(cycle = cycleFor + 1 + restartCount)`, `restartCount++`.
- `PuzzleGrid` and `CipherKeyboard` exactly as §8 Play. Cell size adapts so the longest word fits the width; words never break across lines.
- Results screen in this brief is a stub that reads args and shows time/stars with "Next level" (full version in Brief 3).

### Brief 3 — Results + Stats

Files: `ui/results/ResultsScreen.kt`, `ui/stats/StatsScreen.kt`, `StatsViewModel.kt`; modify `PlayViewModel.kt` (pass `ResultArgs` via `savedStateHandle`), `NavGraph.kt`.

- `ResultArgs(kind: LEVEL/DAILY, difficulty, level: Int?, date: String?, quoteId, timeMs, mistakes, hintsUsed, stars)` — `@Serializable`, encoded as a route argument string.
- Results exactly as §8. "Next level" navigates `play/level/{difficulty}/{level+1}` with `popUpTo(home)`. Share uses `Intent.ACTION_SEND` text.
- `StatsUiState(bands: List<BandStats>, dailyStreak, bestStreak, dailiesSolved)`; `BandStats(difficulty, solved, bestTimeMs?, avgTimeMs?, stars)`. Best streak stored in DataStore key `bestStreak: Int`, updated whenever current streak is computed (Brief 4).

### Brief 4 — Daily

Files: `data/daily/DailyRemoteSource.kt`, `DailyRepository.kt`, `DailySyncWorker.kt`, `DailyModels.kt` (`@Serializable DailyFile`, `DailyPick(quoteId, seed)`), `ui/daily/DailyScreen.kt`, `DailyViewModel.kt`; modify `HomeViewModel.kt` (populate `daily`, streak), `PlayViewModel.kt` (daily mode: slot `(DAILY, difficulty)` with `date`; on solve write `DailyResultEntity`, no `progress` row, no `currentLevel` change), `CryptogramApp.kt` (enqueue `DailySyncWorker` unique periodic 6 h, `KEEP`), `NavGraph.kt`.

- `DailyRepository.getToday(LocalDate)` exactly as §6 including fallback and prefetch. `hash(date)` = `splitmix64(date.toEpochDay())`, `hash(date, band)` = `splitmix64(date.toEpochDay() * 7 + band.ordinal)`.
- Streak computed from `DailyResultDao.allDatesSolvedAllFour()` per §6; update `bestStreak`.
- Daily screen exactly as §8. A fallback daily is visually identical to a fetched one.

### Brief 5 — Ads (consent, banner, rewarded)

Files: `ads/ConsentManager.kt`, `ads/AdManager.kt`, `ads/BannerAd.kt`, `ads/RewardedHintAd.kt`; modify `HomeScreen.kt`, `LevelSelectScreen.kt`, `ResultsScreen.kt` (banner slot), `PlayScreen.kt`/`PlayViewModel.kt` (ad-hint button + `adHintLoaded`), `MainActivity.kt` (consent on first composition).

- Behaviour exactly as §7. Banner composable returns an empty box (0 dp) when `removeAdsOwned` or ads not initialised, so layouts don't reserve space.
- `RewardedHintAd.load()` on Play open; `show(activity, onReward)`; reload after show. Button label "Hint (watch ad)" when `hintsLeft == 0 && adHintsUsed < 3 && loaded`.

### Brief 6 — Billing + Settings + Sources

Files: `billing/BillingManager.kt`, `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `ui/sources/SourcesScreen.kt`; modify `HomeScreen.kt` ("Remove ads" text button), `AppContainer.kt`, `NavGraph.kt`.

- `BillingManager` exactly as §7; `removeAdsOwned: StateFlow<Boolean>` mirrored into DataStore.
- Settings and Sources exactly as §8. Sound/haptics wired into `PlayScreen` (tap tick, wrong-letter buzz, solve chime — three short bundled `.ogg` files under `res/raw`).

### Brief 7 — Polish

Modify `PlayScreen.kt`, `PuzzleGrid.kt`, `theme/`: solve animation (cells flip to `primaryContainer` sequentially, 20 ms stagger), wrong-letter shake (8 dp, 300 ms), landscape support for Play (keyboard right of grid on width ≥ 600 dp), TalkBack content descriptions on cells and keys, font scaling up to 1.3× without overflow (grid shrinks cell size).

### Brief 8 — Content repo

Separate repository per §3: `validate.py`, `publish_daily.py`, `build_assets.py`, `profanity.txt`, both workflows, `curation/authors.txt`, `corpus/min_app_corpus_id.txt`. `validate.yml` runs on every push/PR; `daily.yml` cron `0 3 * * *` with `contents: write` permission, commits as `github-actions[bot]`. `build_assets.py` writes `{"version": <git commit count>, "quotes": [...]}`.

### Brief 9 — Release

- `res/xml/data_extraction_rules.xml`: exclude `in_progress` from cloud backup (it's fine to include everything else).
- Privacy policy page in `cryptogram-content` served via GitHub Pages; URL in Settings and Play Console.
- Play Console: Data Safety form — "Advertising ID collected by third-party SDK (AdMob)", no account data. Content rating questionnaire. App category Games > Word.
- Release signing via Play App Signing; `minifyEnabled true`, keep rules for Room, kotlinx.serialization, Billing.
- Internal testing track first with corpus v1 (2,000 rows) and `min_app_corpus_id.txt` = max id of that corpus.

---

## 10. Play Compliance & Launch Checklist

- [ ] UMP consent flow verified in EEA test mode; ads never initialise before consent.
- [ ] No ad on Play screen in any state; no interstitial anywhere (grep for `InterstitialAd` = 0 hits).
- [ ] Sources screen reachable from Settings; Wikiquote CC BY-SA 3.0 credited.
- [ ] Corpus: ≥2,000 rows, ≥300 per band, `validate.py` green, ≤10 rows per author.
- [ ] `daily/` contains today+1 and today+2 before release.
- [ ] Fallback path tested: airplane mode → Daily still shows four playable puzzles.
- [ ] Reinstall test: `remove_ads` restored via `queryPurchasesAsync` on first launch.
- [ ] Level stability test: install corpus v1, solve Hard 1–5, upgrade to corpus v2 with appended rows, confirm Hard 1–5 map to the same quotes.
- [ ] Zero-lives test on each tier: Easy/Medium lose on 5th/4th wrong letter, Hard on 3rd failed Check, Extreme on 3rd wrong full grid; restart yields a different cipher key for the same quote.

---

## 11. Deferred (not in v1)

- Extreme variants (no word breaks, timed); Play Games sign-in, cloud save, daily leaderboard; downloadable content packs; LLM-assisted weekly daily pre-selection; iOS.

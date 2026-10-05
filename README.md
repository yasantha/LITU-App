# Life in the UK Test Prep

Offline-first Android study app for the Life in the UK Test, built to the
[Developer and Design Specification v1.1](docs/LITU-App-Developer-and-Design-Spec_v1.1.pdf).
The question bank ships inside the app as a read-only SQLite database, progress stays on the phone,
and Firebase runs on the free Spark plan for sign-in, optional backup, flags and crash reports.

Developed by Yasantha Hettiarachchi.

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore · WorkManager · Media3 ·
Firebase (Auth, Firestore, Remote Config, Crashlytics, Analytics, App Check) · RevenueCat · AdMob.

## Getting started

Requirements: Android Studio with SDK 37, JDK 17 or 21, Python 3.12 for the content pipeline,
Node 22 for the Firestore rules tests.

```sh
./gradlew assembleDevDebug        # debug build, installs as com.myday.litu.dev
./gradlew installDevDebug
```

The app builds and runs with no accounts set up. Without the optional configuration below,
backup, Remote Config, Crashlytics and Analytics are off, and debug builds use a local test
store on the paywall (marked as such on screen). Study features never need a network.

> detekt 1.23 does not run on JDK 25. Use JDK 17 or 21, as CI does.

### Optional configuration

| What | Where | Effect |
|---|---|---|
| Firebase `litu-dev` | `app/src/dev/google-services.json` | Turns on Firebase for dev builds |
| Firebase `litu-prod` | `app/src/prod/google-services.json` | Turns on Firebase for prod builds |
| RevenueCat public SDK key | `LITU_REVENUECAT_KEY` in `~/.gradle/gradle.properties` or the environment | Real Google Play billing, entitlement `pro` |
| Upload key | `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) or `LITU_UPLOAD_*` env vars | Signed release builds |
| AdMob | `LITU_ADMOB_APP_ID`, `LITU_ADMOB_BANNER_ID`, `LITU_ADMOB_INTERSTITIAL_ID` (gradle.properties or environment) | Real ads for free users in release builds; debug builds always show Google test ads |
| Play service account | `play-service-account.json` | `publishProdReleaseBundle` uploads to internal testing |

None of these files are committed (see `.gitignore`). Firebase setup from spec 12: create the
Firestore database in Standard edition, location `europe-west2`, deploy the rules with
`firebase deploy --only firestore:rules`, register the debug, release and Play App Signing
SHA-1/SHA-256 fingerprints for Google sign-in, and enforce App Check once production is live.

## Project layout

```
app/                 Application, MainActivity, navigation, reminders, launcher icon
core/model           Pure Kotlin models
core/domain          Algorithms and use cases (spec 9): SM-2, readiness, mock builder, streaks, sync merge
core/content         content.db (Room, read-only) and ContentRepository
core/progress        user.db (Room) and settings (DataStore)
core/sync            Anonymous auth, Google linking, Firestore backup, sync worker
core/billing         RevenueCat entitlement "pro"
core/config          Remote Config keys (spec 12.5)
core/audio           Read-aloud from the audio pack, TextToSpeech fallback
core/analytics       Funnel events (spec 12.6)
core/ads             AdMob banner and interstitial for free users, Google consent (UMP)
core/designsystem    Tokens (spec 19), themes, fonts, logo, components (spec 23.1)
feature/*            onboarding, home, practice, review, mock, notes, progress, timer, paywall, settings
audio_pack/          Fast-follow Play Asset Delivery pack
benchmark/           Macrobenchmark and Baseline Profile generator
content/             Question pipeline (see content/README.md)
firebase/            Firestore rules and emulator tests
```

Feature modules depend only on core modules. Screen IDs in code comments (S01–S20, S05b…)
match spec section 22.

## Testing

| Command | Covers |
|---|---|
| `./gradlew :core:domain:test` | Scheduler, readiness, mock builder, streaks, sync merge, use cases |
| `./gradlew testDebugUnitTest :app:testDevDebugUnitTest` | DAOs, user.db migrations, bundled content.db check |
| `./gradlew verifyRoborazziDebug` | Component screenshots in light, dark and 200% font (`recordRoborazziDebug` to update) |
| `./gradlew lintDevDebug detekt` | Lint and static analysis |
| `cd firebase && npm ci && npm test` | Firestore security rules on the emulator |
| `python -m unittest discover content/tests` | Content validator |
| `./gradlew :benchmark:connectedDevBenchmarkReleaseAndroidTest` | Cold start and frame timing on a device |
| `./gradlew :app:generateProdReleaseBaselineProfile` | Regenerates the Baseline Profile on a device |

CI (`.github/workflows/ci.yml`) runs all but the device benchmarks on every pull request.

## Content

The bundled `content.db` holds 161 original questions across all five handbook chapters and 23
section notes. The draft, build and audio steps are in [content/README.md](content/README.md).

## Releasing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`; rebuild content if it changed.
2. Push a `v*` tag. `release.yml` builds the signed `prodRelease` bundle with the audio pack and
   uploads it to Play internal testing (secrets listed at the top of the workflow).
3. Follow the release checklist in spec section 15: smoke test, closed testing, staged rollout.
4. To pull a wrong question immediately, add its ID to the Remote Config key `hidden_question_ids`.

## Differences from the spec

- **Firestore size limit.** `request.resource.size()` in spec 12.4 does not count bytes, so it
  never blocks large writes (the rules tests showed this). The rules bound the document's shape
  instead: known fields only, at most 3,000 review entries, 30 mocks and 120 days. Owners can
  delete their document so in-app deletion works.
- **Offline reports.** `user.db` has a `pending_report` table so S11b reports queue offline and
  send with the next sync.
- **Audio clips.** Options are shuffled on screen, so `tts.py` voices the stem, each option, the
  option letters and the explanation as separate clips; the player queues them in display order.
- **Ads in the free version.** The spec says no ads; the free version now shows AdMob ads. A
  banner sits above the bottom tabs, and an interstitial may appear when leaving a finished
  session or mock results, at most once every 5 minutes. Never during questions, the timed mock,
  onboarding or the paywall. Google's consent form runs before any ad request (UK GDPR) and
  Settings offers "Ad privacy choices". Pro removes all ads. Remote Config `ads_enabled` turns
  ads off without an update. Release builds show no ads until real ad unit IDs are set.
- **Free tier.** The sample, the review queue and Mock 1 are free; topic, mixed and timer
  practice, notes and further mocks open the paywall.
- **Audio.** Read-aloud uses the phone's built-in text-to-speech with an en-GB voice, so no
  paid voice service is needed. The `audio_pack` module and `content/pipeline/tts.py` stay in
  place: if recorded clips are added later, the app plays them automatically instead.
- **App name.** "Life in the UK Test Prep" in the store and on the splash; "UK Test Prep" under
  the launcher icon, where longer names are cut off.

## Still to do before launch

- Final prices (spec section 25). The package name `com.myday.litu` is final.
- AdMob: create the app and two ad units, set the IDs above, add `app-ads.txt` on the developer
  website, declare ads and advertising ID in the Play Data safety form, and cover AdMob in the
  privacy policy.
- Legal pages: replace the placeholder URLs in `LegalLinks.kt` (privacy, terms, support,
  account deletion).
- Create the Firebase projects, RevenueCat app and Play Console listing, then add the
  configuration above.
- Run the benchmarks and a TalkBack pass on the low-end and mid-range reference phones.

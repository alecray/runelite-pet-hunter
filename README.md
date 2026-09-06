# Pet Hunter

A RuneLite plugin for OSRS pet hunters. It tracks which collection-log pets you've obtained and
helps you decide what to chase next.

## Features

- **Owned-pet sync from your account.** Open the in-game Collection Log to **Other → All Pets** and
  the plugin reads which of the ~68 pets you already have. A **Sync Pets** button is injected into
  the collection log header for an explicit on-demand sync. New pet drops are also picked up live
  from the collection-log chat message.
- **Ranked next-task suggestion.** "Suggest next pet" picks the best un-obtained target from your
  active filters and pins it, with the full ranked list visible below.
- **Filters.** Hide pets you already own, filter by **solo / group** activity, and filter by tag.
- **Custom tags.** Tag any pet with your own labels (e.g. `ironman`, `afk`, `weekend`) and filter
  by them. Tags and obtained state persist per RuneLite profile.

## Project structure

```
runelite-pet-hunter/
├── build.gradle                     # Gradle build; pulls net.runelite:client + Lombok
├── settings.gradle                  # Root project name (pet-hunter)
├── gradle.properties                # Gradle JVM args
├── runelite-plugin.properties       # Plugin Hub manifest (name, author, tags, entry class)
├── gradlew / gradlew.bat            # Gradle wrapper scripts
├── gradle/wrapper/                  # Wrapper jar + properties (Gradle 8.10)
├── LICENSE                          # BSD 2-Clause
├── README.md
│
└── src/
    ├── main/
    │   ├── java/com/alecray/pethunter/
    │   │   ├── PetHunterPlugin.java          # Entry point: wiring, events, sync-button injection, nav button
    │   │   ├── PetHunterConfig.java          # User settings (hide obtained, rank mode, auto-sync, notify…)
    │   │   ├── PetHunterConfigManager.java   # JSON persistence via RuneLite ConfigManager (per profile)
    │   │   ├── PetHunterPanel.java           # Sidebar UI: status, filters, current task, pet list
    │   │   ├── PetCard.java                  # One row in the list: icon, info, "Task"/"Tags" actions
    │   │   ├── PetDataManager.java           # Loads pets.json, merges obtained/tag state, change events
    │   │   ├── CollectionLogReader.java      # Reads the open collection-log page (opacity = obtained)
    │   │   ├── TaskService.java              # Filters + ranks pets, picks the suggested next target
    │   │   ├── PetFilter.java                # Active filter state (hide obtained / activity / tag)
    │   │   ├── RankMode.java                 # Easiest-first / rarest-first / dataset-order ranking
    │   │   ├── PetNames.java                 # Normalizes names into a stable match key
    │   │   └── data/
    │   │       ├── Pet.java                  # Pet model (name, source, type, rarity, tags, obtained)
    │   │       └── ActivityType.java         # SOLO / GROUP / BOTH + filter-match logic
    │   │
    │   └── resources/com/alecray/pethunter/
    │       └── pets.json                     # Dataset: all 68 collection-log pets + metadata
    │
    └── test/java/com/alecray/pethunter/
        └── PetHunterPluginTest.java          # main() that launches a dev RuneLite client with the plugin
```

### How the pieces fit together

```
                 Collection Log open (COLLECTION_DRAW_LIST)  ┐
                 "New item added…" chat message              │  events
                 Sync button (in-game header / side panel)   ┘
                                   │
                                   ▼
                        CollectionLogReader ──── reads obtained pets ───┐
                                                                        ▼
   pets.json ──► PetDataManager ◄──── persists/loads ──── PetHunterConfigManager ──► RuneLite config
                      │   ▲                                                            (per profile)
        change events │   │ queries
                      ▼   │
   PetHunterPanel ──► TaskService (filter + rank) ──► suggested next pet + ranked list
        │
        └─ PetCard rows (set task, edit tags, open wiki)

   PetHunterPlugin wires all of the above and owns the sidebar nav button + event subscriptions.
```

## Getting started (compile & test)

This repo was scaffolded but has **not yet been compiled** against the RuneLite client. Follow these
steps to build it, run it in a dev client, and verify the features end to end.

### 1. Prerequisites

- **JDK 11** (RuneLite requires Java 11+; it will not build on Java 8). Verify with `java -version`.
  - If your default JDK is older, point Gradle at a JDK 11 via `org.gradle.java.home` in
    `gradle.properties`, or select it as the Project SDK in IntelliJ.
- **IntelliJ IDEA** (Community is fine) with the **Lombok** plugin installed and
  *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation
  processing* turned on. This project uses Lombok (`@Data`, `@Value`, `@Slf4j`).
- Internet access on first build (Gradle downloads the RuneLite client from `repo.runelite.net`).

### 2. Compile

From the project root:

```sh
./gradlew build        # macOS/Linux
gradlew.bat build      # Windows
```

The first build pulls `net.runelite:client:latest.release`. Fix any compile errors before moving on
(see **Known things to verify** below for the spots most likely to need a tweak per RuneLite version).

> Opening the project in IntelliJ ("Open" → select the `build.gradle`) and letting it import the
> Gradle project is the easiest path — it resolves the RuneLite API so you get autocomplete and can
> jump to the API definitions referenced in the code.

### 3. Run in a dev RuneLite client

Run the `main` method in
[`PetHunterPluginTest`](src/test/java/com/alecray/pethunter/PetHunterPluginTest.java) from IntelliJ
(right-click → Run). It calls `ExternalPluginManager.loadBuiltin(PetHunterPlugin.class)` and launches
RuneLite with the plugin already loaded. Log in to an account to test against real data.

### 4. Verify the features

1. **Sync** — open the in-game **Collection Log → Other → All Pets**. The sidebar panel's
   `Obtained: X / 68` should update and obtained pets should be marked. Click the side-panel
   **Sync from collection log** button (and the injected **Sync Pets** header button) and confirm the
   console message.
2. **Filters** — toggle *Hide obtained*, switch *Activity* (Solo/Group), and pick a *Tag*; the list
   should filter accordingly.
3. **Suggestion** — click **Suggest next pet**; a sensible un-obtained pet should pin as the current
   task. Change *Rank by* and re-suggest.
4. **Tags** — click **Tags** on a card, add a comma-separated tag, and confirm it appears and becomes
   selectable in the *Tag* filter.
5. **Persistence** — restart the client and reopen the panel; obtained state, custom tags, and the
   current task should all survive (they are stored per RuneLite profile).
6. **Live drop** *(optional)* — if you obtain a pet, the "New item added to your collection log"
   message should mark it obtained and (if enabled) fire a notification.

### Known things to verify / likely tweak points

- **In-game header "Sync Pets" button position.** `PetHunterPlugin.addSyncButton()` injects the button
  into the collection-log title bar using a best-effort child index (`getWidget(group, 2)`). Widget
  layout varies between RuneLite versions, so if the button doesn't appear or is misplaced, use the
  RuneLite dev tools **Widget Inspector** to find the correct title component and adjust. This is
  wrapped in try/catch — a wrong index won't crash; the side-panel sync button always works.
- **Pet item IDs.** Names drive matching (normalized), so the plugin works even if an `itemId` in
  `pets.json` is `0`/unknown — only the panel icon is affected. Fill in any missing IDs for nicer
  icons. Newer pets (e.g. Bran, Dom, Yami, Soup, Moxi, Huberte) currently have `itemId: 0`.
- **`pets.json` coverage.** The dataset is plain data at
  [`src/main/resources/com/alecray/pethunter/pets.json`](src/main/resources/com/alecray/pethunter/pets.json)
  — edit sources, solo/group flags, rarities, and tags freely. A pet present in the collection log but
  missing from the dataset is simply ignored.

### Publishing to the Plugin Hub (later)

To submit to the RuneLite Plugin Hub, the repo must be public and pass the hub's checkstyle/CI. The
code follows RuneLite conventions; run the hub's verification template against it before opening the
PR. No third-party dependencies are used (Gson ships with RuneLite), so no Gradle
verification-metadata changes are required.

## Pet wheel (web/)

`web/pet-wheel.html` is a single self-contained page (no build step, no server) for randomly
picking which pet to hunt next and sizing an attempt "chunk" for one session, using the same
rarity/time data as `pet_ranker.py`.

The wheel's schema is "attempts", not "kills": every pet has an `attemptUnit` (`kill`, `game`,
`action`, `clue`, or `raid`) and a `rollsPerAttempt` (how many independent 1/rarity chances one
attempt buys — 1 for a plain boss kill, more for reward-roll minigames like Tempoross or Guardians
of the Rift). This is what lets skilling pets (actions), clue pets (clues), and raid/minigame pets
(games/raids) share the wheel with boss kills. Filter tabs above the hunt list (All / Bosses /
Skilling / Minigame) also gate which pets the wheel can land on; the choice persists per RSN.

Skilling pets (the 9 skill-training pets plus Herbi and Quetzin) have **editable** rate and
attempts/hour fields right in the hunt table — the OSRS Wiki gives a per-level-99 rate for one
"best method" per skill, but real playstyles vary, so click into the fields to enter your own and
use **Reset** to restore the wiki default. Each row's `methodNote` (hover the source cell, or see
`pets.json`) states which method and wiki page the default came from, and flags whether the
attempts/hour figure is wiki-stated or an estimate.

### Opening it

Double-click `web/pet-wheel.html`, or open it via File > Open in any modern browser. It works
entirely offline; only the optional live-fetch feature below makes a network request.

### Getting live KC / kill-rate data

- Enter your RSN in the "Player" box and click **Fetch from Wise Old Man**. This calls the public
  [Wise Old Man](https://wiseoldman.net) API directly from the page (its `/players/{name}` and
  `/efficiency/rates` endpoints send `Access-Control-Allow-Origin: *`, so a `file://` page can call
  them without a server in between) to pull your boss KC, clue count, community-average kills/hour,
  and per-skill levels, which replace the wheel's hardcoded per-attempt time estimate where
  available (skilling pets are the exception — see above, their rate is always user-editable, never
  WOM-driven; WOM only supplies their skill level as read-only info).
- Wise Old Man has **no concept of pet ownership** — it only knows KC/clues/levels. Every pet stays
  in the hunt list; uncheck the ones you already own.
- `soul_wars_zeal` and Barbarian Assault have no usable WOM metric (zeal is a points total, not a
  game count, and WOM tracks no Barbarian Assault metric at all — both confirmed live 2026-09-05),
  so Lil' creator and Pet penance queen always use their hardcoded rate.
- Optionally, also run `python pet_ranker.py --rsn "your name" --json snapshot.json` and load that
  file (see the "Advanced" section on the page) as a KC fallback for the handful of activities Wise
  Old Man doesn't track under a matching boss metric (Guardians of the Rift, Wintertodt, Zalcano,
  Tempoross). `pet_ranker.py` no longer queries WikiSync for ownership: that endpoint rejects
  third-party callers (`403 Please do not use WikiSync in your own projects`, confirmed live
  2026-09-05), so `--json` snapshots always report `ownership_known: false` now.

### What's persisted, and where

Everything lives in the browser's `localStorage` for that page, namespaced per RSN so multiple
accounts don't overwrite each other's data. Nothing is sent anywhere except the two Wise Old Man
requests above:

- Current RSN, and the (shared, not per-account) Wise Old Man kill-rate cache.
- Per RSN: cached Wise Old Man player KC/clues/levels, an optional loaded `pet_ranker.py` snapshot,
  hunt-list checkbox selections, per-pet boost multipliers, skilling-pet rate overrides, the active
  filter tab, the session-length setting, and the last 20 spins.

Use the **Export/Import** buttons in the "Backup" panel to save or restore all of this as one JSON
file (for example, before clearing browser data).

### Regenerating the embedded pet dataset

`web/pet-wheel.html` embeds a merged pet dataset (rarity, attemptUnit, rollsPerAttempt, time
estimate, wiki link, methodNote, Wise Old Man metric) built from `pets.json` and `pet_ranker.py`'s
`PETS` tuple. After editing either source, regenerate it with:

```
python web/build_pet_data.py
```

A pet is included if EITHER source supplies a timing estimate: a `pet_ranker.py` PETS row (fixed-
rate boss kills and the hiscore-trackable roll/raid pets), or `pets.json`'s own `attemptUnit` +
`attemptsPerHour` fields (skilling pets and the fixed-rate minigame/clue pets). Two pets are
excluded from the wheel permanently regardless of any rate data — see "Known gaps" below.

### Known gaps

- **Broav and Cat are permanently excluded from the wheel** (`web/build_pet_data.py`'s
  `EXCLUDED_PETS`): both are one-time quest rewards (While Guthix Sleeps / Gertrude's Cat), not
  repeatable content, so "expected hours to obtain" doesn't apply. `pets.json` still lists them —
  the RuneLite plugin's collection-log tracking is unaffected — only the wheel skips them.
- No live ownership source (see above) — ownership filtering exists in the code but is inert until
  a legitimate replacement for WikiSync exists; the hunt list is managed by hand for now.
- Wise Old Man reports `ehb: 0` (no kill-rate) for Guardians of the Rift, Wintertodt, Zalcano, and
  Tempoross; those pets keep their hardcoded per-attempt time estimate.
- Chambers of Xeric Challenge Mode, Tombs of Amascut Expert Mode, and Theatre of Blood Hard Mode all
  have a materially better pet rate than normal mode, but weren't added as separate wheel rows — the
  OSRS Wiki excerpts checked (2026-09-05) didn't give a clean, distinct denominator for any of the
  three. The wheel uses each raid's normal-mode (best-case) rate for all three; see each pet's
  `methodNote` for the citation.
- Several skilling pets' `attemptsPerHour` default is a rough, explicitly-flagged estimate rather
  than a wiki-stated actions/hour figure (the wiki usually gives xp/hour, not actions/hour) —
  Baby chinchompa, Beaver, Heron, Rift guardian, Rock golem, Rocky, Soup, and Tangleroot all carry
  `assumedRate: true` for this reason. Giant squirrel, Herbi, and Chompy chick have a wiki-stated
  rate and are not flagged. Since all skilling-pet rates are user-editable in the hunt table anyway,
  this mainly affects the out-of-the-box default before you tune it to your own pace.
- `pets.json` still carries `itemId: 0` for several newer pets (Bran, Dom, Huberte, Moxi, Nid,
  Yami); `web/build_pet_data.py` substitutes `pet_ranker.py`'s id where one exists. Soup's id
  (`31283`) was found directly on its wiki page and added to `pets.json`.
  `pets.json`'s previous Butch id (`28249`) conflicted with `pet_ranker.py`'s (`28248`); the OSRS
  Wiki's Butch item infobox confirms `28248` is correct, and `pets.json` has been corrected to match.

## How pet detection works

RuneLite has no API to read the whole collection log at once — data is only available while a
collection-log page is open. The plugin parses the open page's item widgets on the
`COLLECTION_DRAW_LIST` script event and treats fully-opaque item icons as obtained. Opening the
**All Pets** page captures all pets in one pass.

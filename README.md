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

## How pet detection works

RuneLite has no API to read the whole collection log at once — data is only available while a
collection-log page is open. The plugin parses the open page's item widgets on the
`COLLECTION_DRAW_LIST` script event and treats fully-opaque item icons as obtained. Opening the
**All Pets** page captures all pets in one pass.

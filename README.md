# ToA Applicant Gear

![ToA Applicant Gear icon](icon.png)

Inspect nearby players' worn equipment directly from the **Tombs of Amascut party interface**.

Open **Members** or **Applicants**, then hover a player's name or the magnifying glass beside it. An equipment card shows item icons and names in the familiar equipment-slot layout. You can inspect your own party or another party you are considering joining.

## Features

- Nine visible equipment slots: head, cape, neck, weapon, body, shield, legs, hands and feet.
- Current equipment for players loaded nearby in the same world; other players do not need the plugin.
- Updates when players change gear and explains when an appearance is unavailable.
- Supports sorting, scrolling, party changes and automatic interface refreshes.
- Empty member slots have no inspection icon or hover card.
- Equipment icons are centred by their visible artwork, with padding inside each slot.
- Preserves native party controls. Clicking inside an inspection card cannot activate a button obscured beneath it.

## Using the plugin

Enable **ToA Applicant Gear**, open a party at the ToA obelisk, and select Members or Applicants. Hover a name or magnifier; move onto the card to inspect individual slots and their full labels. Move away to close it.

**Hover player names** is enabled by default. Turn it off to open cards only from the magnifier column.

Equipment is read from a nearby player's public appearance, not from a remote equipment container. Ring, ammunition, inventory, gear switches, quantities and charges are unavailable and are not displayed. Local cosmetic/transmog plugins can affect appearance data. Extremely wide player names may wrap in the narrower native name label; the card keeps the full name.

## Installation status

This source is prepared for Plugin Hub submission. It has not yet been submitted or approved. Once accepted, install it through RuneLite's Plugin Hub. A local source checkout can be run with the development launcher below.

## Build and development

Use **Java 11** and the checked-in Gradle wrapper. No separate Gradle installation or IDE is required.

Linux / macOS:

```bash
bash ./gradlew clean build
bash ./gradlew run
```

CachyOS with jdk11-openjdk installed:

```bash
env JAVA_HOME=/usr/lib/jvm/java-11-openjdk bash ./gradlew run
```

Windows PowerShell:

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

The run task starts RuneLite in developer mode and registers the local plugin. Use RuneLite's [Jagex Account panel](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to sign in if needed. Keep the terminal open while using this launcher.

The default dependency is `latest.release`, as recommended by the Plugin Hub guide. To reproduce validation against the current Hub version:

```bash
bash ./gradlew clean build -PruneliteVersion=1.13.6
```

Automated tests cover equipment decoding, matching, both party tables, empty seats, ordering, clipping, equipment changes, icon centring, reload lifecycle, popup stability, close behaviour and click protection. The refresh fix was also confirmed in-game by user testing. GitHub Actions runs the Java 11 build and tests on pushes and pull requests.

## Publishing and review

See [SUBMISSION.md](SUBMISSION.md) for creating the public GitHub repository and the two-line Plugin Hub marker. [Review notes](docs/REVIEW_NOTES.md) explain the data source, interface changes and runtime dependencies.

The plugin uses Java and standard RuneLite APIs. It performs no network requests, sends no game actions, stores no equipment history and has no additional runtime dependencies. Only the normal plugin configuration is persisted by RuneLite.

## Licence

[BSD 2-Clause](LICENSE), copyright William Chamberlain. The Gradle wrapper retains its original Apache 2.0 notices and bundled licence.
# ToA-Gear-Inspector
# ToA-Gear-Inspector
# ToA-Gear-Inspector

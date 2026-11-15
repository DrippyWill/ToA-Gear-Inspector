# Review notes

## Data and scope

The plugin reads the native ToA party-details interface, group 774, and matches its displayed member/applicant names to players loaded in the local client. It uses PlayerComposition.getEquipmentId(KitType) for nine public appearance slots. It does not infer hidden slots, inspect inventory, query external services or share player data.

Members use a native child stride of 13 and Applicants a stride of 20; both place the player-name widget at offset 1. Empty/dash placeholders are excluded. Both tables work when viewing any party.

## Interface behaviour

A 26-pixel inspection column is reserved within the name column by adjusting only name-label widths. Native text, stat columns, sort data and accept/reject/kick/reorder actions retain their data and listeners. Original widths are restored when labels are no longer inspected or the plugin stops.

The overlay draws at the final frame pass and hides for native right-click menus. The popup shows current equipment and no longer opens for empty member seats. A mouse listener consumes clicks inside the card to protect obscured native buttons; it does not generate mouse events or game actions.

The refresh lifecycle listens to existing native scripts; it never calls them. Scripts 6615, 6729 and 6748 identify initialization, completion and the automatic timer. WidgetClosed can be followed by immediate reopening in the same client cycle. The plugin validates current table visibility before painting and preserves coherent geometry for that reload cycle, then resamples the settled layout. Equipment continues updating; actual disappearance, native interactions and logout clear normally.

## Runtime and packaging

Production code is Java 11 and depends only on RuneLite and its transitive dependencies. No additional runtime dependencies, network clients, filesystem access, reflection, subprocesses or diagnostic logging are used by the plugin. Tests use JUnit/Mockito; a test fixture uses reflection only to inject private fields and is not included in the standard Hub build.

runelite-plugin.properties selects build=standard, so Hub tooling replaces the local build files and compiles src/main with its standard template. Production code does not depend on custom Gradle-generated resources or local files. The local build adds tests and a developer launcher only.

## Validation

The functional and reload fix was confirmed by user testing in-game. Local automated checks cover both row layouts, placeholders, identity after sorting, viewport clipping, gear changes, despawning, sprite centring/padding, asynchronous images, canvas bounds, hover/click protection and actual plugin close/init/complete handlers across high-FPS frames. The cleaned source is also compiled using the current official Hub standard-build/settings templates with the Hub's current RuneLite version.

## References

- https://github.com/runelite/plugin-hub
- https://github.com/runelite/plugin-hub-tooling
- https://github.com/runelite/cs2-scripts
- https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/PlayerComposition.java
- https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/InterfaceID.java
